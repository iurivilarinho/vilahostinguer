package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.CertificateStatus;
import com.bancada.models.PortalSettings;
import com.bancada.records.RoutesChangedEvent;
import com.bancada.repository.PortalSettingsRepository;
import com.bancada.request.PortalSettingsRequest;
import com.bancada.response.PortalInfoResponse;
import com.bancada.response.PortalSettingsResponse;
import com.bancada.validation.Hostnames;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalSettingsService {

    private final PortalSettingsRepository portalSettingsRepository;
    private final SecretCipherService secretCipherService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PortalSettingsService(PortalSettingsRepository portalSettingsRepository, SecretCipherService secretCipherService,
                                 AuditService auditService, ApplicationEventPublisher applicationEventPublisher) {
        this.portalSettingsRepository = portalSettingsRepository;
        this.secretCipherService = secretCipherService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public PortalSettings get() {
        return portalSettingsRepository.findById(PortalSettings.SINGLETON_ID)
            .orElseGet(() -> portalSettingsRepository.save(new PortalSettings()));
    }

    /** Publishing or moving the panel reopens the gateway ports. */
    @Transactional
    public PortalSettings update(PortalSettingsRequest request) {
        for (String name : new String[] {request.hostname(), request.customerSitesDomain(), request.sshHost()}) {
            String normalized = Hostnames.normalize(name);
            if (normalized != null && !Hostnames.isValid(normalized)) {
                throw new IllegalArgumentException("Endereço inválido: " + name);
            }
        }
        if (request.portRangeEnd() - request.portRangeStart() < 2) {
            throw new IllegalArgumentException("A faixa de portas dos clientes precisa ter ao menos 3 portas.");
        }
        if (request.cancelAfterDays() <= request.suspendAfterDays()) {
            throw new IllegalArgumentException("O cancelamento por atraso precisa vir depois da suspensão.");
        }
        if (request.enabled() && (request.hostname() == null || request.hostname().isBlank())) {
            throw new IllegalArgumentException("Informe o endereço do painel para publicá-lo.");
        }
        PortalSettings settings = get();
        PortalSettingsResponse before = new PortalSettingsResponse(settings);
        String token = request.mercadoPagoAccessToken() == null || request.mercadoPagoAccessToken().isBlank() ? null
            : secretCipherService.encrypt(request.mercadoPagoAccessToken().trim());
        settings.update(request, token);
        PortalSettings saved = portalSettingsRepository.save(settings);
        auditService.record(AuditAction.UPDATE, "PortalSettings", saved.getId(), before, new PortalSettingsResponse(saved), null);
        applicationEventPublisher.publishEvent(new RoutesChangedEvent("Painel do cliente alterado"));
        return saved;
    }

    @Transactional
    public void recordCertificate(CertificateStatus status, String message, String hostname, LocalDateTime expiresAt) {
        PortalSettings settings = get();
        settings.recordCertificate(status, message, hostname, expiresAt);
        portalSettingsRepository.save(settings);
    }

    public String mercadoPagoToken() {
        PortalSettings settings = get();
        return settings.hasMercadoPago() ? secretCipherService.decrypt(settings.getEncryptedMercadoPagoToken()) : null;
    }

    public PortalInfoResponse publicInfo() {
        PortalSettings settings = get();
        return new PortalInfoResponse(settings.getCompanyName(), settings.isRegistrationOpen(), settings.hasMercadoPago(),
            settings.getManualPaymentInstructions(), settings.getSupportEmail());
    }
}
