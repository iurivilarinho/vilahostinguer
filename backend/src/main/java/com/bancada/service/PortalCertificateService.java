package com.bancada.service;

import com.bancada.enums.CertificateStatus;
import com.bancada.models.PortalSettings;
import com.bancada.records.RoutesChangedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * HTTPS certificate of the customer panel, from Let's Encrypt with the HTTP-01 challenge: the
 * challenge answer is served by the panel itself under /.well-known/acme-challenge, so the panel
 * name must reach this PC on port 80. The gateway ends TLS with the certificate for that name and
 * the panel sees plain HTTP with X-Forwarded-Proto: https. Renewed 30 days before it expires.
 */
@Service
public class PortalCertificateService {

    private static final Logger LOG = LoggerFactory.getLogger(PortalCertificateService.class);
    private static final String LETS_ENCRYPT = "acme://letsencrypt.org";
    private static final Duration ACME_WAIT = Duration.ofSeconds(90);
    private static final Duration RENEW_BEFORE = Duration.ofDays(30);
    private static final int KEY_SIZE = 2048;
    private static final char[] KEYSTORE_PASSWORD = "bancada".toCharArray();

    private final PortalSettingsService portalSettingsService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Path directory;
    private final Map<String, String> challenges = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "portal-certificate");
        thread.setDaemon(true);
        return thread;
    });
    private volatile SSLContext sslContext;
    private volatile String contextHostname;

    public PortalCertificateService(PortalSettingsService portalSettingsService, ApplicationEventPublisher applicationEventPublisher,
                                    @Value("${bancada.data-dir}") String dataDir) {
        this.portalSettingsService = portalSettingsService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.directory = Paths.get(dataDir, "portal-tls");
    }

    /** Loads the certificate issued before, if any. */
    @PostConstruct
    public void load() {
        PortalSettings settings = portalSettingsService.get();
        Path chain = directory.resolve("chain.pem");
        Path domainKey = directory.resolve("domain.key");
        if (settings.getCertificateStatus() == CertificateStatus.ACTIVE && Files.exists(chain) && Files.exists(domainKey)) {
            try (Reader keyReader = Files.newBufferedReader(domainKey); InputStream chainInput = Files.newInputStream(chain)) {
                sslContext = buildContext(KeyPairUtils.readKeyPair(keyReader), readChain(chainInput));
                contextHostname = settings.getCertificateHostname();
            } catch (IOException | GeneralSecurityException exception) {
                LOG.warn("Portal certificate could not be loaded: {}", exception.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /** Starts the issue in the background; the result lands in the panel settings. */
    public void requestIssue() {
        PortalSettings settings = portalSettingsService.get();
        if (!settings.isPublished()) {
            throw new IllegalStateException("Publique o painel com um endereço antes de pedir o certificado.");
        }
        if (settings.getCertificateStatus() == CertificateStatus.ISSUING) {
            throw new IllegalStateException("Já há uma emissão em andamento.");
        }
        String hostname = settings.getHostname();
        portalSettingsService.recordCertificate(CertificateStatus.ISSUING, "Pedindo o certificado de " + hostname + " ao Let's Encrypt...",
            null, null);
        executor.execute(() -> issue(hostname, settings.getAcmeEmail()));
    }

    /** Answer to a challenge Let's Encrypt fetches over HTTP. */
    public Optional<String> challengeResponse(String token) {
        return Optional.ofNullable(challenges.get(token));
    }

    /** TLS context for the panel name, when a certificate for it exists. */
    public SSLContext contextFor(String hostname) {
        return hostname != null && hostname.equals(contextHostname) ? sslContext : null;
    }

    public boolean isActiveFor(String hostname) {
        return contextFor(hostname) != null;
    }

    @Scheduled(initialDelay = 300_000, fixedDelay = 43_200_000)
    public void renewIfNeeded() {
        PortalSettings settings = portalSettingsService.get();
        LocalDateTime expires = settings.getCertificateExpiresAt();
        if (settings.isPublished() && settings.getCertificateStatus() == CertificateStatus.ACTIVE && expires != null
            && expires.minus(RENEW_BEFORE).isBefore(LocalDateTime.now())) {
            LOG.info("Renewing the portal certificate (expires {}).", expires);
            requestIssue();
        }
    }

    private void issue(String hostname, String email) {
        try {
            Files.createDirectories(directory);
            Session session = new Session(LETS_ENCRYPT);
            AccountBuilder accountBuilder = new AccountBuilder().agreeToTermsOfService().useKeyPair(keyPair("account.key"));
            if (email != null) {
                accountBuilder.addEmail(email);
            }
            Account account = accountBuilder.create(session);
            Order order = account.newOrder().domains(hostname).create();
            for (Authorization authorization : order.getAuthorizations()) {
                if (authorization.getStatus() == Status.VALID) {
                    continue;
                }
                Http01Challenge challenge = authorization.findChallenge(Http01Challenge.class)
                    .orElseThrow(() -> new IllegalStateException("O Let's Encrypt não ofereceu o desafio HTTP."));
                challenges.put(challenge.getToken(), challenge.getAuthorization());
                challenge.trigger();
                Status status = challenge.waitForCompletion(ACME_WAIT);
                challenges.remove(challenge.getToken());
                if (status != Status.VALID) {
                    String detail = challenge.getError().map(problem -> problem.getDetail().orElse(problem.toString())).orElse("sem detalhes");
                    throw new IllegalStateException("O Let's Encrypt não conseguiu acessar http://" + hostname
                        + "/.well-known/acme-challenge (porta 80 redirecionada para este PC?): " + detail);
                }
            }
            order.waitUntilReady(ACME_WAIT);
            KeyPair domainKey = KeyPairUtils.createKeyPair(KEY_SIZE);
            order.execute(domainKey);
            if (order.waitForCompletion(ACME_WAIT) != Status.VALID) {
                throw new IllegalStateException("O pedido do certificado não foi concluído: "
                    + order.getError().map(Object::toString).orElse("sem detalhes"));
            }
            List<X509Certificate> chain = order.getCertificate().getCertificateChain();
            try (Writer keyWriter = Files.newBufferedWriter(directory.resolve("domain.key"))) {
                KeyPairUtils.writeKeyPair(domainKey, keyWriter);
            }
            try (Writer chainWriter = Files.newBufferedWriter(directory.resolve("chain.pem"), StandardCharsets.US_ASCII)) {
                order.getCertificate().writeCertificate(chainWriter);
            }
            sslContext = buildContext(domainKey, chain);
            contextHostname = hostname;
            LocalDateTime expires = LocalDateTime.ofInstant(chain.get(0).getNotAfter().toInstant(), ZoneId.systemDefault());
            portalSettingsService.recordCertificate(CertificateStatus.ACTIVE, "Certificado emitido pelo Let's Encrypt.", hostname, expires);
            applicationEventPublisher.publishEvent(new RoutesChangedEvent("Certificado do painel do cliente emitido"));
            LOG.info("Portal certificate issued for {} (valid until {}).", hostname, expires);
        } catch (AcmeException | IOException | GeneralSecurityException | RuntimeException exception) {
            LOG.warn("Portal certificate for {} failed: {}", hostname, exception.getMessage());
            portalSettingsService.recordCertificate(CertificateStatus.FAILED, exception.getMessage(), null, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            portalSettingsService.recordCertificate(CertificateStatus.FAILED, "Emissão interrompida.", null, null);
        } finally {
            challenges.clear();
        }
    }

    private KeyPair keyPair(String fileName) throws IOException {
        Path file = directory.resolve(fileName);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                return KeyPairUtils.readKeyPair(reader);
            }
        }
        KeyPair keyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
        try (Writer writer = Files.newBufferedWriter(file)) {
            KeyPairUtils.writeKeyPair(keyPair, writer);
        }
        return keyPair;
    }

    /** Server-side TLS context from a private key and its certificate chain (leaf first). */
    public static SSLContext buildContext(KeyPair keyPair, List<X509Certificate> chain) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("portal", keyPair.getPrivate(), KEYSTORE_PASSWORD, chain.toArray(new X509Certificate[0]));
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, KEYSTORE_PASSWORD);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        return context;
    }

    private static List<X509Certificate> readChain(InputStream input) throws GeneralSecurityException {
        return CertificateFactory.getInstance("X.509").generateCertificates(input).stream()
            .map(X509Certificate.class::cast)
            .toList();
    }
}
