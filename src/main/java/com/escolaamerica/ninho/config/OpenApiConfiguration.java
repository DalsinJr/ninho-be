package com.escolaamerica.ninho.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI ninhoOpenApi() {
        return new OpenAPI()
            .info(new Info().title("Ninho API").description("Plataforma de Talentos Escola América").version("v1"));
    }
}
