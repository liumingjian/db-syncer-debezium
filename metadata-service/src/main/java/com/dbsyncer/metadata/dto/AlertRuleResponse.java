package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.AlertRule;
import com.dbsyncer.metadata.entity.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponse {

    private UUID id;
    private String name;
    private Boolean enabled;
    private Boolean onTaskFailure;
    private AlertSeverity minSeverity;
    private String emailRecipients;
    private String webhookUrl;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AlertRuleResponse fromEntity(AlertRule rule) {
        return AlertRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .enabled(rule.getEnabled())
                .onTaskFailure(rule.getOnTaskFailure())
                .minSeverity(rule.getMinSeverity())
                .emailRecipients(rule.getEmailRecipients())
                .webhookUrl(rule.getWebhookUrl())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

