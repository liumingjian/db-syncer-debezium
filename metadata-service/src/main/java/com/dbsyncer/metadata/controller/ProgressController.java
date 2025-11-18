package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.dto.ProgressResponse;
import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing migration progress.
 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/progress")
@RequiredArgsConstructor
@Slf4j
public class ProgressController {

    private final ProgressTrackingService progressTrackingService;

    /**
     * Get all progress entries for a task.
     */
    @GetMapping
    public ResponseEntity<List<ProgressResponse>> getTaskProgress(@PathVariable UUID taskId) {
        log.debug("REST request to get progress for task: {}", taskId);
        List<ProgressResponse> progress = progressTrackingService.getTaskProgress(taskId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get progress for a specific table.
     */
    @GetMapping("/table")
    public ResponseEntity<ProgressResponse> getTableProgress(
            @PathVariable UUID taskId,
            @RequestParam(required = false) String sourceSchema,
            @RequestParam String sourceTable) {
        log.debug("REST request to get progress for table {}.{}", sourceSchema, sourceTable);
        ProgressResponse progress = progressTrackingService.getTableProgress(taskId, sourceSchema, sourceTable);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get progress summary for a task.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<ProgressStatus, Long>> getProgressSummary(@PathVariable UUID taskId) {
        log.debug("REST request to get progress summary for task: {}", taskId);
        Map<ProgressStatus, Long> summary = progressTrackingService.getProgressSummary(taskId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get total rows processed.
     */
    @GetMapping("/total-rows")
    public ResponseEntity<Long> getTotalRowsProcessed(@PathVariable UUID taskId) {
        log.debug("REST request to get total rows processed for task: {}", taskId);
        Long totalRows = progressTrackingService.getTotalRowsProcessed(taskId);
        return ResponseEntity.ok(totalRows);
    }

    /**
     * Get average lag for streaming tables.
     */
    @GetMapping("/average-lag")
    public ResponseEntity<Double> getAverageLag(@PathVariable UUID taskId) {
        log.debug("REST request to get average lag for task: {}", taskId);
        Double averageLag = progressTrackingService.getAverageLag(taskId);
        return ResponseEntity.ok(averageLag);
    }

    /**
     * Get ETA (seconds) for task completion if it can be estimated, otherwise returns null.
     */
    @GetMapping("/eta")
    public ResponseEntity<Long> getEtaSeconds(@PathVariable UUID taskId) {
        log.debug("REST request to get ETA for task: {}", taskId);
        Long etaSeconds = progressTrackingService.estimateEtaSeconds(taskId);
        return ResponseEntity.ok(etaSeconds);
    }

    /**
     * Get tables with errors.
     */
    @GetMapping("/errors")
    public ResponseEntity<List<ProgressResponse>> getTablesWithErrors(@PathVariable UUID taskId) {
        log.debug("REST request to get tables with errors for task: {}", taskId);
        List<ProgressResponse> tables = progressTrackingService.getTablesWithErrors(taskId);
        return ResponseEntity.ok(tables);
    }
}
