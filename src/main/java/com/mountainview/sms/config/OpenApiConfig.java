package com.mountainview.sms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Adds a title and an "Authorize" button to the Swagger page at /swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI smsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mountain View Primary and Infant School Management System")
                        .version("1.0")
                        .description("COMP2140 group THUR_5-7_G01"))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}