package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancada.exception.PortalAuthenticationException;
import com.bancada.exception.TooManyAttemptsException;
import com.bancada.models.Customer;
import com.bancada.models.PortalSettings;
import com.bancada.records.PortalTokenClaims;
import com.bancada.repository.CustomerRepository;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.CustomerRegisterRequest;
import com.bancada.request.LoginRequest;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Customer login: password check, lockout after wrong attempts, and the session token. */
class PortalAuthTest {

    @TempDir
    Path dataDir;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private CustomerService customerService;
    private PortalTokenService portalTokenService;
    private Customer maria;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        PortalSettingsService settings = mock(PortalSettingsService.class);
        when(settings.get()).thenReturn(new PortalSettings());
        customerService = new CustomerService(customerRepository, mock(SubscriptionRepository.class), passwordEncoder, new LoginAttemptService(),
            settings, mock(AuditService.class));
        portalTokenService = new PortalTokenService(dataDir.toString());
        maria = new Customer(new CustomerRegisterRequest("Maria", "Maria@Teste.com ", "senha-forte-1", null, null, true),
            passwordEncoder.encode("senha-forte-1"));
        Field id = Customer.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(maria, 7L);
        when(customerRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(maria));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rightPasswordEntersAndEmailIsCaseInsensitive() {
        Customer logged = customerService.authenticate(new LoginRequest("MARIA@teste.com", "senha-forte-1"), "200.1.1.1");

        assertEquals(7L, logged.getId());
        assertTrue(logged.getLastLoginAt() != null);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
        PortalAuthenticationException wrong = assertThrows(PortalAuthenticationException.class,
            () -> customerService.authenticate(new LoginRequest("maria@teste.com", "errada"), "200.1.1.1"));
        PortalAuthenticationException unknown = assertThrows(PortalAuthenticationException.class,
            () -> customerService.authenticate(new LoginRequest("ninguem@teste.com", "errada"), "200.1.1.1"));

        assertEquals(wrong.getMessage(), unknown.getMessage(), "the login must not reveal which e-mails exist");
    }

    @Test
    void fiveWrongPasswordsLockTheEmailEvenWithTheRightOne() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(PortalAuthenticationException.class,
                () -> customerService.authenticate(new LoginRequest("maria@teste.com", "errada"), "200.1.1.1"));
        }

        assertThrows(TooManyAttemptsException.class,
            () -> customerService.authenticate(new LoginRequest("maria@teste.com", "senha-forte-1"), "200.9.9.9"));
    }

    @Test
    void tokenCarriesTheCustomerAndStopsWorkingAfterAPasswordChange() {
        String token = portalTokenService.issue(maria);
        PortalTokenClaims claims = portalTokenService.parse(token).orElseThrow();
        assertEquals(7L, claims.customerId());
        assertEquals(maria.getTokenVersion(), claims.tokenVersion());

        maria.changePassword(passwordEncoder.encode("outra-senha-1"));

        assertTrue(portalTokenService.parse(token).orElseThrow().tokenVersion() != maria.getTokenVersion(),
            "the filter compares versions: an old session no longer matches");
        assertFalse(portalTokenService.parse(token + "x").isPresent(), "a tampered token is refused");
        assertFalse(portalTokenService.parse("qualquer.coisa.aqui").isPresent());
    }

    @Test
    void sessionCookieIsHttpOnlyLaxAndSecureOnHttps() {
        ResponseCookie cookie = portalTokenService.cookie("abc", true);

        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("token", cookie.getName());
    }
}
