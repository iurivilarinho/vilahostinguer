package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Discos deste PC e o servidor que os entrega aos dispositivos")
public record VolumeServerResponse(

    @Schema(description = "Porta TCP onde os dispositivos buscam os discos (NBD)", example = "10809")
    int port,

    @Schema(description = "O servidor está no ar")
    boolean running,

    @Schema(description = "Por que não está", example = "A porta 10809 dos discos do PC não abriu: Address already in use")
    String error,

    @Schema(description = "Discos deste PC")
    List<HostDiskResponse> disks
) {
}
