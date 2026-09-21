package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Situação do gateway de acesso remoto")
public record GatewayStatusResponse(

    @Schema(description = "Gateway ligado")
    boolean enabled,

    @Schema(description = "Porta compartilhada dos sites HTTP", example = "80")
    int httpPort,

    @Schema(description = "Porta compartilhada dos sites HTTPS", example = "443")
    int tlsPort,

    @Schema(description = "IP público desta rede", example = "187.10.20.30")
    String publicIp,

    @Schema(description = "Por que o IP público não foi obtido")
    String publicIpError,

    @Schema(description = "IPs deste PC na rede local (para o redirecionamento de portas no roteador)")
    List<String> lanAddresses,

    @Schema(description = "A operadora compartilha o IP (CGNAT): conexões de fora não chegam ao roteador")
    boolean cgnatSuspected,

    @Schema(description = "Explicação do CGNAT detectado")
    String cgnatReason,

    @Schema(description = "Portas abertas no PC")
    List<GatewayListenerResponse> listeners,

    @Schema(description = "Tráfego por rota")
    List<RouteTrafficResponse> traffic,

    @Schema(description = "UPnP")
    UpnpStatusResponse upnp
) {
}
