package com.dbsyncer.metadata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "connect")
@Getter
@Setter
public class ConnectProperties {
    private String restUrl;
    private long waitTimeoutMs = 60000;
    private long pollIntervalMs = 2000;
    /**
     * Base URL of metadata-service as seen from Kafka Connect workers.
     * For example: http://metadata-service:8080 in docker-compose,
     * or http://host.testcontainers.internal:8080 in Testcontainers-based tests.
     */
    private String metadataServiceUrl;

    /**
     * Directory inside Kafka Connect container where Debezium schema history
     * files should be stored. Defaults to the docker-compose path
     * /kafka/connect/custom-connectors/schema-history.
     */
    private String schemaHistoryDir;

    /**
     * Whether to enable the custom ProgressReporting SMT in sink connector
     * configurations. When disabled, connectors will run without the
     * 'progress' transform which is useful in environments where the SMT
     * JAR is not available on the Kafka Connect plugin path.
     * <p>
     * Default is false to make local/testing setups work out-of-the-box.
     */
    private boolean enableProgressSmt = false;
}
