package co.com.auth.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "CrediYa - Auth API", version = "v1", description = "APIs de autenticación/usuarios"))
public class OpenApiConfig {
}
