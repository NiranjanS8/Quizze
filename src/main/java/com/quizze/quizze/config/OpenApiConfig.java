package com.quizze.quizze.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quizzeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quizze API")
                        .description("Backend APIs for the Quizze online quiz platform")
                        .version("v1")
                        .contact(new Contact().name("Quizze Team"))
                        .license(new License().name("MIT")));
    }
}
