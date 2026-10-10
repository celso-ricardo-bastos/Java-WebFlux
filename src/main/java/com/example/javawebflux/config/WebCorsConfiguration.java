package com.example.javawebflux.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebCorsConfiguration implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://127.0.0.1:3000",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .maxAge(3600);
    }
}
