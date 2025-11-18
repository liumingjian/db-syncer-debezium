package com.dbsyncer.metadata.service;

import com.dbsyncer.metadata.dto.ProgressResponse;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.exception.TaskNotFoundException;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.repository.TableProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressTrackingServiceTest {

    @Mock
    private TableProgressRepository progressRepository;

    @Mock
    private MigrationTaskRepository taskRepository;

    @InjectMocks
    private ProgressTrackingService progressTrackingService;

    private UUID taskId;
    private UUID progressId;
    private MigrationTask task;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        progressId = UUID.randomUUID();
        task = MigrationTask.builder()
                .id(taskId)
                .taskName("test-task")
                .build();
    }

    @Test
    @DisplayName("createTableProgress should persist progress entity and return response")
    void createTableProgressPersistsEntity() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(progressRepository.save(any(TableProgress.class)))
                .thenAnswer(invocation -> {
                    TableProgress tp = invocation.getArgument(0);
                    tp.setId(progressId);
                    return tp;
                });

        ProgressResponse response = progressTrackingService.createTableProgress(
                taskId, "public", "users", "public", "users", 1000L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(progressId);
        assertThat(response.getSourceSchema()).isEqualTo("public");
        assertThat(response.getSourceTable()).isEqualTo("users");
        assertThat(response.getStatus()).isEqualTo(ProgressStatus.PENDING);

        verify(taskRepository).findById(taskId);
        verify(progressRepository).save(any(TableProgress.class));
    }

    @Test
    @DisplayName("createTableProgress should throw when task not found")
    void createTableProgressTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressTrackingService.createTableProgress(
                taskId, "public", "users", "public", "users", 1000L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("completeTableMigration should mark table completed and update task completedTables")
    void completeTableMigrationUpdatesTaskCompletedTables() {
        TableProgress progress = TableProgress.builder()
                .id(progressId)
                .task(task)
                .sourceSchema("public")
                .sourceTable("orders")
                .targetSchema("public")
                .targetTable("orders")
                .status(ProgressStatus.SNAPSHOTTING)
                .build();

        when(progressRepository.findById(progressId)).thenReturn(Optional.of(progress));
        when(progressRepository.save(any(TableProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.countByTaskIdAndStatus(taskId, ProgressStatus.COMPLETED))
                .thenReturn(5L);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ProgressResponse response = progressTrackingService.completeTableMigration(progressId);

        assertThat(response.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(task.getCompletedTables()).isEqualTo(5);

        verify(progressRepository).countByTaskIdAndStatus(taskId, ProgressStatus.COMPLETED);
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("estimateEtaSeconds should return null when there is no progress data")
    void estimateEtaReturnsNullWhenNoProgress() {
        when(progressRepository.findByTaskId(taskId)).thenReturn(Collections.emptyList());

        Long eta = progressTrackingService.estimateEtaSeconds(taskId);

        assertThat(eta).isNull();
    }

    @Test
    @DisplayName("estimateEtaSeconds should return 0 when all estimated rows are processed")
    void estimateEtaReturnsZeroWhenCompleted() {
        TableProgress tp = TableProgress.builder()
                .id(progressId)
                .task(task)
                .estimatedRows(200L)
                .snapshotRowsWritten(200L)
                .snapshotStartedAt(OffsetDateTime.now().minusSeconds(10))
                .build();

        when(progressRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(tp));

        Long eta = progressTrackingService.estimateEtaSeconds(taskId);

        assertThat(eta).isEqualTo(0L);
    }

    @Test
    @DisplayName("updateSnapshotProgress should move status to SNAPSHOTTING and set snapshot fields")
    void updateSnapshotProgressUpdatesFields() {
        TableProgress existing = TableProgress.builder()
                .id(progressId)
                .task(task)
                .status(ProgressStatus.PENDING)
                .snapshotRowsRead(0L)
                .snapshotRowsWritten(0L)
                .build();

        when(progressRepository.findById(progressId)).thenReturn(Optional.of(existing));
        when(progressRepository.save(any(TableProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressTrackingService.updateSnapshotProgress(progressId, 50L, 40L);

        assertThat(response.getStatus()).isEqualTo(ProgressStatus.SNAPSHOTTING);
        assertThat(response.getSnapshotRowsRead()).isEqualTo(50L);
        assertThat(response.getSnapshotRowsWritten()).isEqualTo(40L);
        assertThat(response.getSnapshotStartedAt()).isNotNull();

        verify(progressRepository).findById(progressId);
        verify(progressRepository).save(any(TableProgress.class));
    }
}

