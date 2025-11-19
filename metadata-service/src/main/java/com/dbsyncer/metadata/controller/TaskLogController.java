package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.dto.TaskLogResponse;
import com.dbsyncer.metadata.entity.TaskLog;
import com.dbsyncer.metadata.repository.TaskLogRepository;
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
import java.util.stream.Collectors;

/**
 * REST controller for querying task execution logs.
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/logs")
@RequiredArgsConstructor
@Slf4j
public class TaskLogController {

    private final TaskLogRepository taskLogRepository;

    @GetMapping
    public ResponseEntity<List<TaskLogResponse>> getTaskLogs(
            @PathVariable("taskId") UUID taskId,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "limit", required = false, defaultValue = "100") int limit) {
        log.debug("REST request to get logs for task {} with level={} and limit={}", taskId, level, limit);

        List<TaskLog> logs;
        if (level != null && !level.isBlank()) {
            logs = taskLogRepository.findByTaskIdAndLogLevelOrderByLoggedAtDesc(taskId, level.toUpperCase());
        } else {
            logs = taskLogRepository.findByTaskIdOrderByLoggedAtDesc(taskId);
        }

        List<TaskLogResponse> body = logs.stream()
                .limit(limit)
                .map(TaskLogResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(body);
    }
}
