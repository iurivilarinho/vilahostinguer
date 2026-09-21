package com.bancada.service;

import com.bancada.enums.CredentialAuthType;
import com.bancada.exception.DeviceAuthenticationException;
import com.bancada.exception.DeviceConnectionException;
import com.bancada.models.Credential;
import com.bancada.models.Device;
import com.bancada.records.CommandResult;
import com.bancada.records.HostKeyProbe;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Every SSH conversation with the devices goes through here: host key pinning, logins, commands,
 * binary streams (backups) and interactive shells (terminal).
 *
 * <p>The host key of a registered device is pinned: a device that suddenly answers with another key
 * is refused instead of silently trusted. One session per device is kept open and reused, since a
 * phone over USB takes a noticeable time for every new key exchange.
 */
@Service
public class SshService {

    private static final Logger LOG = LoggerFactory.getLogger(SshService.class);

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int PROBE_TIMEOUT_MS = 4_000;
    private static final int CHANNEL_TIMEOUT_MS = 8_000;
    private static final int SERVER_ALIVE_INTERVAL_MS = 15_000;
    private static final int READ_BUFFER_SIZE = 8_192;
    private static final String PROFILE_PREAMBLE = ". /etc/profile >/dev/null 2>&1\n";

    private final SecretCipherService secretCipherService;
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> sessionSignatures = new ConcurrentHashMap<>();
    private final Map<Long, Object> sessionLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ssh-timeouts");
        thread.setDaemon(true);
        return thread;
    });

    public SshService(SecretCipherService secretCipherService) {
        this.secretCipherService = secretCipherService;
    }

    /** Reads the server host key without logging in. Returns null when the address does not speak SSH. */
    public HostKeyProbe probeHostKey(String host, int port) {
        CapturingHostKeyRepository repository = new CapturingHostKeyRepository(null);
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(repository);
        Session session = null;
        try {
            session = jsch.getSession("bancada-probe", host, port);
            session.setConfig("PreferredAuthentications", "publickey");
            session.setConfig("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa");
            session.connect(PROBE_TIMEOUT_MS);
        } catch (JSchException exception) {
            LOG.debug("Probe of {}:{} ended with {}", host, port, exception.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
        if (repository.capturedFingerprint == null) {
            return null;
        }
        return new HostKeyProbe(repository.capturedFingerprint, repository.capturedType);
    }

    /** Opens a fresh session with the device credential, just to prove it works. */
    public void verifyLogin(Device device) {
        invalidate(device.getId());
        session(device);
    }

    /** Runs a script (sent on stdin) and collects its output. */
    public CommandResult run(Device device, String script, List<String> args, boolean elevated, Duration timeout) {
        Session session = session(device);
        ChannelExec channel = null;
        ScheduledFuture<?> deadline = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(shellCommand(device, elevated, args));
            channel.setInputStream(new ByteArrayInputStream(wrap(script).getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            channel.setErrStream(errors);
            InputStream output = channel.getInputStream();
            channel.connect(CHANNEL_TIMEOUT_MS);
            ChannelExec running = channel;
            deadline = watchdog.schedule(running::disconnect, timeout.toMillis(), TimeUnit.MILLISECONDS);
            byte[] bytes = output.readAllBytes();
            int exitCode = waitExitStatus(channel);
            return new CommandResult(exitCode, new String(bytes, StandardCharsets.UTF_8), errors.toString(StandardCharsets.UTF_8));
        } catch (JSchException | IOException exception) {
            invalidate(device.getId());
            throw new DeviceConnectionException("Falha ao executar comando em " + device.getName() + ": " + exception.getMessage(), exception);
        } finally {
            if (deadline != null) {
                deadline.cancel(false);
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * Runs a script and forwards its merged stdout/stderr as text while it runs. The channel is handed
     * to {@code onChannel} so the caller can cancel by disconnecting it.
     */
    public int stream(Device device, String script, boolean elevated, Consumer<String> onOutput, Consumer<Channel> onChannel) {
        Session session = session(device);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(shellCommand(device, elevated, List.of()));
            String fullScript = wrap("exec 2>&1\n" + script);
            channel.setInputStream(new ByteArrayInputStream(fullScript.getBytes(StandardCharsets.UTF_8)));
            InputStream output = channel.getInputStream();
            channel.connect(CHANNEL_TIMEOUT_MS);
            onChannel.accept(channel);
            try (Reader reader = new InputStreamReader(output, StandardCharsets.UTF_8)) {
                char[] buffer = new char[READ_BUFFER_SIZE];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    onOutput.accept(new String(buffer, 0, read));
                }
            }
            return waitExitStatus(channel);
        } catch (JSchException | IOException exception) {
            throw new DeviceConnectionException("Falha ao executar em " + device.getName() + ": " + exception.getMessage(), exception);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /** Runs a script whose stdout is binary (a tar stream) and copies it into {@code sink}. */
    public CommandResult download(Device device, String script, boolean elevated, OutputStream sink, Consumer<Channel> onChannel) {
        Session session = session(device);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(shellCommand(device, elevated, List.of()));
            channel.setInputStream(new ByteArrayInputStream(wrap(script).getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            channel.setErrStream(errors);
            InputStream output = channel.getInputStream();
            channel.connect(CHANNEL_TIMEOUT_MS);
            onChannel.accept(channel);
            output.transferTo(sink);
            int exitCode = waitExitStatus(channel);
            return new CommandResult(exitCode, "", errors.toString(StandardCharsets.UTF_8));
        } catch (JSchException | IOException exception) {
            throw new DeviceConnectionException("Falha ao copiar dados de " + device.getName() + ": " + exception.getMessage(), exception);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /** Runs {@code command} feeding {@code source} as its stdin (restores, file uploads). */
    public CommandResult upload(Device device, String command, boolean elevated, InputStream source, Consumer<Channel> onChannel) {
        Session session = session(device);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(elevate(device, elevated) + "sh -c " + quote(command));
            channel.setInputStream(source, true);
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            channel.setErrStream(errors);
            InputStream output = channel.getInputStream();
            channel.connect(CHANNEL_TIMEOUT_MS);
            onChannel.accept(channel);
            byte[] bytes = output.readAllBytes();
            int exitCode = waitExitStatus(channel);
            return new CommandResult(exitCode, new String(bytes, StandardCharsets.UTF_8), errors.toString(StandardCharsets.UTF_8));
        } catch (JSchException | IOException exception) {
            throw new DeviceConnectionException("Falha ao enviar dados para " + device.getName() + ": " + exception.getMessage(), exception);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /** Opens an interactive login shell with a PTY, for the web terminal. */
    public ChannelShell openShell(Device device, int columns, int rows) {
        Session session = session(device);
        try {
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPtyType("xterm-256color", columns, rows, 0, 0);
            channel.setEnv("LANG", "C.UTF-8");
            return channel;
        } catch (JSchException exception) {
            invalidate(device.getId());
            throw new DeviceConnectionException("Não foi possível abrir o terminal: " + exception.getMessage(), exception);
        }
    }

    /** Runs {@code command} with a PTY (e.g. {@code docker exec -it}), for terminals into machines. */
    public ChannelExec openCommandPty(Device device, String command, boolean elevated, int columns, int rows) {
        Session session = session(device);
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(elevate(device, elevated) + command);
            channel.setPty(true);
            channel.setPtyType("xterm-256color", columns, rows, 0, 0);
            return channel;
        } catch (JSchException exception) {
            invalidate(device.getId());
            throw new DeviceConnectionException("Não foi possível abrir o terminal: " + exception.getMessage(), exception);
        }
    }

    public void invalidate(Long deviceId) {
        Session session = sessions.remove(deviceId);
        sessionSignatures.remove(deviceId);
        if (session != null) {
            session.disconnect();
        }
    }

    @PreDestroy
    public void closeAll() {
        sessions.keySet().forEach(this::invalidate);
        watchdog.shutdownNow();
    }

    /** Quotes a value for a POSIX shell: {@code it's} becomes {@code 'it'\''s'}. */
    public static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String fingerprint(byte[] key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key);
            return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public boolean runsAsRoot(Device device) {
        Credential credential = device.getCredential();
        return device.isRootAccess() || (credential != null && "root".equals(credential.getUsername()));
    }

    /**
     * The script travels on stdin ({@code sh -s}), so a command inside it that reads stdin would swallow
     * the rest of the script. The brace group is parsed whole before running and gets /dev/null instead.
     */
    private static String wrap(String script) {
        return PROFILE_PREAMBLE + "{\n" + script + "\n} </dev/null\n";
    }

    private String shellCommand(Device device, boolean elevated, List<String> args) {
        StringBuilder command = new StringBuilder(elevate(device, elevated)).append("sh -s");
        if (!args.isEmpty()) {
            command.append(" --");
            args.forEach(arg -> command.append(' ').append(quote(arg)));
        }
        return command.toString();
    }

    private String elevate(Device device, boolean elevated) {
        return elevated && !runsAsRoot(device) ? "sudo -n " : "";
    }

    private int waitExitStatus(Channel channel) {
        long limit = System.currentTimeMillis() + 5_000;
        while (!channel.isClosed() && System.currentTimeMillis() < limit) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return channel.getExitStatus();
    }

    private Session session(Device device) {
        Credential credential = device.getCredential();
        if (credential == null) {
            throw new DeviceAuthenticationException("Vincule uma credencial ao dispositivo " + device.getName() + ".");
        }
        String signature = device.getHost() + ":" + device.getPort() + "|" + credential.getId() + "|" + credential.getUpdatedAt()
            + "|" + device.getHostKeyFingerprint();
        Object lock = sessionLocks.computeIfAbsent(device.getId(), id -> new Object());
        synchronized (lock) {
            Session current = sessions.get(device.getId());
            if (current != null && current.isConnected() && signature.equals(sessionSignatures.get(device.getId()))) {
                return current;
            }
            invalidate(device.getId());
            Session created = connect(device, credential);
            sessions.put(device.getId(), created);
            sessionSignatures.put(device.getId(), signature);
            return created;
        }
    }

    private Session connect(Device device, Credential credential) {
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(new CapturingHostKeyRepository(device.getHostKeyFingerprint()));
        String secret = secretCipherService.decrypt(credential.getEncryptedSecret());
        try {
            if (credential.getAuthType() == CredentialAuthType.PRIVATE_KEY) {
                String passphrase = secretCipherService.decrypt(credential.getEncryptedPassphrase());
                jsch.addIdentity("bancada-" + credential.getId(), secret.getBytes(StandardCharsets.UTF_8), null,
                    passphrase == null ? null : passphrase.getBytes(StandardCharsets.UTF_8));
            }
            Session session = jsch.getSession(credential.getUsername(), device.getHost(), device.getPort());
            if (credential.getAuthType() == CredentialAuthType.PASSWORD) {
                session.setPassword(secret);
                session.setUserInfo(new PasswordUserInfo(secret));
            }
            session.setConfig("StrictHostKeyChecking", "yes");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa");
            session.setConfig("PubkeyAcceptedAlgorithms", session.getConfig("PubkeyAcceptedAlgorithms") + ",ssh-rsa");
            session.setServerAliveInterval(SERVER_ALIVE_INTERVAL_MS);
            session.setServerAliveCountMax(3);
            session.connect(CONNECT_TIMEOUT_MS);
            return session;
        } catch (JSchException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage();
            if (message.contains("Auth fail") || message.contains("Auth cancel") || message.contains("USERAUTH fail")) {
                throw new DeviceAuthenticationException("O dispositivo " + device.getName()
                    + " recusou a credencial \"" + credential.getName() + "\".", exception);
            }
            if (message.contains("HostKey has been changed") || message.contains("reject HostKey")) {
                throw new DeviceConnectionException("A chave do servidor SSH de " + device.getName()
                    + " mudou. Se o sistema foi reinstalado, remova e cadastre o dispositivo de novo.", exception);
            }
            throw new DeviceConnectionException("Não foi possível conectar em " + device.getHost() + ":" + device.getPort()
                + " (" + message + ").", exception);
        }
    }

    /**
     * Pins the expected host key, or records the offered one when nothing is expected yet (probes).
     * Private helper: it only exists to plug into JSch.
     */
    private static final class CapturingHostKeyRepository implements HostKeyRepository {

        private final String expectedFingerprint;
        private String capturedFingerprint;
        private String capturedType;

        private CapturingHostKeyRepository(String expectedFingerprint) {
            this.expectedFingerprint = expectedFingerprint;
        }

        @Override
        public int check(String host, byte[] key) {
            capturedFingerprint = fingerprint(key);
            capturedType = keyType(key);
            if (expectedFingerprint == null || expectedFingerprint.equals(capturedFingerprint)) {
                return OK;
            }
            return CHANGED;
        }

        private static String keyType(byte[] key) {
            if (key.length < 4) {
                return null;
            }
            int length = ByteBuffer.wrap(key, 0, 4).getInt();
            if (length <= 0 || length > key.length - 4) {
                return null;
            }
            return new String(key, 4, length, StandardCharsets.US_ASCII);
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {
        }

        @Override
        public void remove(String host, String type) {
        }

        @Override
        public void remove(String host, String type, byte[] key) {
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return "bancada";
        }

        @Override
        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return new HostKey[0];
        }
    }

    /** Answers password and keyboard-interactive prompts with the stored password (dropbear uses both). */
    private static final class PasswordUserInfo implements UserInfo, UIKeyboardInteractive {

        private final String password;

        private PasswordUserInfo(String password) {
            this.password = password;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String message) {
            return true;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptYesNo(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            String[] answers = new String[prompt.length];
            Arrays.fill(answers, password);
            return answers;
        }
    }
}
