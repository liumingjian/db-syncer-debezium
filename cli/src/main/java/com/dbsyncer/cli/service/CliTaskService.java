package com.dbsyncer.cli.service;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.service.ProgressTrackingService;
import com.dbsyncer.metadata.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CliTaskService {

    private final TaskService taskService;
    private final ProgressTrackingService progressService;

    public CliTaskService(TaskService taskService, ProgressTrackingService progressService) {
        this.taskService = taskService;
        this.progressService = progressService;
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
        return taskService.updateTaskStatus(taskId, TaskStatus.STARTING);
    }

    public TaskResponse stopTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return taskService.updateTaskStatus(taskId, TaskStatus.STOPPED);
    }

    public TaskResponse pauseTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return taskService.updateTaskStatus(taskId, TaskStatus.PAUSED);
    }

    public TaskResponse resumeTask(String identifier) {
        UUID taskId = resolveTaskId(identifier);
        return taskService.updateTaskStatus(taskId, TaskStatus.RUNNING);
    }

    public List<TableProgress> getTableProgress(UUID taskId) {
        return progressService.getTableProgress(taskId);
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
