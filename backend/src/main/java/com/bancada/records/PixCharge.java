package com.bancada.records;

import java.time.LocalDateTime;

/** Pix created at the payment provider for an invoice. */
public record PixCharge(String paymentId, String code, String qrBase64, LocalDateTime expiresAt) {
}
