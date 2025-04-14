-- liquibase formatted sql

-- changeset ilkinmehdiyev:create-customers-table
CREATE TABLE customers
(
    id           BIGSERIAL PRIMARY KEY NOT NULL,
    uid          UUID                  NOT NULL,
    name         VARCHAR(31)           NOT NULL,
    surname      VARCHAR(31)           NOT NULL,
    birth_date   DATE                  NOT NULL,
    phone_number VARCHAR(16)           NOT NULL,
    balance      DECIMAL                        DEFAULT 100.0 NOT NULL,
    created_at   TIMESTAMPTZ           NOT NULL DEFAULT now(),
    created_by   VARCHAR(50)           NOT NULL DEFAULT 'system',
    updated_at   TIMESTAMPTZ           NOT NULL DEFAULT now(),
    updated_by   VARCHAR(50)           NOT NULL DEFAULT 'system'
);
-- rollback DROP TABLE customers;

-- changeset ilkinmehdiyev:create-customers-log-table
CREATE TABLE customers_log
(
    id          BIGSERIAL PRIMARY KEY NOT NULL,
    log_created TIMESTAMPTZ           NOT NULL DEFAULT now(),
    operation   VARCHAR(1)            NOT NULL,
    customer_id BIGINT                NOT NULL,
    changed_by  VARCHAR(50)
);
-- rollback DROP TABLE customers;


-- changeset ilkinmehdiyev:create-customers-uid-index
CREATE INDEX idx_customer_uid ON customers (uid);
-- rollback DROP INDEX IF EXISTS idx_customer_uid;

-- changeset ilkinmehdiyev:create-customers-log-seq
CREATE SEQUENCE IF NOT EXISTS customers_log_id_seq
    INCREMENT BY 1
    START WITH 1;
-- rollback DROP SEQUENCE customers_log_id_seq;

-- changeset ilkinmehdiyev:create-customers-log-index
CREATE INDEX idx_customers_log_customers_id ON customers_log (customer_id);
-- rollback DROP INDEX IF EXISTS idx_customers_log_customers_id;

-- changeset ilkinmehdiyev:create-insert-customers-log-row-function runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION insert_customers_log_row()
    RETURNS TRIGGER
AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO customers_log
            (id, log_created, operation, customer_id)
        VALUES (nextval('customers_log_id_seq'), now(), 'D', old.id);
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO customers_log
            (id, log_created, operation, customer_id)
        VALUES (nextval('customers_log_id_seq'), now(), 'U', new.id);
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO customers_log
            (id, log_created, operation, customer_id)
        VALUES (nextval('customers_log_id_seq'), now(), 'I', new.id);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;
-- rollback DROP FUNCTION IF EXISTS insert_customers_log_row();

-- changeset ilkinmehdiyev:create-log-customers-trigger
CREATE
    OR REPLACE TRIGGER log_customers
    AFTER INSERT OR
        UPDATE OR
        DELETE
    ON customers
    FOR EACH ROW
EXECUTE PROCEDURE insert_customers_log_row();
-- rollback DROP TRIGGER IF EXISTS log_customers ON customers;