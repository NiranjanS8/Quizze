# Quizze Architecture

This document explains the architecture of Quizze in detail, including the major modules, request flow, data flow, event-driven components, infrastructure dependencies, and observability setup.

## 1. High-Level Overview

Quizze is a full-stack quiz platform built as a modular monolith backend with a separate React frontend. The backend exposes REST APIs for authentication, quiz management, quiz attempts, analytics, monitoring, and notifications.

The project combines:

- synchronous REST APIs for core business flows
- PostgreSQL for primary persistence
- Redis for cache-backed read optimization
- Kafka for asynchronous notification workflows
- Prometheus and Grafana for metrics and dashboards

## 2. System Context

```mermaid
flowchart LR
    U["User / Admin"] --> F["React + Vite Frontend"]
    F --> B["Spring Boot Backend"]
    B --> PG["PostgreSQL"]
    B --> R["Redis"]
    B --> K["Kafka"]
    B --> SMTP["SMTP Provider"]
    P["Prometheus"] --> B
    G["Grafana"] --> P
```

## 3. Architectural Style

The backend follows a modular monolith style.

That means:

- all business capabilities live in one deployable Spring Boot application
- modules are separated by package boundaries and clear responsibilities
- communication inside the app is mostly direct service calls
- some workflows are decoupled using Spring application events
- some workflows are offloaded asynchronously through Kafka

This gives the project a good balance:

- simpler to build and deploy than microservices
- structured enough to show production-style boundaries
- flexible enough to add event-driven workflows without forcing full distributed complexity everywhere

## 4. Backend Module Breakdown

Main backend packages:

- `auth`
- `user`
- `quiz`
- `notification`
- `audit`
- `cache`
- `monitoring`
- `config`
- `common`

### 4.1 Auth Module

Responsibilities:

- registration
- login
- JWT generation and validation
- forgot password OTP flow
- password reset
- registration-time notification preference

Key concepts:

- `AuthController`
- `AuthService`
- JWT service and filter
- password reset OTP persistence
- user registration event publishing

### 4.2 User Module

Responsibilities:

- current user profile
- attempt history
- result history
- personal analytics

Key concepts:

- `UserController`
- `User` entity
- profile and analytics DTO mapping

### 4.3 Quiz Module

Responsibilities:

- quiz CRUD
- question CRUD
- quiz catalog browsing
- attempts and submissions
- scoring
- result review
- leaderboard
- quiz analytics

Key concepts:

- `AdminController`
- `UserQuizController`
- `AdminQuizService`
- `UserQuizService`
- leaderboard and analytics services

### 4.4 Notification Module

Responsibilities:

- welcome email
- password reset OTP email
- new quiz notification emails
- quiz result emails
- Kafka producers and consumers for async notification flows

Key concepts:

- mail configuration
- Kafka payload models
- Kafka producers and listeners
- batch email sending for new quiz notifications

### 4.5 Audit Module

Responsibilities:

- capture admin actions for quiz and question management
- expose audit log history to admins

### 4.6 Cache Module

Responsibilities:

- configure cache manager
- Redis-backed caching
- cache invalidation on quiz writes and submissions

### 4.7 Monitoring Module

Responsibilities:

- custom Micrometer counters
- actuator exposure
- Prometheus integration

## 5. Layered Backend Design

The backend uses a layered structure inside each module.

```mermaid
flowchart TD
    C["Controller Layer"] --> S["Service Layer"]
    S --> M["Mapper Layer"]
    S --> R["Repository Layer"]
    R --> DB["Database"]
    S --> E["Event / Kafka / Cache / Mail Integrations"]
```

### Controller Layer

Responsibilities:

- define REST endpoints
- validate request input
- enforce HTTP-level concerns
- delegate business logic to services

Examples:

- `AuthController`
- `UserController`
- `AdminController`
- `UserQuizController`

### Service Layer

Responsibilities:

- implement business rules
- coordinate repositories
- publish events
- handle scoring, analytics, and workflow logic

Examples:

- `AuthService`
- `AdminQuizService`
- `UserQuizService`
- analytics services

### Mapper Layer

Responsibilities:

- transform domain models into API response DTOs
- keep controllers and services cleaner
- reduce entity leakage into API contracts

### Repository Layer

Responsibilities:

- data access using Spring Data JPA
- entity persistence and queries

## 6. Request Flow

Below is the typical synchronous request lifecycle.

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant PostgreSQL

    Client->>Controller: HTTP Request
    Controller->>Service: Validated DTO / params
    Service->>Repository: Query / save entities
    Repository->>PostgreSQL: SQL via JPA
    PostgreSQL-->>Repository: Rows / generated values
    Repository-->>Service: Domain entities
    Service-->>Controller: Response DTO
    Controller-->>Client: JSON Response
```

## 7. Authentication and Security Architecture

Quizze uses JWT-based stateless authentication.

### Security Components

- Spring Security filter chain
- JWT authentication filter
- JWT service for token generation and validation
- role-based access control

### Roles

- `USER`
- `ADMIN`

### Access Model

- public endpoints for register, login, forgot password, reset password
- authenticated endpoints for user quiz flows
- admin-only endpoints for quiz management, analytics, and audit logs
- selected actuator endpoints exposed publicly or restricted

### Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthController
    participant AuthService
    participant Security
    participant DB

    User->>AuthController: POST /api/auth/login
    AuthController->>AuthService: LoginRequest
    AuthService->>Security: authenticate(username/password)
    Security-->>AuthService: Authenticated principal
    AuthService->>DB: load user + role
    AuthService-->>AuthController: JWT response
    AuthController-->>User: token + user details
```

## 8. Domain Model Overview

Main entities:

- `User`
- `Role`
- `Category`
- `Quiz`
- `Question`
- `Option`
- `QuizAttempt`
- `AttemptAnswer`
- `PasswordResetOtp`
- `AdminAuditLog`

## 9. Core Data Relationships

```mermaid
erDiagram
    ROLE ||--o{ USER : has
    CATEGORY ||--o{ QUIZ : groups
    QUIZ ||--o{ QUESTION : contains
    QUESTION ||--o{ OPTION : offers
    USER ||--o{ QUIZ_ATTEMPT : makes
    QUIZ ||--o{ QUIZ_ATTEMPT : belongs_to
    QUIZ_ATTEMPT ||--o{ ATTEMPT_ANSWER : stores
    QUESTION ||--o{ ATTEMPT_ANSWER : references
    USER ||--o{ PASSWORD_RESET_OTP : owns
    USER ||--o{ ADMIN_AUDIT_LOG : performs
```

## 10. Quiz Attempt and Scoring Flow

The quiz lifecycle for a user is:

1. fetch published quizzes
2. open quiz details
3. start attempt
4. fetch attempt questions
5. answer and submit
6. score attempt
7. store result
8. review answers and leaderboard

### Attempt Submission Flow

```mermaid
sequenceDiagram
    participant User
    participant UserQuizController
    participant UserQuizService
    participant DB
    participant EventBus
    participant Kafka

    User->>UserQuizController: POST /api/quizzes/attempts/{id}/submit
    UserQuizController->>UserQuizService: answers
    UserQuizService->>DB: load attempt, quiz, questions, options
    UserQuizService->>UserQuizService: validate answers
    UserQuizService->>UserQuizService: calculate score
    UserQuizService->>DB: persist attempt + answers
    UserQuizService->>EventBus: publish QuizSubmittedEvent
    EventBus-->>Kafka: publish quiz result message (if enabled)
    UserQuizService-->>UserQuizController: result summary
    UserQuizController-->>User: submission response
```

### Scoring Rules

Current behavior includes:

- auto-evaluation of submitted answers
- support for negative marking
- score clamped to valid readable result values
- correct and wrong answer counts
- detailed review response with correct answers

## 11. Analytics Architecture

Analytics are built from persisted quiz attempts and answers.

There are several analytics layers:

- leaderboard per quiz
- per-quiz performance analytics
- hardest/easiest question analytics
- global admin overview
- personal user analytics

### Analytics Read Path

```mermaid
flowchart LR
    A["API Request"] --> S["Analytics Service"]
    S --> C["Cache Layer"]
    C -->|cache miss| DB["PostgreSQL"]
    DB --> S
    S --> C
    C --> A
```

This pattern is useful for:

- leaderboard
- overview analytics
- quiz analytics
- question analytics
- user analytics

## 12. Cache Architecture

Redis is used to improve performance for expensive read-heavy endpoints.

Cached areas include:

- leaderboard
- admin overview analytics
- quiz performance analytics
- question analytics
- user analytics

### Cache Strategy

- cache read-heavy derived data
- invalidate on writes
- invalidate on quiz submission
- invalidate on quiz or question management changes

### Cache Invalidation Flow

```mermaid
flowchart TD
    W["Quiz write / submission"] --> I["Cache invalidation service"]
    I --> L["Leaderboard cache"]
    I --> A["Analytics cache"]
    I --> U["User analytics cache"]
```

## 13. Event-Driven Design

The project uses two levels of event-driven behavior.

### 13.1 In-Process Spring Events

Used when:

- producer and consumer are inside the same app
- decoupling is useful
- durability is not required

Examples:

- `UserRegisteredEvent`
- `QuizSubmittedEvent`
- `QuizPublishedEvent`

### 13.2 Kafka Events

Used when:

- async processing is preferred
- email sending should not block core requests
- fan-out behavior is useful
- failures should not break request handling

Examples:

- new quiz published notifications
- quiz result notification emails

## 14. Event Flow Diagrams

### User Registration Event

```mermaid
flowchart LR
    A["AuthService register"] --> B["Publish UserRegisteredEvent"]
    B --> C["UserRegisteredEventListener"]
    C --> D["WelcomeEmailService"]
```

### New Quiz Notification Flow

```mermaid
flowchart LR
    A["Admin publishes quiz"] --> B["Publish QuizPublishedEvent"]
    B --> C["QuizPublishedKafkaProducer"]
    C --> K["Kafka Topic"]
    K --> D["NewQuizNotificationKafkaConsumer"]
    D --> E["NewQuizNotificationEmailService"]
    E --> F["Batch opted-in users"]
    F --> G["SMTP Provider"]
```

### Quiz Result Notification Flow

```mermaid
flowchart LR
    A["User submits quiz"] --> B["Publish QuizSubmittedEvent"]
    B --> C["QuizSubmittedKafkaProducer"]
    C --> K["Kafka Topic"]
    K --> D["QuizResultKafkaConsumer"]
    D --> E["QuizResultEmailService"]
    E --> F["SMTP Provider"]
```

## 15. Notification Architecture

Notifications are configurable and resilient.

### Design Goals

- do not block admin publish flow on email
- do not block quiz submission on email
- allow feature toggles by config
- retry without crashing Kafka consumers
- process new quiz emails in batches
- only notify opted-in users

### Notification Preferences

At registration, users can choose whether they want notifications for new quizzes.

This preference is stored in the `User` entity and used later by the batch email notification flow.

## 16. Audit Logging Architecture

Admin actions are persisted into an audit log table.

Tracked actions include:

- quiz create
- quiz update
- quiz delete
- question create
- question update
- question delete

Each audit entry stores details such as:

- admin user
- action type
- target type
- target id
- description
- timestamp

## 17. Monitoring and Observability

The app exposes operational and business metrics through Spring Boot Actuator and Micrometer.

### Exposed Endpoints

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

### Custom Metrics

Examples include:

- successful registrations
- successful logins
- password reset requests
- quiz attempts started
- quiz attempts submitted
- Kafka publish success/failure
- Kafka consume success/failure
- new quiz email sent/failed
- quiz result email sent/failed

### Monitoring Stack

```mermaid
flowchart LR
    B["Spring Boot App"] --> A["Actuator / Prometheus endpoint"]
    A --> P["Prometheus"]
    P --> G["Grafana"]
```

Provisioned dashboards:

- `Quizze Overview`
- `Quizze Messaging & Cache`

## 18. Frontend Architecture

The frontend is a React + Vite single-page application.

Responsibilities:

- authentication flow
- role-based navigation
- user dashboard
- quiz catalog and filtering
- quiz attempt UI
- result review UI
- admin dashboard and management screens
- analytics presentation

### Frontend-to-Backend Interaction

- frontend stores JWT after login
- authenticated API calls include `Authorization: Bearer <token>`
- UI changes based on role
- admin and user experiences are separated in navigation and routes

## 19. Deployment View

A simple deployment model is:

```mermaid
flowchart TD
    FE["Frontend Hosting"] --> API["Spring Boot API"]
    API --> DB["PostgreSQL"]
    API --> REDIS["Redis"]
    API --> KAFKA["Kafka"]
    API --> SMTP["Mail Server"]
    PROM["Prometheus"] --> API
    GRAF["Grafana"] --> PROM
```

For local development, Kafka, Redis, Prometheus, and Grafana are started with Docker Compose.

## 20. Why This Architecture Works Well

This architecture is a strong fit for the project because it combines:

- simple backend deployment
- strong module boundaries
- secure role-based access
- scalable async notifications
- fast analytics reads with Redis
- real monitoring and dashboards
- enough complexity to feel production-oriented without becoming unnecessarily fragmented

## 21. Future Architecture Enhancements

Possible future improvements:

- move from Hibernate auto-update to versioned database migrations using Flyway
- add CI/CD pipeline for build, test, and deployment
- add Grafana alerting for Kafka or email failures
- add admin API integration tests
- add deployment diagrams for cloud hosting
- add screenshot references for major flows
