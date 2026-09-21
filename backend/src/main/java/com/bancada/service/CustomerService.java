package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.CustomerStatus;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.exception.PortalAuthenticationException;
import com.bancada.filter.CustomerFilter;
import com.bancada.models.Customer;
import com.bancada.records.CustomerSnapshot;
import com.bancada.repository.CustomerRepository;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.ChangePasswordRequest;
import com.bancada.request.CustomerProfileRequest;
import com.bancada.request.CustomerRegisterRequest;
import com.bancada.request.LoginRequest;
import com.bancada.specification.CustomerSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final String ENTITY = "Customer";
    /** Same answer for unknown e-mail and wrong password: the login must not reveal who is a customer. */
    private static final String BAD_LOGIN = "E-mail ou senha incorretos.";

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PortalSettingsService portalSettingsService;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, SubscriptionRepository subscriptionRepository, PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService, PortalSettingsService portalSettingsService, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.portalSettingsService = portalSettingsService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(CustomerFilter filter, Pageable pageable) {
        Specification<Customer> specification = Specification.where(CustomerSpecification.search(filter.getSearch()))
            .and(CustomerSpecification.statusIn(filter.getStatus()));
        return customerRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado para ID: " + id));
    }

    @Transactional
    public Customer register(CustomerRegisterRequest request) {
        if (!portalSettingsService.get().isRegistrationOpen()) {
            throw new IllegalStateException("O cadastro de clientes novos está fechado no momento.");
        }
        String email = Customer.normalizeEmail(request.email());
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new DataIntegrityViolationException("Já existe uma conta com este e-mail. Entre ou use outro e-mail.");
        }
        Customer customer = customerRepository.save(new Customer(request, passwordEncoder.encode(request.password())));
        customer.stamp(customer.getId());
        Customer saved = customerRepository.save(customer);
        auditService.record(AuditAction.CREATE, ENTITY, saved.getId(), null, new CustomerSnapshot(saved), "Cadastro pelo painel");
        return saved;
    }

    /** Checks the password; wrong attempts count towards the lockout of the e-mail and of the address. */
    @Transactional
    public Customer authenticate(LoginRequest request, String address) {
        String email = Customer.normalizeEmail(request.email());
        loginAttemptService.checkAllowed(email, address);
        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null || !passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            loginAttemptService.recordFailure(email, address);
            throw new PortalAuthenticationException(BAD_LOGIN);
        }
        if (customer.getStatus() == CustomerStatus.CLOSED) {
            throw new PortalAuthenticationException("Esta conta foi encerrada. Fale com o suporte.");
        }
        loginAttemptService.recordSuccess(email);
        customer.registerLogin();
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateProfile(Long id, CustomerProfileRequest request) {
        Customer customer = findById(id);
        String email = Customer.normalizeEmail(request.email());
        if (customerRepository.existsByEmailAndIdNot(email, id)) {
            throw new DataIntegrityViolationException("Já existe uma conta com este e-mail.");
        }
        CustomerSnapshot before = new CustomerSnapshot(customer);
        customer.updateProfile(request);
        customer.stamp(auditService.currentActorId());
        Customer saved = customerRepository.save(customer);
        auditService.record(AuditAction.UPDATE, ENTITY, id, before, new CustomerSnapshot(saved), null);
        return saved;
    }

    /** Changing the password ends every other session of the customer. */
    @Transactional
    public Customer changePassword(Long id, ChangePasswordRequest request) {
        Customer customer = findById(id);
        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        customer.changePassword(passwordEncoder.encode(request.newPassword()));
        customer.stamp(id);
        Customer saved = customerRepository.save(customer);
        auditService.record(AuditAction.PASSWORD_CHANGE, ENTITY, id, null, null, "Trocada pelo cliente");
        return saved;
    }

    @Transactional
    public Customer resetPassword(Long id, String newPassword) {
        Customer customer = findById(id);
        customer.changePassword(passwordEncoder.encode(newPassword));
        customer.stamp(null);
        Customer saved = customerRepository.save(customer);
        auditService.record(AuditAction.PASSWORD_CHANGE, ENTITY, id, null, null, "Definida pelo administrador");
        return saved;
    }

    /** Blocking keeps the servers running: it only locks the panel. Closing needs no live subscription. */
    @Transactional
    public Customer changeStatus(Long id, CustomerStatus status, String reason) {
        Customer customer = findById(id);
        if (status == CustomerStatus.CLOSED && subscriptionRepository.existsByCustomerIdAndStatusIn(id, List.of(
            SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.PROVISIONING, SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED))) {
            throw new IllegalStateException("Cancele as assinaturas do cliente antes de encerrar a conta.");
        }
        CustomerSnapshot before = new CustomerSnapshot(customer);
        customer.changeStatus(status);
        customer.stamp(null);
        Customer saved = customerRepository.save(customer);
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, id, before, new CustomerSnapshot(saved), reason);
        return saved;
    }

    /** Customer allowed to buy and to act on servers (a blocked one only reads and pays). */
    @Transactional(readOnly = true)
    public Customer requireActive(Long id) {
        Customer customer = findById(id);
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Sua conta está bloqueada. Fale com o suporte.");
        }
        return customer;
    }
}
