package com.bancada.controller;

import com.bancada.filter.AuditLogFilter;
import com.bancada.response.AuditLogResponse;
import com.bancada.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoria", description = "Quem mexeu em clientes, planos, assinaturas, faturas e servidores")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Histórico de movimentações")
    @ApiResponse(responseCode = "200", description = "Página do histórico")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> list(AuditLogFilter filter, Pageable pageable) {
        return ResponseEntity.ok(auditService.search(filter, pageable).map(AuditLogResponse::new));
    }
}
