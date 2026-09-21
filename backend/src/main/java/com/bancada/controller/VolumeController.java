package com.bancada.controller;

import com.bancada.filter.VolumeFilter;
import com.bancada.models.Volume;
import com.bancada.request.VolumeAttachRequest;
import com.bancada.request.VolumeDeleteRequest;
import com.bancada.request.VolumeRequest;
import com.bancada.response.OperationResponse;
import com.bancada.response.VolumeResponse;
import com.bancada.response.VolumeServerResponse;
import com.bancada.service.HostDiskService;
import com.bancada.service.VolumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volumes")
@Validated
@Tag(name = "Discos do PC", description = "Espaço dos discos deste PC entregue aos dispositivos e máquinas (NBD)")
public class VolumeController {

    private final VolumeService volumeService;
    private final HostDiskService hostDiskService;

    public VolumeController(VolumeService volumeService, HostDiskService hostDiskService) {
        this.volumeService = volumeService;
        this.hostDiskService = hostDiskService;
    }

    @Operation(summary = "Lista os discos virtuais (os excluídos ficam de fora sem filtro)")
    @ApiResponse(responseCode = "200", description = "Página de discos")
    @GetMapping
    public ResponseEntity<Page<VolumeResponse>> list(VolumeFilter filter, Pageable pageable) {
        return ResponseEntity.ok(volumeService.search(filter, pageable)
            .map(volume -> new VolumeResponse(volume, volumeService.writtenBytes(volume))));
    }

    @Operation(summary = "Discos deste PC, quanto cabe em cada um e a situação do servidor de discos")
    @ApiResponse(responseCode = "200", description = "Discos e servidor")
    @GetMapping("/host")
    public ResponseEntity<VolumeServerResponse> host() {
        String error = volumeService.serverError();
        return ResponseEntity.ok(new VolumeServerResponse(volumeService.port(), error == null, error, hostDiskService.list()));
    }

    @Operation(summary = "Busca um disco virtual")
    @ApiResponse(responseCode = "200", description = "Disco encontrado")
    @ApiResponse(responseCode = "404", description = "Disco inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<VolumeResponse> findById(@PathVariable Long id) {
        Volume volume = volumeService.findById(id);
        return ResponseEntity.ok(new VolumeResponse(volume, volumeService.writtenBytes(volume)));
    }

    @Operation(summary = "Cria um disco virtual num disco deste PC (o espaço fica reservado)")
    @ApiResponse(responseCode = "201", description = "Disco criado")
    @ApiResponse(responseCode = "409", description = "Nome já usado")
    @PostMapping
    public ResponseEntity<VolumeResponse> create(@Valid @RequestBody VolumeRequest request) {
        Volume volume = volumeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new VolumeResponse(volume, volumeService.writtenBytes(volume)));
    }

    @Operation(summary = "Entrega o disco a um dispositivo (montado numa pasta) ou a uma máquina (a máquina é recriada)")
    @ApiResponse(responseCode = "201", description = "Conexão iniciada")
    @PostMapping("/{id}/attach")
    public ResponseEntity<OperationResponse> attach(@PathVariable Long id, @Valid @RequestBody VolumeAttachRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(volumeService.attach(id, request)));
    }

    @Operation(summary = "Desmonta e desconecta o disco do dispositivo (tirando-o da máquina antes)")
    @ApiResponse(responseCode = "201", description = "Desconexão iniciada")
    @PostMapping("/{id}/detach")
    public ResponseEntity<OperationResponse> detach(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(volumeService.detach(id)));
    }

    @Operation(summary = "Libera o disco sem falar com o dispositivo (para um dispositivo que sumiu)")
    @ApiResponse(responseCode = "200", description = "Disco livre")
    @PostMapping("/{id}/release")
    public ResponseEntity<VolumeResponse> release(@PathVariable Long id) {
        Volume volume = volumeService.release(id);
        return ResponseEntity.ok(new VolumeResponse(volume, volumeService.writtenBytes(volume)));
    }

    @Operation(summary = "Apaga o disco e tudo o que está nele (o registro fica no histórico)")
    @ApiResponse(responseCode = "200", description = "Disco apagado")
    @PostMapping("/{id}/delete")
    public ResponseEntity<VolumeResponse> delete(@PathVariable Long id, @Valid @RequestBody VolumeDeleteRequest request) {
        Volume volume = volumeService.delete(id, request.confirmation());
        return ResponseEntity.ok(new VolumeResponse(volume, 0));
    }
}
