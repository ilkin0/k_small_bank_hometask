# K Small Banking REST API

A small banking application REST API that allows customers to manage their accounts, process transactions, and handle
various banking operations.

## Features

- Account management
- Transaction processing (TOP_UP, PURCHASE, PARTIAL_REFUND)
- JWT-based authentication
- HTTPS/SSL support
- Idempotent API requests

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.x, Spring Security 6, Spring Data JDBC
- **Database**: PostgreSQL
- **Security**: JWT Authentication, HTTPS/SSL
- **Testing**: JUnit 5, Mockito, AssertJ, TestContainers, RestAssured
- **Build Tool**: Gradle
- **Containerization**: Docker, Docker Compose
- **API Docs**: Postman Collection

## API Documentation

### Authentication

All API requests require authentication using a Bearer token.

```http
Authorization: Bearer <jwt_token>
```

To obtain a token, use the login endpoint:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "phoneNumber": "+994501234567",
  "password": "123456"
}

```

Response:

```json
{
  "status": {
    "code": 200,
    "message": "OK"
  },
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}

```

### Transaction Endpoints

#### Process Transaction

```http
POST /api/v1/account/transactions
```

Headers (All required):

```http
Authorization: Bearer <jwt_token>
x-idempotency-key: <uuid>
```

#### Transaction Request Fields Explained

| Field                   | Description                                                                                                                                                                         |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type                    | Transaction type: TOP_UP, PURCHASE, or PARTIAL_REFUND                                                                                                                               |
| amount                  | Transaction amount (positive decimal)                                                                                                                                               |
| referenceTransactionUid | Required only for PARTIAL_REFUND transactions. This should be the UUID of the original PURCHASE transaction that is being refunded. Must point to a COMPLETED PURCHASE transaction. |

Request body for TOP_UP:

```json
{
  "type": "TOP_UP",
  "amount": 100.00,
  "referenceTransactionUid": null
}

```

Request body for PURCHASE:

```json
{
  "type": "PURCHASE",
  "amount": 50.00,
  "referenceTransactionUid": null
}

```

Request body for PARTIAL_REFUND:

```json
{
  "type": "PARTIAL_REFUND",
  "amount": 25.00,
  "referenceTransactionUid": "019630c5-eccf-7b24-b814-a39c97c64b8b"
}

```

Response:

```json
{
  "status": {
    "code": 200,
    "message": "OK"
  },
  "data": {
    "transactionUid": "019630c5-eccf-7b24-b814-a39c97c64b8b",
    "status": "COMPLETED",
    "createdAt": "2025-04-17T19:47:08.123456Z"
  }
}

```

## Using Postman

A Postman collection is included with the project for easy API testing. Import the
`k-small-banking.postman_collection.json` file into Postman to get started.

### SSL/TLS Configuration

This API uses HTTPS with SSL/TLS for secure communication. To use the API with Postman:

1. Import the `src/main/resources/cert.pem` certificate into Postman.
2. Go to Settings > Certificates > CA Certificates and add the cert.pem file.
3. Alternatively, you can disable SSL verification in Postman settings (not recommended for production).

## Local Development

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Gradle

### Running with Docker Compose

To start the application and its dependencies (PostgreSQL):

```bash
docker-compose up -d
```

This will start:

- PostgreSQL database
- The small banking REST API application

### Default Credentials

The system comes with a default customer account for testing:

- Phone Number: +994501234567
- Password: 123456

## Security Features

- JWT-based authentication
- Secure password storage with BCrypt
- HTTPS/SSL support
- Idempotent API requests to prevent duplicate transactions
- Session management with ThreadLocalStorage
