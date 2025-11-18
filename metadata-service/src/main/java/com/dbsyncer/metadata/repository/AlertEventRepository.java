package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {

    List<AlertEvent> findByTaskIdOrderByCreatedAtDesc(UUID taskId);
}

