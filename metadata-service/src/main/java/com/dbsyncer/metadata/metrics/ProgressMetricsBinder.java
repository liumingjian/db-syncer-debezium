package com.dbsyncer.metadata.metrics;

import com.dbsyncer.metadata.entity.ProgressStatus;
import com.dbsyncer.metadata.entity.TaskStatus;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.metadata.repository.TableProgressRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Registers Micrometer metrics for task and table migration progress.
 */
@Component
public class ProgressMetricsBinder implements MeterBinder {

    private final MigrationTaskRepository taskRepository;
    private final TableProgressRepository tableProgressRepository;

    public ProgressMetricsBinder(MigrationTaskRepository taskRepository,
                                 TableProgressRepository tableProgressRepository) {
        this.taskRepository = taskRepository;
        this.tableProgressRepository = tableProgressRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerTaskMetrics(registry);
        registerTableMetrics(registry);
        registerThroughputMetrics(registry);
        registerLagMetrics(registry);
        registerErrorMetrics(registry);
        registerMvpTaskMetrics(registry);
    }

    private void registerTaskMetrics(MeterRegistry registry) {
        Gauge.builder("dbsyncer.tasks.total", taskRepository, MigrationTaskRepository::count)
                .description("Total number of migration tasks")
                .register(registry);

        Gauge.builder("dbsyncer.tasks.running", taskRepository,
                        repo -> repo.countByStatus(TaskStatus.RUNNING))
                .description("Number of running migration tasks")
                .register(registry);

        Gauge.builder("dbsyncer.tasks.completed", taskRepository,
                        repo -> repo.countByStatus(TaskStatus.COMPLETED))
                .description("Number of successfully completed migration tasks")
                .register(registry);

        Gauge.builder("dbsyncer.tasks.failed", taskRepository,
                        repo -> repo.countByStatus(TaskStatus.FAILED))
                .description("Number of failed migration tasks")
                .register(registry);

        // MVP-style task state metric, aggregated by status.
        for (TaskStatus status : TaskStatus.values()) {
            Gauge.builder("dbsyncer_task_state", taskRepository,
                            repo -> repo.countByStatus(status))
                    .description("Number of tasks in status " + status)
                    .tag("status", status.name())
                    .register(registry);
        }
    }

    private void registerTableMetrics(MeterRegistry registry) {
        Gauge.builder("dbsyncer.tables.total", tableProgressRepository,
                        repo -> repo.count())
                .description("Total number of tables tracked for migration")
                .register(registry);

        Gauge.builder("dbsyncer.tables.snapshotting", tableProgressRepository,
                        repo -> repo.countByStatus(ProgressStatus.SNAPSHOTTING))
                .description("Number of tables currently in snapshot phase")
                .register(registry);

        Gauge.builder("dbsyncer.tables.streaming", tableProgressRepository,
                        repo -> repo.countByStatus(ProgressStatus.STREAMING))
                .description("Number of tables currently in streaming phase")
                .register(registry);

        Gauge.builder("dbsyncer.tables.completed", tableProgressRepository,
                        repo -> repo.countByStatus(ProgressStatus.COMPLETED))
                .description("Number of tables whose migration has completed")
                .register(registry);
    }

    private void registerThroughputMetrics(MeterRegistry registry) {
        Gauge.builder("dbsyncer.records.processed", tableProgressRepository,
                        repo -> {
                            Long total = repo.getTotalRowsProcessedForAllTasks();
                            return total != null ? total.doubleValue() : 0.0;
                        })
                .description("Total number of records processed across all tasks")
                .baseUnit("records")
                .register(registry);
    }

    private void registerLagMetrics(MeterRegistry registry) {
        Gauge.builder("dbsyncer.connector.lag.avg",
                        tableProgressRepository,
                        repo -> {
                            Double lag = repo.getGlobalAverageLag();
                            return lag != null ? lag : 0.0;
                        })
                .description("Average streaming lag across all tables in milliseconds")
                .baseUnit("milliseconds")
                .register(registry);
    }

    private void registerErrorMetrics(MeterRegistry registry) {
        Gauge.builder("dbsyncer.tables.error.count",
                        tableProgressRepository,
                        TableProgressRepository::countTablesWithErrors)
                .description("Number of tables that currently have migration errors")
                .register(registry);
    }

    /**
     * Metrics aligned with MVP criteria and operations guide.
     * These provide task-centric aggregates for Prometheus/Grafana dashboards.
     */
    private void registerMvpTaskMetrics(MeterRegistry registry) {
        // Total processed records across all tasks (snapshot + streaming).
        Gauge.builder("dbsyncer_task_processed_records_total", tableProgressRepository,
                        repo -> {
                            Long total = repo.getTotalRowsProcessedForAllTasks();
                            return total != null ? total.doubleValue() : 0.0;
                        })
                .description("Total number of processed records across all migration tasks")
                .baseUnit("records")
                .register(registry);

        // Total failed records/events across all tasks (based on errorCount).
        Gauge.builder("dbsyncer_task_failed_records_total", tableProgressRepository,
                        repo -> {
                            Long totalErrors = repo.getTotalErrorCountForAllTasks();
                            return totalErrors != null ? totalErrors.doubleValue() : 0.0;
                        })
                .description("Total number of failed records/events across all migration tasks")
                .baseUnit("records")
                .register(registry);

        // Average lag in seconds across all streaming tables.
        Gauge.builder("dbsyncer_task_lag_seconds", tableProgressRepository,
                        repo -> {
                            Double lagMs = repo.getGlobalAverageLag();
                            double ms = lagMs != null ? lagMs : 0.0;
                            return ms / 1000.0;
                        })
                .description("Average end-to-end lag across all tasks in seconds")
                .baseUnit("seconds")
                .register(registry);

        // Most recent event timestamp across all tasks, as Unix epoch seconds.
        Gauge.builder("dbsyncer_task_last_event_timestamp", tableProgressRepository,
                        repo -> {
                            java.time.OffsetDateTime ts = repo.getLastEventTimestampForAllTasks();
                            return ts != null ? (double) ts.toEpochSecond() : 0.0;
                        })
                .description("Last observed event timestamp across all tasks (Unix epoch seconds)")
                .baseUnit("seconds")
                .register(registry);
    }
}
