package com.dbsyncer.metadata.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing task execution logs.
 */
@Entity
@Table(name = "task_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private MigrationTask task;

    @NotBlank
    @Column(name = "log_level", nullable = false, length = 10)
    private String logLevel;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Type(JsonBinaryType.class)
    @Column(name = "context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Column(name = "source_component", length = 100)
    private String sourceComponent;

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        if (loggedAt == null) {
            loggedAt = OffsetDateTime.now();
        }
    }
}

