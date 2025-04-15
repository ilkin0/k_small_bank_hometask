-- liquibase formatted sql

-- changeset ilkinmehdiyev:create-transactions-table
CREATE TABLE transactions
(
    id               BIGSERIAL PRIMARY KEY NOT NULL,
    uid              UUID                  NOT NULL,
    customer_id      BIGINT                NOT NULL,
    type             VARCHAR(16)           NOT NULL,
    amount           DECIMAL               NOT NULL,
    description      VARCHAR(255),
    transaction_date TIMESTAMPTZ           NOT NULL DEFAULT now(),
    reference_uid    UUID,
    status           VARCHAR(10)           NOT NULL,
    created_at       TIMESTAMPTZ           NOT NULL DEFAULT now(),
    created_by       VARCHAR(50)           NOT NULL DEFAULT 'system',

    CONSTRAINT fk_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_transaction_type CHECK (type IN ('TOP_UP', 'PURCHASE', 'REFUND', 'PARTIAL_REFUND')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('COMPLETED', 'FAILED', 'PENDING', 'REFUNDED')),
    CONSTRAINT uq_idempotency_key UNIQUE (uid)
);

-- Add index on customer_id for faster lookups
CREATE INDEX idx_transactions_uid ON transactions (uid);

-- Add index on customer_id for faster lookups
CREATE INDEX idx_transactions_customer ON transactions (customer_id);

-- Add index on transaction date for reporting queries
CREATE INDEX idx_transactions_date ON transactions (transaction_date);

-- changeset ilkinmehdiyev:create-transactions-log-sequence
CREATE SEQUENCE IF NOT EXISTS transactions_log_id_sequence
    INCREMENT BY 1
    START WITH 1;
-- rollback DROP SEQUENCE transactions_log_id_sequence;

-- changeset ilkinmehdiyev:create-transactions-log-table
CREATE TABLE transactions_log
(
    id              BIGSERIAL PRIMARY KEY NOT NULL,
    log_created     TIMESTAMPTZ           NOT NULL DEFAULT now(),
    operation       VARCHAR(1)            NOT NULL,
    transaction_id  BIGINT                NOT NULL,
    changed_by      VARCHAR(50)           NOT NULL DEFAULT 'system',
    previous_status VARCHAR(10),
    new_status      VARCHAR(10)
);
-- rollback DROP TABLE transactions_log;

-- changeset ilkinmehdiyev:create-transactions-log-index
CREATE INDEX ON transactions_log (transaction_id);
-- rollback DROP INDEX IF EXISTS idx_transactions_log_transaction_id;

-- changeset ilkinmehdiyev:create-insert-transactions-log-row-function runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION insert_transactions_log_row()
    RETURNS TRIGGER
AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO transactions_log
        (id, log_created, operation, transaction_id, changed_by, previous_status, new_status)
        VALUES (nextval('transactions_log_id_sequence'), now(), 'D', old.id, 'system', old.status, NULL);
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        -- Only log updates that change the status
        IF (old.status IS DISTINCT FROM new.status) THEN
            INSERT INTO transactions_log
            (id, log_created, operation, transaction_id, changed_by, previous_status, new_status)
            VALUES (nextval('transactions_log_id_sequence'), now(), 'U', new.id, 'system', old.status, new.status);
        END IF;
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO transactions_log
        (id, log_created, operation, transaction_id, changed_by, previous_status, new_status)
        VALUES (nextval('transactions_log_id_sequence'), now(), 'I', new.id, 'system', NULL, new.status);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
-- rollback DROP FUNCTION IF EXISTS insert_transactions_log_row();

-- changeset ilkinmehdiyev:create-log-transactions-trigger
CREATE TRIGGER log_transactions
    AFTER INSERT OR UPDATE OR DELETE
    ON transactions
    FOR EACH ROW
EXECUTE PROCEDURE insert_transactions_log_row();
-- rollback DROP TRIGGER IF EXISTS log_transactions ON transactions;

-- rollback DROP TABLE transactions;