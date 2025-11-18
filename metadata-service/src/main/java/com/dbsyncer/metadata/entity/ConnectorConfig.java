package com.dbsyncer.metadata.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;
import com.dbsyncer.metadata.entity.converter.ConnectorTypeConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a Kafka Connect connector configuration.
 */
@Entity
@Table(name = "connector_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private MigrationTask task;

    @NotBlank
    @Column(name = "connector_name", nullable = false, unique = true)
    private String connectorName;

    @NotBlank
    @Column(name = "connector_class", nullable = false)
    private String connectorClass;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, columnDefinition = "connector_type")
    private ConnectorType connectorType;

    @NotNull
    @Type(JsonBinaryType.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> config;

    @Column(name = "deployed")
    @Builder.Default
    private Boolean deployed = false;

    @Column(name = "deployed_at")
    private OffsetDateTime deployedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "tasks_count")
    @Builder.Default
    private Integer tasksCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

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
     * Check if this is a source connector.
     */
    public boolean isSourceConnector() {
        return connectorType == ConnectorType.SOURCE;
    }

    /**
     * Check if this is a sink connector.
     */
    public boolean isSinkConnector() {
        return connectorType == ConnectorType.SINK;
    }
}
