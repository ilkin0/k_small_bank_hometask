-- liquibase formatted sql

-- changeset ilkinmehdiyev:create-tokens-table
CREATE TABLE tokens
(
    id          BIGSERIAL PRIMARY KEY NOT NULL,
    value       VARCHAR(255)          NOT NULL,
    issued_at   TIMESTAMPTZ           NOT NULL,
    expires_at  TIMESTAMPTZ           NOT NULL,
    is_expired  BOOLEAN               NOT NULL DEFAULT false,
    is_active   BOOLEAN               NOT NULL DEFAULT true,
    customer_id BIGINT                NOT NULL,
    CONSTRAINT fk_customer_id FOREIGN KEY (customer_id) REFERENCES customers (id)
);
-- rollback DROP TABLE tokens;

-- changeset ilkinmehdiyev:create-tokens-index
CREATE INDEX idx_tokens_value ON tokens (value);
-- rollback DROP INDEX IF EXISTS idx_tokens_value;

-- changeset ilkinmehdiyev:create-tokens-customer-id-index
CREATE INDEX idx_tokens_customer_id ON tokens (customer_id);
-- rollback DROP INDEX IF EXISTS idx_tokens_customer_id;