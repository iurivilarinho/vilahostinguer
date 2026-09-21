package com.bancada.controller;

import com.bancada.filter.BackupFilter;
import com.bancada.records.BackupStatusRequest;
import com.bancada.request.BackupRequest;
import com.bancada.response.BackupResponse;
import com.bancada.response.OperationResponse;
import com.bancada.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backups")
@Validated
@Tag(name = "Backups", description = "Backups de pastas dos dispositivos")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @Operation(summary = "Lista backups")
    @ApiResponse(responseCode = "200", description = "Página de backups")
    @GetMapping
    public ResponseEntity<Page<BackupResponse>> list(BackupFilter filter, Pageable pageable) {
        return ResponseEntity.ok(backupService.search(filter, pageable).map(BackupResponse::new));
    }

    @Operation(summary = "Busca um backup")
    @ApiResponse(responseCode = "200", description = "Backup encontrado")
    @ApiResponse(responseCode = "404", description = "Backup inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<BackupResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new BackupResponse(backupService.findById(id)));
    }

    @Operation(summary = "Gera um backup das pastas escolhidas (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Backup iniciado")
    @PostMapping
    public ResponseEntity<BackupResponse> create(@Valid @RequestBody BackupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new BackupResponse(backupService.create(request)));
    }

    @Operation(summary = "Restaura um backup no dispositivo de origem, sobrescrevendo os arquivos")
    @ApiResponse(responseCode = "201", description = "Restauração iniciada")
    @PostMapping("/{id}/restore")
    public ResponseEntity<OperationResponse> restore(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(backupService.restore(id)));
    }

    @Operation(summary = "Muda a situação de um backup (DISCARDED apaga o arquivo e mantém o registro)")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<BackupResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody BackupStatusRequest request) {
        return ResponseEntity.ok(new BackupResponse(backupService.changeStatus(id, request.status())));
    }

    @Operation(summary = "Baixa o arquivo .tar.gz do backup")
    @ApiResponse(responseCode = "200", description = "Arquivo do backup")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Path file = backupService.file(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString())
            .contentType(MediaType.parseMediaType("application/gzip"))
            .body(new FileSystemResource(file));
    }
}
