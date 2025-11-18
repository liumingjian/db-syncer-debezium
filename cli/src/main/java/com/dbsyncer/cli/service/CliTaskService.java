package com.dbsyncer.cli.service;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.entity.TaskLog;
import com.dbsyncer.metadata.service.ProgressTrackingService;
import com.dbsyncer.metadata.service.TaskExecutionService;
import com.dbsyncer.metadata.service.TaskService;
import com.dbsyncer.metadata.repository.TaskLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CliTaskService {

    private final TaskService taskService;
    private final TaskExecutionService executionService;
    private final ProgressTrackingService progressService;
    private final TaskLogRepository taskLogRepository;

    public CliTaskService(TaskService taskService, ProgressTrackingService progressService,
                          TaskExecutionService executionService, TaskLogRepository taskLogRepository) {
        this.taskService = taskService;
        this.progressService = progressService;
        this.executionService = executionService;
        this.taskLogRepository = taskLogRepository;
    }

    public TaskResponse createTask(TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    public TaskResponse getTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return taskService.getTask(taskId);
    }

    public List<TaskResponse> getAllTasks(int page, int size) {
        return taskService.getAllTasks(page, size).getContent();
    }

    public List<TaskResponse> getTasksByStatus(TaskStatus status) {
        return taskService.getTasksByStatus(status);
    }

    public void deleteTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        taskService.deleteTask(taskId);
    }

    public TaskResponse startTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return executionService.startTask(taskId);
    }

    public TaskResponse stopTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return executionService.stopTask(taskId);
    }

    public TaskResponse pauseTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return executionService.pauseTask(taskId);
    }

    public TaskResponse resumeTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return executionService.resumeTask(taskId);
    }

    public List<TableProgress> getTableProgress(UUID taskId) {
        return progressService.getTableProgress(taskId);
    }

    public Long getTotalRowsProcessed(UUID taskId) {
        return progressService.getTotalRowsProcessed(taskId);
    }

    public Long getTotalEstimatedRows(UUID taskId) {
        return progressService.getTotalEstimatedRows(taskId);
    }

    public Double getAverageLag(UUID taskId) {
        return progressService.getAverageLag(taskId);
    }

    public Long estimateEtaSeconds(UUID taskId) {
        return progressService.estimateEtaSeconds(taskId);
    }

    public java.util.List<TaskLog> getTaskLogs(UUID taskId, String level, int limit) {
        java.util.List<TaskLog> logs;
        if (level != null && !level.isBlank()) {
            logs = taskLogRepository.findByTaskIdAndLogLevelOrderByLoggedAtDesc(taskId, level.toUpperCase());
        } else {
            logs = taskLogRepository.findByTaskIdOrderByLoggedAtDesc(taskId);
        }
        if (logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    private UUID resolveTaskId(String identifier) {
        try {
            return UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            TaskResponse task = taskService.getTaskByName(identifier);
            return task.getId();
        }
    }
}
