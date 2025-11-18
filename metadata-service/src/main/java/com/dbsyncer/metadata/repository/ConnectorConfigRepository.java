package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.ConnectorConfig;
import com.dbsyncer.metadata.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ConnectorConfig entity operations.
 */
@Repository
public interface ConnectorConfigRepository extends JpaRepository<ConnectorConfig, UUID> {

    /**
     * Find a connector by its name.
     */
    Optional<ConnectorConfig> findByConnectorName(String connectorName);

    /**
     * Check if a connector with the given name exists.
     */
    boolean existsByConnectorName(String connectorName);

    /**
     * Find all connectors for a task.
     */
    List<ConnectorConfig> findByTaskId(UUID taskId);

    /**
     * Find connector by task and type.
     */
    Optional<ConnectorConfig> findByTaskIdAndConnectorType(UUID taskId, ConnectorType connectorType);

    /**
     * Find all connectors for a task by type.
     */
    List<ConnectorConfig> findByTaskIdAndConnectorType(UUID taskId, ConnectorType connectorType, Sort sort);

    /**
     * Find all deployed connectors.
     */
    List<ConnectorConfig> findByDeployedTrue();

    /**
     * Find all non-deployed connectors.
     */
    List<ConnectorConfig> findByDeployedFalse();

    /**
     * Find deployed connectors for a task.
     */
    List<ConnectorConfig> findByTaskIdAndDeployed(UUID taskId, Boolean deployed);

    /**
     * Count deployed connectors for a task.
     */
    long countByTaskIdAndDeployed(UUID taskId, Boolean deployed);

    /**
     * Find connectors with errors.
     */
    List<ConnectorConfig> findByErrorMessageIsNotNull();

    /**
     * Find source connector for a task.
     */
    Optional<ConnectorConfig> findFirstByTaskIdAndConnectorType(UUID taskId, ConnectorType connectorType);

    default Optional<ConnectorConfig> findSourceConnector(UUID taskId) {
        return findFirstByTaskIdAndConnectorType(taskId, ConnectorType.SOURCE);
    }

    /**
     * Find sink connector for a task.
     */
    default Optional<ConnectorConfig> findSinkConnector(UUID taskId) {
        return findFirstByTaskIdAndConnectorType(taskId, ConnectorType.SINK);
    }

    /**
     * Delete all connectors for a task.
     */
    void deleteByTaskId(UUID taskId);

    // Update operations are handled by saving the entity; no JPQL update needed
}
