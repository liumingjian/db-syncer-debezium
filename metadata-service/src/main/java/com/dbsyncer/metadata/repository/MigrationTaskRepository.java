package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MigrationTask entity operations.
 */
@Repository
public interface MigrationTaskRepository extends JpaRepository<MigrationTask, UUID> {

    /**
     * Find a task by its name.
     */
    Optional<MigrationTask> findByTaskName(String taskName);

    /**
     * Check if a task with the given name exists.
     */
    boolean existsByTaskName(String taskName);

    /**
     * Find all tasks with a specific status.
     */
    List<MigrationTask> findByStatus(TaskStatus status);

    /**
     * Find all tasks with statuses in the given list.
     */
    List<MigrationTask> findByStatusIn(List<TaskStatus> statuses);

    /**
     * Find tasks by status with pagination.
     */
    Page<MigrationTask> findByStatus(TaskStatus status, Pageable pageable);

    /**
     * Find all running tasks.
     */
    @Query("SELECT t FROM MigrationTask t WHERE t.status = 'RUNNING'")
    List<MigrationTask> findRunningTasks();

    /**
     * Find all active tasks (not completed, stopped, or failed).
     */
    @Query("SELECT t FROM MigrationTask t WHERE t.status IN ('CREATED', 'CONFIGURING', 'STARTING', 'RUNNING', 'PAUSED', 'STOPPING')")
    List<MigrationTask> findActiveTasks();

    /**
     * Find tasks created after a specific time.
     */
    List<MigrationTask> findByCreatedAtAfter(OffsetDateTime timestamp);

    /**
     * Find tasks by source database type.
     */
    List<MigrationTask> findBySourceType(com.dbsyncer.metadata.entity.DatabaseType sourceType);

    /**
     * Find tasks by target database type.
     */
    List<MigrationTask> findByTargetType(com.dbsyncer.metadata.entity.DatabaseType targetType);

    /**
     * Find tasks by source and target database types.
     */
    List<MigrationTask> findBySourceTypeAndTargetType(
            com.dbsyncer.metadata.entity.DatabaseType sourceType,
            com.dbsyncer.metadata.entity.DatabaseType targetType);

    /**
     * Count tasks by status.
     */
    long countByStatus(TaskStatus status);

    /**
     * Find tasks with pagination and sorting.
     */
    Page<MigrationTask> findAll(Pageable pageable);

    /**
     * Search tasks by name pattern.
     */
    @Query("SELECT t FROM MigrationTask t WHERE t.taskName LIKE %:pattern%")
    List<MigrationTask> searchByTaskName(@Param("pattern") String pattern);

    /**
     * Find tasks that have been running for longer than the specified duration.
     */
    @Query("SELECT t FROM MigrationTask t WHERE t.status = 'RUNNING' AND t.startedAt < :threshold")
    List<MigrationTask> findLongRunningTasks(@Param("threshold") OffsetDateTime threshold);

    /**
     * Get task statistics summary.
     */
    @Query("SELECT t.status, COUNT(t) FROM MigrationTask t GROUP BY t.status")
    List<Object[]> getTaskStatusSummary();

    /**
     * Delete tasks older than specified date that are in terminal status.
     */
    @Query("DELETE FROM MigrationTask t WHERE t.status IN ('COMPLETED', 'STOPPED', 'FAILED') AND t.updatedAt < :threshold")
    int deleteOldCompletedTasks(@Param("threshold") OffsetDateTime threshold);
}
