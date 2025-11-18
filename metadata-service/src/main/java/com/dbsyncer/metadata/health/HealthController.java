package com.dbsyncer.metadata.health;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple proxy controller to expose /health and /ready endpoints.
 * Internally delegates to Spring Boot Actuator's HealthEndpoint.
 */
@RestController
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    public HealthComponent health() {
        return healthEndpoint.health();
    }

    @GetMapping("/ready")
    public HealthComponent ready() {
        // For now, reuse overall health; can be refined to readiness group later.
        return healthEndpoint.health();
    }
}
