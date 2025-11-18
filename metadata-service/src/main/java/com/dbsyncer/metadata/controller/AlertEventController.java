package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.entity.AlertEvent;
import com.dbsyncer.metadata.repository.AlertEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for querying alert events (history).
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertEventController {

    private final AlertEventRepository alertEventRepository;

    @GetMapping
    public ResponseEntity<List<AlertEvent>> getTaskAlerts(@PathVariable UUID taskId,
                                                          @RequestParam(name = "limit", defaultValue = "100") int limit) {
        List<AlertEvent> events = alertEventRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        if (events.size() > limit) {
            events = events.subList(0, limit);
        }
        return ResponseEntity.ok(events);
    }
}

