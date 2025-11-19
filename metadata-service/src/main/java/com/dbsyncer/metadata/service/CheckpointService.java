package com.dbsyncer.metadata.service;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import com.dbsyncer.connectors.client.model.ConnectorStatus;
import com.dbsyncer.metadata.entity.*;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.ConnectorConfigRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.repository.TableProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing checkpoint and resume functionality.
 * Enables tasks to be resumed from their last known state after restart or failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckpointService {

    private final MigrationTaskRepository taskRepository;
    private final ConnectorConfigRepository connectorConfigRepository;
    private final TableProgressRepository tableProgressRepository;
    private final KafkaConnectClient kafkaConnectClient;

    /**
     * Check if a task can be resumed.
     * A task can be resumed if:
     * - It was previously RUNNING, PAUSED, or FAILED
     * - It has connector configurations saved
     * - It has progress tracking data
     */
    @Transactional(readOnly = true)
    public boolean canResume(UUID taskId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        // Check task status
        TaskStatus status = task.getStatus();
        if (status != TaskStatus.STOPPED &&
            status != TaskStatus.FAILED &&
            status != TaskStatus.PAUSED) {
            log.debug("Task {} cannot be resumed: status is {}", taskId, status);
            return false;
        }

        // Check if connector configs exist
        List<ConnectorConfig> connectors = connectorConfigRepository.findByTaskId(taskId);
        if (connectors.isEmpty()) {
            log.debug("Task {} cannot be resumed: no connector configs found", taskId);
            return false;
        }

        // Check if progress tracking exists
        List<TableProgress> progress = tableProgressRepository.findByTaskId(taskId);
        if (progress.isEmpty()) {
            log.debug("Task {} has no progress tracking data, but can attempt resume", taskId);
        }

        return true;
    }

    /**
     * Get the checkpoint state for a task.
     * This includes last known progress, connector states, and any persisted offsets.
     */
    @Transactional(readOnly = true)
    public CheckpointState getCheckpoint(UUID taskId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        List<ConnectorConfig> connectors = connectorConfigRepository.findByTaskId(taskId);
        List<TableProgress> progress = tableProgressRepository.findByTaskId(taskId);

        return CheckpointState.builder()
                .taskId(taskId)
                .taskStatus(task.getStatus())
                .lastStartedAt(task.getStartedAt())
                .connectorConfigs(connectors)
                .tableProgress(progress)
                .totalTables(task.getTotalTables())
                .completedTables(task.getCompletedTables())
                .processedRecords(task.getProcessedRecords())
                .checkpointTime(OffsetDateTime.now())
                .build();
    }

    /**
     * Validate that connectors still exist in Kafka Connect.
     * If connectors are missing, they need to be redeployed.
     */
    @Transactional(readOnly = true)
    public ConnectorValidationResult validateConnectors(UUID taskId) {
        List<ConnectorConfig> configs = connectorConfigRepository.findByTaskId(taskId);

        ConnectorValidationResult result = new ConnectorValidationResult();
        result.setTaskId(taskId);

        for (ConnectorConfig config : configs) {
            String connectorName = config.getConnectorName();
            try {
                boolean exists = kafkaConnectClient.connectorExists(connectorName);

                if (exists) {
                    ConnectorStatus status = kafkaConnectClient.getConnectorStatus(connectorName);
                    result.addExisting(connectorName, status.getConnector().getState());
                } else {
                    result.addMissing(connectorName);
                }
            } catch (Exception e) {
                log.warn("Failed to check connector {}: {}", connectorName, e.getMessage());
                result.addFailed(connectorName, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Get incomplete tables that need to be continued.
     * These are tables in PENDING, SNAPSHOTTING, or STREAMING status.
     */
    @Transactional(readOnly = true)
    public List<TableProgress> getIncompleteTables(UUID taskId) {
        return tableProgressRepository.findByTaskIdAndStatusIn(
                taskId,
                List.of(ProgressStatus.PENDING, ProgressStatus.SNAPSHOTTING, ProgressStatus.STREAMING)
        );
    }

    /**
     * Get failed tables that may need manual intervention or retry.
     */
    @Transactional(readOnly = true)
    public List<TableProgress> getFailedTables(UUID taskId) {
        return tableProgressRepository.findByTaskIdAndStatus(taskId, ProgressStatus.FAILED);
    }

    /**
     * Calculate total rows that still need to be processed.
     */
    @Transactional(readOnly = true)
    public Long getRemainingRows(UUID taskId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Long totalRecords = task.getTotalRecords();
        Long processedRecords = task.getProcessedRecords();

        if (totalRecords == null || processedRecords == null) {
            return null;
        }

        return Math.max(0, totalRecords - processedRecords);
    }

    /**
     * Mark a checkpoint when task is paused or stopped.
     * This ensures all progress is persisted before shutdown.
     */
    @Transactional
    public void createCheckpoint(UUID taskId, String reason) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        log.info("Creating checkpoint for task {} - reason: {}", taskId, reason);

        // Update task metadata
        task.setUpdatedAt(OffsetDateTime.now());
        taskRepository.save(task);

        // Update all in-progress table states
        List<TableProgress> inProgress = getIncompleteTables(taskId);
        for (TableProgress tp : inProgress) {
            tp.setUpdatedAt(OffsetDateTime.now());
            tableProgressRepository.save(tp);
        }

        log.info("Checkpoint created for task {}: {} tables in progress, {} completed",
                taskId, inProgress.size(), task.getCompletedTables());
    }

    /**
     * DTO for checkpoint state
     */
    @lombok.Data
    @lombok.Builder
    public static class CheckpointState {
        private UUID taskId;
        private TaskStatus taskStatus;
        private OffsetDateTime lastStartedAt;
        private List<ConnectorConfig> connectorConfigs;
        private List<TableProgress> tableProgress;
        private Integer totalTables;
        private Integer completedTables;
        private Long processedRecords;
        private OffsetDateTime checkpointTime;
    }

    /**
     * DTO for connector validation result
     */
    @lombok.Data
    public static class ConnectorValidationResult {
        private UUID taskId;
        private java.util.Map<String, String> existingConnectors = new java.util.HashMap<>();
        private java.util.List<String> missingConnectors = new java.util.ArrayList<>();
        private java.util.Map<String, String> failedValidations = new java.util.HashMap<>();

        public void addExisting(String name, String state) {
            existingConnectors.put(name, state);
        }

        public void addMissing(String name) {
            missingConnectors.add(name);
        }

        public void addFailed(String name, String error) {
            failedValidations.put(name, error);
        }

        public boolean allConnectorsExist() {
            return missingConnectors.isEmpty() && failedValidations.isEmpty();
        }

        public boolean hasIssues() {
            return !missingConnectors.isEmpty() || !failedValidations.isEmpty();
        }
    }
}
