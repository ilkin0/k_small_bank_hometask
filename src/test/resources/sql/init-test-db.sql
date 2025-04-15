-- cleanup tables
DELETE
FROM transactions;

DELETE
FROM customers
WHERE uid = '019630c5-eccf-7b24-b814-a39c97c64b8b';

-- Initialize test customer data
INSERT INTO customers (id, uid, name, surname, birth_date, phone_number, balance)
VALUES (1, '019630c5-eccf-7b24-b814-a39c97c64b8b', 'Eldar', 'Mammadov', '1985-03-15', '+994501234567', 100.00);


-- Initialize test transaction data
INSERT INTO transactions
(uid, customer_id, type, amount, description, transaction_date, status, reference_id)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 1, 'TOP_UP', 100.00, 'Test transaction',
        CURRENT_TIMESTAMP, 'PENDING', '');
