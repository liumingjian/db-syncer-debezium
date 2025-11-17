package com.dbsyncer.cli.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class UserConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = ".dbsyncer/config.yml";
    private final Map<String, Object> userConfig = new HashMap<>();

    public UserConfigLoader() {
        loadUserConfig();
    }

    private void loadUserConfig() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(content);
                if (config != null) {
                    userConfig.putAll(config);
                    log.debug("Loaded user configuration from {}", configPath);
                }
            } catch (IOException e) {
                log.warn("Failed to load user configuration: {}", e.getMessage());
            }
        } else {
            log.debug("User configuration file not found: {}", configPath);
            createDefaultConfig(configPath);
        }
    }

    private void createDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            String defaultConfig = """
                # DB Syncer CLI Configuration
                # Location: ~/.dbsyncer/config.yml

                # Default output format (table, json, yaml)
                output-format: table

                # Metadata service connection
                server:
                  url: http://localhost:8080
                  connection-timeout: 5000
                  read-timeout: 30000

                # Default task settings
                defaults:
                  batch-size: 10000
                  max-queue-size: 20000
                  poll-interval-ms: 1000
                  snapshot-mode: initial

                # Aliases for common operations
                aliases:
                  ls: task list
                  ps: status
                  rm: task delete --force
                """;
            Files.writeString(configPath, defaultConfig);
            log.info("Created default configuration file: {}", configPath);
        } catch (IOException e) {
            log.warn("Failed to create default configuration: {}", e.getMessage());
        }
    }

    public Path getConfigPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, DEFAULT_CONFIG_FILE);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        String[] keys = key.split("\\.");
        Object current = userConfig;

        for (String k : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(k);
            } else {
                return defaultValue;
            }
        }

        if (current == null) {
            return defaultValue;
        }

        try {
            return (T) current;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        Object value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public void reload() {
        userConfig.clear();
        loadUserConfig();
    }
}
