CREATE TABLE experiments (
    id UUID PRIMARY KEY,
    experiment_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(240) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    salt VARCHAR(120) NOT NULL,
    traffic_allocation_bp INTEGER NOT NULL DEFAULT 10000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE variants (
    id UUID PRIMARY KEY,
    experiment_id UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    variant_key VARCHAR(120) NOT NULL,
    weight INTEGER NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (experiment_id, variant_key)
);

CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    experiment_key VARCHAR(120) NOT NULL,
    user_hash CHAR(64) NOT NULL,
    variant_key VARCHAR(120) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (experiment_key, user_hash)
);

CREATE TABLE exposure_events (
    id UUID PRIMARY KEY,
    experiment_key VARCHAR(120) NOT NULL,
    user_hash CHAR(64) NOT NULL,
    variant_key VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE tracking_events (
    id UUID PRIMARY KEY,
    experiment_key VARCHAR(120) NOT NULL,
    user_hash CHAR(64) NOT NULL,
    variant_key VARCHAR(120) NOT NULL,
    event_name VARCHAR(160) NOT NULL,
    event_value NUMERIC(20, 6) NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE variant_metrics (
    id UUID PRIMARY KEY,
    experiment_key VARCHAR(120) NOT NULL,
    variant_key VARCHAR(120) NOT NULL,
    metric_name VARCHAR(160) NOT NULL,
    event_count BIGINT NOT NULL DEFAULT 0,
    total_value NUMERIC(20, 6) NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (experiment_key, variant_key, metric_name)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_key VARCHAR(240) NOT NULL,
    event_type VARCHAR(160) NOT NULL,
    kafka_topic VARCHAR(180) NOT NULL,
    kafka_key VARCHAR(240) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(40) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMPTZ
);

CREATE INDEX idx_variants_experiment_id ON variants(experiment_id);
CREATE INDEX idx_assignments_lookup ON assignments(experiment_key, user_hash);
CREATE INDEX idx_exposure_events_experiment_variant ON exposure_events(experiment_key, variant_key, occurred_at);
CREATE INDEX idx_tracking_events_experiment_variant_name ON tracking_events(experiment_key, variant_key, event_name, occurred_at);
CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
