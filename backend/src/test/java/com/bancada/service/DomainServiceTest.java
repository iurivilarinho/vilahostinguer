package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.DdnsSyncResult;
import com.bancada.enums.DnsProvider;
import com.bancada.models.Domain;
import com.bancada.records.DdnsUpdateResult;
import com.bancada.repository.DomainRepository;
import com.bancada.request.DomainRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private SecretCipherService secretCipherService;

    @Mock
    private DdnsUpdateService ddnsUpdateService;

    @Mock
    private PublicIpService publicIpService;

    @Mock
    private RouteService routeService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private DomainService domainService;

    @BeforeEach
    void setUp() {
        when(domainRepository.save(any(Domain.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(secretCipherService.encrypt(anyString())).thenAnswer(invocation -> "cifrado:" + invocation.getArgument(0));
        when(secretCipherService.decrypt(anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).substring(8));
    }

    @Test
    void duckDnsAcceptsJustTheSubdomainAndEncryptsTheToken() {
        Domain domain = domainService.create(new DomainRequest("Casa", DnsProvider.DUCKDNS, " tok-123 ", false, true, 30));

        assertEquals("casa.duckdns.org", domain.getName());
        assertEquals("cifrado:tok-123", domain.getEncryptedSecret());
        verify(routeService).linkDomains();
    }

    @Test
    void providerThatNeedsATokenRefusesNone() {
        assertThrows(IllegalArgumentException.class,
            () -> domainService.create(new DomainRequest("exemplo.com.br", DnsProvider.CLOUDFLARE, " ", true, true, 30)));
        assertThrows(IllegalArgumentException.class,
            () -> domainService.create(new DomainRequest("exemplo.com.br", DnsProvider.DUCKDNS, "t", false, true, 30)));
        verify(domainRepository, never()).save(any());
    }

    @Test
    void successfulUpdateRemembersIpAndZone() {
        Domain domain = new Domain(new DomainRequest("exemplo.com.br", DnsProvider.CLOUDFLARE, "t", true, true, 30), "exemplo.com.br",
            "cifrado:token");
        when(domainRepository.findById(1L)).thenReturn(Optional.of(domain));
        when(publicIpService.current()).thenReturn("187.10.20.30");
        when(ddnsUpdateService.update(domain, "token", "187.10.20.30")).thenReturn(new DdnsUpdateResult("ok", "zona-1"));

        domainService.sync(1L);

        assertEquals(DdnsSyncResult.SYNCED, domain.getLastResult());
        assertEquals("187.10.20.30", domain.getLastIp());
        assertEquals("zona-1", domain.getZoneId());
    }

    @Test
    void providerFailureIsStoredAndRetriedOnlyAfterAPause() {
        Domain domain = new Domain(new DomainRequest("casa.duckdns.org", DnsProvider.DUCKDNS, "t", false, true, 30), "casa.duckdns.org",
            "cifrado:errado");
        when(domainRepository.findById(1L)).thenReturn(Optional.of(domain));
        when(publicIpService.current()).thenReturn("187.10.20.30");
        when(ddnsUpdateService.update(eq(domain), anyString(), anyString())).thenThrow(new IllegalStateException("KO"));

        domainService.sync(1L);

        assertEquals(DdnsSyncResult.FAILED, domain.getLastResult());
        assertEquals("KO", domain.getLastMessage());
        assertNull(domain.getLastIp());
        assertTrue(!domain.isSyncDue("187.10.20.30", LocalDateTime.now().plusMinutes(1)));
        assertTrue(domain.isSyncDue("187.10.20.30", LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    void publicIpChangeMakesASyncedDomainDue() {
        Domain domain = new Domain(new DomainRequest("casa.duckdns.org", DnsProvider.DUCKDNS, "t", false, true, 30), "casa.duckdns.org", "x");
        domain.recordSync(DdnsSyncResult.SYNCED, "1.1.1.1", "1.1.1.1", "ok", null);

        assertTrue(!domain.isSyncDue("1.1.1.1", LocalDateTime.now()));
        assertTrue(domain.isSyncDue("2.2.2.2", LocalDateTime.now()));
        assertTrue(domain.isSyncDue("1.1.1.1", LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void duplicatedActiveNameIsRejected() {
        when(domainRepository.existsByNameAndActiveTrueAndIdNot(eq("casa.duckdns.org"), anyLong())).thenReturn(true);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> domainService.create(new DomainRequest("casa.duckdns.org", DnsProvider.DUCKDNS, "t", false, true, 30)));
    }
}
