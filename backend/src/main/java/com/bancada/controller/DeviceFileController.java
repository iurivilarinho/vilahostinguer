package com.bancada.controller;

import com.bancada.records.CreateFolderRequest;
import com.bancada.response.FileEntryResponse;
import com.bancada.response.FileUploadResponse;
import com.bancada.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/devices/{deviceId}/files")
@Validated
@Tag(name = "Arquivos", description = "Gerenciador de arquivos do dispositivo")
public class DeviceFileController {

    private final FileService fileService;

    public DeviceFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Lista o conteúdo de uma pasta")
    @ApiResponse(responseCode = "200", description = "Arquivos e pastas")
    @GetMapping
    public ResponseEntity<List<FileEntryResponse>> list(@PathVariable Long deviceId, @RequestParam(defaultValue = "/") String path) {
        return ResponseEntity.ok(fileService.list(deviceId, path));
    }

    @Operation(summary = "Baixa um arquivo")
    @ApiResponse(responseCode = "200", description = "Conteúdo do arquivo")
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long deviceId, @RequestParam String path) {
        long size = fileService.fileSize(deviceId, path);
        String name = path.substring(path.lastIndexOf('/') + 1);
        StreamingResponseBody body = output -> fileService.download(deviceId, path, output);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(size)
            .body(body);
    }

    @Operation(summary = "Envia um arquivo para uma pasta do dispositivo")
    @ApiResponse(responseCode = "201", description = "Arquivo gravado")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(@PathVariable Long deviceId, @RequestPart("path") String path,
                                                     @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.upload(deviceId, path, file));
    }

    @Operation(summary = "Cria uma pasta")
    @ApiResponse(responseCode = "204", description = "Pasta criada")
    @PostMapping("/folders")
    public ResponseEntity<Void> createFolder(@PathVariable Long deviceId, @Valid @RequestBody CreateFolderRequest request) {
        fileService.createFolder(deviceId, request.path());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apaga um arquivo ou uma pasta vazia")
    @ApiResponse(responseCode = "204", description = "Apagado")
    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long deviceId, @RequestParam String path) {
        fileService.delete(deviceId, path);
        return ResponseEntity.noContent().build();
    }
}
