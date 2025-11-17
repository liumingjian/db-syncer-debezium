package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for task response.
 */
@Data
public class TaskResponse {
    private UUID id;
    private String taskName;
    private String description;

    // Source info (without sensitive data)
    private DatabaseType sourceType;
    private String sourceHost;
    private Integer sourcePort;
    private String sourceDatabase;
    private String sourceUsername;

    // Target info (without sensitive data)
    private DatabaseType targetType;
    private String targetHost;
    private Integer targetPort;
    private String targetDatabase;
    private String targetUsername;

    // Table selection
    private List<String> includeTables;
    private List<String> excludeTables;

    // Configuration
    private String snapshotMode;
    private Integer batchSize;
    private Integer maxQueueSize;
    private Integer pollIntervalMs;

    // Status
    private TaskStatus status;
    private String errorMessage;

    // Statistics
    private Integer totalTables;
    private Integer completedTables;
    private Long totalRecords;
    private Long processedRecords;
    private Double progressPercentage;

    // Timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    // Metadata
    private String createdBy;
    private List<String> tags;

    /**
     * Create a TaskResponse from a MigrationTask entity.
     */
    public static TaskResponse fromEntity(MigrationTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setDescription(task.getDescription());

        response.setSourceType(task.getSourceType());
        response.setSourceHost(task.getSourceHost());
        response.setSourcePort(task.getSourcePort());
        response.setSourceDatabase(task.getSourceDatabase());
        response.setSourceUsername(task.getSourceUsername());

        response.setTargetType(task.getTargetType());
        response.setTargetHost(task.getTargetHost());
        response.setTargetPort(task.getTargetPort());
        response.setTargetDatabase(task.getTargetDatabase());
        response.setTargetUsername(task.getTargetUsername());

        response.setIncludeTables(task.getIncludeTables());
        response.setExcludeTables(task.getExcludeTables());

        response.setSnapshotMode(task.getSnapshotMode());
        response.setBatchSize(task.getBatchSize());
        response.setMaxQueueSize(task.getMaxQueueSize());
        response.setPollIntervalMs(task.getPollIntervalMs());

        response.setStatus(task.getStatus());
        response.setErrorMessage(task.getErrorMessage());

        response.setTotalTables(task.getTotalTables());
        response.setCompletedTables(task.getCompletedTables());
        response.setTotalRecords(task.getTotalRecords());
        response.setProcessedRecords(task.getProcessedRecords());
        response.setProgressPercentage(task.getProgressPercentage());

        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setStartedAt(task.getStartedAt());
        response.setCompletedAt(task.getCompletedAt());

        response.setCreatedBy(task.getCreatedBy());
        response.setTags(task.getTags());

        return response;
    }
}
