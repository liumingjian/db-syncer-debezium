package com.dbsyncer.metadata.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing the progress of migrating a single table.
 */
@Entity
@Table(name = "table_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "source_schema", "source_table"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private MigrationTask task;

    @Column(name = "source_schema")
    private String sourceSchema;

    @NotBlank
    @Column(name = "source_table", nullable = false)
    private String sourceTable;

    @Column(name = "target_schema")
    private String targetSchema;

    @NotBlank
    @Column(name = "target_table", nullable = false)
    private String targetTable;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "progress_status")
    @Builder.Default
    private ProgressStatus status = ProgressStatus.PENDING;

    // Snapshot progress
    @Column(name = "estimated_rows")
    private Long estimatedRows;

    @Column(name = "snapshot_rows_read")
    @Builder.Default
    private Long snapshotRowsRead = 0L;

    @Column(name = "snapshot_rows_written")
    @Builder.Default
    private Long snapshotRowsWritten = 0L;

    @Column(name = "snapshot_completed")
    @Builder.Default
    private Boolean snapshotCompleted = false;

    @Column(name = "snapshot_started_at")
    private OffsetDateTime snapshotStartedAt;

    @Column(name = "snapshot_completed_at")
    private OffsetDateTime snapshotCompletedAt;

    // Streaming progress
    @Column(name = "streaming_events_processed")
    @Builder.Default
    private Long streamingEventsProcessed = 0L;

    @Column(name = "last_event_timestamp")
    private OffsetDateTime lastEventTimestamp;

    @Column(name = "current_lag_ms")
    @Builder.Default
    private Long currentLagMs = 0L;

    // Error tracking
    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Get the full source table name including schema.
     */
    public String getFullSourceTableName() {
        if (sourceSchema != null && !sourceSchema.isEmpty()) {
            return sourceSchema + "." + sourceTable;
        }
        return sourceTable;
    }

    /**
     * Get the full target table name including schema.
     */
    public String getFullTargetTableName() {
        if (targetSchema != null && !targetSchema.isEmpty()) {
            return targetSchema + "." + targetTable;
        }
        return targetTable;
    }

    /**
     * Calculate snapshot progress percentage.
     */
    public double getSnapshotProgressPercentage() {
        if (estimatedRows == null || estimatedRows == 0) {
            return 0.0;
        }
        return (snapshotRowsRead * 100.0) / estimatedRows;
    }

    /**
     * Get total rows processed (snapshot + streaming).
     */
    public long getTotalRowsProcessed() {
        return snapshotRowsWritten + streamingEventsProcessed;
    }
}
