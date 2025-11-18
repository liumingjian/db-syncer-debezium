package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.entity.*;
import com.dbsyncer.metadata.repository.AlertEventRepository;
import com.dbsyncer.metadata.repository.AlertRuleRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for evaluating alert rules and recording alert events.
 * Email/webhook delivery is logged for now and can be wired to external systems later.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final MigrationTaskRepository taskRepository;

    /**
     * Trigger alerts when a task fails.
     */
    @Transactional
    public void onTaskFailure(UUID taskId, String errorMessage, boolean retryable) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElse(null);
        if (task == null) {
            log.warn("Task {} not found while triggering alert", taskId);
            return;
        }

        List<AlertRule> rules = alertRuleRepository.findByEnabledTrue();
        if (rules.isEmpty()) {
            return;
        }

        for (AlertRule rule : rules) {
            if (Boolean.FALSE.equals(rule.getOnTaskFailure())) {
                continue;
            }
            // Currently all task failures are treated as ERROR severity
            if (rule.getMinSeverity() != AlertSeverity.ERROR) {
                continue;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", taskId.toString());
            payload.put("taskName", task.getTaskName());
            payload.put("errorMessage", errorMessage);
            payload.put("retryable", retryable);
            payload.put("status", task.getStatus() != null ? task.getStatus().name() : null);

            if (rule.getEmailRecipients() != null && !rule.getEmailRecipients().isBlank()) {
                createEvent(task, rule, AlertChannel.EMAIL, payload, errorMessage);
            }
            if (rule.getWebhookUrl() != null && !rule.getWebhookUrl().isBlank()) {
                createEvent(task, rule, AlertChannel.WEBHOOK, payload, errorMessage);
            }
        }
    }

    private void createEvent(MigrationTask task,
                             AlertRule rule,
                             AlertChannel channel,
                             Map<String, Object> payload,
                             String message) {
        AlertEvent event = AlertEvent.builder()
                .task(task)
                .rule(rule)
                .severity(AlertSeverity.ERROR)
                .channel(channel)
                .message(message)
                .payload(payload)
                .status("PENDING")
                .build();
        event = alertEventRepository.save(event);

        try {
            // Stub delivery: just log for now. Real implementation can integrate with email/webhook.
            log.info("Alert [{}] channel={} task={} rule={} message={}",
                    event.getId(), channel, task.getId(), rule.getName(), message);
            event.setStatus("SENT");
            event.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            log.warn("Failed to deliver alert {} via {}: {}", event.getId(), channel, e.getMessage());
        }

        alertEventRepository.save(event);
    }
}
