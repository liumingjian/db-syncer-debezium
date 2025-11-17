package com.dbsyncer.connectors.config;

/**
 * Snapshot modes for Debezium connectors.
 */
public enum SnapshotMode {

    /**
     * Perform a snapshot when needed.
     * MySQL: initial (default)
     * PostgreSQL: initial (default)
     * Oracle: initial (default)
     */
    INITIAL("initial"),

    /**
     * Perform a snapshot on first connector start.
     */
    INITIAL_ONLY("initial_only"),

    /**
     * Never perform a snapshot.
     */
    NEVER("never"),

    /**
     * Perform a snapshot when no offset is found.
     * MySQL only.
     */
    WHEN_NEEDED("when_needed"),

    /**
     * Always perform a snapshot.
     * Oracle only.
     */
    ALWAYS("always"),

    /**
     * Perform a schema-only snapshot.
     */
    SCHEMA_ONLY("schema_only"),

    /**
     * Perform a schema-only snapshot when no offset is found.
     */
    SCHEMA_ONLY_RECOVERY("schema_only_recovery");

    private final String value;

    SnapshotMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
