package com.libraryms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.libraryms.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OpenApiDocumentationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("OpenAPI JSON docs and Swagger UI are publicly accessible without credentials")
    void openApiEndpointsAreAccessible() {
        ResponseEntity<String> apiDocsRes = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(apiDocsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiDocsRes.getBody()).contains("Library Management System REST API");
        assertThat(apiDocsRes.getBody()).contains("BearerAuth");

        ResponseEntity<String> swaggerRes = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(swaggerRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
