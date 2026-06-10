# Chat App Backend

Real-time chat backend built with Spring Boot 3.2, STOMP over WebSocket, JWT auth, and PostgreSQL. Runs on port 8081.

## Stack

- Java 17
- Spring Boot 3.2
- Spring WebSocket (STOMP) + SockJS
- Spring Security + JJWT 0.11.5
- Spring Data JPA + PostgreSQL
- Lombok

## Setup

### 1. Create the database

```bash
createdb chatapp
```

### 2. Edit `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chatapp
spring.datasource.username=YOUR_PG_USER
spring.datasource.password=YOUR_PG_PASSWORD

app.jwt.secret=change_this_to_something_long_and_random
app.jwt.expiration-ms=86400000
```

### 3. Run

```bash
export JAVA_HOME=/path/to/java17
mvn clean spring-boot:run
```

Server starts on `http://localhost:8081`. Hibernate creates tables automatically on first run.

## REST API

### Auth

```
POST /api/auth/register   { name, email, password }
POST /api/auth/login      { email, password }
```

Returns `{ token, userId, name, email }`.

### Users

All endpoints require `Authorization: Bearer <token>`.

```
GET /api/users          all users except the caller
GET /api/users/{id}     single user
```

### Conversations

```
GET  /api/conversations             accepted conversations for current user
POST /api/conversations/{userId}    send a DM request to userId
GET  /api/conversations/pending     incoming DM requests waiting for a response
PUT  /api/conversations/{id}/accept accept a DM request
PUT  /api/conversations/{id}/reject reject a DM request
GET  /api/messages/{conversationId} message history (accepted convs only)
```

New conversations start as `PENDING`. The recipient sees them under `/pending` and can accept or reject. Only accepted conversations appear in the main list.

## WebSocket (STOMP)

Connect to `ws://localhost:8081/ws`. Send JWT in the CONNECT frame:

```
CONNECT
Authorization:Bearer <token>
```

### Subscribe

```
/topic/messages/{conversationId}   incoming messages + read receipts
/topic/typing/{conversationId}     typing indicators
/topic/status/{userId}             online/offline status
```

### Send

```
/app/chat.send     { type:"MESSAGE", conversationId, content }
/app/chat.typing   { type:"TYPING",  conversationId }
/app/chat.read     { type:"READ",    conversationId }
```

## Package layout

```
com.chatapp
├── config/       SecurityConfig, WebSocketConfig, CorsConfig
├── controller/   AuthController, UserController, ChatController
├── service/      AuthService, UserService, ChatService
├── repository/   UserRepository, ConversationRepository, MessageRepository
├── model/        User, Conversation, Message
├── dto/          LoginRequest, RegisterRequest, UserDTO, ConversationDTO, MessageDTO, ChatNotification
└── security/     JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
```

## Notes

- WebSocket connections are authenticated at the STOMP CONNECT frame level, not just HTTP.
- Conversation status: `PENDING` → `ACCEPTED` or `REJECTED`. Users can only exchange messages in accepted conversations.
- Passwords are BCrypt-hashed. JWT is HS256 with a configurable expiry (default 24h).
- CORS is open by default — lock it down before deploying anywhere public.
