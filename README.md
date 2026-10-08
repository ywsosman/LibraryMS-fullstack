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

## 📂 Architecture & Package Organization

The project is structured according to a **Feature-Driven Architecture with Dedicated Subpackages**. Each feature module is self-contained and subdivided into explicit technical layers to enforce strict single-responsibility principles:

```text
com.libraryms
├── LibraryApplication.java           # Spring Boot application entry point
│
├── auth/                             # Authentication, Registration & Token Management
│   ├── controller/                   # AuthController (register, login, refresh, logout)
│   ├── service/                      # AuthService (token issue, rotation), AdminBootstrap (fail-fast seed)
│   ├── repository/                   # RefreshTokenRepository
│   ├── entity/                       # RefreshToken (opaque hash entity)
│   └── dto/                          # AuthResponse, LoginRequest, RegisterRequest, RefreshTokenRequest
│
├── user/                             # User Accounts & Role-Based Access Control
│   ├── controller/                   # UserController (self-service me, admin user management, member links)
│   ├── service/                      # UserService (credentials, self-delete, role checks)
│   ├── repository/                   # UserRepository, RoleRepository
│   ├── entity/                       # User, Role, RoleName (ROLE_USER, ROLE_ADMIN)
│   ├── mapper/                       # UserMapper (MapStruct)
│   └── dto/                          # UserResponse, UpdateUserRequest, ChangePasswordRequest
│
├── author/                           # Book Authors Domain
│   ├── controller/                   # AuthorController (CRUD, paginated lookup)
│   ├── service/                      # AuthorService (business validation, @Audited actions)
│   ├── repository/                   # AuthorRepository
│   ├── entity/                       # Author (JPA aggregate)
│   ├── mapper/                       # AuthorMapper (MapStruct)
│   └── dto/                          # AuthorResponse, CreateAuthorRequest, UpdateAuthorRequest
│
├── book/                             # Catalog Books Domain
│   ├── controller/                   # BookController (CRUD, multi-field filters)
│   ├── service/                      # BookService (ISBN normalization, author linkages)
│   ├── repository/                   # BookRepository
│   ├── entity/                       # Book (JPA aggregate with @Version)
│   ├── mapper/                       # BookMapper (MapStruct with AuthorSummary projections)
│   └── dto/                          # BookResponse, CreateBookRequest, UpdateBookRequest
│
├── copy/                             # Physical Book Copies Domain
│   ├── controller/                   # BookCopyController (barcode tracking, status toggles)
│   ├── service/                      # BookCopyService (optimistic lock updates, copy lifecycle)
│   ├── repository/                   # BookCopyRepository
│   ├── entity/                       # BookCopy, CopyStatus (AVAILABLE, ON_LOAN, LOST)
│   ├── mapper/                       # BookCopyMapper (MapStruct with BookSummary projections)
│   └── dto/                          # CopyResponse, CreateCopyRequest, UpdateCopyStatusRequest
│
├── member/                           # Library Patrons Domain
│   ├── controller/                   # MemberController (CRUD, active loan checks)
│   ├── service/                      # MemberService (soft delete logic, loan status guards)
│   ├── repository/                   # MemberRepository
│   ├── entity/                       # Member (JPA aggregate with deleted_at soft-delete timestamp)
│   ├── mapper/                       # MemberMapper (MapStruct)
│   ├── security/                     # MemberSecurity (Spring Security SpEL access checks)
│   └── dto/                          # MemberResponse, CreateMemberRequest, UpdateMemberRequest
│
├── loan/                             # Borrowing, Returns & Fine Calculations
│   ├── controller/                   # LoanController (borrow, return, fine settlements)
│   ├── service/                      # LoanService (fine policy, concurrent borrow protection)
│   ├── repository/                   # LoanRepository (partial unique index ux_loans_open_copy)
│   ├── entity/                       # Loan (JPA aggregate with @Version)
│   ├── mapper/                       # LoanMapper (MapStruct with MemberSummary & CopySummary)
│   ├── security/                     # LoanSecurity (user/member ownership verification)
│   ├── config/                       # LoanProperties (configurable loan durations & daily fines)
│   └── dto/                          # LoanResponse, CreateLoanRequest, LoanStatusFilter
│
├── audit/                            # Transactional & Security Auditing
│   ├── controller/                   # AuditController (admin audit trail queries)
│   ├── service/                      # AuditService (transactional and REQUIRES_NEW persistence)
│   ├── repository/                   # AuditLogRepository
│   ├── entity/                       # AuditLog (snapshot columns, ON DELETE SET NULL user FK)
│   ├── mapper/                       # AuditLogMapper (MapStruct)
│   ├── aspect/                       # @Audited annotation & AuditAspect (ordered inside transaction)
│   ├── config/                       # AuditConfig (AOP enable)
│   └── dto/                          # AuditLogResponse
│
└── common/                           # Cross-Cutting Infrastructure & Platform Concerns
    ├── config/                       # ClockConfig, OpenApiConfig (Swagger & JWT security scheme)
    ├── dto/summary/                  # Cycle-free summary projections (AuthorSummary, BookSummary, CopySummary, MemberSummary)
    ├── error/                        # Unified error handling (ApiException, ApiErrorResponse, GlobalExceptionHandler)
    ├── persistence/                  # BaseEntity (Id, @Version, createdAt, updatedAt)
    ├── security/                     # SecurityConfig, JwtProperties, JwtTokenService, LoginAttemptLimiter, Handlers
    └── validation/                   # Custom Bean Validation constraints (@ValidIsbn, IsbnValidator)
```

### 🎯 Purpose of Each Subpackage Layer
| Subpackage | Role & Architectural Responsibilities |
|---|---|
| **`controller/`** | Exposes HTTP REST endpoints under `/api/v1/*`. Handles HTTP verbs, path variables, query pagination, `@Valid` triggers, and `@PreAuthorize` security checks. Never accesses database entities or repositories directly. |
| **`service/`** | Implements core business logic and transactional boundaries (`@Transactional`). Coordinates repository calls, triggers `@Audited` operations, calculates loan fines, and prevents business rule violations. |
| **`repository/`** | Spring Data JPA interfaces interfacing directly with PostgreSQL. Encapsulates queries, partial unique index validations, and pessimistic/optimistic concurrency mechanisms. |
| **`entity/`** | JPA database models mapped directly to PostgreSQL tables. Extends `BaseEntity` for optimistic locking (`@Version`) and automatic timestamp auditing. |
| **`mapper/`** | Compile-time MapStruct mappers (`ReportingPolicy.ERROR`) ensuring type-safe entity-to-DTO conversions and eliminating circular JSON references. |
| **`dto/`** | Immutable Java records defining public API request and response contracts. Prevents entity leakage to API consumers. |
| **`security/`** | Feature-specific authorization helpers and SpEL security expressions (e.g. verifying that a patron only views their own loans/profile). |
| **`config/`** | Feature-specific configuration properties (e.g., loan durations, daily fine amounts) mapped from Spring Boot properties. |
| **`aspect/`** | Aspect-Oriented Programming (AOP) interceptors and custom declarative annotations (e.g., `@Audited`). |

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
6. Optional: load sample data for local development (32 books, copies, members, reader accounts and loans). Run it once the API has started, so Flyway has created the tables. It does nothing if books already exist.
   ```bash
   docker exec -i libraryms-db-1 sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < db/seed/dev-seed.sql
   ```
   The sample reader logins are listed at the top of [`db/seed/dev-seed.sql`](db/seed/dev-seed.sql).

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
| `POST` | `/members/me` | Authenticated | Self-service card activation: creates a member with the account's email and links it (409 if already linked, or if a card with that email exists) |
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
