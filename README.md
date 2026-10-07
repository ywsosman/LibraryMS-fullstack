# LibraryMS — Library Management System REST API

Spring Boot 3.5 · Java 21 · PostgreSQL 17 · Flyway · Docker

> Work in progress — built phase by phase. Full documentation lands in the final phase.

## Quick start (Docker)

```bash
cp .env.example .env        # then fill in DB_PASSWORD and JWT_SECRET
docker compose up --build
curl http://localhost:8080/actuator/health
```

## Build & test

Requires JDK 21 and a running Docker daemon (integration tests use Testcontainers).

```bash
./mvnw verify               # Windows: .\mvnw.cmd verify
```

- Unit tests: `*Test.java` (Surefire)
- Integration tests: `*IT.java` (Failsafe + Testcontainers PostgreSQL)
- Coverage report: `target/site/jacoco/index.html`
