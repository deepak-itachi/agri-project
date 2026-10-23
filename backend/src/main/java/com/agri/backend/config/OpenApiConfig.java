package com.agri.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Live API documentation at /swagger-ui/index.html (use the Authorize button with a token from /api/auth/login). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agriSetuOpenApi() {
        return new OpenAPI()
                .info(new Info().title("AgriSetu API").version("2.0")
                        .description("Farmer learning, seller marketplace, buyer ordering and admin console."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
