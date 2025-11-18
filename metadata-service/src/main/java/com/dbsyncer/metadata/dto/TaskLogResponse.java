package com.dbsyncer.metadata.dto;

import com.dbsyncer.metadata.entity.TaskLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO for task execution log entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLogResponse {

    private Long id;
    private String logLevel;
    private String message;
    private Map<String, Object> context;
    private String sourceComponent;
    private OffsetDateTime loggedAt;

    public static TaskLogResponse fromEntity(TaskLog log) {
        return TaskLogResponse.builder()
                .id(log.getId())
                .logLevel(log.getLogLevel())
                .message(log.getMessage())
                .context(log.getContext())
                .sourceComponent(log.getSourceComponent())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}

