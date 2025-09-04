package com.example.githubscreenshotmailer.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Configuration class named {@link OpenApiConfig} for OpenAPI documentation in the Link Converter application..
 * This class defines the metadata for the OpenAPI documentation, including the title, version,
 * description, contact information for the API
 */
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Sercan Noyan Germiyanoğlu",
                        url = "https://github.com/Rapter1990/githubscreenshotmailer"
                ),
                description = "Case Study - Live Betting" +
                        " (Java 21, Spring Boot, MySql, JUnit, Docker, Jenkins) ",
                title = "githubscreenshotmailer",
                version = "1.0.0"
        )
)
public class OpenApiConfig {

}
