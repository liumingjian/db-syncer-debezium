package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.exception.InvalidTaskStateException;
import com.dbsyncer.metadata.exception.TaskAlreadyExistsException;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private MigrationTaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private TaskCreateRequest createRequest;
    private MigrationTask savedTask;

    @BeforeEach
    void setUp() {
        createRequest = new TaskCreateRequest();
        createRequest.setTaskName("test-task");
        createRequest.setDescription("Test task");
        createRequest.setSourceType(DatabaseType.MYSQL);
        createRequest.setSourceHost("localhost");
        createRequest.setSourcePort(3306);
        createRequest.setSourceDatabase("sourcedb");
        createRequest.setSourceUsername("root");
        createRequest.setSourcePassword("password");
        createRequest.setTargetType(DatabaseType.POSTGRESQL);
        createRequest.setTargetHost("localhost");
        createRequest.setTargetPort(5432);
        createRequest.setTargetDatabase("targetdb");
        createRequest.setTargetUsername("postgres");
        createRequest.setTargetPassword("password");

        savedTask = MigrationTask.builder()
                .id(UUID.randomUUID())
                .taskName("test-task")
                .description("Test task")
                .sourceType(DatabaseType.MYSQL)
                .sourceHost("localhost")
                .sourcePort(3306)
                .sourceDatabase("sourcedb")
                .sourceUsername("root")
                .sourcePassword("password")
                .targetType(DatabaseType.POSTGRESQL)
                .targetHost("localhost")
                .targetPort(5432)
                .targetDatabase("targetdb")
                .targetUsername("postgres")
                .targetPassword("password")
                .status(TaskStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("Should create task successfully")
    void shouldCreateTaskSuccessfully() {
        when(taskRepository.existsByTaskName("test-task")).thenReturn(false);
        when(taskRepository.save(any(MigrationTask.class))).thenReturn(savedTask);

        TaskResponse response = taskService.createTask(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTaskName()).isEqualTo("test-task");
        assertThat(response.getSourceType()).isEqualTo(DatabaseType.MYSQL);
        assertThat(response.getTargetType()).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.CREATED);

        verify(taskRepository).existsByTaskName("test-task");
        verify(taskRepository).save(any(MigrationTask.class));
    }

    @Test
    @DisplayName("Should throw exception when task already exists")
    void shouldThrowExceptionWhenTaskAlreadyExists() {
        when(taskRepository.existsByTaskName("test-task")).thenReturn(true);

        assertThatThrownBy(() -> taskService.createTask(createRequest))
                .isInstanceOf(TaskAlreadyExistsException.class)
                .hasMessageContaining("test-task");

        verify(taskRepository).existsByTaskName("test-task");
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() {
        UUID taskId = savedTask.getId();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        TaskResponse response = taskService.getTask(taskId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getTaskName()).isEqualTo("test-task");
    }

    @Test
    @DisplayName("Should throw exception when task not found")
    void shouldThrowExceptionWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(taskId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining(taskId.toString());
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.STOPPED);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        taskService.deleteTask(taskId);

        verify(taskRepository).delete(savedTask);
    }

    @Test
    @DisplayName("Should not delete running task")
    void shouldNotDeleteRunningTask() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.RUNNING);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        assertThatThrownBy(() -> taskService.deleteTask(taskId))
                .isInstanceOf(InvalidTaskStateException.class)
                .hasMessageContaining("active");

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update task status to STARTING")
    void shouldUpdateTaskStatusToStarting() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.CREATED);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(MigrationTask.class))).thenReturn(savedTask);

        TaskResponse response = taskService.updateTaskStatus(taskId, TaskStatus.STARTING);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(MigrationTask.class));
    }

    @Test
    @DisplayName("Should not start already running task")
    void shouldNotStartAlreadyRunningTask() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.RUNNING);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        assertThatThrownBy(() -> taskService.updateTaskStatus(taskId, TaskStatus.STARTING))
                .isInstanceOf(InvalidTaskStateException.class)
                .hasMessageContaining("start");
    }

    @Test
    @DisplayName("Should pause running task")
    void shouldPauseRunningTask() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.RUNNING);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
        when(taskRepository.save(any(MigrationTask.class))).thenReturn(savedTask);

        TaskResponse response = taskService.updateTaskStatus(taskId, TaskStatus.PAUSED);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(MigrationTask.class));
    }

    @Test
    @DisplayName("Should not pause non-running task")
    void shouldNotPauseNonRunningTask() {
        UUID taskId = savedTask.getId();
        savedTask.setStatus(TaskStatus.CREATED);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

        assertThatThrownBy(() -> taskService.updateTaskStatus(taskId, TaskStatus.PAUSED))
                .isInstanceOf(InvalidTaskStateException.class)
                .hasMessageContaining("pause");
    }
}
