package com.dbsyncer.metadata.service;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import com.dbsyncer.connectors.client.exception.KafkaConnectException;
import com.dbsyncer.connectors.client.model.ConnectorInfo;
import com.dbsyncer.connectors.client.model.ConnectorStatus;
import com.dbsyncer.metadata.config.ConnectProperties;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.*;
import com.dbsyncer.metadata.exception.InvalidTaskStateException;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.ConnectorConfigRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.repository.TaskLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskExecutionServiceTest {

    @Mock
    private MigrationTaskRepository taskRepository;

    @Mock
    private ConnectorConfigRepository connectorConfigRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private KafkaConnectClient connectClient;

    @Mock
    private ConnectProperties connectProperties;

    @Mock
    private TaskLogRepository taskLogRepository;

    @InjectMocks
    private TaskExecutionService taskExecutionService;

    private MigrationTask task;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        task = MigrationTask.builder()
                .id(taskId)
                .taskName("test-task")
                .sourceType(DatabaseType.MYSQL)
                .sourceHost("localhost")
                .sourcePort(3306)
                .sourceDatabase("sourcedb")
                .sourceUsername("root")
                .sourcePassword("pwd")
                .targetType(DatabaseType.POSTGRESQL)
                .targetHost("localhost")
                .targetPort(5432)
                .targetDatabase("targetdb")
                .targetUsername("pg")
                .targetPassword("pgpwd")
                .status(TaskStatus.CREATED)
                .batchSize(1000)
                .maxQueueSize(8192)
                .pollIntervalMs(1000)
                .build();

        when(connectProperties.getWaitTimeoutMs()).thenReturn(2000L);
        when(connectProperties.getPollIntervalMs()).thenReturn(10L);
    }

    @Test
    @DisplayName("startTask should deploy source and sink connectors and mark task RUNNING")
    void startTaskHappyPath() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        when(connectorConfigRepository.findSourceConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.findSinkConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.save(any(ConnectorConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(connectClient.connectorExists(anyString())).thenReturn(false);
        ConnectorInfo info = new ConnectorInfo();
        info.setConfig(new HashMap<>());
        info.setTasks(new com.dbsyncer.connectors.client.model.ConnectorInfo.TaskInfo[0]);
        when(connectClient.createConnector(anyString(), anyMap())).thenReturn(info);

        ConnectorStatus runningStatus = mock(ConnectorStatus.class);
        when(runningStatus.isRunning()).thenReturn(true);
        when(connectClient.waitForConnectorRunning(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(connectClient.getConnectorStatus(anyString())).thenReturn(runningStatus);

        when(taskRepository.save(any(MigrationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskExecutionService.startTask(taskId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(response.getStartedAt()).isNotNull();

        verify(taskService).updateTaskStatus(taskId, TaskStatus.STARTING);
        verify(connectClient, times(2)).createConnector(anyString(), anyMap());
        verify(connectClient, times(2)).waitForConnectorRunning(anyString(), anyLong(), anyLong());
        verify(taskRepository, atLeastOnce()).save(any(MigrationTask.class));
    }

    @Test
    @DisplayName("startTask should classify failure and call updateTaskError after retries")
    void startTaskFailureWithRetryAndErrorClassification() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        when(connectorConfigRepository.findSourceConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.findSinkConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.save(any(ConnectorConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(connectClient.connectorExists(anyString())).thenReturn(false);
        when(connectClient.createConnector(anyString(), anyMap()))
                .thenThrow(new KafkaConnectException("temporary connect failure"));

        assertThatThrownBy(() -> taskExecutionService.startTask(taskId))
                .isInstanceOf(KafkaConnectException.class);

        verify(connectClient, atLeast(3)).createConnector(anyString(), anyMap());
        verify(taskService).updateTaskError(eq(taskId), anyString());
        verify(taskLogRepository).save(any(TaskLog.class));
    }

    @Test
    @DisplayName("startTask should apply incremental snapshot and parallel tables configs to connectors")
    void startTaskAppliesIncrementalSnapshotAndParallelTablesConfig() {
        task.setIncrementalSnapshot(true);
        task.setSnapshotChunkSize(5000);
        task.setParallelTables(4);
        task.setIncludeTables(java.util.Arrays.asList("public.customers", "public.orders"));
        task.setExcludeTables(java.util.Collections.singletonList("public.logs"));

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(connectorConfigRepository.findSourceConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.findSinkConnector(taskId)).thenReturn(Optional.empty());
        when(connectorConfigRepository.save(any(ConnectorConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(connectClient.connectorExists(anyString())).thenReturn(false);
        ConnectorInfo info = new ConnectorInfo();
        info.setConfig(new HashMap<>());
        info.setTasks(new com.dbsyncer.connectors.client.model.ConnectorInfo.TaskInfo[0]);
        when(connectClient.createConnector(anyString(), anyMap())).thenReturn(info);

        ConnectorStatus runningStatus = mock(ConnectorStatus.class);
        when(runningStatus.isRunning()).thenReturn(true);
        when(connectClient.waitForConnectorRunning(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(connectClient.getConnectorStatus(anyString())).thenReturn(runningStatus);

        when(taskRepository.save(any(MigrationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskExecutionService.startTask(taskId);

        org.mockito.ArgumentCaptor<ConnectorConfig> cfgCaptor = org.mockito.ArgumentCaptor.forClass(ConnectorConfig.class);
        verify(connectorConfigRepository, atLeast(2)).save(cfgCaptor.capture());

        java.util.List<ConnectorConfig> savedConfigs = cfgCaptor.getAllValues();
        ConnectorConfig sourceCfg = savedConfigs.stream()
                .filter(c -> c.getConnectorType() == ConnectorType.SOURCE)
                .findFirst()
                .orElseThrow();
        ConnectorConfig sinkCfg = savedConfigs.stream()
                .filter(c -> c.getConnectorType() == ConnectorType.SINK)
                .findFirst()
                .orElseThrow();

        // Verify source incremental snapshot config
        java.util.Map<String, String> srcConfig = sourceCfg.getConfig();
        assertThat(srcConfig.get("snapshot.mode")).isEqualTo("initial_only");
        assertThat(srcConfig.get("incremental.snapshot.enabled")).isEqualTo("true");
        assertThat(srcConfig.get("incremental.snapshot.chunk.size")).isEqualTo("5000");
        assertThat(srcConfig.get("table.include.list")).isEqualTo("public.customers,public.orders");
        assertThat(srcConfig.get("table.exclude.list")).isEqualTo("public.logs");
        assertThat(srcConfig.get("max.queue.size")).isEqualTo(String.valueOf(task.getMaxQueueSize()));
        assertThat(srcConfig.get("poll.interval.ms")).isEqualTo(String.valueOf(task.getPollIntervalMs()));

        // Verify sink parallel tables config
        java.util.Map<String, String> sinkConfig = sinkCfg.getConfig();
        assertThat(sinkConfig.get("tasks.max")).isEqualTo("4");
        assertThat(sinkConfig.get("batch.size")).isEqualTo(String.valueOf(task.getBatchSize()));
    }

    @Test
    @DisplayName("startTask should fail for non-startable task state")
    void startTaskInvalidState() {
        task.setStatus(TaskStatus.RUNNING);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskExecutionService.startTask(taskId))
                .isInstanceOf(InvalidTaskStateException.class);

        verify(taskService, never()).updateTaskStatus(any(), any());
    }

    @Test
    @DisplayName("startTask should throw when task not found")
    void startTaskTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskExecutionService.startTask(taskId))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
