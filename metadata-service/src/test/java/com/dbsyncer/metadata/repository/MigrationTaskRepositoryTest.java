package com.dbsyncer.metadata.repository;

import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MigrationTaskRepositoryTest {

    @Autowired
    private MigrationTaskRepository taskRepository;

    private MigrationTask testTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        testTask = MigrationTask.builder()
                .taskName("test-task")
                .description("Test migration task")
                .sourceType(DatabaseType.MYSQL)
                .sourceHost("localhost")
                .sourcePort(3306)
                .sourceDatabase("sourcedb")
                .sourceUsername("root")
                .sourcePassword("password")
                .targetType(DatabaseType.POSTGRESQL)
                .targetHost("localhost")
                .targetPort(5432)
                .targetDatabase("targetdb")
                .targetUsername("postgres")
                .targetPassword("password")
                .status(TaskStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve task by ID")
    void shouldSaveAndRetrieveTaskById() {
        MigrationTask saved = taskRepository.save(testTask);

        Optional<MigrationTask> found = taskRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTaskName()).isEqualTo("test-task");
        assertThat(found.get().getSourceType()).isEqualTo(DatabaseType.MYSQL);
        assertThat(found.get().getTargetType()).isEqualTo(DatabaseType.POSTGRESQL);
    }

    @Test
    @DisplayName("Should find task by name")
    void shouldFindTaskByName() {
        taskRepository.save(testTask);

        Optional<MigrationTask> found = taskRepository.findByTaskName("test-task");

        assertThat(found).isPresent();
        assertThat(found.get().getTaskName()).isEqualTo("test-task");
    }

    @Test
    @DisplayName("Should check if task exists by name")
    void shouldCheckIfTaskExistsByName() {
        taskRepository.save(testTask);

        boolean exists = taskRepository.existsByTaskName("test-task");
        boolean notExists = taskRepository.existsByTaskName("non-existent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find tasks by status")
    void shouldFindTasksByStatus() {
        testTask.setStatus(TaskStatus.RUNNING);
        taskRepository.save(testTask);

        MigrationTask task2 = MigrationTask.builder()
                .taskName("test-task-2")
                .sourceType(DatabaseType.MYSQL)
                .sourceHost("localhost")
                .sourcePort(3306)
                .sourceDatabase("sourcedb")
                .sourceUsername("root")
                .sourcePassword("password")
                .targetType(DatabaseType.POSTGRESQL)
                .targetHost("localhost")
                .targetPort(5432)
                .targetDatabase("targetdb")
                .targetUsername("postgres")
                .targetPassword("password")
                .status(TaskStatus.CREATED)
                .build();
        taskRepository.save(task2);

        List<MigrationTask> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
        List<MigrationTask> createdTasks = taskRepository.findByStatus(TaskStatus.CREATED);

        assertThat(runningTasks).hasSize(1);
        assertThat(runningTasks.get(0).getTaskName()).isEqualTo("test-task");
        assertThat(createdTasks).hasSize(1);
        assertThat(createdTasks.get(0).getTaskName()).isEqualTo("test-task-2");
    }

    @Test
    @DisplayName("Should count tasks by status")
    void shouldCountTasksByStatus() {
        testTask.setStatus(TaskStatus.RUNNING);
        taskRepository.save(testTask);

        long count = taskRepository.countByStatus(TaskStatus.RUNNING);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find tasks by source and target type")
    void shouldFindTasksBySourceAndTargetType() {
        taskRepository.save(testTask);

        List<MigrationTask> tasks = taskRepository.findBySourceTypeAndTargetType(
                DatabaseType.MYSQL, DatabaseType.POSTGRESQL);

        assertThat(tasks).hasSize(1);
    }

    @Test
    @DisplayName("Should search tasks by name pattern")
    void shouldSearchTasksByNamePattern() {
        taskRepository.save(testTask);

        List<MigrationTask> tasks = taskRepository.searchByTaskName("test");

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTaskName()).contains("test");
    }

    @Test
    @DisplayName("Should update task status")
    void shouldUpdateTaskStatus() {
        MigrationTask saved = taskRepository.save(testTask);

        saved.setStatus(TaskStatus.RUNNING);
        taskRepository.save(saved);

        Optional<MigrationTask> found = taskRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TaskStatus.RUNNING);
    }
}
