# PROJECT_CONTEXT

## Quick start for a new model
- Read [README.md](/C:/Users/Niranjan/Desktop/Quizze/README.md) for the product/features view.
- Read [ARCHITECTURE.md](/C:/Users/Niranjan/Desktop/Quizze/ARCHITECTURE.md) for the system/module breakdown.
- Read [pom.xml](/C:/Users/Niranjan/Desktop/Quizze/pom.xml) and [src/main/resources/application.properties](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application.properties) for the backend runtime and dependencies.
- Read [frontend/src/App.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/App.jsx) and [frontend/src/styles.css](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/styles.css) for the current UI implementation.
- Read [src/main/java/com/quizze/quizze/config/SecurityConfig.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/config/SecurityConfig.java) and the main controllers for API shape and access rules.

## Files worth reading first
- [README.md](/C:/Users/Niranjan/Desktop/Quizze/README.md)
- [ARCHITECTURE.md](/C:/Users/Niranjan/Desktop/Quizze/ARCHITECTURE.md)
- [pom.xml](/C:/Users/Niranjan/Desktop/Quizze/pom.xml)
- [src/main/java/com/quizze/quizze/QuizzeApplication.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/QuizzeApplication.java)
- [src/main/java/com/quizze/quizze/config/SecurityConfig.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/config/SecurityConfig.java)
- [src/main/java/com/quizze/quizze/auth/controller/AuthController.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/auth/controller/AuthController.java)
- [src/main/java/com/quizze/quizze/admin/controller/AdminController.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/admin/controller/AdminController.java)
- [src/main/java/com/quizze/quizze/quiz/controller/UserQuizController.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/quiz/controller/UserQuizController.java)
- [src/main/resources/application-dev.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-dev.yml)
- [src/main/resources/application-prod.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-prod.yml)
- [frontend/src/App.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/App.jsx)
- [frontend/package.json](/C:/Users/Niranjan/Desktop/Quizze/frontend/package.json)

## Safe assumptions
- The backend is the primary source of truth for business logic and data validation.
- The main frontend is the React app under [frontend/](/C:/Users/Niranjan/Desktop/Quizze/frontend), not the legacy static files under [src/main/resources/static/](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/static).
- Authentication is JWT-based and stateless.
- PostgreSQL is the primary database in non-test environments.
- Kafka and Redis are feature-dependent and can be disabled by configuration.

## Unsafe assumptions
- Do not assume the README is fully current; at least one mismatch exists (`README.md` says Java 21, `pom.xml` uses Java 17).
- Do not assume production deployment is finalized; there is no verified CI/CD config in the repo.
- Do not assume DB schema migrations exist; Hibernate schema management is still used.
- Do not assume all frontend code is cleanly modularized; [frontend/src/App.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/App.jsx) is a very large file and contains most UI logic.

## Project name
- `Quizze`

## One-paragraph summary
- Quizze is a full-stack online quiz platform with a Spring Boot backend and a React + Vite frontend. It supports JWT authentication, role-based user/admin flows, quiz creation and publication, timed quiz attempts, auto-scoring, leaderboards, analytics, OTP-based password reset, in-app notifications, Kafka-driven email workflows, Redis-backed caching, and Prometheus/Grafana observability.

## Primary goal / business purpose
- Provide an assessment platform where admins can create/publish quizzes and analyze results, while users can discover quizzes, attempt them, review answers, and track performance.

## Tech stack
- Backend: Java 17, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Spring Validation, Spring Mail, Spring Kafka, Spring Boot Actuator, Micrometer, Springdoc OpenAPI
- Frontend: React 18, React Router 6, Vite 5
- Data: PostgreSQL, Redis, H2 (tests)
- Messaging: Kafka
- Observability: Prometheus, Grafana
- Build tooling: Maven, npm
- Containerization: Docker, Docker Compose

## Repository layout
- [src/main/java/com/quizze/quizze/](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze): backend source
- [src/main/resources/](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources): backend config and legacy static frontend
- [src/test/java/com/quizze/quizze/](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze): backend tests
- [src/test/resources/application-test.properties](/C:/Users/Niranjan/Desktop/Quizze/src/test/resources/application-test.properties): test profile config
- [frontend/](/C:/Users/Niranjan/Desktop/Quizze/frontend): React frontend
- [monitoring/](/C:/Users/Niranjan/Desktop/Quizze/monitoring): Prometheus and Grafana configs/dashboards
- [database/test-quiz-seed.sql](/C:/Users/Niranjan/Desktop/Quizze/database/test-quiz-seed.sql): seed data for manual testing
- [docker-compose.backend.yml](/C:/Users/Niranjan/Desktop/Quizze/docker-compose.backend.yml): local backend + infra stack
- [docker-compose.kafka.yml](/C:/Users/Niranjan/Desktop/Quizze/docker-compose.kafka.yml): local Kafka/Redis/Prometheus/Grafana stack

## Entry points
- Backend app: [src/main/java/com/quizze/quizze/QuizzeApplication.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/QuizzeApplication.java)
- Frontend app: [frontend/src/main.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/main.jsx)
- Backend Docker image: [Dockerfile](/C:/Users/Niranjan/Desktop/Quizze/Dockerfile)

## Core modules and responsibilities
- `auth`: registration, login, JWT, forgot/reset password OTP, rate limiting
- `security`: JWT filter, custom user details, auth provider integration
- `user`: current user profile, attempt history, result history, user analytics, notification center APIs
- `quiz`: quiz CRUD, question CRUD, catalog browsing, attempt lifecycle, scoring, results, leaderboard, analytics projections
- `notification`: mail config, welcome email, password reset email, new-quiz email, quiz-result email, Kafka producers/consumers, in-app notification model
- `audit`: admin action event model and audit log persistence
- `eventstream`: persisted event stream entries for async workflow tracking
- `cache`: Redis cache config and quiz cache invalidation
- `monitoring`: custom Micrometer counters
- `common`: base entity, API response wrapper, exception handling
- `config`: Spring config, security config, async executor config, role seeding

## Data flow / request flow
- Browser -> React frontend -> REST API (`/api/auth`, `/api/users`, `/api/quizzes`, `/api/admin`)
- Controllers validate and delegate to services
- Services use repositories for PostgreSQL persistence
- Selected reads use Redis-backed cache when enabled
- Services publish Spring events for decoupled post-commit work
- Kafka is used for asynchronous email workflows:
  - quiz published -> notify opted-in users
  - quiz submitted -> email result summary
- Actuator exposes metrics -> Prometheus scrapes -> Grafana dashboards visualize

## Build / run / test commands
- Backend compile: `mvn -q -DskipTests compile`
- Backend tests: `mvn test`
- Run backend locally: `mvn spring-boot:run`
- Frontend install: `cd frontend && npm.cmd install`
- Frontend dev server: `cd frontend && npm.cmd run dev`
- Frontend build: `cd frontend && npm.cmd run build`
- Dockerized backend stack: `docker compose -f docker-compose.backend.yml up --build`
- Monitoring stack: `docker compose -f docker-compose.kafka.yml up -d`

## Environment variables and configuration
- Base config: [src/main/resources/application.properties](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application.properties)
- Dev overrides: [src/main/resources/application-dev.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-dev.yml)
- Prod overrides: [src/main/resources/application-prod.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-prod.yml)
- Example env: [.env.example](/C:/Users/Niranjan/Desktop/Quizze/.env.example)
- Important groups:
  - app/profile/port: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`
  - DB: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - JWT: `JWT_SECRET`, `JWT_EXPIRATION`
  - mail: `APP_MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
  - Redis: `APP_CACHE_REDIS_ENABLED`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
  - Kafka: `APP_KAFKA_LISTENERS_ENABLED`, `KAFKA_BOOTSTRAP_SERVERS`
  - notifications: `APP_NOTIFICATIONS_NEW_QUIZ_*`, `APP_NOTIFICATIONS_QUIZ_RESULT_*`
  - auth rate limiting: `APP_RATE_LIMIT_AUTH_*`

## External services / APIs / databases
- PostgreSQL: primary DB
- Redis: cache store when enabled
- Kafka: async notification delivery when enabled
- SMTP provider: outgoing email
- Prometheus: metrics scraping
- Grafana: dashboards
- OpenAPI/Swagger UI served by backend

## Coding conventions and architectural patterns
- Modular monolith backend with package-by-feature boundaries
- Layering: controller -> service -> mapper/repository -> domain
- DTO-based API boundaries; entities are generally not exposed directly
- `ApiResponse<T>` used as standard response envelope
- `BaseEntity` + JPA auditing enabled
- JWT stateless auth
- Spring events for internal decoupling; Kafka for externalized async notifications
- React frontend appears route-driven but mostly concentrated in one large component file

## Important dependencies
- [pom.xml](/C:/Users/Niranjan/Desktop/Quizze/pom.xml):
  - `spring-boot-starter-web`
  - `spring-boot-starter-security`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-mail`
  - `spring-boot-starter-data-redis`
  - `spring-kafka`
  - `spring-boot-starter-actuator`
  - `micrometer-registry-prometheus`
  - `springdoc-openapi-starter-webmvc-ui`
  - `jjwt-*`
  - `postgresql`
  - test: `spring-boot-starter-test`, `spring-security-test`, `h2`
- [frontend/package.json](/C:/Users/Niranjan/Desktop/Quizze/frontend/package.json):
  - `react`
  - `react-dom`
  - `react-router-dom`
  - `vite`
  - `@vitejs/plugin-react`

## Testing strategy and current test locations
- Unit tests:
  - [src/test/java/com/quizze/quizze/auth/service/AuthServiceTest.java](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze/auth/service/AuthServiceTest.java)
  - [src/test/java/com/quizze/quizze/quiz/service/AdminQuizServiceTest.java](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze/quiz/service/AdminQuizServiceTest.java)
  - [src/test/java/com/quizze/quizze/quiz/service/UserQuizServiceTest.java](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze/quiz/service/UserQuizServiceTest.java)
- Integration tests:
  - [src/test/java/com/quizze/quizze/auth/controller/AuthControllerIntegrationTest.java](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze/auth/controller/AuthControllerIntegrationTest.java)
  - [src/test/java/com/quizze/quizze/quiz/controller/UserQuizControllerIntegrationTest.java](/C:/Users/Niranjan/Desktop/Quizze/src/test/java/com/quizze/quizze/quiz/controller/UserQuizControllerIntegrationTest.java)
- Test DB/profile: H2 with PostgreSQL compatibility mode in [src/test/resources/application-test.properties](/C:/Users/Niranjan/Desktop/Quizze/src/test/resources/application-test.properties)
- Unknown / needs verification:
  - frontend automated tests do not exist in the repo
  - admin API integration tests do not appear to exist

## Deployment / CI/CD notes
- Dockerization exists for local backend stack via [Dockerfile](/C:/Users/Niranjan/Desktop/Quizze/Dockerfile) and [docker-compose.backend.yml](/C:/Users/Niranjan/Desktop/Quizze/docker-compose.backend.yml)
- Separate infra/monitoring compose exists in [docker-compose.kafka.yml](/C:/Users/Niranjan/Desktop/Quizze/docker-compose.kafka.yml)
- `prod` profile uses `ddl-auto=validate`
- No Flyway/Liquibase migrations found
- No GitHub Actions / CI config found
- No Kubernetes / Helm / Terraform found

## Known issues / risks / technical debt
- No DB migration tool yet; schema still depends on Hibernate behavior
- README mismatch: says Java 21, but [pom.xml](/C:/Users/Niranjan/Desktop/Quizze/pom.xml) sets Java 17
- [frontend/src/App.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/App.jsx) is large (~75 KB) and likely a maintenance hotspot
- Legacy static frontend files still exist under [src/main/resources/static/](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/static); unclear if they are still intended for use
- Deployment story is partial; Docker exists, but CI/CD and production release workflow are not present
- Kafka/Redis/SMTP features are configuration-sensitive and can fail if infra is enabled but unavailable

## Current priorities / roadmap clues
- Production hardening and deployment readiness
- Database migrations
- Deployment setup for backend/frontend
- Continued observability and infrastructure polish
- Possible further split between dev/prod behavior and documentation cleanup

## Glossary of project-specific terms
- `Quizze`: project/product name
- `QuizPublishedEvent`: internal event for newly published quizzes
- `QuizSubmittedEvent`: internal event after quiz submission/scoring
- `QuizAnalyticsProjection`: precomputed analytics entity for faster admin analytics reads
- `UserNotification`: in-app notification entity exposed to end users
- `AdminAuditLog`: persisted record of admin quiz/question changes
- `ProcessedKafkaEvent`: deduplication record used for idempotent Kafka consumer handling
- `oneAttemptOnly`: quiz flag restricting users to one attempt
- `negativeMarkingEnabled`: quiz flag enabling score penalty for wrong answers

## Open questions / unknowns
- Which deployment target is intended (Render, Railway, VPS, Kubernetes, etc.)? Unknown.
- Is the legacy static frontend under [src/main/resources/static/](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/static) still needed? Needs verification.
- Are there any expected admin seed users beyond role seeding? No admin user seeding was found; only roles are seeded.
- Is Kafka mandatory in production or optional for initial deployment? Needs verification.
- Is Redis mandatory in production or optional? Needs verification.

## Recommended starting files for a new model
- [README.md](/C:/Users/Niranjan/Desktop/Quizze/README.md)
- [ARCHITECTURE.md](/C:/Users/Niranjan/Desktop/Quizze/ARCHITECTURE.md)
- [pom.xml](/C:/Users/Niranjan/Desktop/Quizze/pom.xml)
- [src/main/resources/application.properties](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application.properties)
- [src/main/resources/application-dev.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-dev.yml)
- [src/main/resources/application-prod.yml](/C:/Users/Niranjan/Desktop/Quizze/src/main/resources/application-prod.yml)
- [src/main/java/com/quizze/quizze/config/SecurityConfig.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/config/SecurityConfig.java)
- [src/main/java/com/quizze/quizze/auth/service/AuthService.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/auth/service/AuthService.java)
- [src/main/java/com/quizze/quizze/quiz/service/UserQuizService.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/quiz/service/UserQuizService.java)
- [src/main/java/com/quizze/quizze/quiz/service/AdminQuizService.java](/C:/Users/Niranjan/Desktop/Quizze/src/main/java/com/quizze/quizze/quiz/service/AdminQuizService.java)
- [frontend/src/App.jsx](/C:/Users/Niranjan/Desktop/Quizze/frontend/src/App.jsx)

## Change log note
- Last regenerated from repository state on 2026-04-17
