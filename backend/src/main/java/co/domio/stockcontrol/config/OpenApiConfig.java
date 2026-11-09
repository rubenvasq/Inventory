package co.domio.stockcontrol.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentacion OpenAPI/Swagger.
 * UI disponible en /swagger-ui.html y el JSON en /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockControlOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kardex API")
                        .description("API REST del sistema de gestion de inventario de DOMIO")
                        .version("v1")
                        .contact(new Contact().name("DOMIO").email("domio.co@outlook.com"))
                        .license(new License().name("Propietario")));
    }
}
