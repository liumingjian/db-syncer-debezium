package com.dbsyncer.metadata.service;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import com.dbsyncer.connectors.client.exception.KafkaConnectException;
import com.dbsyncer.connectors.client.model.ConnectorInfo;
import com.dbsyncer.connectors.client.model.ConnectorStatus;
import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.ConnectorConfig;
import com.dbsyncer.metadata.entity.ConnectorType;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.repository.ConnectorConfigRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "connect.wait-timeout-ms=1000",
        "connect.poll-interval-ms=10",
        "connect.rest-url=http://connect:8083"
})
class TaskExecutionServiceIntegrationTest {

    @Autowired
    private TaskExecutionService taskExecutionService;

    @SpyBean
    private TaskService taskService;

    @Autowired
    private MigrationTaskRepository taskRepository;

    @Autowired
    private ConnectorConfigRepository connectorConfigRepository;

    @MockBean
    private KafkaConnectClient connectClient;

    @BeforeEach
    void cleanDatabase() {
        connectorConfigRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration: startTask successfully deploys connectors and marks task RUNNING")
    void startTaskHappyPathIntegration() {
        TaskResponse created = taskService.createTask(buildCreateRequest());
        UUID taskId = created.getId();

        when(connectClient.connectorExists(anyString())).thenReturn(false);

        ConnectorInfo connectorInfo = ConnectorInfo.builder()
                .name("test-connector")
                .config(Map.of("connector.class", "io.debezium.connector.mysql.MySqlConnector"))
                .tasks(new ConnectorInfo.TaskInfo[]{new ConnectorInfo.TaskInfo("test-connector", 0)})
                .type("source")
                .build();
        when(connectClient.createConnector(anyString(), anyMap())).thenReturn(connectorInfo);
        when(connectClient.waitForConnectorRunning(anyString(), anyLong(), anyLong())).thenReturn(true);

        ConnectorStatus.ConnectorState connectorState =
                new ConnectorStatus.ConnectorState("RUNNING", "worker-1", null);
        ConnectorStatus.TaskState taskState =
                new ConnectorStatus.TaskState(0, "RUNNING", "worker-1", null);
        ConnectorStatus runningStatus = ConnectorStatus.builder()
                .name("test-connector")
                .connector(connectorState)
                .tasks(List.of(taskState))
                .type("source")
                .build();
        when(connectClient.getConnectorStatus(anyString())).thenReturn(runningStatus);

        TaskResponse started = taskExecutionService.startTask(taskId);

        assertThat(started).isNotNull();
        assertThat(started.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(started.getStartedAt()).isNotNull();

        MigrationTask persisted = taskRepository.findById(taskId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(persisted.getStartedAt()).isNotNull();

        List<ConnectorConfig> connectors = connectorConfigRepository.findByTaskId(taskId);
        assertThat(connectors).hasSize(2);
        assertThat(connectors)
                .extracting(ConnectorConfig::getConnectorType)
                .containsExactlyInAnyOrder(ConnectorType.SOURCE, ConnectorType.SINK);
        assertThat(connectors)
                .allMatch(cfg -> Boolean.TRUE.equals(cfg.getDeployed()));
        assertThat(connectors)
                .allMatch(cfg -> "RUNNING".equals(cfg.getStatus()));

        verify(taskService).updateTaskStatus(taskId, TaskStatus.STARTING);
    }

    @Test
    @DisplayName("Integration: startTask retries on failure and marks task FAILED with error")
    void startTaskFailureWithRetryIntegration() {
        TaskResponse created = taskService.createTask(buildCreateRequest());
        UUID taskId = created.getId();

        when(connectClient.connectorExists(anyString())).thenReturn(false);
        when(connectClient.createConnector(anyString(), anyMap()))
                .thenThrow(new KafkaConnectException("simulated connect failure"));

        assertThatThrownBy(() -> taskExecutionService.startTask(taskId))
                .isInstanceOf(KafkaConnectException.class)
                .hasMessageContaining("simulated connect failure");

        verify(connectClient, atLeast(3)).createConnector(anyString(), anyMap());
        verify(taskService).updateTaskStatus(taskId, TaskStatus.STARTING);
        verify(taskService, atLeast(1)).updateTaskError(
                org.mockito.ArgumentMatchers.eq(taskId),
                org.mockito.ArgumentMatchers.contains("simulated connect failure"));

        MigrationTask failedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(failedTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(failedTask.getErrorMessage()).contains("simulated connect failure");
        assertThat(failedTask.getCompletedAt()).isNotNull();

        List<ConnectorConfig> connectors = connectorConfigRepository.findByTaskId(taskId);
        assertThat(connectors).hasSize(2);
        assertThat(connectors)
                .allMatch(cfg -> !Boolean.TRUE.equals(cfg.getDeployed()));
    }

    private TaskCreateRequest buildCreateRequest() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskName("integration-task-" + UUID.randomUUID());
        request.setDescription("Integration test task");
        request.setSourceType(DatabaseType.MYSQL);
        request.setSourceHost("localhost");
        request.setSourcePort(3306);
        request.setSourceDatabase("sourcedb");
        request.setSourceUsername("root");
        request.setSourcePassword("password");
        request.setTargetType(DatabaseType.POSTGRESQL);
        request.setTargetHost("localhost");
        request.setTargetPort(5432);
        request.setTargetDatabase("targetdb");
        request.setTargetUsername("postgres");
        request.setTargetPassword("password");
        return request;
    }
}
