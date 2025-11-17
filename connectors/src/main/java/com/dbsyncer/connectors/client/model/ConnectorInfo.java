package com.dbsyncer.connectors.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents connector information from Kafka Connect REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorInfo {

    private String name;

    @JsonProperty("config")
    private Map<String, String> config;

    @JsonProperty("tasks")
    private TaskInfo[] tasks;

    private String type;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskInfo {
        private String connector;
        private int task;
    }
}
