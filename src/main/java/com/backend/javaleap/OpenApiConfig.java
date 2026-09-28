package com.backend.javaleap;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI documentation - available at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI padaShaktiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PadaShakti API")
                        .version("1.0.0")
                        .description("Smart Urban Subway Kinetic Floor Energy Harvesting Network - "
                                + "live tile telemetry, energy analytics, crowd safety and alerts.")
                        .contact(new Contact().name("PadaShakti Team"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
