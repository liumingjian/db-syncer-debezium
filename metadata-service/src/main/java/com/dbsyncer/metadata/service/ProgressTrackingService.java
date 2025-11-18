package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.dto.ProgressResponse;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.repository.TableProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tracking migration progress.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgressTrackingService {

    private final TableProgressRepository progressRepository;
    private final MigrationTaskRepository taskRepository;
    private static final long MINIMUM_PROGRESS_EVENTS_FOR_ETA = 100L;

    /**
     * Create progress tracking entry for a table.
     */
    public ProgressResponse createTableProgress(UUID taskId, String sourceSchema, String sourceTable,
                                                 String targetSchema, String targetTable, Long estimatedRows) {
        log.info("Creating progress entry for table {}.{} in task {}", sourceSchema, sourceTable, taskId);

        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        TableProgress progress = TableProgress.builder()
                .task(task)
                .sourceSchema(sourceSchema)
                .sourceTable(sourceTable)
                .targetSchema(targetSchema)
                .targetTable(targetTable)
                .estimatedRows(estimatedRows)
                .status(ProgressStatus.PENDING)
                .build();

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Get all progress entries for a task.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getTaskProgress(UUID taskId) {
        return progressRepository.findByTaskId(taskId).stream()
                .map(ProgressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all table progress entities for a task.
     */
    @Transactional(readOnly = true)
    public List<TableProgress> getTableProgress(UUID taskId) {
        return progressRepository.findByTaskId(taskId);
    }

    /**
     * Get progress for a specific table.
     */
    @Transactional(readOnly = true)
    public ProgressResponse getTableProgress(UUID taskId, String sourceSchema, String sourceTable) {
        TableProgress progress = progressRepository
                .findByTaskIdAndSourceSchemaAndSourceTable(taskId, sourceSchema, sourceTable)
                .orElseThrow(() -> new RuntimeException(
                        "Progress not found for table: " + sourceSchema + "." + sourceTable));
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Update snapshot progress.
     */
    public ProgressResponse updateSnapshotProgress(UUID progressId, Long rowsRead, Long rowsWritten) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setSnapshotRowsRead(rowsRead);
        progress.setSnapshotRowsWritten(rowsWritten);
        progress.setStatus(ProgressStatus.SNAPSHOTTING);

        if (progress.getSnapshotStartedAt() == null) {
            progress.setSnapshotStartedAt(OffsetDateTime.now());
        }

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Mark snapshot as completed.
     */
    public ProgressResponse completeSnapshot(UUID progressId) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setSnapshotCompleted(true);
        progress.setSnapshotCompletedAt(OffsetDateTime.now());
        progress.setStatus(ProgressStatus.STREAMING);

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Update streaming progress.
     */
    public ProgressResponse updateStreamingProgress(UUID progressId, Long eventsProcessed,
                                                    OffsetDateTime lastEventTime, Long lagMs) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setStreamingEventsProcessed(eventsProcessed);
        progress.setLastEventTimestamp(lastEventTime);
        progress.setCurrentLagMs(lagMs);

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Record an error for a table.
     */
    public ProgressResponse recordError(UUID progressId, String errorMessage) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setErrorCount(progress.getErrorCount() + 1);
        progress.setLastError(errorMessage);
        progress.setLastErrorAt(OffsetDateTime.now());

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Mark table migration as completed.
     */
    public ProgressResponse completeTableMigration(UUID progressId) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setStatus(ProgressStatus.COMPLETED);
        progress = progressRepository.save(progress);

        // Update task statistics
        updateTaskCompletedTables(progress.getTask().getId());

        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Mark table migration as failed.
     */
    public ProgressResponse failTableMigration(UUID progressId, String errorMessage) {
        TableProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("Progress not found: " + progressId));

        progress.setStatus(ProgressStatus.FAILED);
        progress.setLastError(errorMessage);
        progress.setLastErrorAt(OffsetDateTime.now());

        progress = progressRepository.save(progress);
        return ProgressResponse.fromEntity(progress);
    }

    /**
     * Get progress summary for a task.
     */
    @Transactional(readOnly = true)
    public Map<ProgressStatus, Long> getProgressSummary(UUID taskId) {
        List<Object[]> summary = progressRepository.getProgressSummary(taskId);
        return summary.stream()
                .collect(Collectors.toMap(
                        row -> (ProgressStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    /**
     * Get total rows processed for a task.
     */
    @Transactional(readOnly = true)
    public Long getTotalRowsProcessed(UUID taskId) {
        return progressRepository.getTotalRowsProcessed(taskId);
    }

    /**
     * Get total estimated rows for a task (sum of estimatedRows across tables).
     */
    @Transactional(readOnly = true)
    public Long getTotalEstimatedRows(UUID taskId) {
        return progressRepository.getTotalEstimatedRows(taskId);
    }

    /**
     * Get average lag for streaming tables.
     */
    @Transactional(readOnly = true)
    public Double getAverageLag(UUID taskId) {
        return progressRepository.getAverageLag(taskId);
    }

    /**
     * Get tables with errors.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getTablesWithErrors(UUID taskId) {
        return progressRepository.findTablesWithErrors(taskId).stream()
                .map(ProgressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Estimate ETA (in seconds) for a task based on snapshot progress.
     * Returns null when estimation is not reliable (e.g. missing estimates or too few processed rows).
     */
    @Transactional(readOnly = true)
    public Long estimateEtaSeconds(UUID taskId) {
        List<TableProgress> tables = progressRepository.findByTaskId(taskId);
        if (tables.isEmpty()) {
            return null;
        }

        long totalEstimated = 0L;
        long totalProcessed = 0L;
        OffsetDateTime earliestStart = null;

        for (TableProgress tp : tables) {
            if (tp.getEstimatedRows() == null || tp.getEstimatedRows() <= 0) {
                continue;
            }
            if (tp.getSnapshotStartedAt() == null) {
                continue;
            }
            totalEstimated += tp.getEstimatedRows();
            totalProcessed += tp.getTotalRowsProcessed();
            if (earliestStart == null || tp.getSnapshotStartedAt().isBefore(earliestStart)) {
                earliestStart = tp.getSnapshotStartedAt();
            }
        }

        if (totalEstimated <= 0 || totalProcessed < MINIMUM_PROGRESS_EVENTS_FOR_ETA || earliestStart == null) {
            return null;
        }

        long elapsedSeconds = Duration.between(earliestStart, OffsetDateTime.now()).getSeconds();
        if (elapsedSeconds <= 0) {
            return null;
        }

        double rate = totalProcessed / (double) elapsedSeconds;
        if (rate <= 0) {
            return null;
        }

        long remaining = totalEstimated - totalProcessed;
        if (remaining <= 0) {
            return 0L;
        }

        return (long) (remaining / rate);
    }

    private void updateTaskCompletedTables(UUID taskId) {
        long completedCount = progressRepository.countByTaskIdAndStatus(taskId, ProgressStatus.COMPLETED);
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setCompletedTables((int) completedCount);
        taskRepository.save(task);
    }
}
