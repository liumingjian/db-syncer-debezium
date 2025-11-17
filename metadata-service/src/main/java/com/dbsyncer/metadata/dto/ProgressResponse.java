package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.entity.TableProgress;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for table progress response.
 */
@Data
public class ProgressResponse {
    private UUID id;
    private UUID taskId;
    private String sourceSchema;
    private String sourceTable;
    private String targetSchema;
    private String targetTable;
    private String fullSourceTableName;
    private String fullTargetTableName;

    private ProgressStatus status;

    // Snapshot progress
    private Long estimatedRows;
    private Long snapshotRowsRead;
    private Long snapshotRowsWritten;
    private Boolean snapshotCompleted;
    private Double snapshotProgressPercentage;
    private OffsetDateTime snapshotStartedAt;
    private OffsetDateTime snapshotCompletedAt;

    // Streaming progress
    private Long streamingEventsProcessed;
    private OffsetDateTime lastEventTimestamp;
    private Long currentLagMs;

    // Error tracking
    private Integer errorCount;
    private String lastError;
    private OffsetDateTime lastErrorAt;

    // Total
    private Long totalRowsProcessed;

    // Timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Create a ProgressResponse from a TableProgress entity.
     */
    public static ProgressResponse fromEntity(TableProgress progress) {
        ProgressResponse response = new ProgressResponse();
        response.setId(progress.getId());
        response.setTaskId(progress.getTask().getId());
        response.setSourceSchema(progress.getSourceSchema());
        response.setSourceTable(progress.getSourceTable());
        response.setTargetSchema(progress.getTargetSchema());
        response.setTargetTable(progress.getTargetTable());
        response.setFullSourceTableName(progress.getFullSourceTableName());
        response.setFullTargetTableName(progress.getFullTargetTableName());

        response.setStatus(progress.getStatus());

        response.setEstimatedRows(progress.getEstimatedRows());
        response.setSnapshotRowsRead(progress.getSnapshotRowsRead());
        response.setSnapshotRowsWritten(progress.getSnapshotRowsWritten());
        response.setSnapshotCompleted(progress.getSnapshotCompleted());
        response.setSnapshotProgressPercentage(progress.getSnapshotProgressPercentage());
        response.setSnapshotStartedAt(progress.getSnapshotStartedAt());
        response.setSnapshotCompletedAt(progress.getSnapshotCompletedAt());

        response.setStreamingEventsProcessed(progress.getStreamingEventsProcessed());
        response.setLastEventTimestamp(progress.getLastEventTimestamp());
        response.setCurrentLagMs(progress.getCurrentLagMs());

        response.setErrorCount(progress.getErrorCount());
        response.setLastError(progress.getLastError());
        response.setLastErrorAt(progress.getLastErrorAt());

        response.setTotalRowsProcessed(progress.getTotalRowsProcessed());

        response.setCreatedAt(progress.getCreatedAt());
        response.setUpdatedAt(progress.getUpdatedAt());

        return response;
    }
}
