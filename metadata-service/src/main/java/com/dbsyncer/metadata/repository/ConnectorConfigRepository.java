package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.ConnectorConfig;
import com.dbsyncer.metadata.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<ConnectorConfig> findByTaskIdAndConnectorType(UUID taskId, ConnectorType connectorType, org.springframework.data.domain.Sort sort);

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
    @Query("SELECT cc FROM ConnectorConfig cc WHERE cc.errorMessage IS NOT NULL")
    List<ConnectorConfig> findConnectorsWithErrors();

    /**
     * Find source connector for a task.
     */
    @Query("SELECT cc FROM ConnectorConfig cc WHERE cc.task.id = :taskId AND cc.connectorType = 'SOURCE'")
    Optional<ConnectorConfig> findSourceConnector(@Param("taskId") UUID taskId);

    /**
     * Find sink connector for a task.
     */
    @Query("SELECT cc FROM ConnectorConfig cc WHERE cc.task.id = :taskId AND cc.connectorType = 'SINK'")
    Optional<ConnectorConfig> findSinkConnector(@Param("taskId") UUID taskId);

    /**
     * Delete all connectors for a task.
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Update deployed status for a connector.
     */
    @Query("UPDATE ConnectorConfig cc SET cc.deployed = :deployed, cc.deployedAt = CURRENT_TIMESTAMP WHERE cc.id = :id")
    void updateDeployedStatus(@Param("id") UUID id, @Param("deployed") Boolean deployed);
}
