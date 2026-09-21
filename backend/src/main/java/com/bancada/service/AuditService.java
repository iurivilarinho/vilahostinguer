package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.filter.AuditLogFilter;
import com.bancada.models.AuditLog;
import com.bancada.repository.AuditLogRepository;
import com.bancada.repository.CustomerRepository;
import com.bancada.specification.AuditLogSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Audit trail of what people did. Recorded in the caller's transaction: a rolled-back change leaves
 * no row, and a failed audit write fails the change. Values are small snapshot records, never
 * entities, so no password or token ever reaches the table.
 */
@Service
public class AuditService {

    private static final int VALUE_LIMIT = 1990;
    private static final String ADMIN = "Administrador";
    private static final String SYSTEM = "Sistema";

    private final AuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;
    private final AuthenticatedCustomerService authenticatedCustomerService;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, CustomerRepository customerRepository,
                        AuthenticatedCustomerService authenticatedCustomerService, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.customerRepository = customerRepository;
        this.authenticatedCustomerService = authenticatedCustomerService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(AuditAction action, String entityType, Long entityId, Object oldValue, Object newValue, String reason) {
        Optional<Long> customerId = authenticatedCustomerService.currentCustomerId();
        HttpServletRequest request = currentRequest();
        String userName = customerId.flatMap(customerRepository::findById).map(customer -> customer.getName())
            .orElse(request == null ? SYSTEM : ADMIN);
        String source = request == null ? "agendador" : truncate(request.getMethod() + " " + request.getRequestURI(), 120);
        auditLogRepository.save(new AuditLog(entityType, entityId, action, customerId.orElse(null), userName, serialize(oldValue),
            serialize(newValue), reason == null || reason.isBlank() ? null : truncate(reason.trim(), 1000), source));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilter filter, Pageable pageable) {
        Specification<AuditLog> specification = Specification.where(AuditLogSpecification.entityType(filter.getEntityType()))
            .and(AuditLogSpecification.entityId(filter.getEntityId()))
            .and(AuditLogSpecification.user(filter.getUserId()))
            .and(AuditLogSpecification.actionIn(filter.getAction()))
            .and(AuditLogSpecification.between(filter.getStartDate(), filter.getEndDate()));
        return auditLogRepository.findAll(specification, pageable);
    }

    /** Id of whoever acts now, for the createdBy/updatedBy stamps (null = administrator or system). */
    public Long currentActorId() {
        return authenticatedCustomerService.currentCustomerId().orElse(null);
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value), VALUE_LIMIT);
        } catch (JsonProcessingException exception) {
            return truncate(String.valueOf(value), VALUE_LIMIT);
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    private static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit);
    }
}
