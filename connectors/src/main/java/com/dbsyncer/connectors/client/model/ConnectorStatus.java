package com.dbsyncer.connectors.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents connector status from Kafka Connect REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorStatus {

    private String name;

    @JsonProperty("connector")
    private ConnectorState connector;

    @JsonProperty("tasks")
    private List<TaskState> tasks;

    private String type;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectorState {
        private String state;

        @JsonProperty("worker_id")
        private String workerId;

        private String trace;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskState {
        private int id;
        private String state;

        @JsonProperty("worker_id")
        private String workerId;

        private String trace;
    }

    /**
     * Check if the connector is running.
     */
    public boolean isRunning() {
        if (connector == null || !"RUNNING".equals(connector.getState())) {
            return false;
        }
        if (tasks == null || tasks.isEmpty()) {
            return false;
        }
        return tasks.stream().allMatch(t -> "RUNNING".equals(t.getState()));
    }

    /**
     * Check if the connector is paused.
     */
    public boolean isPaused() {
        return connector != null && "PAUSED".equals(connector.getState());
    }

    /**
     * Check if the connector has failed.
     */
    public boolean isFailed() {
        if (connector != null && "FAILED".equals(connector.getState())) {
            return true;
        }
        if (tasks != null) {
            return tasks.stream().anyMatch(t -> "FAILED".equals(t.getState()));
        }
        return false;
    }

    /**
     * Get the error trace if any task has failed.
     */
    public String getErrorTrace() {
        if (connector != null && "FAILED".equals(connector.getState()) && connector.getTrace() != null) {
            return connector.getTrace();
        }
        if (tasks != null) {
            return tasks.stream()
                    .filter(t -> "FAILED".equals(t.getState()) && t.getTrace() != null)
                    .map(TaskState::getTrace)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
