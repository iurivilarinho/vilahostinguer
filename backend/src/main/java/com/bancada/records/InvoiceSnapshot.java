package com.bancada.records;

import com.bancada.enums.InvoiceStatus;
import com.bancada.enums.PaymentMethod;
import com.bancada.models.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** What the audit log keeps of an invoice. */
public record InvoiceSnapshot(InvoiceStatus status, BigDecimal amount, LocalDate dueDate, PaymentMethod paymentMethod,
                              LocalDateTime paidAt) {

    public InvoiceSnapshot(Invoice invoice) {
        this(invoice.getStatus(), invoice.getAmount(), invoice.getDueDate(), invoice.getPaymentMethod(), invoice.getPaidAt());
    }
}
