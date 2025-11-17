package com.dbsyncer.cli.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dbsyncer.cli")
public class CliConfiguration {

    private String defaultOutputFormat = "table";
    private String configFile = "~/.dbsyncer/config.yml";
    private String historyFile = "~/.dbsyncer/history";
    private int maxHistorySize = 1000;

    private ServerConfig server = new ServerConfig();

    @Data
    public static class ServerConfig {
        private String url = "http://localhost:8080";
        private int connectionTimeout = 5000;
        private int readTimeout = 30000;
    }
}
