package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.entity.AlertChannel;
import com.dbsyncer.metadata.entity.AlertEvent;
import com.dbsyncer.metadata.entity.AlertRule;
import com.dbsyncer.metadata.entity.AlertSeverity;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.repository.AlertEventRepository;
import com.dbsyncer.metadata.repository.AlertRuleRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
     * Lightweight JSON encoder and HTTP client for webhook delivery.
     * These are created locally to avoid additional Spring configuration.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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
            switch (channel) {
                case EMAIL -> {
                    // For MVP, email delivery is represented as a structured log entry.
                    log.info("Alert[{}] EMAIL to={} task={} rule={} message={}",
                            event.getId(), rule.getEmailRecipients(), task.getId(), rule.getName(), message);
                    event.setStatus("SENT");
                    event.setSentAt(OffsetDateTime.now());
                }
                case WEBHOOK -> {
                    deliverWebhook(rule.getWebhookUrl(), payload, event, task);
                }
                default -> {
                    log.warn("Unsupported alert channel {} for event {}", channel, event.getId());
                    event.setStatus("FAILED");
                    event.setErrorMessage("Unsupported channel: " + channel);
                }
            }
        } catch (Exception e) {
            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            log.warn("Failed to deliver alert {} via {}: {}", event.getId(), channel, e.getMessage());
        }

        alertEventRepository.save(event);
    }

    private void deliverWebhook(String webhookUrl,
                                Map<String, Object> payload,
                                AlertEvent event,
                                MigrationTask task) throws JsonProcessingException {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must not be blank");
        }

        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Alert[{}] WEBHOOK delivered to={} task={} status={}",
                        event.getId(), webhookUrl, task.getId(), statusCode);
                event.setStatus("SENT");
                event.setSentAt(OffsetDateTime.now());
            } else {
                String error = "Unexpected HTTP status " + statusCode + " from webhook";
                log.warn("Alert[{}] WEBHOOK delivery failed: {}", event.getId(), error);
                event.setStatus("FAILED");
                event.setErrorMessage(error);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            String error = "Webhook delivery interrupted: " + ie.getMessage();
            log.warn("Alert[{}] WEBHOOK delivery interrupted", event.getId(), ie);
            event.setStatus("FAILED");
            event.setErrorMessage(error);
        } catch (Exception e) {
            String error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Alert[{}] WEBHOOK delivery error: {}", event.getId(), error);
            event.setStatus("FAILED");
            event.setErrorMessage(error);
        }
    }
}
