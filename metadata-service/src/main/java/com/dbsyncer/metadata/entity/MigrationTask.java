package com.dbsyncer.metadata.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.Type;
import com.dbsyncer.metadata.entity.converter.DatabaseTypeConverter;
import com.dbsyncer.metadata.entity.converter.TaskStatusConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Entity representing a database migration task.
 */
@Entity
@Table(name = "migration_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "task_name", nullable = false, unique = true)
    private String taskName;

    @Column(name = "description")
    private String description;

    // Source database configuration
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "database_type")
    private DatabaseType sourceType;

    @NotBlank
    @Column(name = "source_host", nullable = false)
    private String sourceHost;

    @Positive
    @Column(name = "source_port", nullable = false)
    private Integer sourcePort;

    @NotBlank
    @Column(name = "source_database", nullable = false)
    private String sourceDatabase;

    @NotBlank
    @Column(name = "source_username", nullable = false)
    private String sourceUsername;

    @NotBlank
    @Column(name = "source_password", nullable = false)
    private String sourcePassword;

    @Type(JsonBinaryType.class)
    @Column(name = "source_properties", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> sourceProperties = new HashMap<>();

    // Target database configuration
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, columnDefinition = "database_type")
    private DatabaseType targetType;

    @NotBlank
    @Column(name = "target_host", nullable = false)
    private String targetHost;

    @Positive
    @Column(name = "target_port", nullable = false)
    private Integer targetPort;

    @NotBlank
    @Column(name = "target_database", nullable = false)
    private String targetDatabase;

    @NotBlank
    @Column(name = "target_username", nullable = false)
    private String targetUsername;

    @NotBlank
    @Column(name = "target_password", nullable = false)
    private String targetPassword;

    @Type(JsonBinaryType.class)
    @Column(name = "target_properties", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> targetProperties = new HashMap<>();

    // Table selection
    @Type(ListArrayType.class)
    @Column(name = "include_tables", columnDefinition = "text[]")
    private List<String> includeTables;

    @Type(ListArrayType.class)
    @Column(name = "exclude_tables", columnDefinition = "text[]")
    private List<String> excludeTables;

    // Task configuration
    @Column(name = "snapshot_mode")
    @Builder.Default
    private String snapshotMode = "initial";

    @Column(name = "batch_size")
    @Builder.Default
    private Integer batchSize = 10000;

    @Column(name = "max_queue_size")
    @Builder.Default
    private Integer maxQueueSize = 8192;

    @Column(name = "poll_interval_ms")
    @Builder.Default
    private Integer pollIntervalMs = 1000;

    @Column(name = "incremental_snapshot")
    @Builder.Default
    private Boolean incrementalSnapshot = false;

    @Column(name = "snapshot_chunk_size")
    @Builder.Default
    private Integer snapshotChunkSize = 10000;

    @Column(name = "parallel_tables")
    @Builder.Default
    private Integer parallelTables = 1;

    // Task status
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "task_status")
    @Builder.Default
    private TaskStatus status = TaskStatus.CREATED;

    @Column(name = "error_message")
    private String errorMessage;

    // Statistics
    @Column(name = "total_tables")
    @Builder.Default
    private Integer totalTables = 0;

    @Column(name = "completed_tables")
    @Builder.Default
    private Integer completedTables = 0;

    @Column(name = "total_records")
    @Builder.Default
    private Long totalRecords = 0L;

    @Column(name = "processed_records")
    @Builder.Default
    private Long processedRecords = 0L;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // Metadata
    @Column(name = "created_by")
    private String createdBy;

    @Type(JsonBinaryType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // Relationships
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TableProgress> tableProgresses = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConnectorConfig> connectorConfigs = new ArrayList<>();

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
     * Calculate the overall progress percentage.
     */
    public double getProgressPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (processedRecords * 100.0) / totalRecords;
    }

    /**
     * Check if the task can be started.
     */
    public boolean canStart() {
        return status == TaskStatus.CREATED || status == TaskStatus.STOPPED || status == TaskStatus.FAILED;
    }

    /**
     * Check if the task can be paused.
     */
    public boolean canPause() {
        return status == TaskStatus.RUNNING;
    }

    /**
     * Check if the task can be resumed.
     */
    public boolean canResume() {
        return status == TaskStatus.PAUSED;
    }

    /**
     * Check if the task can be stopped.
     */
    public boolean canStop() {
        return status == TaskStatus.RUNNING || status == TaskStatus.PAUSED;
    }
}
