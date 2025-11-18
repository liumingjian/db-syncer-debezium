package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.dto.AlertRuleRequest;
import com.dbsyncer.metadata.dto.AlertRuleResponse;
import com.dbsyncer.metadata.entity.AlertRule;
import com.dbsyncer.metadata.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing alert rules.
 */
@RestController
@RequestMapping("/api/v1/alerts/rules")
@RequiredArgsConstructor
@Slf4j
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> getRules() {
        List<AlertRuleResponse> body = alertRuleRepository.findAll().stream()
                .map(AlertRuleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> createRule(@RequestBody AlertRuleRequest request) {
        AlertRule rule = AlertRule.builder()
                .name(request.getName())
                .enabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE)
                .onTaskFailure(request.getOnTaskFailure() != null ? request.getOnTaskFailure() : Boolean.TRUE)
                .minSeverity(request.getMinSeverity() != null ? request.getMinSeverity() : com.dbsyncer.metadata.entity.AlertSeverity.ERROR)
                .emailRecipients(request.getEmailRecipients())
                .webhookUrl(request.getWebhookUrl())
                .description(request.getDescription())
                .build();
        rule = alertRuleRepository.save(rule);
        return ResponseEntity
                .created(URI.create("/api/v1/alerts/rules/" + rule.getId()))
                .body(AlertRuleResponse.fromEntity(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> updateRule(@PathVariable UUID id,
                                                        @RequestBody AlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found: " + id));

        if (request.getName() != null) {
            rule.setName(request.getName());
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getOnTaskFailure() != null) {
            rule.setOnTaskFailure(request.getOnTaskFailure());
        }
        if (request.getMinSeverity() != null) {
            rule.setMinSeverity(request.getMinSeverity());
        }
        if (request.getEmailRecipients() != null) {
            rule.setEmailRecipients(request.getEmailRecipients());
        }
        if (request.getWebhookUrl() != null) {
            rule.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getDescription() != null) {
            rule.setDescription(request.getDescription());
        }

        rule = alertRuleRepository.save(rule);
        return ResponseEntity.ok(AlertRuleResponse.fromEntity(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        if (alertRuleRepository.existsById(id)) {
            alertRuleRepository.deleteById(id);
        }
        return ResponseEntity.noContent().build();
    }
}

