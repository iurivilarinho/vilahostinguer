package com.bancada.controller;

import com.bancada.records.MachineActionRequest;
import com.bancada.request.MachineBackupRequest;
import com.bancada.request.MachineReinstallRequest;
import com.bancada.request.ServerPasswordRequest;
import com.bancada.response.DistributionResponse;
import com.bancada.response.MachineStatsResponse;
import com.bancada.response.PortalBackupResponse;
import com.bancada.response.PortalOperationResponse;
import com.bancada.response.ServerResponse;
import com.bancada.service.AuthenticatedCustomerService;
import com.bancada.service.PlanService;
import com.bancada.service.PortalServerService;
import com.bancada.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/servers")
@Validated
@Tag(name = "Painel do cliente — servidores", description = "Os servidores do cliente logado")
public class PortalServerController {

    private final PortalServerService portalServerService;
    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final AuthenticatedCustomerService authenticatedCustomerService;

    public PortalServerController(PortalServerService portalServerService, SubscriptionService subscriptionService, PlanService planService,
                                  AuthenticatedCustomerService authenticatedCustomerService) {
        this.portalServerService = portalServerService;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.authenticatedCustomerService = authenticatedCustomerService;
    }

    @Operation(summary = "Servidores do cliente (os cancelados ficam de fora)")
    @ApiResponse(responseCode = "200", description = "Servidores")
    @GetMapping
    public ResponseEntity<List<ServerResponse>> list() {
        return ResponseEntity.ok(portalServerService.list(customer()));
    }

    @Operation(summary = "Um servidor, com o acesso SSH e o endereço do site")
    @ApiResponse(responseCode = "200", description = "Servidor")
    @GetMapping("/{id}")
    public ResponseEntity<ServerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(portalServerService.get(customer(), id));
    }

    @Operation(summary = "Liga, desliga ou reinicia")
    @ApiResponse(responseCode = "201", description = "Tarefa iniciada")
    @PostMapping("/{id}/action")
    public ResponseEntity<PortalOperationResponse> action(@PathVariable Long id, @Valid @RequestBody MachineActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new PortalOperationResponse(portalServerService.action(customer(), id, request.action())));
    }

    @Operation(summary = "Uso de CPU e memória agora")
    @ApiResponse(responseCode = "200", description = "Uso atual")
    @GetMapping("/{id}/stats")
    public ResponseEntity<MachineStatsResponse> stats(@PathVariable Long id) {
        return ResponseEntity.ok(portalServerService.stats(customer(), id));
    }

    @Operation(summary = "Sistemas que podem ser instalados neste servidor")
    @ApiResponse(responseCode = "200", description = "Sistemas")
    @GetMapping("/{id}/distributions")
    public ResponseEntity<List<DistributionResponse>> distributions(@PathVariable Long id) {
        return ResponseEntity.ok(planService.distributions(subscriptionService.findForCustomer(id, customer()).getPlan()));
    }

    @Operation(summary = "Reinstala do zero, no mesmo ou em outro sistema")
    @ApiResponse(responseCode = "201", description = "Reinstalação iniciada")
    @PostMapping("/{id}/reinstall")
    public ResponseEntity<PortalOperationResponse> reinstall(@PathVariable Long id, @Valid @RequestBody MachineReinstallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new PortalOperationResponse(portalServerService.reinstall(customer(), id, request)));
    }

    @Operation(summary = "Troca a senha do usuário do servidor (SSH e sudo)")
    @ApiResponse(responseCode = "204", description = "Senha trocada")
    @PostMapping("/{id}/password")
    public ResponseEntity<Void> password(@PathVariable Long id, @Valid @RequestBody ServerPasswordRequest request) {
        portalServerService.changePassword(customer(), id, request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Backups do servidor")
    @ApiResponse(responseCode = "200", description = "Backups")
    @GetMapping("/{id}/backups")
    public ResponseEntity<List<PortalBackupResponse>> backups(@PathVariable Long id) {
        return ResponseEntity.ok(portalServerService.backups(customer(), id));
    }

    @Operation(summary = "Faz um backup do servidor inteiro agora")
    @ApiResponse(responseCode = "201", description = "Backup iniciado")
    @PostMapping("/{id}/backups")
    public ResponseEntity<PortalBackupResponse> createBackup(@PathVariable Long id, @Valid @RequestBody MachineBackupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new PortalBackupResponse(portalServerService.createBackup(customer(), id, request.name())));
    }

    @Operation(summary = "Volta o servidor ao estado de um backup")
    @ApiResponse(responseCode = "201", description = "Restauração iniciada")
    @PostMapping("/{id}/backups/{backupId}/restore")
    public ResponseEntity<PortalOperationResponse> restore(@PathVariable Long id, @PathVariable Long backupId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new PortalOperationResponse(portalServerService.restore(customer(), id, backupId)));
    }

    @Operation(summary = "Apaga um backup (libera espaço do plano)")
    @ApiResponse(responseCode = "200", description = "Backup apagado")
    @PostMapping("/{id}/backups/{backupId}/discard")
    public ResponseEntity<PortalBackupResponse> discard(@PathVariable Long id, @PathVariable Long backupId) {
        return ResponseEntity.ok(new PortalBackupResponse(portalServerService.discardBackup(customer(), id, backupId)));
    }

    @Operation(summary = "Baixa o arquivo .tar.gz de um backup")
    @ApiResponse(responseCode = "200", description = "Arquivo")
    @GetMapping("/{id}/backups/{backupId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long backupId) {
        Path file = portalServerService.backupFile(customer(), id, backupId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString())
            .body(new FileSystemResource(file));
    }

    @Operation(summary = "Andamento de uma tarefa do servidor")
    @ApiResponse(responseCode = "200", description = "Tarefa")
    @GetMapping("/{id}/operations/{operationId}")
    public ResponseEntity<PortalOperationResponse> operation(@PathVariable Long id, @PathVariable Long operationId) {
        return ResponseEntity.ok(new PortalOperationResponse(portalServerService.operation(customer(), id, operationId)));
    }

    private Long customer() {
        return authenticatedCustomerService.requireCustomerId();
    }
}
