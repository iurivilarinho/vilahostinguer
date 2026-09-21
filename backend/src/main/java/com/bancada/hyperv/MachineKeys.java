package com.bancada.hyperv;

import com.bancada.service.SshService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Keys made by the panel for a new machine: the SSH host key the machine will present (its
 * fingerprint is pinned before the first boot) and the key the panel logs in with as root.
 */
public record MachineKeys(String hostPrivateKey, String hostPublicKey, String hostFingerprint, String panelPrivateKey,
                          String panelPublicKey) {

    private static final int RSA_BITS = 3072;

    public static MachineKeys generate(String machineName) {
        JSch jsch = new JSch();
        try {
            KeyPair host = KeyPair.genKeyPair(jsch, KeyPair.RSA, RSA_BITS);
            KeyPair panel = KeyPair.genKeyPair(jsch, KeyPair.RSA, RSA_BITS);
            try {
                return new MachineKeys(privateKey(host), publicKey(host, "root@" + machineName),
                    SshService.fingerprint(host.getPublicKeyBlob()), privateKey(panel), publicKey(panel, "bancada"));
            } finally {
                host.dispose();
                panel.dispose();
            }
        } catch (JSchException exception) {
            throw new IllegalStateException("Não foi possível gerar as chaves da máquina: " + exception.getMessage(), exception);
        }
    }

    /** The keys of an existing machine again (reinstall keeps its identity and the panel access). */
    public static MachineKeys restore(String hostPrivateKey, String panelPrivateKey, String machineName) {
        JSch jsch = new JSch();
        try {
            KeyPair host = KeyPair.load(jsch, hostPrivateKey.getBytes(StandardCharsets.US_ASCII), null);
            KeyPair panel = KeyPair.load(jsch, panelPrivateKey.getBytes(StandardCharsets.US_ASCII), null);
            try {
                return new MachineKeys(hostPrivateKey, publicKey(host, "root@" + machineName), SshService.fingerprint(host.getPublicKeyBlob()),
                    panelPrivateKey, publicKey(panel, "bancada"));
            } finally {
                host.dispose();
                panel.dispose();
            }
        } catch (JSchException exception) {
            throw new IllegalStateException("As chaves guardadas da máquina não abrem: " + exception.getMessage(), exception);
        }
    }

    private static String privateKey(KeyPair pair) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        pair.writePrivateKey(output);
        return output.toString(StandardCharsets.US_ASCII);
    }

    private static String publicKey(KeyPair pair, String comment) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        pair.writePublicKey(output, comment);
        return output.toString(StandardCharsets.US_ASCII).trim();
    }
}
