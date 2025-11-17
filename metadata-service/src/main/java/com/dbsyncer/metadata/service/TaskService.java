package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.dto.TaskUpdateRequest;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.exception.InvalidTaskStateException;
import com.dbsyncer.metadata.exception.TaskAlreadyExistsException;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing migration tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final MigrationTaskRepository taskRepository;

    /**
     * Create a new migration task.
     */
    public TaskResponse createTask(TaskCreateRequest request) {
        log.info("Creating task: {}", request.getTaskName());

        if (taskRepository.existsByTaskName(request.getTaskName())) {
            throw new TaskAlreadyExistsException(request.getTaskName());
        }

        MigrationTask task = MigrationTask.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .sourceType(request.getSourceType())
                .sourceHost(request.getSourceHost())
                .sourcePort(request.getSourcePort())
                .sourceDatabase(request.getSourceDatabase())
                .sourceUsername(request.getSourceUsername())
                .sourcePassword(request.getSourcePassword())
                .sourceProperties(request.getSourceProperties() != null ? request.getSourceProperties() : new java.util.HashMap<>())
                .targetType(request.getTargetType())
                .targetHost(request.getTargetHost())
                .targetPort(request.getTargetPort())
                .targetDatabase(request.getTargetDatabase())
                .targetUsername(request.getTargetUsername())
                .targetPassword(request.getTargetPassword())
                .targetProperties(request.getTargetProperties() != null ? request.getTargetProperties() : new java.util.HashMap<>())
                .includeTables(request.getIncludeTables())
                .excludeTables(request.getExcludeTables())
                .snapshotMode(request.getSnapshotMode())
                .batchSize(request.getBatchSize())
                .maxQueueSize(request.getMaxQueueSize())
                .pollIntervalMs(request.getPollIntervalMs())
                .createdBy(request.getCreatedBy())
                .tags(request.getTags() != null ? request.getTags() : new java.util.ArrayList<>())
                .status(TaskStatus.CREATED)
                .build();

        task = taskRepository.save(task);
        log.info("Created task with id: {}", task.getId());

        return TaskResponse.fromEntity(task);
    }

    /**
     * Get a task by ID.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        return TaskResponse.fromEntity(task);
    }

    /**
     * Get a task by name.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskByName(String taskName) {
        MigrationTask task = taskRepository.findByTaskName(taskName)
                .orElseThrow(() -> new TaskNotFoundException(taskName));
        return TaskResponse.fromEntity(task);
    }

    /**
     * Get all tasks with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable)
                .map(TaskResponse::fromEntity);
    }

    /**
     * Get all tasks with pagination (convenience method).
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(int page, int size) {
        return getAllTasks(org.springframework.data.domain.PageRequest.of(page, size));
    }

    /**
     * Get tasks by status.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update a task.
     */
    public TaskResponse updateTask(UUID taskId, TaskUpdateRequest request) {
        log.info("Updating task: {}", taskId);

        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        // Only allow updates if task is not running
        if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.STARTING) {
            throw new InvalidTaskStateException("Cannot update task while it is running");
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getIncludeTables() != null) {
            task.setIncludeTables(request.getIncludeTables());
        }
        if (request.getExcludeTables() != null) {
            task.setExcludeTables(request.getExcludeTables());
        }
        if (request.getSnapshotMode() != null) {
            task.setSnapshotMode(request.getSnapshotMode());
        }
        if (request.getBatchSize() != null) {
            task.setBatchSize(request.getBatchSize());
        }
        if (request.getMaxQueueSize() != null) {
            task.setMaxQueueSize(request.getMaxQueueSize());
        }
        if (request.getPollIntervalMs() != null) {
            task.setPollIntervalMs(request.getPollIntervalMs());
        }
        if (request.getSourceProperties() != null) {
            task.setSourceProperties(request.getSourceProperties());
        }
        if (request.getTargetProperties() != null) {
            task.setTargetProperties(request.getTargetProperties());
        }
        if (request.getTags() != null) {
            task.setTags(request.getTags());
        }

        task = taskRepository.save(task);
        log.info("Updated task: {}", taskId);

        return TaskResponse.fromEntity(task);
    }

    /**
     * Delete a task.
     */
    public void deleteTask(UUID taskId) {
        log.info("Deleting task: {}", taskId);

        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        // Only allow deletion if task is not active
        if (task.getStatus() == TaskStatus.RUNNING ||
            task.getStatus() == TaskStatus.STARTING ||
            task.getStatus() == TaskStatus.PAUSED) {
            throw new InvalidTaskStateException("Cannot delete an active task. Stop the task first.");
        }

        taskRepository.delete(task);
        log.info("Deleted task: {}", taskId);
    }

    /**
     * Update task status.
     */
    public TaskResponse updateTaskStatus(UUID taskId, TaskStatus newStatus) {
        log.info("Updating task {} status to {}", taskId, newStatus);

        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        validateStatusTransition(task, newStatus);

        task.setStatus(newStatus);

        if (newStatus == TaskStatus.RUNNING && task.getStartedAt() == null) {
            task.setStartedAt(OffsetDateTime.now());
        }
        if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED || newStatus == TaskStatus.STOPPED) {
            task.setCompletedAt(OffsetDateTime.now());
        }

        task = taskRepository.save(task);
        log.info("Task {} status updated to {}", taskId, newStatus);

        return TaskResponse.fromEntity(task);
    }

    /**
     * Update task error message.
     */
    public TaskResponse updateTaskError(UUID taskId, String errorMessage) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setErrorMessage(errorMessage);
        task.setStatus(TaskStatus.FAILED);
        task.setCompletedAt(OffsetDateTime.now());

        task = taskRepository.save(task);
        return TaskResponse.fromEntity(task);
    }

    /**
     * Update task statistics.
     */
    public TaskResponse updateTaskStatistics(UUID taskId, Integer totalTables, Integer completedTables,
                                             Long totalRecords, Long processedRecords) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (totalTables != null) {
            task.setTotalTables(totalTables);
        }
        if (completedTables != null) {
            task.setCompletedTables(completedTables);
        }
        if (totalRecords != null) {
            task.setTotalRecords(totalRecords);
        }
        if (processedRecords != null) {
            task.setProcessedRecords(processedRecords);
        }

        task = taskRepository.save(task);
        return TaskResponse.fromEntity(task);
    }

    /**
     * Get task statistics summary.
     */
    @Transactional(readOnly = true)
    public java.util.Map<TaskStatus, Long> getTaskStatisticsSummary() {
        List<Object[]> summary = taskRepository.getTaskStatusSummary();
        return summary.stream()
                .collect(Collectors.toMap(
                        row -> (TaskStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    /**
     * Search tasks by name pattern.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTasks(String pattern) {
        return taskRepository.searchByTaskName(pattern).stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(MigrationTask task, TaskStatus newStatus) {
        TaskStatus currentStatus = task.getStatus();

        switch (newStatus) {
            case STARTING:
                if (!task.canStart()) {
                    throw new InvalidTaskStateException("start", currentStatus);
                }
                break;
            case PAUSED:
                if (!task.canPause()) {
                    throw new InvalidTaskStateException("pause", currentStatus);
                }
                break;
            case RUNNING:
                if (currentStatus != TaskStatus.STARTING && currentStatus != TaskStatus.PAUSED) {
                    throw new InvalidTaskStateException("resume", currentStatus);
                }
                break;
            case STOPPING:
                if (!task.canStop()) {
                    throw new InvalidTaskStateException("stop", currentStatus);
                }
                break;
            default:
                // Allow other transitions
                break;
        }
    }
}
