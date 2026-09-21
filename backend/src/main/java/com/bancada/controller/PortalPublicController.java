package com.bancada.controller;

import com.bancada.response.PortalInfoResponse;
import com.bancada.response.PortalPlanResponse;
import com.bancada.service.PlanService;
import com.bancada.service.PortalSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/public")
@Tag(name = "Painel do cliente — vitrine", description = "Informações e planos visíveis sem entrar")
public class PortalPublicController {

    private final PortalSettingsService portalSettingsService;
    private final PlanService planService;

    public PortalPublicController(PortalSettingsService portalSettingsService, PlanService planService) {
        this.portalSettingsService = portalSettingsService;
        this.planService = planService;
    }

    @Operation(summary = "Nome da empresa, cadastro aberto e formas de pagamento")
    @ApiResponse(responseCode = "200", description = "Informações públicas")
    @GetMapping("/info")
    public ResponseEntity<PortalInfoResponse> info() {
        return ResponseEntity.ok(portalSettingsService.publicInfo());
    }

    @Operation(summary = "Planos à venda, com preços por período, sistemas e estoque")
    @ApiResponse(responseCode = "200", description = "Vitrine")
    @GetMapping("/plans")
    public ResponseEntity<List<PortalPlanResponse>> plans() {
        return ResponseEntity.ok(planService.storefront());
    }
}
