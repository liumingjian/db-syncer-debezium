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
}

