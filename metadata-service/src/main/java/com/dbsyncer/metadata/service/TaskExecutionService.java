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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TaskExecutionService {

    private final MigrationTaskRepository taskRepository;
    private final ConnectorConfigRepository configRepository;
    private final TaskService taskService;
    private final KafkaConnectClient connectClient;
    private final ConnectProperties connectProperties;
    private final TaskLogRepository taskLogRepository;
    private final AlertService alertService;
    private final CheckpointService checkpointService;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String DEFAULT_METADATA_SERVICE_URL = "http://metadata-service:8080";

    @Autowired
    @org.springframework.context.annotation.Lazy
    private TaskExecutionService self;

    public TaskExecutionService(MigrationTaskRepository taskRepository,
                               ConnectorConfigRepository configRepository,
                               TaskService taskService,
                               KafkaConnectClient connectClient,
                               ConnectProperties connectProperties,
                               TaskLogRepository taskLogRepository,
                               AlertService alertService,
                               CheckpointService checkpointService) {
        this.taskRepository = taskRepository;
        this.configRepository = configRepository;
        this.taskService = taskService;
        this.connectClient = connectClient;
        this.connectProperties = connectProperties;
        this.taskLogRepository = taskLogRepository;
        this.alertService = alertService;
        this.checkpointService = checkpointService;
    }

    @Transactional
    public TaskResponse startTask(UUID taskId) {
        MDC.put("taskId", taskId.toString());
        try {
            MigrationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new TaskNotFoundException(taskId));

            if (!task.canStart()) {
                throw new InvalidTaskStateException("start", task.getStatus());
            }

            taskService.updateTaskStatus(taskId, TaskStatus.STARTING);

            try {
                ConnectorConfig sourceCfg = ensureSourceConnectorConfig(task);
                ConnectorConfig sinkCfg = ensureSinkConnectorConfig(task);

                deployConnectorWithRetry(sourceCfg);
                waitAndUpdateStatus(sourceCfg);

                deployConnectorWithRetry(sinkCfg);
                waitAndUpdateStatus(sinkCfg);

                task.setStatus(TaskStatus.RUNNING);
                if (task.getStartedAt() == null) {
                    task.setStartedAt(OffsetDateTime.now());
                }
                taskRepository.save(task);
                return TaskResponse.fromEntity(task);
            } catch (Exception e) {
                // Use self-reference to ensure REQUIRES_NEW transaction propagation works
                // This ensures error state is persisted even when outer transaction rolls back
                if (self != null) {
                    self.classifyAndHandleStartFailure(taskId, e);
                }
                throw e;
            }
        } finally {
            MDC.remove("taskId");
        }
    }

    @Transactional
    public TaskResponse pauseTask(UUID taskId) {
        MDC.put("taskId", taskId.toString());
        try {
            MigrationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new TaskNotFoundException(taskId));

            if (!task.canPause()) {
                throw new InvalidTaskStateException("pause", task.getStatus());
            }

            configRepository.findSourceConnector(taskId).ifPresent(c -> safePause(c.getConnectorName()));
            configRepository.findSinkConnector(taskId).ifPresent(c -> safePause(c.getConnectorName()));

            // Create checkpoint before pausing
            checkpointService.createCheckpoint(taskId, "Task paused by user");

            return taskService.updateTaskStatus(taskId, TaskStatus.PAUSED);
        } finally {
            MDC.remove("taskId");
        }
    }

    @Transactional
    public TaskResponse resumeTask(UUID taskId) {
        MDC.put("taskId", taskId.toString());
        try {
            MigrationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new TaskNotFoundException(taskId));

            if (!task.canResume()) {
                throw new InvalidTaskStateException("resume", task.getStatus());
            }

            configRepository.findSourceConnector(taskId).ifPresent(c -> safeResume(c.getConnectorName()));
            configRepository.findSinkConnector(taskId).ifPresent(c -> safeResume(c.getConnectorName()));

            return taskService.updateTaskStatus(taskId, TaskStatus.RUNNING);
        } finally {
            MDC.remove("taskId");
        }
    }

    @Transactional
    public TaskResponse stopTask(UUID taskId) {
        MDC.put("taskId", taskId.toString());
        try {
            MigrationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new TaskNotFoundException(taskId));

            if (!task.canStop() && task.getStatus() != TaskStatus.STARTING) {
                throw new InvalidTaskStateException("stop", task.getStatus());
            }

            // Create checkpoint before stopping
            checkpointService.createCheckpoint(taskId, "Task stopped by user");

            configRepository.findSourceConnector(taskId).ifPresent(c -> safeDelete(c));
            configRepository.findSinkConnector(taskId).ifPresent(c -> safeDelete(c));

            return taskService.updateTaskStatus(taskId, TaskStatus.STOPPED);
        } finally {
            MDC.remove("taskId");
        }
    }

    private void deployConnectorWithRetry(ConnectorConfig cfg) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                deployOrUpdateConnectorOnce(cfg);
                return;
            } catch (Exception e) {
                if (!isRetryable(e) || attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("Deployment failed for connector {} after {} attempts: {}",
                            cfg.getConnectorName(), attempts, e.getMessage());
                    throw e;
                }
                long backoff = computeBackoffMillis(attempts);
                log.warn("Retryable error deploying connector {} (attempt {}/{}), backing off {} ms: {}",
                        cfg.getConnectorName(), attempts, MAX_RETRY_ATTEMPTS, backoff, e.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private void deployOrUpdateConnectorOnce(ConnectorConfig cfg) {
        Map<String, String> config = cfg.getConfig();
        String name = cfg.getConnectorName();
        boolean exists = connectClient.connectorExists(name);
        ConnectorInfo info = exists
                ? connectClient.updateConnector(name, config)
                : connectClient.createConnector(name, config);

        cfg.setDeployed(true);
        cfg.setDeployedAt(OffsetDateTime.now());
        cfg.setConnectorClass(info.getConfig() != null ? info.getConfig().get("connector.class") : cfg.getConnectorClass());
        cfg.setTasksCount(info.getTasks() != null ? info.getTasks().length : 0);
        configRepository.save(cfg);
    }

    private boolean isRetryable(Exception e) {
        return e instanceof KafkaConnectException || e instanceof DataAccessException;
    }

    private long computeBackoffMillis(int attempt) {
        long base = connectProperties.getPollIntervalMs();
        long max = connectProperties.getWaitTimeoutMs();
        long backoff = base * (1L << Math.min(attempt - 1, 4));
        return Math.min(backoff, max);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void classifyAndHandleStartFailure(UUID taskId, Exception e) {
        boolean retryable = isRetryable(e);
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.error("Failed to start task {}: {} (retryable={})", taskId, message, retryable, e);

        // Update task status to FAILED
        taskService.updateTaskError(taskId, message);

        // Persist execution log for troubleshooting and monitoring
        taskRepository.findById(taskId).ifPresent(task -> {
            Map<String, Object> context = new HashMap<>();
            context.put("exceptionType", e.getClass().getName());
            context.put("retryable", retryable);
            context.put("taskStatus", task.getStatus() != null ? task.getStatus().name() : null);

            TaskLog logEntry = TaskLog.builder()
                    .task(task)
                    .logLevel("ERROR")
                    .message(message)
                    .context(context)
                    .sourceComponent("TaskExecutionService")
                    .build();
            taskLogRepository.save(logEntry);

            // Trigger alerts based on rules
            alertService.onTaskFailure(taskId, message, retryable);
        });

        // TODO: persist failed records / DLQ routing once available
        // TODO: integrate with alerting mechanism (Phase 7)
    }

    private void waitAndUpdateStatus(ConnectorConfig cfg) {
        boolean running = connectClient.waitForConnectorRunning(
                cfg.getConnectorName(),
                connectProperties.getWaitTimeoutMs(),
                connectProperties.getPollIntervalMs());
        try {
            ConnectorStatus status = connectClient.getConnectorStatus(cfg.getConnectorName());
            cfg.setStatus(status.getConnector() != null ? status.getConnector().getState() : (running ? "RUNNING" : "UNKNOWN"));
            String workerId = status.getConnector() != null ? status.getConnector().getWorkerId() : null;
            cfg.setWorkerId(workerId);
            if (status.isFailed()) {
                cfg.setErrorMessage(status.getErrorTrace());
            } else {
                cfg.setErrorMessage(null);
            }
        } catch (KafkaConnectException e) {
            cfg.setStatus(running ? "RUNNING" : "UNKNOWN");
        }
        configRepository.save(cfg);
    }

    private void safePause(String name) {
        try {
            connectClient.pauseConnector(name);
        } catch (Exception e) {
            log.warn("Pause connector {} failed: {}", name, e.getMessage());
        }
    }

    private void safeResume(String name) {
        try {
            connectClient.resumeConnector(name);
        } catch (Exception e) {
            log.warn("Resume connector {} failed: {}", name, e.getMessage());
        }
    }

    private void safeDelete(ConnectorConfig cfg) {
        try {
            connectClient.deleteConnector(cfg.getConnectorName());
        } catch (Exception e) {
            log.warn("Delete connector {} failed: {}", cfg.getConnectorName(), e.getMessage());
        }
        cfg.setDeployed(false);
        cfg.setStatus("DELETED");
        cfg.setWorkerId(null);
        cfg.setTasksCount(0);
        configRepository.save(cfg);
    }

    private ConnectorConfig ensureSourceConnectorConfig(MigrationTask task) {
        Map<String, String> fresh = buildSourceConfig(task);
        Optional<ConnectorConfig> existing = configRepository.findSourceConnector(task.getId());
        if (existing.isPresent()) {
            ConnectorConfig cfg = existing.get();
            cfg.setConnectorClass(resolveSourceConnectorClass(task.getSourceType()));
            cfg.setConfig(fresh);
            cfg.setDeployed(false);
            return configRepository.save(cfg);
        }
        ConnectorConfig cfg = ConnectorConfig.builder()
                .task(task)
                .connectorType(ConnectorType.SOURCE)
                .connectorName(task.getTaskName() + "-source")
                .connectorClass(resolveSourceConnectorClass(task.getSourceType()))
                .config(fresh)
                .deployed(false)
                .build();
        return configRepository.save(cfg);
    }

    private ConnectorConfig ensureSinkConnectorConfig(MigrationTask task) {
        Map<String, String> fresh = buildSinkConfig(task);
        Optional<ConnectorConfig> existing = configRepository.findSinkConnector(task.getId());
        if (existing.isPresent()) {
            ConnectorConfig cfg = existing.get();
            cfg.setConnectorClass("io.debezium.connector.jdbc.JdbcSinkConnector");
            cfg.setConfig(fresh);
            cfg.setDeployed(false);
            return configRepository.save(cfg);
        }
        ConnectorConfig cfg = ConnectorConfig.builder()
                .task(task)
                .connectorType(ConnectorType.SINK)
                .connectorName(task.getTaskName() + "-sink")
                .connectorClass("io.debezium.connector.jdbc.JdbcSinkConnector")
                .config(fresh)
                .deployed(false)
                .build();
        return configRepository.save(cfg);
    }

    private String resolveSourceConnectorClass(DatabaseType type) {
        return switch (type) {
            case MYSQL -> "io.debezium.connector.mysql.MySqlConnector";
            case POSTGRESQL -> "io.debezium.connector.postgresql.PostgresConnector";
            case ORACLE -> "io.debezium.connector.oracle.OracleConnector";
        };
    }

    private Map<String, String> buildSourceConfig(MigrationTask task) {
        Map<String, String> config = new HashMap<>();
        config.put("name", task.getTaskName() + "-source");
        config.put("connector.class", resolveSourceConnectorClass(task.getSourceType()));
        config.put("tasks.max", "1");

        switch (task.getSourceType()) {
            case MYSQL -> {
                config.put("database.hostname", task.getSourceHost());
                config.put("database.port", String.valueOf(task.getSourcePort()));
                config.put("database.user", task.getSourceUsername());
                config.put("database.password", task.getSourcePassword());
                // Debezium 2.x uses topic.prefix instead of database.server.name
                config.put("topic.prefix", task.getTaskName());
                config.put("database.include.list", task.getSourceDatabase());
                // Provide a default server id derived from task id for uniqueness
                int serverId = Math.abs(task.getId().hashCode() % 100000) + 5400;
                config.put("database.server.id", String.valueOf(serverId));
                // Use file-based schema history inside the Connect container for E2E demo
                config.put("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory");
                String historyDir = connectProperties.getSchemaHistoryDir();
                if (historyDir == null || historyDir.isBlank()) {
                    historyDir = "/kafka/connect/custom-connectors/schema-history";
                }
                if (!historyDir.endsWith("/")) {
                    historyDir = historyDir + "/";
                }
                config.put("schema.history.internal.file.filename", historyDir + task.getTaskName() + ".dat");
                config.put("snapshot.mode", task.getSnapshotMode());
                if (Boolean.TRUE.equals(task.getIncrementalSnapshot())) {
                    config.put("snapshot.mode", "initial_only");
                    config.put("incremental.snapshot.enabled", "true");
                    config.put("incremental.snapshot.chunk.size", String.valueOf(task.getSnapshotChunkSize()));
                }
                // Avoid FTWRL requirement in demo env without RELOAD privilege
                config.put("snapshot.locking.mode", "none");
            }
            case POSTGRESQL -> {
                config.put("database.hostname", task.getSourceHost());
                config.put("database.port", String.valueOf(task.getSourcePort()));
                config.put("database.user", task.getSourceUsername());
                config.put("database.password", task.getSourcePassword());
                config.put("database.dbname", task.getSourceDatabase());
                config.put("plugin.name", "pgoutput");
                config.put("slot.name", task.getTaskName() + "_slot");
                config.put("publication.name", task.getTaskName() + "_pub");
                config.put("topic.prefix", task.getTaskName());
                config.put("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory");
                config.put("schema.history.internal.file.filename", "/kafka/connect/custom-connectors/schema-history/" + task.getTaskName() + ".dat");
                config.put("snapshot.mode", task.getSnapshotMode());
                if (Boolean.TRUE.equals(task.getIncrementalSnapshot())) {
                    config.put("incremental.snapshot.enabled", "true");
                    config.put("incremental.snapshot.chunk.size", String.valueOf(task.getSnapshotChunkSize()));
                }
            }
            case ORACLE -> {
                config.put("database.hostname", task.getSourceHost());
                config.put("database.port", String.valueOf(task.getSourcePort()));
                config.put("database.user", task.getSourceUsername());
                config.put("database.password", task.getSourcePassword());
                config.put("database.dbname", task.getSourceDatabase());
                config.put("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory");
                config.put("schema.history.internal.file.filename", "/kafka/connect/custom-connectors/schema-history/" + task.getTaskName() + ".dat");
                config.put("snapshot.mode", task.getSnapshotMode());
                if (Boolean.TRUE.equals(task.getIncrementalSnapshot())) {
                    config.put("incremental.snapshot.enabled", "true");
                    config.put("incremental.snapshot.chunk.size", String.valueOf(task.getSnapshotChunkSize()));
                }
            }
        }

        if (task.getIncludeTables() != null && !task.getIncludeTables().isEmpty()) {
            config.put("table.include.list", String.join(",", task.getIncludeTables()));
        }
        if (task.getExcludeTables() != null && !task.getExcludeTables().isEmpty()) {
            config.put("table.exclude.list", String.join(",", task.getExcludeTables()));
        }

        config.put("max.queue.size", String.valueOf(task.getMaxQueueSize()));
        config.put("poll.interval.ms", String.valueOf(task.getPollIntervalMs()));
        config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("key.converter.schemas.enable", "true");
        config.put("value.converter.schemas.enable", "true");
        return config;
    }

    private String resolveKafkaBootstrapServers() {
        // When running in docker-compose from README, Kafka is reachable at kafka:29092
        // Allow override via task targetProperties in future if needed.
        return "kafka:29092";
    }

    private Map<String, String> buildSinkConfig(MigrationTask task) {
        Map<String, String> config = new HashMap<>();
        config.put("name", task.getTaskName() + "-sink");
        config.put("connector.class", "io.debezium.connector.jdbc.JdbcSinkConnector");
        config.put("tasks.max", String.valueOf(task.getParallelTables() != null && task.getParallelTables() > 0
                ? task.getParallelTables()
                : 1));

        String jdbcUrl = switch (task.getTargetType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s", task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s", task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s", task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
        };
        config.put("connection.url", jdbcUrl);
        config.put("connection.username", task.getTargetUsername());
        config.put("connection.password", task.getTargetPassword());

        String taskNameEsc = task.getTaskName().replace(".", "\\.");
        String srcDbEsc = task.getSourceDatabase() != null ? task.getSourceDatabase().replace(".", "\\.") : ".*";
        config.put("topics.regex", taskNameEsc + "\\." + srcDbEsc + "\\..*");
        config.put("insert.mode", "upsert");
        config.put("delete.enabled", "true");
        config.put("primary.key.mode", "record_key");
        config.put("schema.evolution", "basic");

        config.put("batch.size", String.valueOf(task.getBatchSize()));

        // Unwrap Debezium envelope to flat records, optionally report progress,
        // then route topic to plain table name.
        boolean enableProgressSmt = connectProperties.isEnableProgressSmt();
        if (enableProgressSmt) {
            config.put("transforms", "unwrap,progress,route");
        } else {
            config.put("transforms", "unwrap,route");
        }

        config.put("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        config.put("transforms.unwrap.drop.tombstones", "true");

        if (enableProgressSmt) {
            config.put("transforms.progress.type", "com.dbsyncer.transformations.smt.ProgressReporting");
            config.put("transforms.progress.task.id", task.getId().toString());
            config.put("transforms.progress.metadata.service.url", resolveMetadataServiceUrl());
            config.put("transforms.progress.batch.size", "100");
            config.put("transforms.progress.flush.interval.ms", String.valueOf(connectProperties.getPollIntervalMs() * 2));
        }

        config.put("transforms.route.type", "org.apache.kafka.connect.transforms.RegexRouter");
        config.put("transforms.route.regex", "^" + task.getTaskName().replace(".", "\\.") + "\\." + task.getSourceDatabase().replace(".", "\\.") + "\\.(.*)$");
        config.put("transforms.route.replacement", "$1");

        config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("key.converter.schemas.enable", "true");
        config.put("value.converter.schemas.enable", "true");
        return config;
    }

    private String resolveMetadataServiceUrl() {
        String fromProps = connectProperties.getMetadataServiceUrl();
        if (fromProps != null && !fromProps.isBlank()) {
            return fromProps;
        }
        String env = System.getenv("DBSYNCER_METADATA_SERVICE_URL");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return DEFAULT_METADATA_SERVICE_URL;
    }
}
