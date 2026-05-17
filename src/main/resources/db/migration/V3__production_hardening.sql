CREATE INDEX IF NOT EXISTS idx_outbox_publishable ON outbox_events(status, attempts, created_at);
CREATE INDEX IF NOT EXISTS idx_variant_metrics_dashboard ON variant_metrics(experiment_key, variant_key, metric_name);
