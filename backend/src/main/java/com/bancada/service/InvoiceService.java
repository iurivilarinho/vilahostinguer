package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.InvoiceStatus;
import com.bancada.enums.PaymentMethod;
import com.bancada.filter.InvoiceFilter;
import com.bancada.models.Invoice;
import com.bancada.models.Subscription;
import com.bancada.records.InvoicePaidEvent;
import com.bancada.records.InvoiceSnapshot;
import com.bancada.records.PixCharge;
import com.bancada.repository.InvoiceRepository;
import com.bancada.specification.InvoiceSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invoices of the subscriptions. Paying one publishes {@link InvoicePaidEvent}; the subscription
 * side creates, renews or reactivates the server from there.
 */
@Service
public class InvoiceService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceService.class);
    private static final String ENTITY = "Invoice";
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceRepository invoiceRepository;
    private final MercadoPagoService mercadoPagoService;
    private final PortalSettingsService portalSettingsService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InvoiceService(InvoiceRepository invoiceRepository, MercadoPagoService mercadoPagoService,
                          PortalSettingsService portalSettingsService, AuditService auditService,
                          ApplicationEventPublisher applicationEventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.portalSettingsService = portalSettingsService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> search(InvoiceFilter filter, Pageable pageable) {
        Specification<Invoice> specification = Specification.where(InvoiceSpecification.customer(filter.getCustomerId()))
            .and(InvoiceSpecification.subscription(filter.getSubscriptionId()))
            .and(InvoiceSpecification.statusIn(filter.getStatus()))
            .and(InvoiceSpecification.overdue(filter.getOverdue(), LocalDate.now()))
            .and(InvoiceSpecification.dueBetween(filter.getDueFrom(), filter.getDueTo()));
        return invoiceRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Fatura não encontrada para ID: " + id));
    }

    /** Only the owner sees an invoice; any other id answers as not found. */
    @Transactional(readOnly = true)
    public Invoice findForCustomer(Long id, Long customerId) {
        Invoice invoice = findById(id);
        if (!invoice.getCustomer().getId().equals(customerId)) {
            throw new EntityNotFoundException("Fatura não encontrada para ID: " + id);
        }
        return invoice;
    }

    /** First invoice of a new contract, due today. */
    @Transactional
    public Invoice createFirst(Subscription subscription) {
        String description = subscription.getPlan().getName() + " — " + subscription.getCycle().getDescription().toLowerCase() + " — "
            + subscription.getHostname();
        return create(new Invoice(subscription, description, false, LocalDate.now()));
    }

    /** Next period, due on the day the paid period ends. */
    @Transactional
    public Invoice createRenewal(Subscription subscription) {
        LocalDate start = subscription.getNextDueDate();
        LocalDate end = start.plusMonths(subscription.getCycle().getMonths());
        String description = "Renovação " + subscription.getPlan().getName() + " — " + subscription.getHostname() + " — "
            + PERIOD.format(start) + " a " + PERIOD.format(end);
        return create(new Invoice(subscription, description, true, start));
    }

    public boolean hasOpenInvoice(Long subscriptionId) {
        return invoiceRepository.existsBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.OPEN);
    }

    /** Oldest open invoice of a subscription, the one that decides suspension and cancellation. */
    @Transactional(readOnly = true)
    public Invoice oldestOpen(Long subscriptionId) {
        return invoiceRepository.findBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.OPEN).stream()
            .min((first, second) -> first.getDueDate().compareTo(second.getDueDate()))
            .orElse(null);
    }

    /**
     * Pix for the invoice. With Mercado Pago configured, a charge is created (or the still valid one
     * returned); without it, the invoice comes back as is and the panel shows the manual instructions.
     * No transaction around it: the database is not held while Mercado Pago answers.
     */
    public Invoice pay(Long id, Long customerId) {
        Invoice invoice = findForCustomer(id, customerId);
        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new IllegalStateException("Esta fatura está " + invoice.getStatus().getDescription().toLowerCase() + ".");
        }
        String token = portalSettingsService.mercadoPagoToken();
        if (token == null || invoice.hasValidPix(LocalDateTime.now())) {
            return invoice;
        }
        PixCharge charge = mercadoPagoService.createPix(invoice, token);
        invoice.attachPix(charge.paymentId(), charge.code(), charge.qrBase64(), charge.expiresAt());
        return invoiceRepository.save(invoice);
    }

    /** "Já paguei": asks Mercado Pago now instead of waiting for the next round. */
    public Invoice checkPayment(Long id, Long customerId) {
        Invoice invoice = findForCustomer(id, customerId);
        String token = portalSettingsService.mercadoPagoToken();
        if (invoice.getStatus() == InvoiceStatus.OPEN && invoice.getProviderPaymentId() != null && token != null
            && MercadoPagoService.isApproved(mercadoPagoService.status(invoice.getProviderPaymentId(), token))) {
            return markPaid(id, PaymentMethod.PIX_MERCADO_PAGO, "Pix confirmado pelo Mercado Pago");
        }
        return invoice;
    }

    @Transactional
    public Invoice markPaid(Long id, PaymentMethod method, String note) {
        Invoice invoice = findById(id);
        InvoiceSnapshot before = new InvoiceSnapshot(invoice);
        invoice.markPaid(method, note);
        invoice.stamp(auditService.currentActorId());
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(AuditAction.PAYMENT, ENTITY, id, before, new InvoiceSnapshot(saved), note);
        applicationEventPublisher.publishEvent(new InvoicePaidEvent(id));
        return saved;
    }

    @Transactional
    public Invoice cancel(Long id, String note) {
        Invoice invoice = findById(id);
        InvoiceSnapshot before = new InvoiceSnapshot(invoice);
        invoice.cancel(note);
        invoice.stamp(auditService.currentActorId());
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, id, before, new InvoiceSnapshot(saved), note);
        return saved;
    }

    @Transactional
    public void cancelOpen(Long subscriptionId, String note) {
        for (Invoice invoice : invoiceRepository.findBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.OPEN)) {
            cancel(invoice.getId(), note);
        }
    }

    /** Administrator: confirm a payment received outside Mercado Pago, or cancel an invoice. */
    @Transactional
    public Invoice changeStatus(Long id, InvoiceStatus status, String reason) {
        return switch (status) {
            case PAID -> markPaid(id, PaymentMethod.MANUAL, reason == null || reason.isBlank() ? "Pagamento confirmado pelo administrador" : reason);
            case CANCELED -> cancel(id, reason);
            case OPEN -> throw new IllegalArgumentException("Uma fatura não volta a ficar em aberto.");
        };
    }

    /** Every minute: open invoices with a Pix are checked at Mercado Pago. */
    @Scheduled(initialDelay = 45_000, fixedDelay = 60_000)
    public void pollPixPayments() {
        String token = portalSettingsService.mercadoPagoToken();
        if (token == null) {
            return;
        }
        for (Invoice invoice : invoiceRepository.findByStatusAndProviderPaymentIdIsNotNull(InvoiceStatus.OPEN)) {
            try {
                if (MercadoPagoService.isApproved(mercadoPagoService.status(invoice.getProviderPaymentId(), token))) {
                    markPaid(invoice.getId(), PaymentMethod.PIX_MERCADO_PAGO, "Pix confirmado pelo Mercado Pago");
                }
            } catch (RuntimeException exception) {
                LOG.warn("Could not check the Pix of invoice {}: {}", invoice.getId(), exception.getMessage());
            }
        }
    }

    private Invoice create(Invoice invoice) {
        invoice.stamp(auditService.currentActorId());
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record(AuditAction.CREATE, ENTITY, saved.getId(), null, new InvoiceSnapshot(saved), saved.getDescription());
        return saved;
    }
}
