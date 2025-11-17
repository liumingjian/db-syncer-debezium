package com.dbsyncer.metadata.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for updating an existing migration task.
 */
@Data
public class TaskUpdateRequest {
    private String description;

    // Table selection (can be updated before starting)
    private List<String> includeTables;
    private List<String> excludeTables;

    // Configuration (can be updated before starting)
    private String snapshotMode;
    private Integer batchSize;
    private Integer maxQueueSize;
    private Integer pollIntervalMs;

    // Source properties (can be updated before starting)
    private Map<String, Object> sourceProperties;

    // Target properties (can be updated before starting)
    private Map<String, Object> targetProperties;

    // Metadata
    private List<String> tags;
}
