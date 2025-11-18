package com.dbsyncer.metadata.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing an alert rule.
 */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "on_task_failure", nullable = false)
    @Builder.Default
    private Boolean onTaskFailure = Boolean.TRUE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, columnDefinition = "alert_severity")
    @Builder.Default
    private AlertSeverity minSeverity = AlertSeverity.ERROR;

    @Column(name = "email_recipients")
    private String emailRecipients;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "description")
    private String description;

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
}

