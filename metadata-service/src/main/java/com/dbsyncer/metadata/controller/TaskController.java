package com.dbsyncer.metadata.controller;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.dto.TaskUpdateRequest;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.service.TaskExecutionService;
import com.dbsyncer.metadata.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing migration tasks.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final TaskExecutionService executionService;

    /**
     * Create a new migration task.
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        log.info("REST request to create task: {}", request.getTaskName());
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a task by ID.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        log.debug("REST request to get task: {}", taskId);
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a task by name.
     */
    @GetMapping("/name/{taskName}")
    public ResponseEntity<TaskResponse> getTaskByName(@PathVariable String taskName) {
        log.debug("REST request to get task by name: {}", taskName);
        TaskResponse response = taskService.getTaskByName(taskName);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all tasks with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to get all tasks");
        Page<TaskResponse> page = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get tasks by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(@PathVariable TaskStatus status) {
        log.debug("REST request to get tasks by status: {}", status);
        List<TaskResponse> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Search tasks by name pattern.
     */
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasks(@RequestParam String query) {
        log.debug("REST request to search tasks: {}", query);
        List<TaskResponse> tasks = taskService.searchTasks(query);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update a task.
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        log.info("REST request to update task: {}", taskId);
        TaskResponse response = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a task.
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        log.info("REST request to delete task: {}", taskId);
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Start a task.
     */
    @PostMapping("/{taskId}/start")
    public ResponseEntity<TaskResponse> startTask(@PathVariable UUID taskId) {
        log.info("REST request to start task: {}", taskId);
        TaskResponse response = executionService.startTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Stop a task.
     */
    @PostMapping("/{taskId}/stop")
    public ResponseEntity<TaskResponse> stopTask(@PathVariable UUID taskId) {
        log.info("REST request to stop task: {}", taskId);
        TaskResponse response = executionService.stopTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Pause a task.
     */
    @PostMapping("/{taskId}/pause")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable UUID taskId) {
        log.info("REST request to pause task: {}", taskId);
        TaskResponse response = executionService.pauseTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resume a task.
     */
    @PostMapping("/{taskId}/resume")
    public ResponseEntity<TaskResponse> resumeTask(@PathVariable UUID taskId) {
        log.info("REST request to resume task: {}", taskId);
        TaskResponse response = executionService.resumeTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get task statistics summary.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<TaskStatus, Long>> getTaskStatistics() {
        log.debug("REST request to get task statistics");
        Map<TaskStatus, Long> stats = taskService.getTaskStatisticsSummary();
        return ResponseEntity.ok(stats);
    }
}
