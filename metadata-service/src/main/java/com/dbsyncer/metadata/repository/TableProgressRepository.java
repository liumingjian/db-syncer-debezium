package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.entity.TableProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TableProgress entity operations.
 */
@Repository
public interface TableProgressRepository extends JpaRepository<TableProgress, UUID> {

    /**
     * Find all progress records for a task.
     */
    List<TableProgress> findByTaskId(UUID taskId);

    /**
     * Find progress for a specific table in a task.
     */
    Optional<TableProgress> findByTaskIdAndSourceSchemaAndSourceTable(
            UUID taskId, String sourceSchema, String sourceTable);

    /**
     * Find all tables with a specific status for a task.
     */
    List<TableProgress> findByTaskIdAndStatus(UUID taskId, ProgressStatus status);

    /**
     * Count tables by status across all tasks.
     */
    long countByStatus(ProgressStatus status);

    /**
     * Count tables by status for a task.
     */
    long countByTaskIdAndStatus(UUID taskId, ProgressStatus status);

    /**
     * Find tables that are currently being processed (snapshotting or streaming).
     */
    @Query("SELECT tp FROM TableProgress tp WHERE tp.task.id = :taskId AND tp.status IN ('SNAPSHOTTING', 'STREAMING')")
    List<TableProgress> findProcessingTables(@Param("taskId") UUID taskId);

    /**
     * Find tables with errors.
     */
    @Query("SELECT tp FROM TableProgress tp WHERE tp.task.id = :taskId AND tp.errorCount > 0")
    List<TableProgress> findTablesWithErrors(@Param("taskId") UUID taskId);

    /**
     * Get total rows processed for a task.
     */
    @Query("SELECT COALESCE(SUM(tp.snapshotRowsWritten), 0) + COALESCE(SUM(tp.streamingEventsProcessed), 0) FROM TableProgress tp WHERE tp.task.id = :taskId")
    Long getTotalRowsProcessed(@Param("taskId") UUID taskId);

    /**
     * Get total rows processed for all tasks.
     */
    @Query("SELECT COALESCE(SUM(tp.snapshotRowsWritten), 0) + COALESCE(SUM(tp.streamingEventsProcessed), 0) FROM TableProgress tp")
    Long getTotalRowsProcessedForAllTasks();

    /**
     * Get total estimated rows for a task.
     */
    @Query("SELECT COALESCE(SUM(tp.estimatedRows), 0) FROM TableProgress tp WHERE tp.task.id = :taskId")
    Long getTotalEstimatedRows(@Param("taskId") UUID taskId);

    /**
     * Get average lag in milliseconds for a task.
     */
    @Query("SELECT COALESCE(AVG(tp.currentLagMs), 0) FROM TableProgress tp WHERE tp.task.id = :taskId AND tp.status = 'STREAMING'")
    Double getAverageLag(@Param("taskId") UUID taskId);

    /**
     * Get average lag in milliseconds for all streaming tables.
     */
    @Query("SELECT COALESCE(AVG(tp.currentLagMs), 0) FROM TableProgress tp WHERE tp.status = 'STREAMING'")
    Double getGlobalAverageLag();

    /**
     * Get progress summary for a task.
     */
    @Query("SELECT tp.status, COUNT(tp) FROM TableProgress tp WHERE tp.task.id = :taskId GROUP BY tp.status")
    List<Object[]> getProgressSummary(@Param("taskId") UUID taskId);

    /**
     * Delete all progress records for a task.
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Find tables that haven't been updated recently (potentially stale).
     */
    @Query("SELECT tp FROM TableProgress tp WHERE tp.task.id = :taskId AND tp.status IN ('SNAPSHOTTING', 'STREAMING') AND tp.updatedAt < :threshold")
    List<TableProgress> findStaleTables(@Param("taskId") UUID taskId, @Param("threshold") java.time.OffsetDateTime threshold);

    /**
     * Count tables that currently have errors.
     */
    @Query("SELECT COUNT(tp) FROM TableProgress tp WHERE tp.errorCount > 0")
    long countTablesWithErrors();
}
