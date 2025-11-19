-- V1__initial_schema.sql
-- Initial database schema for DB-Syncer-Debezium metadata storage

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enum type for task status
CREATE TYPE task_status AS ENUM (
    'CREATED',
    'CONFIGURING',
    'STARTING',
    'RUNNING',
    'PAUSED',
    'STOPPING',
    'STOPPED',
    'COMPLETED',
    'FAILED'
);

-- Enum type for database type (used by type_mapping_rules and other metadata tables)
CREATE TYPE database_type AS ENUM (
    'MYSQL',
    'POSTGRESQL',
    'ORACLE'
);

-- Enum type for connector type
CREATE TYPE connector_type AS ENUM (
    'SOURCE',
    'SINK'
);

-- Enum type for table progress status
CREATE TYPE progress_status AS ENUM (
    'PENDING',
    'SNAPSHOTTING',
    'STREAMING',
    'COMPLETED',
    'FAILED'
);

-- Main migration tasks table
CREATE TABLE migration_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,

    -- Source database configuration
    -- Use plain VARCHAR for database type to simplify JDBC mappings
    source_type VARCHAR(50) NOT NULL,
    source_host VARCHAR(255) NOT NULL,
    source_port INTEGER NOT NULL,
    source_database VARCHAR(255) NOT NULL,
    source_username VARCHAR(255) NOT NULL,
    source_password VARCHAR(255) NOT NULL,
    source_properties JSONB DEFAULT '{}'::jsonb,

    -- Target database configuration
    target_type VARCHAR(50) NOT NULL,
    target_host VARCHAR(255) NOT NULL,
    target_port INTEGER NOT NULL,
    target_database VARCHAR(255) NOT NULL,
    target_username VARCHAR(255) NOT NULL,
    target_password VARCHAR(255) NOT NULL,
    target_properties JSONB DEFAULT '{}'::jsonb,

    -- Table selection
    include_tables TEXT[],
    exclude_tables TEXT[],

    -- Task configuration
    snapshot_mode VARCHAR(50) DEFAULT 'initial',
    batch_size INTEGER DEFAULT 10000,
    max_queue_size INTEGER DEFAULT 8192,
    poll_interval_ms INTEGER DEFAULT 1000,

    -- Task status (store as VARCHAR to simplify JDBC mappings)
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    error_message TEXT,

    -- Statistics
    total_tables INTEGER DEFAULT 0,
    completed_tables INTEGER DEFAULT 0,
    total_records BIGINT DEFAULT 0,
    processed_records BIGINT DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    -- Metadata
    created_by VARCHAR(255),
    tags JSONB DEFAULT '[]'::jsonb
);

-- Index for task queries
CREATE INDEX idx_migration_tasks_status ON migration_tasks(status);
CREATE INDEX idx_migration_tasks_created_at ON migration_tasks(created_at DESC);
CREATE INDEX idx_migration_tasks_task_name ON migration_tasks(task_name);

-- Table-level progress tracking
CREATE TABLE table_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES migration_tasks(id) ON DELETE CASCADE,

    -- Table identification
    source_schema VARCHAR(255),
    source_table VARCHAR(255) NOT NULL,
    target_schema VARCHAR(255),
    target_table VARCHAR(255) NOT NULL,

    -- Progress tracking
    status progress_status NOT NULL DEFAULT 'PENDING',

    -- Snapshot progress
    estimated_rows BIGINT,
    snapshot_rows_read BIGINT DEFAULT 0,
    snapshot_rows_written BIGINT DEFAULT 0,
    snapshot_completed BOOLEAN DEFAULT FALSE,
    snapshot_started_at TIMESTAMP WITH TIME ZONE,
    snapshot_completed_at TIMESTAMP WITH TIME ZONE,

    -- Streaming progress
    streaming_events_processed BIGINT DEFAULT 0,
    last_event_timestamp TIMESTAMP WITH TIME ZONE,
    current_lag_ms BIGINT DEFAULT 0,

    -- Error tracking
    error_count INTEGER DEFAULT 0,
    last_error TEXT,
    last_error_at TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    UNIQUE(task_id, source_schema, source_table)
);

-- Index for progress queries
CREATE INDEX idx_table_progress_task_id ON table_progress(task_id);
CREATE INDEX idx_table_progress_status ON table_progress(status);

-- Connector configurations
CREATE TABLE connector_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES migration_tasks(id) ON DELETE CASCADE,

    -- Connector identification
    connector_name VARCHAR(255) NOT NULL UNIQUE,
    connector_class VARCHAR(500) NOT NULL,
    connector_type connector_type NOT NULL,

    -- Configuration
    config JSONB NOT NULL,

    -- Deployment status
    deployed BOOLEAN DEFAULT FALSE,
    deployed_at TIMESTAMP WITH TIME ZONE,

    -- Status tracking
    status VARCHAR(50),
    worker_id VARCHAR(255),
    tasks_count INTEGER DEFAULT 0,

    -- Error information
    error_message TEXT,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for connector queries
CREATE INDEX idx_connector_configs_task_id ON connector_configs(task_id);
CREATE INDEX idx_connector_configs_connector_name ON connector_configs(connector_name);
CREATE INDEX idx_connector_configs_deployed ON connector_configs(deployed);

-- Debezium offset storage
CREATE TABLE debezium_offsets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES migration_tasks(id) ON DELETE CASCADE,

    -- Partition identification
    partition_key VARCHAR(512) NOT NULL,

    -- Offset data
    offset_value JSONB NOT NULL,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    UNIQUE(task_id, partition_key)
);

-- Index for offset queries
CREATE INDEX idx_debezium_offsets_task_id ON debezium_offsets(task_id);
CREATE INDEX idx_debezium_offsets_partition_key ON debezium_offsets(partition_key);

-- Schema history storage
CREATE TABLE schema_history (
    id BIGSERIAL PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES migration_tasks(id) ON DELETE CASCADE,

    -- History record
    history_id VARCHAR(255) NOT NULL,
    source_partition JSONB NOT NULL,
    source_offset JSONB NOT NULL,
    database_name VARCHAR(255),
    schema_name VARCHAR(255),
    table_name VARCHAR(255),

    -- DDL information
    ddl_statements TEXT,
    table_changes JSONB,

    -- Timestamps
    is_snapshot BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for schema history queries
CREATE INDEX idx_schema_history_task_id ON schema_history(task_id);
CREATE INDEX idx_schema_history_history_id ON schema_history(history_id);
CREATE INDEX idx_schema_history_recorded_at ON schema_history(recorded_at);

-- Task execution logs
CREATE TABLE task_logs (
    id BIGSERIAL PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES migration_tasks(id) ON DELETE CASCADE,

    -- Log information
    log_level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    context JSONB DEFAULT '{}'::jsonb,

    -- Source information
    source_component VARCHAR(100),

    -- Timestamp
    logged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for log queries
CREATE INDEX idx_task_logs_task_id ON task_logs(task_id);
CREATE INDEX idx_task_logs_logged_at ON task_logs(logged_at DESC);
CREATE INDEX idx_task_logs_log_level ON task_logs(log_level);

-- Type mapping rules (customizable)
CREATE TABLE type_mapping_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Mapping identification
    source_db_type database_type NOT NULL,
    target_db_type database_type NOT NULL,
    source_data_type VARCHAR(100) NOT NULL,
    target_data_type VARCHAR(100) NOT NULL,

    -- Transformation rules
    transformation_expression TEXT,

    -- Priority (higher = more specific)
    priority INTEGER DEFAULT 0,

    -- Metadata
    description TEXT,
    is_default BOOLEAN DEFAULT TRUE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    UNIQUE(source_db_type, target_db_type, source_data_type, priority)
);

-- Index for type mapping queries
CREATE INDEX idx_type_mapping_rules_source_target ON type_mapping_rules(source_db_type, target_db_type);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply the trigger to relevant tables
CREATE TRIGGER update_migration_tasks_updated_at
    BEFORE UPDATE ON migration_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_table_progress_updated_at
    BEFORE UPDATE ON table_progress
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_connector_configs_updated_at
    BEFORE UPDATE ON connector_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_debezium_offsets_updated_at
    BEFORE UPDATE ON debezium_offsets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_type_mapping_rules_updated_at
    BEFORE UPDATE ON type_mapping_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default type mappings for MySQL -> PostgreSQL
INSERT INTO type_mapping_rules (source_db_type, target_db_type, source_data_type, target_data_type, description) VALUES
-- Numeric types
('MYSQL', 'POSTGRESQL', 'TINYINT', 'SMALLINT', 'MySQL TINYINT to PostgreSQL SMALLINT'),
('MYSQL', 'POSTGRESQL', 'SMALLINT', 'SMALLINT', 'MySQL SMALLINT to PostgreSQL SMALLINT'),
('MYSQL', 'POSTGRESQL', 'MEDIUMINT', 'INTEGER', 'MySQL MEDIUMINT to PostgreSQL INTEGER'),
('MYSQL', 'POSTGRESQL', 'INT', 'INTEGER', 'MySQL INT to PostgreSQL INTEGER'),
('MYSQL', 'POSTGRESQL', 'INTEGER', 'INTEGER', 'MySQL INTEGER to PostgreSQL INTEGER'),
('MYSQL', 'POSTGRESQL', 'BIGINT', 'BIGINT', 'MySQL BIGINT to PostgreSQL BIGINT'),
('MYSQL', 'POSTGRESQL', 'FLOAT', 'REAL', 'MySQL FLOAT to PostgreSQL REAL'),
('MYSQL', 'POSTGRESQL', 'DOUBLE', 'DOUBLE PRECISION', 'MySQL DOUBLE to PostgreSQL DOUBLE PRECISION'),
('MYSQL', 'POSTGRESQL', 'DECIMAL', 'DECIMAL', 'MySQL DECIMAL to PostgreSQL DECIMAL'),
('MYSQL', 'POSTGRESQL', 'NUMERIC', 'NUMERIC', 'MySQL NUMERIC to PostgreSQL NUMERIC'),

-- String types
('MYSQL', 'POSTGRESQL', 'CHAR', 'CHAR', 'MySQL CHAR to PostgreSQL CHAR'),
('MYSQL', 'POSTGRESQL', 'VARCHAR', 'VARCHAR', 'MySQL VARCHAR to PostgreSQL VARCHAR'),
('MYSQL', 'POSTGRESQL', 'TINYTEXT', 'TEXT', 'MySQL TINYTEXT to PostgreSQL TEXT'),
('MYSQL', 'POSTGRESQL', 'TEXT', 'TEXT', 'MySQL TEXT to PostgreSQL TEXT'),
('MYSQL', 'POSTGRESQL', 'MEDIUMTEXT', 'TEXT', 'MySQL MEDIUMTEXT to PostgreSQL TEXT'),
('MYSQL', 'POSTGRESQL', 'LONGTEXT', 'TEXT', 'MySQL LONGTEXT to PostgreSQL TEXT'),

-- Date/Time types
('MYSQL', 'POSTGRESQL', 'DATE', 'DATE', 'MySQL DATE to PostgreSQL DATE'),
('MYSQL', 'POSTGRESQL', 'TIME', 'TIME', 'MySQL TIME to PostgreSQL TIME'),
('MYSQL', 'POSTGRESQL', 'DATETIME', 'TIMESTAMP', 'MySQL DATETIME to PostgreSQL TIMESTAMP'),
('MYSQL', 'POSTGRESQL', 'TIMESTAMP', 'TIMESTAMP WITH TIME ZONE', 'MySQL TIMESTAMP to PostgreSQL TIMESTAMP WITH TIME ZONE'),
('MYSQL', 'POSTGRESQL', 'YEAR', 'INTEGER', 'MySQL YEAR to PostgreSQL INTEGER'),

-- Binary types
('MYSQL', 'POSTGRESQL', 'BINARY', 'BYTEA', 'MySQL BINARY to PostgreSQL BYTEA'),
('MYSQL', 'POSTGRESQL', 'VARBINARY', 'BYTEA', 'MySQL VARBINARY to PostgreSQL BYTEA'),
('MYSQL', 'POSTGRESQL', 'TINYBLOB', 'BYTEA', 'MySQL TINYBLOB to PostgreSQL BYTEA'),
('MYSQL', 'POSTGRESQL', 'BLOB', 'BYTEA', 'MySQL BLOB to PostgreSQL BYTEA'),
('MYSQL', 'POSTGRESQL', 'MEDIUMBLOB', 'BYTEA', 'MySQL MEDIUMBLOB to PostgreSQL BYTEA'),
('MYSQL', 'POSTGRESQL', 'LONGBLOB', 'BYTEA', 'MySQL LONGBLOB to PostgreSQL BYTEA'),

-- Other types
('MYSQL', 'POSTGRESQL', 'JSON', 'JSONB', 'MySQL JSON to PostgreSQL JSONB'),
('MYSQL', 'POSTGRESQL', 'ENUM', 'VARCHAR', 'MySQL ENUM to PostgreSQL VARCHAR'),
('MYSQL', 'POSTGRESQL', 'SET', 'TEXT[]', 'MySQL SET to PostgreSQL TEXT ARRAY'),
('MYSQL', 'POSTGRESQL', 'BIT', 'BIT', 'MySQL BIT to PostgreSQL BIT'),
('MYSQL', 'POSTGRESQL', 'BOOLEAN', 'BOOLEAN', 'MySQL BOOLEAN to PostgreSQL BOOLEAN'),
('MYSQL', 'POSTGRESQL', 'BOOL', 'BOOLEAN', 'MySQL BOOL to PostgreSQL BOOLEAN');

-- Insert default type mappings for Oracle -> PostgreSQL
INSERT INTO type_mapping_rules (source_db_type, target_db_type, source_data_type, target_data_type, description) VALUES
-- Numeric types
('ORACLE', 'POSTGRESQL', 'NUMBER', 'NUMERIC', 'Oracle NUMBER to PostgreSQL NUMERIC'),
('ORACLE', 'POSTGRESQL', 'BINARY_FLOAT', 'REAL', 'Oracle BINARY_FLOAT to PostgreSQL REAL'),
('ORACLE', 'POSTGRESQL', 'BINARY_DOUBLE', 'DOUBLE PRECISION', 'Oracle BINARY_DOUBLE to PostgreSQL DOUBLE PRECISION'),

-- String types
('ORACLE', 'POSTGRESQL', 'CHAR', 'CHAR', 'Oracle CHAR to PostgreSQL CHAR'),
('ORACLE', 'POSTGRESQL', 'VARCHAR2', 'VARCHAR', 'Oracle VARCHAR2 to PostgreSQL VARCHAR'),
('ORACLE', 'POSTGRESQL', 'NCHAR', 'CHAR', 'Oracle NCHAR to PostgreSQL CHAR'),
('ORACLE', 'POSTGRESQL', 'NVARCHAR2', 'VARCHAR', 'Oracle NVARCHAR2 to PostgreSQL VARCHAR'),
('ORACLE', 'POSTGRESQL', 'CLOB', 'TEXT', 'Oracle CLOB to PostgreSQL TEXT'),
('ORACLE', 'POSTGRESQL', 'NCLOB', 'TEXT', 'Oracle NCLOB to PostgreSQL TEXT'),
('ORACLE', 'POSTGRESQL', 'LONG', 'TEXT', 'Oracle LONG to PostgreSQL TEXT'),

-- Date/Time types
('ORACLE', 'POSTGRESQL', 'DATE', 'TIMESTAMP', 'Oracle DATE to PostgreSQL TIMESTAMP'),
('ORACLE', 'POSTGRESQL', 'TIMESTAMP', 'TIMESTAMP', 'Oracle TIMESTAMP to PostgreSQL TIMESTAMP'),
('ORACLE', 'POSTGRESQL', 'TIMESTAMP WITH TIME ZONE', 'TIMESTAMP WITH TIME ZONE', 'Oracle TIMESTAMP WITH TIME ZONE to PostgreSQL'),
('ORACLE', 'POSTGRESQL', 'TIMESTAMP WITH LOCAL TIME ZONE', 'TIMESTAMP WITH TIME ZONE', 'Oracle TIMESTAMP WITH LOCAL TIME ZONE to PostgreSQL'),
('ORACLE', 'POSTGRESQL', 'INTERVAL YEAR TO MONTH', 'INTERVAL', 'Oracle INTERVAL YEAR TO MONTH to PostgreSQL INTERVAL'),
('ORACLE', 'POSTGRESQL', 'INTERVAL DAY TO SECOND', 'INTERVAL', 'Oracle INTERVAL DAY TO SECOND to PostgreSQL INTERVAL'),

-- Binary types
('ORACLE', 'POSTGRESQL', 'RAW', 'BYTEA', 'Oracle RAW to PostgreSQL BYTEA'),
('ORACLE', 'POSTGRESQL', 'LONG RAW', 'BYTEA', 'Oracle LONG RAW to PostgreSQL BYTEA'),
('ORACLE', 'POSTGRESQL', 'BLOB', 'BYTEA', 'Oracle BLOB to PostgreSQL BYTEA'),
('ORACLE', 'POSTGRESQL', 'BFILE', 'BYTEA', 'Oracle BFILE to PostgreSQL BYTEA'),

-- Other types
('ORACLE', 'POSTGRESQL', 'ROWID', 'VARCHAR', 'Oracle ROWID to PostgreSQL VARCHAR'),
('ORACLE', 'POSTGRESQL', 'XMLTYPE', 'XML', 'Oracle XMLTYPE to PostgreSQL XML');

COMMENT ON TABLE migration_tasks IS 'Main table storing migration task definitions and status';
COMMENT ON TABLE table_progress IS 'Tracks progress for each table being migrated';
COMMENT ON TABLE connector_configs IS 'Stores Kafka Connect connector configurations';
COMMENT ON TABLE debezium_offsets IS 'Persists Debezium CDC offsets for recovery';
COMMENT ON TABLE schema_history IS 'Records schema changes during migration';
COMMENT ON TABLE task_logs IS 'Task execution logs for debugging and monitoring';
COMMENT ON TABLE type_mapping_rules IS 'Defines data type mappings between source and target databases';
