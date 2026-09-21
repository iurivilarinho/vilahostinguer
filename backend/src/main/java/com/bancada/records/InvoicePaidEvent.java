package com.bancada.records;

/** Published after an invoice is paid, so the subscription is provisioned, renewed or reactivated. */
public record InvoicePaidEvent(Long invoiceId) {
}
