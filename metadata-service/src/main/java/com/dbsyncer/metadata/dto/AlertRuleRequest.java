package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.AlertSeverity;
import lombok.Data;

/**
 * DTO for creating/updating alert rules.
 */
@Data
public class AlertRuleRequest {

    private String name;
    private Boolean enabled;
    private Boolean onTaskFailure;
    private AlertSeverity minSeverity;
    private String emailRecipients;
    private String webhookUrl;
    private String description;
}

