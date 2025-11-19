package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.service.CheckpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for checkpoint and resume operations.
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/checkpoint")
@RequiredArgsConstructor
public class CheckpointController {

    private final CheckpointService checkpointService;

    @GetMapping("/can-resume")
    public ResponseEntity<CanResumeResponse> canResume(@PathVariable UUID taskId) {
        boolean canResume = checkpointService.canResume(taskId);
        return ResponseEntity.ok(new CanResumeResponse(taskId, canResume));
    }

    @GetMapping
    public ResponseEntity<CheckpointService.CheckpointState> getCheckpoint(@PathVariable UUID taskId) {
        CheckpointService.CheckpointState state = checkpointService.getCheckpoint(taskId);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/validate-connectors")
    public ResponseEntity<CheckpointService.ConnectorValidationResult> validateConnectors(
            @PathVariable UUID taskId) {
        CheckpointService.ConnectorValidationResult result = checkpointService.validateConnectors(taskId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/incomplete-tables")
    public ResponseEntity<List<TableProgress>> getIncompleteTables(@PathVariable UUID taskId) {
        List<TableProgress> tables = checkpointService.getIncompleteTables(taskId);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/failed-tables")
    public ResponseEntity<List<TableProgress>> getFailedTables(@PathVariable UUID taskId) {
        List<TableProgress> tables = checkpointService.getFailedTables(taskId);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/remaining-rows")
    public ResponseEntity<RemainingRowsResponse> getRemainingRows(@PathVariable UUID taskId) {
        Long remainingRows = checkpointService.getRemainingRows(taskId);
        return ResponseEntity.ok(new RemainingRowsResponse(taskId, remainingRows));
    }

    @PostMapping
    public ResponseEntity<Void> createCheckpoint(
            @PathVariable UUID taskId,
            @RequestParam(required = false, defaultValue = "Manual checkpoint") String reason) {
        checkpointService.createCheckpoint(taskId, reason);
        return ResponseEntity.ok().build();
    }

    // DTOs
    public record CanResumeResponse(UUID taskId, boolean canResume) {}
    public record RemainingRowsResponse(UUID taskId, Long remainingRows) {}
}
