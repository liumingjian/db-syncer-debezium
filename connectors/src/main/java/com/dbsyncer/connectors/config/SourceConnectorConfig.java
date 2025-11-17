package com.dbsyncer.connectors.config;

import java.util.Map;

/**
 * Interface for source connector configuration generators.
 */
public interface SourceConnectorConfig {

    /**
     * Generate the connector configuration as a map.
     *
     * @return Map of configuration properties
     */
    Map<String, String> toConfigMap();

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    void validate();

    /**
     * Get the connector class name.
     *
     * @return Fully qualified connector class name
     */
    String getConnectorClass();
}
