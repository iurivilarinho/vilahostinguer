package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GatewayServiceTest {

    @Test
    void carrierGradeNatIsDetectedFromTheRouterAddress() {
        assertTrue(GatewayService.cgnatReason("187.10.20.30", "100.72.1.9").contains("CGNAT"));
        assertTrue(GatewayService.cgnatReason("187.10.20.30", "192.168.100.2").contains("interno"));
        assertTrue(GatewayService.cgnatReason("187.10.20.30", "200.1.2.3").contains("outro roteador"));
        assertNull(GatewayService.cgnatReason("187.10.20.30", "187.10.20.30"));
        assertNull(GatewayService.cgnatReason("187.10.20.30", null));
    }

    @Test
    void privateRangesAreRecognized() {
        assertTrue(GatewayService.isSharedOrPrivate("10.0.0.1"));
        assertTrue(GatewayService.isSharedOrPrivate("172.20.0.1"));
        assertTrue(GatewayService.isSharedOrPrivate("100.64.0.1"));
        assertFalse(GatewayService.isSharedOrPrivate("100.128.0.1"));
        assertFalse(GatewayService.isSharedOrPrivate("8.8.8.8"));
        assertNotNull(GatewayService.cgnatReason(null, "10.1.1.1"));
    }
}
