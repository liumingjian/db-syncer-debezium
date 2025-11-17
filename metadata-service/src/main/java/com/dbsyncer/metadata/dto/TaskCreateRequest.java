package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for creating a new migration task.
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "Task name is required")
    private String taskName;

    private String description;

    // Source database
    @NotNull(message = "Source type is required")
    private DatabaseType sourceType;

    @NotBlank(message = "Source host is required")
    private String sourceHost;

    @NotNull(message = "Source port is required")
    @Positive(message = "Source port must be positive")
    private Integer sourcePort;

    @NotBlank(message = "Source database is required")
    private String sourceDatabase;

    @NotBlank(message = "Source username is required")
    private String sourceUsername;

    @NotBlank(message = "Source password is required")
    private String sourcePassword;

    private Map<String, Object> sourceProperties;

    // Target database
    @NotNull(message = "Target type is required")
    private DatabaseType targetType;

    @NotBlank(message = "Target host is required")
    private String targetHost;

    @NotNull(message = "Target port is required")
    @Positive(message = "Target port must be positive")
    private Integer targetPort;

    @NotBlank(message = "Target database is required")
    private String targetDatabase;

    @NotBlank(message = "Target username is required")
    private String targetUsername;

    @NotBlank(message = "Target password is required")
    private String targetPassword;

    private Map<String, Object> targetProperties;

    // Table selection
    private List<String> includeTables;
    private List<String> excludeTables;

    // Task configuration
    private String snapshotMode = "initial";
    private Integer batchSize = 10000;
    private Integer maxQueueSize = 8192;
    private Integer pollIntervalMs = 1000;

    // Metadata
    private String createdBy;
    private List<String> tags;
}
