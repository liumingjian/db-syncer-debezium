package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.entity.*;
import com.dbsyncer.metadata.repository.AlertEventRepository;
import com.dbsyncer.metadata.repository.AlertRuleRepository;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Mock
    private MigrationTaskRepository taskRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    @DisplayName("onTaskFailure should create alert events for matching rules")
    void onTaskFailureCreatesEventsForMatchingRules() {
        UUID taskId = UUID.randomUUID();
        MigrationTask task = MigrationTask.builder()
                .id(taskId)
                .taskName("test-task")
                .status(TaskStatus.FAILED)
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        AlertRule rule = AlertRule.builder()
                .id(UUID.randomUUID())
                .name("On task failure")
                .enabled(true)
                .onTaskFailure(true)
                .minSeverity(AlertSeverity.ERROR)
                .emailRecipients("ops@example.com")
                .build();
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        when(alertEventRepository.save(any(AlertEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alertService.onTaskFailure(taskId, "simulated failure", false);

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository, atLeastOnce()).save(captor.capture());

        AlertEvent lastEvent = captor.getValue();
        assertThat(lastEvent.getTask()).isEqualTo(task);
        assertThat(lastEvent.getRule()).isEqualTo(rule);
        assertThat(lastEvent.getChannel()).isEqualTo(AlertChannel.EMAIL);
        assertThat(lastEvent.getSeverity()).isEqualTo(AlertSeverity.ERROR);
        assertThat(lastEvent.getMessage()).contains("simulated failure");
        assertThat(lastEvent.getStatus()).isIn("PENDING", "SENT", "FAILED");
        assertThat(lastEvent.getPayload()).containsEntry("taskId", taskId.toString());
    }

    @Test
    @DisplayName("onTaskFailure should do nothing when task not found")
    void onTaskFailureNoTask() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        alertService.onTaskFailure(taskId, "error", true);

        verify(alertRuleRepository, never()).findByEnabledTrue();
        verify(alertEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("onTaskFailure should skip when no enabled rules")
    void onTaskFailureNoRules() {
        UUID taskId = UUID.randomUUID();
        MigrationTask task = MigrationTask.builder()
                .id(taskId)
                .taskName("test-task")
                .status(TaskStatus.FAILED)
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        alertService.onTaskFailure(taskId, "error", true);

        verify(alertEventRepository, never()).save(any());
    }
}

