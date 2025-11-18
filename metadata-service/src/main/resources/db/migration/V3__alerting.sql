-- V3__alerting.sql
-- Alert rules and alert events for task execution.

CREATE TYPE alert_severity AS ENUM (
    'INFO',
    'WARN',
    'ERROR'
);

CREATE TYPE alert_channel AS ENUM (
    'EMAIL',
    'WEBHOOK'
);

-- Alert rules
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Scope
    on_task_failure BOOLEAN NOT NULL DEFAULT TRUE,

    -- Thresholds
    min_severity alert_severity NOT NULL DEFAULT 'ERROR',

    -- Channels / targets
    email_recipients TEXT,
    webhook_url TEXT,

    -- Metadata
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_rules_enabled ON alert_rules(enabled);

-- Alert events (history)
CREATE TABLE alert_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID REFERENCES migration_tasks(id) ON DELETE SET NULL,
    rule_id UUID REFERENCES alert_rules(id) ON DELETE SET NULL,

    severity alert_severity NOT NULL,
    channel alert_channel NOT NULL,

    message TEXT NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,

    status VARCHAR(20) NOT NULL,
    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_alert_events_task_id ON alert_events(task_id);
CREATE INDEX idx_alert_events_created_at ON alert_events(created_at DESC);

