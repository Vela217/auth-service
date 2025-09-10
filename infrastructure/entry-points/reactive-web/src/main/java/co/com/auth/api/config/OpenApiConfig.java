package co.com.auth.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI authOpenAPI() {
        final String schemeName = "bearer-jwt";

        return new OpenAPI()
                .info(new Info()
                        .title("CrediYa - Auth API")
                        .version("v1")
                        .description("APIs de autenticación/usuarios"))
                // Aplica seguridad por defecto a todas las operaciones
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))); // sólo informativo
    }

}
