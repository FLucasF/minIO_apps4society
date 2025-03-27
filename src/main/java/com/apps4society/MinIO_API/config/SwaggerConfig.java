package com.apps4society.MinIO_API.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MinIO API")
                        .version("1.0")
                        .description("API para gerenciamento de mídias usando MinIO"))
                .addSecurityItem(new SecurityRequirement().addList("API Key"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("API Key", new SecurityScheme()
                                .name("x-api-key") //olhar mais sobre
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
