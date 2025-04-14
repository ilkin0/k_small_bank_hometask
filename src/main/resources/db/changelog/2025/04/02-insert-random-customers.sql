-- liquibase formatted sql

-- changeset ilkinmehdiyev:seed-initial-customers
INSERT INTO customers (uid, name, surname, birth_date, phone_number)
VALUES ('019630c5-eccf-7b24-b814-a39c97c64b8b', 'Eldar', 'Mammadov', '1985-03-15', '+994501234567'),
       ('019630c5-eccf-764f-b976-052e3b5c7e48', 'Leyla', 'Aliyeva', '1990-07-22', '+994552345678'),
       ('019630c5-eccf-77d8-be7d-073cb3b9c056', 'Fuad', 'Hasanov', '1978-11-03', '+994503456789'),
       ('019630c5-eccf-734a-9ff4-b826db5c49c4', 'Narmin', 'Karimova', '1992-05-10', '+994554567890'),
       ('019630c5-eccf-779e-a630-6ffb0c32b1f3', 'Tural', 'Huseynov', '1983-09-28', '+994505678901'),
       ('019630c5-eccf-76c4-a266-9ea325156cdf', 'Aysel', 'Ismayilova', '1995-01-17', '+994556789012'),
       ('019630c5-eccf-7cf0-9359-df82a0567fdc', 'Rashad', 'Jafarov', '1980-06-04', '+994507890123'),
       ('019630c5-eccf-743b-ba0f-85de839bc712', 'Gunel', 'Mammadli', '1988-12-11', '+994558901234'),
       ('019630c5-eccf-722d-9a30-6fc5ab0e0e82', 'Elnur', 'Valiyev', '1975-04-23', '+994509012345'),
       ('019630c5-eccf-776a-83b3-913ca5009ce7', 'Sabina', 'Hasanli', '1993-08-09', '+994550123456');
-- rollback DELETE FROM customers WHERE name IN ('Eldar', 'Leyla', 'Fuad', 'Narmin', 'Tural', 'Aysel', 'Rashad', 'Gunel', 'Elnur', 'Sabina');