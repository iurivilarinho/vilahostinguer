package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.CredentialAuthType;
import com.bancada.models.Credential;
import com.bancada.repository.CredentialRepository;
import com.bancada.request.CredentialRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private SecretCipherService secretCipherService;

    @InjectMocks
    private CredentialService credentialService;

    @Test
    void createRequiresASecret() {
        CredentialRequest request = new CredentialRequest("Root", "root", CredentialAuthType.PASSWORD, "", null, false);

        assertThrows(DataIntegrityViolationException.class, () -> credentialService.create(request));
        verify(credentialRepository, never()).save(any(Credential.class));
    }

    @Test
    void createNormalizesWindowsLineEndingsOfPrivateKeys() {
        CredentialRequest request = new CredentialRequest("Chave", "root", CredentialAuthType.PRIVATE_KEY,
            "-----BEGIN OPENSSH PRIVATE KEY-----\r\nabc\r\n-----END OPENSSH PRIVATE KEY-----\r\n", "", false);
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        credentialService.create(request);

        ArgumentCaptor<String> secret = ArgumentCaptor.forClass(String.class);
        verify(secretCipherService, times(2)).encrypt(secret.capture());
        assertEquals("-----BEGIN OPENSSH PRIVATE KEY-----\nabc\n-----END OPENSSH PRIVATE KEY-----\n", secret.getAllValues().get(0));
    }

    @Test
    void aNewDefaultCredentialReplacesThePreviousOne() {
        Credential previous = new Credential(new CredentialRequest("Antiga", "root", CredentialAuthType.PASSWORD, "x", null, true), "enc", null);
        when(credentialRepository.findByDefaultCredentialTrue()).thenReturn(List.of(previous));
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        credentialService.create(new CredentialRequest("Nova", "root", CredentialAuthType.PASSWORD, "senha", null, true));

        assertFalse(previous.isDefaultCredential());
    }

    @Test
    void changingTheAuthTypeRequiresTheNewSecret() {
        Credential existing = new Credential(new CredentialRequest("Root", "root", CredentialAuthType.PASSWORD, "x", null, false), "enc", null);
        when(credentialRepository.findById(7L)).thenReturn(Optional.of(existing));
        CredentialRequest request = new CredentialRequest("Root", "root", CredentialAuthType.PRIVATE_KEY, "", null, false);

        assertThrows(DataIntegrityViolationException.class, () -> credentialService.update(7L, request));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(credentialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> credentialService.findById(99L));
    }
}
