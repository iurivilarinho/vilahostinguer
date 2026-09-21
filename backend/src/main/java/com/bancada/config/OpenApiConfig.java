package com.bancada.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bancadaOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Bancada API")
            .description("Painel para servidores caseiros (celulares, placas e mini PCs) conectados por USB ou pela rede: "
                + "detecção automática, credenciais, terminal, aplicativos, backups e armazenamento.")
            .version("1"));
    }
}
