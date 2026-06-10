# Chat App Backend

Real-time chat application backend built with Spring Boot 3.2, WebSocket (STOMP), JWT authentication, and PostgreSQL.

## Prerequisites

- Java 17+
- PostgreSQL 13+
- Maven 3.8+

## Setup

### 1. Create the PostgreSQL database

```bash
createdb chatapp
```

Or using psql:

```sql
CREATE DATABASE chatapp;
```

The datasource defaults to your system username with no password (standard Homebrew PostgreSQL setup). Update `application.properties` if your PostgreSQL user or password differs.

### 2. Build and run

```bash
cd chat-app-backend
mvn clean spring-boot:run
```

The server starts on **http://localhost:8081**.

---

## API Reference

### Auth

| Method | Endpoint            | Body                              | Description       |
|--------|---------------------|-----------------------------------|-------------------|
| POST   | `/api/auth/register` | `{name, email, password}`        | Register new user |
| POST   | `/api/auth/login`    | `{email, password}`              | Login, get JWT    |

Both return `{ "token": "<jwt>" }`.

### Users

All endpoints require `Authorization: Bearer <token>`.

| Method | Endpoint         | Description                        |
|--------|------------------|------------------------------------|
| GET    | `/api/users`     | List all users except current user |
| GET    | `/api/users/{id}`| Get user by ID                     |

### Conversations

| Method | Endpoint                       | Description                           |
|--------|--------------------------------|---------------------------------------|
| GET    | `/api/conversations`           | List conversations for current user   |
| POST   | `/api/conversations/{userId}`  | Start or get conversation with userId |
| GET    | `/api/messages/{conversationId}` | Get messages in a conversation      |

---

## WebSocket (STOMP)

### Connect

```
ws://localhost:8081/ws
```

Send `Authorization: Bearer <token>` header in the CONNECT frame.

### Subscriptions

| Topic                            | Description                        |
|----------------------------------|------------------------------------|
| `/topic/messages/{convId}`       | New messages / read receipts       |
| `/topic/typing/{convId}`         | Typing indicators                  |
| `/topic/status/{userId}`         | User status updates                |

### Send

| Destination       | Payload (ChatNotification)         | Description         |
|-------------------|------------------------------------|---------------------|
| `/app/chat.send`  | `{conversationId, content}`        | Send a message      |
| `/app/chat.typing`| `{conversationId}`                 | Broadcast typing    |
| `/app/chat.read`  | `{conversationId}`                 | Mark messages read  |

---

## Data Model

- **User**: id, name, email, password (BCrypt), status (ONLINE/OFFLINE), createdAt
- **Conversation**: id, user1, user2, createdAt
- **Message**: id, conversation, sender, content, sentAt, isRead

Tables are auto-created/updated by Hibernate on startup (`ddl-auto=update`).
