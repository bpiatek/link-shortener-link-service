CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE links (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL,
    short_url           CITEXT NOT NULL UNIQUE,
    long_url            TEXT NOT NULL,
    title               VARCHAR(255),
    notes               TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_links_on_user_id ON links (user_id);