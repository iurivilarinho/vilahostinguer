package com.bancada.service;

import com.bancada.enums.CredentialAuthType;
import com.bancada.filter.CredentialFilter;
import com.bancada.models.Credential;
import com.bancada.repository.CredentialRepository;
import com.bancada.request.CredentialRequest;
import com.bancada.response.CredentialSecretResponse;
import com.bancada.specification.CredentialSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final SecretCipherService secretCipherService;

    public CredentialService(CredentialRepository credentialRepository, SecretCipherService secretCipherService) {
        this.credentialRepository = credentialRepository;
        this.secretCipherService = secretCipherService;
    }

    @Transactional(readOnly = true)
    public Page<Credential> search(CredentialFilter filter, Pageable pageable) {
        Specification<Credential> specification = Specification.where(CredentialSpecification.search(filter.getSearch()))
            .and(CredentialSpecification.active(filter.getActive()));
        return credentialRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Credential findById(Long id) {
        return credentialRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Credencial não encontrada para ID: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Credential> findDefault() {
        return credentialRepository.findFirstByDefaultCredentialTrueAndActiveTrue();
    }

    @Transactional
    public Credential create(CredentialRequest request) {
        if (request.secret() == null || request.secret().isEmpty()) {
            throw new DataIntegrityViolationException(request.authType() == CredentialAuthType.PASSWORD
                ? "Informe a senha" : "Cole a chave privada");
        }
        if (request.defaultCredential()) {
            clearDefault();
        }
        Credential credential = new Credential(request, secretCipherService.encrypt(normalizeSecret(request)),
            secretCipherService.encrypt(blankToNull(request.passphrase())));
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential update(Long id, CredentialRequest request) {
        Credential credential = findById(id);
        if (request.defaultCredential() && !credential.isDefaultCredential()) {
            clearDefault();
        }
        boolean secretChanged = request.secret() != null && !request.secret().isEmpty();
        if (!secretChanged && request.authType() != credential.getAuthType()) {
            throw new DataIntegrityViolationException("Ao trocar a forma de autenticação, informe o novo segredo");
        }
        String newSecret = secretChanged ? secretCipherService.encrypt(normalizeSecret(request)) : null;
        String newPassphrase = blankToNull(request.passphrase()) == null ? null : secretCipherService.encrypt(request.passphrase());
        credential.update(request, newSecret, newPassphrase);
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential changeActive(Long id, boolean active) {
        Credential credential = findById(id);
        credential.setActive(active);
        if (!active) {
            credential.setDefaultCredential(false);
        }
        return credentialRepository.save(credential);
    }

    @Transactional(readOnly = true)
    public CredentialSecretResponse reveal(Long id) {
        Credential credential = findById(id);
        return new CredentialSecretResponse(credential.getId(), secretCipherService.decrypt(credential.getEncryptedSecret()),
            secretCipherService.decrypt(credential.getEncryptedPassphrase()));
    }

    private void clearDefault() {
        credentialRepository.findByDefaultCredentialTrue().forEach(existing -> {
            existing.setDefaultCredential(false);
            credentialRepository.save(existing);
        });
    }

    /** Keys pasted from Windows come with CRLF, which JSch rejects as an invalid key. */
    private static String normalizeSecret(CredentialRequest request) {
        if (request.authType() == CredentialAuthType.PRIVATE_KEY) {
            return request.secret().replace("\r\n", "\n").trim() + "\n";
        }
        return request.secret();
    }

    private static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
