package com.friendinneed.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI companionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Friend in Need API")
                .version("v1")
                .description("API for companion profiles, conversations, memories, calendar, face templates, and voice.")
                .license(new License().name("Private deployment")));
    }
}
