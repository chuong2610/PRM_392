package com.wayflo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI wayFloOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("WayFlo Backend API")
                .version("0.1.0")
                .description("Indoor map rendering, location search, and indoor routing."));
    }
}
