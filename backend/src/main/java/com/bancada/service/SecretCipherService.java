package com.bancada.service;

import com.sun.jna.platform.win32.Crypt32Util;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encrypts credential secrets before they touch the database.
 *
 * <p>On Windows it uses DPAPI ({@code CryptProtectData}): the key is derived from the Windows account
 * that runs the panel, so another user of the same machine, or someone copying the database file,
 * cannot decrypt it, and the panel keeps no key of its own. Elsewhere it falls back to AES-GCM with
 * a random key stored next to the database.
 */
@Service
public class SecretCipherService {

    private static final String DPAPI_PREFIX = "dpapi:";
    private static final String AES_PREFIX = "aes:";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int AES_KEY_BYTES = 32;

    private final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    private final Path keyFile;
    private final SecureRandom random = new SecureRandom();

    public SecretCipherService(@Value("${bancada.data-dir}") String dataDir) {
        this.keyFile = Paths.get(dataDir, "secret.key");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
        if (windows) {
            return DPAPI_PREFIX + Base64.getEncoder().encodeToString(Crypt32Util.cryptProtectData(data));
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(loadOrCreateKey(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(data);
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return AES_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Não foi possível cifrar o segredo", exception);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return null;
        }
        if (cipherText.startsWith(DPAPI_PREFIX)) {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(DPAPI_PREFIX.length()));
            return new String(Crypt32Util.cryptUnprotectData(payload), StandardCharsets.UTF_8);
        }
        if (cipherText.startsWith(AES_PREFIX)) {
            try {
                byte[] payload = Base64.getDecoder().decode(cipherText.substring(AES_PREFIX.length()));
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(loadOrCreateKey(), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_BYTES));
                byte[] plain = cipher.doFinal(payload, GCM_IV_BYTES, payload.length - GCM_IV_BYTES);
                return new String(plain, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException | IOException exception) {
                throw new IllegalStateException("Não foi possível decifrar o segredo", exception);
            }
        }
        throw new IllegalStateException("Formato de segredo desconhecido");
    }

    private synchronized byte[] loadOrCreateKey() throws IOException {
        if (Files.exists(keyFile)) {
            return Files.readAllBytes(keyFile);
        }
        byte[] key = new byte[AES_KEY_BYTES];
        random.nextBytes(key);
        Files.createDirectories(keyFile.getParent());
        Files.write(keyFile, key);
        return key;
    }
}
