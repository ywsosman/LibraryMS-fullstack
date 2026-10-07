# Library Management System (LibraryMS) REST API

A production-grade, enterprise-ready Library Management System REST API built from scratch with Spring Boot 3.5, Java 21, and PostgreSQL 17.

---

## 🌟 Highlights & Architecture

- **Strict Layered Separation**: `Controller -> Service -> Repository`. Controllers never touch repositories or domain entities directly.
- **Cycle-Free DTOs**: Zero entity leakage; responses use immutable Java records and nested summary projections (`AuthorSummary`, `BookSummary`, `MemberSummary`, `CopySummary`) to prevent Jackson circular reference loops.
- **Standards-Based Security**: Spring Security OAuth2 Resource Server with signed HS256 JWT access tokens and secure, opaque, hashed refresh tokens. Passwords always validated by database-level regex and encoded with BCrypt.
- **Fail-Fast Configuration**: Validates critical secrets (e.g., `JWT_SECRET` >= 256 bits) at startup before accepting traffic.
- **Rate-Limiting & Security Guardrails**: In-memory login attempt limiter protecting against brute-force attacks with HTTP 429 lockouts and generic invalid credentials responses to block user enumeration.
- **Concurrency & Pessimistic-Free Consistency**: `@Version` optimistic locking on books, copies, members, and loans backed by a partial unique SQL index (`ux_loans_open_copy`) guaranteeing that concurrent borrow attempts yield exactly one 201 Created and 409 Conflict.
- **Soft Deletion & Historical Integrity**: Patron members are soft deleted (`deleted_at`), ensuring loan history is preserved forever while disallowing deletion if open loans exist.
- **Declarative Auditing**: Aspect-oriented auditing via `@Audited` executed inside the active transaction, with autonomous `REQUIRES_NEW` auditing for security events like login failures.
- **Comprehensive Quality Assurance**: Enforced JaCoCo line coverage >= 80% on all `*Service` classes, alongside a complete 14-test regression test suite running against real PostgreSQL containers via Testcontainers.

---

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Runtime | OpenJDK | 21 LTS |
| Framework | Spring Boot (Web, Data JPA, Security, Actuator, Validation) | 3.5.x |
| Database | PostgreSQL | 17 (Alpine) |
| Migrations | Flyway (with flyway-database-postgresql) | Managed |
| Security | Spring OAuth2 Resource Server (Nimbus JWT) + BCrypt | Spring Security 6.x |
| DTO Mapping | MapStruct | 1.6.3 |
| Documentation | SpringDoc OpenAPI / Swagger UI | 2.8.5 |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL), MockMvc | Modern |
| Build Tool | Maven Wrapper (`mvnw` / `mvnw.cmd`) | Maven 3.9+ |
| Containerization | Multi-stage Dockerfile (Alpine JRE, Non-root user) | Docker / Compose |

---

## ⚙️ Environment Variables Reference

| Variable | Default | Required in Production | Description |
|---|---|---|---|
| `SERVER_PORT` | `8080` | No | HTTP port for the API server |
| `DB_URL` | `jdbc:postgresql://localhost:5432/libraryms` | Yes | JDBC connection string |
| `DB_USERNAME` | *(empty)* | Yes | PostgreSQL username |
| `DB_PASSWORD` | *(empty)* | Yes | PostgreSQL password |
| `JWT_SECRET` | *(empty)* | **Yes (min 32 bytes)** | HMAC-SHA256 secret key for signing JWTs |
| `JWT_ACCESS_EXPIRATION` | `900` | No | Access token lifespan in seconds (15 min) |
| `JWT_REFRESH_EXPIRATION` | `604800` | No | Refresh token lifespan in seconds (7 days) |
| `ADMIN_USERNAME` | *(empty)* | No | Optional initial admin username for bootstrap |
| `ADMIN_EMAIL` | *(empty)* | No | Optional initial admin email for bootstrap |
| `ADMIN_PASSWORD` | *(empty)* | No | Optional initial admin password for bootstrap |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | No | Comma-separated list of allowed CORS origins |
| `LOAN_DURATION_DAYS` | `14` | No | Standard borrow duration in days |
| `LOAN_FINE_PER_DAY` | `0.50` | No | Fine amount accrued per overdue day |
| `LOAN_MAX_OPEN_LOANS` | `5` | No | Maximum open active loans per member |

---

## 🚀 Quick Start with Docker

1. Copy the sample environment file:
   ```bash
   cp .env.example .env
   ```
2. Populate `.env` with a secure database password and a 32+ character `JWT_SECRET`.
3. Launch the full application stack (Spring Boot API + PostgreSQL 17):
   ```bash
   docker compose up --build -d
   ```
4. Verify system health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
5. Explore the interactive API documentation at:
   - **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🧪 Testing and Verification

Run the entire build, unit tests, integration tests against Testcontainers, and JaCoCo coverage validation:

```powershell
# Windows
.\mvnw.cmd clean verify

# Linux / macOS
./mvnw clean verify
```

Coverage report is generated at: `target/site/jacoco/index.html`.

---

## 🛡️ Mandatory Regression Tests Suite

The codebase includes dedicated regression tests located in `src/test/java/com/libraryms/regression/`:

| Test Class | Focus / Invariant Tested |
|---|---|
| `Regression01SelfPasswordChangeIT` | Password change stores valid BCrypt hash; old password revoked immediately |
| `Regression02MemberDeleteIT` | Soft deleting member preserves books/copies; member with active open loans yields 409 |
| `Regression03SelfDeleteIT` | Self delete revokes active refresh tokens and terminates session |
| `Regression04UsernameEmailUpdateIT` | Updating username/email keeps active session valid; duplicate yields 409 |
| `Regression05DatabaseConstraintIT` | Database-level SQL constraints reject non-BCrypt hashes, invalid emails, and bad dates |
| `Regression06UserAuditSurvivalIT` | Deleting a user retains historical audit log rows (`user_id` set to NULL, snapshot intact) |
| `Regression07BookJsonCycleIT` | Book serialization with borrowed copy contains no cycles and zero entity/proxy leakage |
| `Regression08SecurityErrorBodyIT` | Unauthenticated requests receive RFC-standard structured JSON error response |
| `Regression09ErrorStatusMappingIT` | Exact HTTP status code mapping (400, 404, 409) across exception hierarchy |
| `Regression10GenericBadCredentialsIT` | Bad login attempts return uniform error message to prevent username enumeration |
| `Regression11InvalidRequestBodyIT` | Validation errors return 400 Bad Request with field-level details |
| `Regression12ConcurrentBorrowIT` | Simultaneous borrow of the same copy via CountDownLatch produces exactly one 201 and one 409 |
| `Regression13StartupFailFastIT` | App fails fast on missing/short JWT secret; zero default admin accounts without env config |
| `Regression14LoginRateLimitIT` | Repeated failed login attempts trigger account lockout and return 429 Too Many Requests |

---

## 📖 REST API Endpoints Overview

All endpoints are prefixed with `/api/v1` unless noted otherwise.

### 🔑 Authentication (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Register a new patron user account (201 Created) |
| `POST` | `/auth/login` | Public | Authenticate credentials; returns access & refresh tokens |
| `POST` | `/auth/refresh` | Public | Rotate opaque refresh token for a fresh access token |
| `POST` | `/auth/logout` | Authenticated | Revoke refresh token and invalidate current session (204) |

### 👤 User Profile & Administration (`/api/v1/users`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users/me` | Authenticated | Retrieve current user profile |
| `PATCH` | `/users/me` | Authenticated | Update current username or email |
| `PUT` | `/users/me/password` | Authenticated | Change current password (requires current password) |
| `DELETE` | `/users/me` | Authenticated | Self-delete current user account (204) |
| `GET` | `/users` | `ROLE_ADMIN` | Paginated list of users (filterable by username, email) |
| `GET` | `/users/{id}` | `ROLE_ADMIN` | Retrieve user by ID |
| `PATCH` | `/users/{id}` | `ROLE_ADMIN` | Update user details by ID |
| `DELETE` | `/users/{id}` | `ROLE_ADMIN` | Delete user account by ID (204) |
| `PUT` | `/users/{id}/member/{memberId}` | `ROLE_ADMIN` | Link user account to a library member profile |
| `DELETE` | `/users/{id}/member` | `ROLE_ADMIN` | Unlink user account from a library member profile |

### ✍️ Authors (`/api/v1/authors`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/authors` | Authenticated | Paginated list of authors (filterable by name) |
| `GET` | `/authors/{id}` | Authenticated | Retrieve author details |
| `POST` | `/authors` | `ROLE_ADMIN` | Create an author |
| `PUT` | `/authors/{id}` | `ROLE_ADMIN` | Update author information |
| `DELETE` | `/authors/{id}` | `ROLE_ADMIN` | Delete author (cascade removes book-author links) |

### 📚 Books & Copies (`/api/v1/books` & `/api/v1/copies`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/books` | Authenticated | Paginated catalog (filterable by title, author, isbn, genre) |
| `GET` | `/books/{id}` | Authenticated | Retrieve book details with copy counts |
| `GET` | `/books/{id}/copies` | Authenticated | Paginated physical copies of a book |
| `POST` | `/books` | `ROLE_ADMIN` | Register a new book title |
| `PUT` | `/books/{id}` | `ROLE_ADMIN` | Update book title, ISBN, or authors |
| `DELETE` | `/books/{id}` | `ROLE_ADMIN` | Delete book (rejected if physical copies exist) |
| `GET` | `/copies` | Authenticated | Paginated copies (filterable by bookId, barcode, status) |
| `GET` | `/copies/{id}` | Authenticated | Retrieve copy by ID |
| `POST` | `/copies` | `ROLE_ADMIN` | Add physical inventory copy |
| `PATCH` | `/copies/{id}` | `ROLE_ADMIN` | Update copy status (AVAILABLE ↔ LOST) |
| `DELETE` | `/copies/{id}` | `ROLE_ADMIN` | Delete copy (rejected if loan history exists) |

### 🪪 Members (`/api/v1/members`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/members` | `ROLE_ADMIN` | Paginated members list (filterable by name, email) |
| `GET` | `/members/me` | Authenticated | Retrieve current user's linked member record |
| `GET` | `/members/{id}` | `ROLE_ADMIN` or Self | Retrieve member profile |
| `POST` | `/members` | `ROLE_ADMIN` | Create member profile |
| `PUT` | `/members/{id}` | `ROLE_ADMIN` or Self | Update member profile |
| `DELETE` | `/members/{id}` | `ROLE_ADMIN` | Soft-delete member (rejected with 409 if loans open) |

### 📖 Loans (`/api/v1/loans`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/loans` | Authenticated | Borrow a book copy (USER for self; ADMIN for any) |
| `POST` | `/loans/{id}/return` | `ROLE_ADMIN` | Return copy, update status, and calculate overdue fine |
| `GET` | `/loans` | `ROLE_ADMIN` | Paginated loans (filterable by memberId, copyId, status) |
| `GET` | `/loans/me` | Authenticated | Paginated loan history for current user's member |
| `GET` | `/loans/{id}` | `ROLE_ADMIN` or Self | Retrieve loan details |

### 📜 Audit Logs (`/api/v1/audit-logs`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/audit-logs` | `ROLE_ADMIN` | Paginated audit trail (filterable by entity, operation, user, date range) |
