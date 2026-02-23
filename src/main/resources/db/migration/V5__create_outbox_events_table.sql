CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_id VARCHAR(50) NOT NULL,
                               topic VARCHAR(100) NOT NULL,
                               event_type VARCHAR(50) NOT NULL,
                               payload BYTEA NOT NULL,
                               processed BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events (created_at) WHERE processed = false;