package com.libraryms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.libraryms.support.IntegrationTest;

@IntegrationTest
class ApplicationIT {

    // Field injection is acceptable in test classes only (JUnit instantiates them).
    @Autowired
    private TestRestTemplate rest;

    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<Map> response = rest.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void livenessAndReadinessProbesAreExposed() {
        assertThat(rest.getForEntity("/actuator/health/liveness", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/actuator/health/readiness", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
