package com.dbsyncer.metadata.health;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Kafka Connect REST API.
 */
@Component("kafkaConnect")
@RequiredArgsConstructor
@Slf4j
public class KafkaConnectHealthIndicator implements HealthIndicator {

    private final KafkaConnectClient connectClient;

    @Override
    public Health health() {
        try {
            connectClient.getConnectorPlugins();
            return Health.up()
                    .withDetail("component", "kafka-connect")
                    .build();
        } catch (Exception e) {
            log.warn("Kafka Connect health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("component", "kafka-connect")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

