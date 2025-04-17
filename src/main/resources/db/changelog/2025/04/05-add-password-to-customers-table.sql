-- liquibase formatted sql

-- changeset ilkinmehdiyev:add-password-to-customers-table
ALTER TABLE customers
    ADD COLUMN password VARCHAR(255);
-- rollback ALTER TABLE customers DROP COLUMN password;

-- changeset ilkinmehdiyev:set-default-password-not-null
UPDATE customers
SET password = '$2a$10$f0zqjKXV4MEwHinjHcWUpeeVpeGH55k4FsHqDQhuAxCUmMrV.CmD.';

ALTER TABLE customers
    ALTER COLUMN password SET NOT NULL;
-- rollback ALTER TABLE customers ALTER COLUMN password DROP NOT NULL;
