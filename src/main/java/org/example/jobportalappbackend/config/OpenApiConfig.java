package org.example.jobportalappbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for Swagger UI. springdoc reads controllers at startup and merges this metadata.
 * Nothing here changes the runtime routes.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

    @Bean
    OpenAPI jobPortalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Job Portal API")
                        .version("v1")
                        .description("""
                                REST API for the job portal learning project.
                                Reading jobs is public. Creating, updating, and deleting a job requires
                                the plain-text JWT from POST /login, sent as: Authorization: Bearer <token>.
                                """))
                .components(new Components().addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                        .name(BEARER_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the raw token returned by POST /login. Do not add the word Bearer here.")));
    }
}
