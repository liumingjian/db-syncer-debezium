package com.dbsyncer.metadata.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for streaming progress updates coming from Kafka Connect SMT.
 */
@Getter
@Setter
public class StreamingProgressUpdateRequest {

    private String sourceSchema;
    private String sourceTable;

    /**
     * Number of processed records since last update.
     */
    private Long processedDelta;

    /**
     * Event timestamp in epoch milliseconds (Kafka record timestamp).
     */
    private Long eventTimestamp;

    /**
     * Current lag in milliseconds at the time of the last event.
     */
    private Long lagMs;
}

