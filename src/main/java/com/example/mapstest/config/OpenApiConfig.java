package com.example.mapstest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mapstestOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dog Spot Map API")
                        .description("A demo app for dog related spots pinned in a map")
                        .version("0.0.1-SNAPSHOT"));
    }

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }
}
