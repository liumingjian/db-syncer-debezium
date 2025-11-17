package com.dbsyncer.cli.service;

import com.dbsyncer.metadata.entity.ConnectorConfig;
import com.dbsyncer.metadata.entity.ConnectorType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.repository.ConnectorConfigRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CliConfigService {

    private final ConnectorConfigRepository configRepository;
    private final MigrationTaskRepository taskRepository;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public CliConfigService(ConnectorConfigRepository configRepository,
                           MigrationTaskRepository taskRepository,
                           TaskService taskService) {
        this.configRepository = configRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<ConnectorConfig> getConfigs(String taskIdentifier) {
        UUID taskId = resolveTaskId(taskIdentifier);
        return configRepository.findByTaskId(taskId);
    }

    public List<ConnectorConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @Transactional
    public void updateConfig(String taskIdentifier, String connectorType, Map<String, String> properties) {
        UUID taskId = resolveTaskId(taskIdentifier);
        ConnectorType type = ConnectorType.valueOf(connectorType.toUpperCase());

        ConnectorConfig config = configRepository.findByTaskIdAndConnectorType(taskId, type)
            .orElseThrow(() -> new RuntimeException("Configuration not found for task " + taskIdentifier
                + " and type " + connectorType));

        Map<String, String> existingConfig = config.getConfig();
        if (existingConfig == null) {
            existingConfig = new HashMap<>();
        }
        existingConfig.putAll(properties);
        config.setConfig(existingConfig);

        configRepository.save(config);
    }

    public String generateConfigTemplate(String taskIdentifier, String connectorType) {
        UUID taskId = resolveTaskId(taskIdentifier);
        MigrationTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskIdentifier));

        ConnectorType type = ConnectorType.valueOf(connectorType.toUpperCase());
        Map<String, Object> template = new HashMap<>();

        if (type == ConnectorType.SOURCE) {
            template = generateSourceConnectorTemplate(task);
        } else if (type == ConnectorType.SINK) {
            template = generateSinkConnectorTemplate(task);
        }

        try {
            return objectMapper.writeValueAsString(template);
        } catch (Exception e) {
            throw new RuntimeException("Error generating configuration template", e);
        }
    }

    private Map<String, Object> generateSourceConnectorTemplate(MigrationTask task) {
        Map<String, Object> config = new HashMap<>();

        String connectorClass = switch (task.getSourceType()) {
            case MYSQL -> "io.debezium.connector.mysql.MySqlConnector";
            case POSTGRESQL -> "io.debezium.connector.postgresql.PostgresConnector";
            case ORACLE -> "io.debezium.connector.oracle.OracleConnector";
        };

        config.put("name", task.getTaskName() + "-source");
        config.put("connector.class", connectorClass);
        config.put("database.hostname", task.getSourceHost());
        config.put("database.port", task.getSourcePort());
        config.put("database.user", task.getSourceUsername());
        config.put("database.password", task.getSourcePassword());
        config.put("database.dbname", task.getSourceDatabase());
        config.put("topic.prefix", task.getTaskName());

        switch (task.getSourceType()) {
            case MYSQL -> {
                config.put("database.server.id", "1");
                config.put("database.include.list", task.getSourceDatabase());
                config.put("schema.history.internal.kafka.bootstrap.servers", "kafka:9092");
                config.put("schema.history.internal.kafka.topic", "schema-changes." + task.getTaskName());
            }
            case POSTGRESQL -> {
                config.put("plugin.name", "pgoutput");
                config.put("slot.name", task.getTaskName().replace("-", "_") + "_slot");
                config.put("publication.name", task.getTaskName().replace("-", "_") + "_pub");
            }
            case ORACLE -> {
                config.put("database.pdb.name", task.getSourceDatabase());
                config.put("database.oracle.jdbc.driver.class", "oracle.jdbc.OracleDriver");
            }
        }

        config.put("tasks.max", "1");
        config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");

        return config;
    }

    private Map<String, Object> generateSinkConnectorTemplate(MigrationTask task) {
        Map<String, Object> config = new HashMap<>();

        config.put("name", task.getTaskName() + "-sink");
        config.put("connector.class", "io.debezium.connector.jdbc.JdbcSinkConnector");

        String jdbcUrl = switch (task.getTargetType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s",
                task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s",
                task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s",
                task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase());
        };

        config.put("connection.url", jdbcUrl);
        config.put("connection.username", task.getTargetUsername());
        config.put("connection.password", task.getTargetPassword());
        config.put("topics.regex", task.getTaskName() + ".*");
        config.put("insert.mode", "upsert");
        config.put("delete.enabled", "true");
        config.put("primary.key.mode", "record_key");
        config.put("schema.evolution", "basic");
        config.put("tasks.max", "1");
        config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");

        return config;
    }

    private UUID resolveTaskId(String identifier) {
        try {
            return UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            return taskService.getTaskByName(identifier).getId();
        }
    }
}
