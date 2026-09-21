package com.bancada.service;

import com.bancada.enums.DdnsSyncResult;
import com.bancada.enums.DeviceEventType;
import com.bancada.enums.DnsProvider;
import com.bancada.filter.DomainFilter;
import com.bancada.models.Domain;
import com.bancada.records.DdnsUpdateResult;
import com.bancada.repository.DomainRepository;
import com.bancada.request.DomainRequest;
import com.bancada.specification.DomainSpecification;
import com.bancada.validation.Hostnames;
import jakarta.persistence.EntityNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domains and their dynamic DNS. The provider calls go to the internet, so {@link #sync(Long)} runs
 * outside a transaction: the database is only touched to read the domain and to store the result.
 */
@Service
public class DomainService {

    private static final Logger LOG = LoggerFactory.getLogger(DomainService.class);
    private static final String DUCKDNS_SUFFIX = ".duckdns.org";

    private final DomainRepository domainRepository;
    private final SecretCipherService secretCipherService;
    private final DdnsUpdateService ddnsUpdateService;
    private final PublicIpService publicIpService;
    private final RouteService routeService;
    private final EventService eventService;

    public DomainService(DomainRepository domainRepository, SecretCipherService secretCipherService, DdnsUpdateService ddnsUpdateService,
                         PublicIpService publicIpService, RouteService routeService, EventService eventService) {
        this.domainRepository = domainRepository;
        this.secretCipherService = secretCipherService;
        this.ddnsUpdateService = ddnsUpdateService;
        this.publicIpService = publicIpService;
        this.routeService = routeService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<Domain> search(DomainFilter filter, Pageable pageable) {
        Specification<Domain> specification = Specification.where(DomainSpecification.search(filter.getSearch()))
            .and(DomainSpecification.providerIn(filter.getProvider()))
            .and(DomainSpecification.active(filter.getActive()));
        return domainRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Domain findById(Long id) {
        return domainRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Domínio não encontrado para ID: " + id));
    }

    @Transactional
    public Domain create(DomainRequest request) {
        String name = validName(request, null);
        String secret = request.secret() == null || request.secret().isBlank() ? null : request.secret().trim();
        if (request.provider().isSecretRequired() && secret == null) {
            throw new IllegalArgumentException(secretHint(request.provider()));
        }
        Domain domain = domainRepository.save(new Domain(request, name, secretCipherService.encrypt(secret)));
        routeService.linkDomains();
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, "Domínio " + name + " cadastrado");
        return domain;
    }

    @Transactional
    public Domain update(Long id, DomainRequest request) {
        Domain domain = findById(id);
        String name = validName(request, id);
        String secret = request.secret() == null || request.secret().isBlank() ? null : request.secret().trim();
        if (request.provider().isSecretRequired() && secret == null
            && (domain.getEncryptedSecret() == null || request.provider() != domain.getProvider())) {
            throw new IllegalArgumentException(secretHint(request.provider()));
        }
        domain.update(request, name, secret == null ? null : secretCipherService.encrypt(secret));
        Domain saved = domainRepository.save(domain);
        routeService.linkDomains();
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, "Domínio " + name + " alterado");
        return saved;
    }

    @Transactional
    public Domain changeActive(Long id, boolean active) {
        Domain domain = findById(id);
        if (active && domainRepository.existsByNameAndActiveTrueAndIdNot(domain.getName(), id)) {
            throw new DataIntegrityViolationException("Já existe um domínio ativo chamado " + domain.getName() + ".");
        }
        domain.changeActive(active);
        Domain saved = domainRepository.save(domain);
        routeService.linkDomains();
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, "Domínio " + domain.getName() + (active ? " reativado" : " arquivado"));
        return saved;
    }

    /** Points the DNS at the current public IP now (or, for manual domains, checks where it points). */
    public Domain sync(Long id) {
        Domain domain = findById(id);
        String ip;
        try {
            ip = publicIpService.current();
        } catch (IllegalStateException exception) {
            return store(domain, DdnsSyncResult.FAILED, null, resolve(domain.getName()), exception.getMessage(), null);
        }
        if (domain.getProvider() == DnsProvider.MANUAL) {
            String resolved = resolve(domain.getName());
            if (ip.equals(resolved)) {
                return store(domain, DdnsSyncResult.SYNCED, ip, resolved, "O DNS aponta para o IP público " + ip + ".", null);
            }
            String message = resolved == null
                ? "O nome ainda não resolve. Crie no seu provedor um registro A apontando para " + ip + "."
                : "O DNS aponta para " + resolved + ", mas o IP público é " + ip + ". Atualize o registro A no provedor.";
            return store(domain, DdnsSyncResult.FAILED, ip, resolved, message, null);
        }
        try {
            String secret = secretCipherService.decrypt(domain.getEncryptedSecret());
            if (secret == null) {
                throw new IllegalStateException(secretHint(domain.getProvider()));
            }
            DdnsUpdateResult result = ddnsUpdateService.update(domain, secret, ip);
            return store(domain, DdnsSyncResult.SYNCED, ip, resolve(domain.getName()), result.message(), result.zoneId());
        } catch (RuntimeException exception) {
            LOG.warn("DDNS of {} failed: {}", domain.getName(), exception.getMessage());
            return store(domain, DdnsSyncResult.FAILED, ip, resolve(domain.getName()), exception.getMessage(), null);
        }
    }

    /** Every minute: domains whose public IP changed, whose interval passed, or that never synced. */
    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void syncDue() {
        List<Domain> candidates = domainRepository.findByActiveTrue().stream().filter(Domain::isDdnsEnabled).toList();
        if (candidates.isEmpty()) {
            return;
        }
        String ip;
        try {
            ip = publicIpService.current();
        } catch (IllegalStateException exception) {
            LOG.debug("DDNS skipped: {}", exception.getMessage());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Domain domain : candidates) {
            if (domain.isSyncDue(ip, now)) {
                try {
                    sync(domain.getId());
                } catch (RuntimeException exception) {
                    LOG.warn("DDNS of {} failed", domain.getName(), exception);
                }
            }
        }
    }

    private Domain store(Domain domain, DdnsSyncResult result, String ip, String resolved, String message, String zoneId) {
        domain.recordSync(result, ip, resolved, message, zoneId);
        Domain saved = domainRepository.save(domain);
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, "DNS de " + domain.getName() + ": "
            + result.getDescription().toLowerCase());
        return saved;
    }

    private String validName(DomainRequest request, Long id) {
        String name = Hostnames.normalize(request.name());
        if (request.provider() == DnsProvider.DUCKDNS && name != null && !name.contains(".")) {
            name = name + DUCKDNS_SUFFIX;
        }
        if (!Hostnames.isValid(name)) {
            throw new IllegalArgumentException("Domínio inválido: " + request.name());
        }
        if (request.provider() == DnsProvider.DUCKDNS && !name.endsWith(DUCKDNS_SUFFIX)) {
            throw new IllegalArgumentException("Um domínio DuckDNS termina em duckdns.org");
        }
        if (domainRepository.existsByNameAndActiveTrueAndIdNot(name, id == null ? -1L : id)) {
            throw new DataIntegrityViolationException("Já existe um domínio ativo chamado " + name + ".");
        }
        return name;
    }

    private static String secretHint(DnsProvider provider) {
        return switch (provider) {
            case CLOUDFLARE -> "Informe o API Token da Cloudflare (permissão Zone → DNS → Edit).";
            case DUCKDNS -> "Informe o token da sua conta DuckDNS.";
            case CUSTOM_URL -> "Informe a URL de atualização, com {ip} onde entra o IP público.";
            case MANUAL -> "Nada a informar.";
        };
    }

    /** IPv4 the name resolves to right now, from this PC; null when it does not resolve. */
    private static String resolve(String name) {
        try {
            return Arrays.stream(InetAddress.getAllByName(name))
                .filter(Inet4Address.class::isInstance)
                .map(InetAddress::getHostAddress)
                .findFirst()
                .orElse(null);
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
