package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for task execution logs.
 */
@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {

    /**
     * Find logs for a task ordered by time descending.
     */
    List<TaskLog> findByTaskIdOrderByLoggedAtDesc(UUID taskId);

    /**
     * Find logs for a task with given level.
     */
    List<TaskLog> findByTaskIdAndLogLevelOrderByLoggedAtDesc(UUID taskId, String logLevel);
}

