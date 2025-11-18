package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    default List<MigrationTask> findRunningTasks() {
        return findByStatus(TaskStatus.RUNNING);
    }

    /**
     * Find all active tasks (not completed, stopped, or failed).
     */
    default List<MigrationTask> findActiveTasks() {
        return findByStatusIn(java.util.Arrays.asList(
                TaskStatus.CREATED,
                TaskStatus.CONFIGURING,
                TaskStatus.STARTING,
                TaskStatus.RUNNING,
                TaskStatus.PAUSED,
                TaskStatus.STOPPING
        ));
    }

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
     * Derived query for searching by task name containing pattern.
     */
    List<MigrationTask> findByTaskNameContaining(String pattern);

    /**
     * Compatibility method name kept for service; delegates to derived query.
     */
    default List<MigrationTask> searchByTaskName(String pattern) {
        return findByTaskNameContaining(pattern);
    }

    /**
     * Find tasks that have been running for longer than the specified duration.
     */
    List<MigrationTask> findByStatusAndStartedAtBefore(TaskStatus status, OffsetDateTime threshold);

    default List<MigrationTask> findLongRunningTasks(OffsetDateTime threshold) {
        return findByStatusAndStartedAtBefore(TaskStatus.RUNNING, threshold);
    }

    /**
     * Get task statistics summary (built via counts to avoid JPQL literals).
     */
    default List<Object[]> getTaskStatusSummary() {
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        for (TaskStatus s : TaskStatus.values()) {
            long c = countByStatus(s);
            result.add(new Object[]{s, c});
        }
        return result;
    }

    /**
     * Delete tasks older than specified date that are in terminal status.
     */
    @Modifying
    int deleteByStatusInAndUpdatedAtBefore(java.util.List<TaskStatus> statuses, OffsetDateTime threshold);

    default int deleteOldCompletedTasks(OffsetDateTime threshold) {
        return deleteByStatusInAndUpdatedAtBefore(java.util.Arrays.asList(TaskStatus.COMPLETED, TaskStatus.STOPPED, TaskStatus.FAILED), threshold);
    }
}
