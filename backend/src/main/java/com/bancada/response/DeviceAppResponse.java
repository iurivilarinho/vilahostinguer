package com.bancada.response;

import com.bancada.enums.AppCategory;
import com.bancada.enums.CatalogApp;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aplicativo do catálogo e sua situação num dispositivo")
public record DeviceAppResponse(

    @Schema(description = "Chave do aplicativo", example = "NGINX")
    CatalogApp key,

    @Schema(description = "Nome", example = "Nginx")
    String name,

    @Schema(description = "Descrição", example = "Servidor web e proxy reverso leve.")
    String description,

    @Schema(description = "Categoria")
    AppCategory category,

    @Schema(description = "Descrição da categoria", example = "Servidor web")
    String categoryDescription,

    @Schema(description = "Disponível para o sistema deste dispositivo")
    boolean available,

    @Schema(description = "Instalado")
    boolean installed,

    @Schema(description = "Versão instalada", example = "nginx version: nginx/1.26.2")
    String version,

    @Schema(description = "Serviço do sistema, quando o aplicativo tem um", example = "nginx")
    String serviceName,

    @Schema(description = "Serviço rodando")
    boolean serviceRunning,

    @Schema(description = "Serviço inicia com o sistema")
    boolean serviceEnabled
) {
}
