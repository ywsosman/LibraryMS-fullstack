# Library Management System (LibraryMS)

A production-grade, enterprise-ready fullstack Library Management System built with a Spring Boot 3.5, Java 21, and PostgreSQL 17 backend and a modern Next.js 16, React 19, and Tailwind CSS v4 frontend.

---

## 🌟 Highlights & Architecture

- **Decoupled Fullstack Architecture**: Built as an enterprise-grade solution featuring a robust Spring Boot 3.5 REST API backend paired with a high-performance Next.js 16 (App Router) web application.
- **Strict Layered Backend Separation**: `Controller -> Service -> Repository`. Controllers never touch repositories or domain entities directly.
- **Cycle-Free DTOs**: Zero entity leakage; responses use immutable Java records and nested summary projections (`AuthorSummary`, `BookSummary`, `MemberSummary`, `CopySummary`) to prevent Jackson circular reference loops.
- **Standards-Based Security**: Spring Security OAuth2 Resource Server with signed HS256 JWT access tokens and secure, opaque, hashed refresh tokens. Passwords always validated by database-level regex and encoded with BCrypt.
- **Fail-Fast Configuration**: Validates critical secrets (e.g., `JWT_SECRET` >= 256 bits) at startup before accepting traffic.
- **Rate-Limiting & Security Guardrails**: In-memory login attempt limiter protecting against brute-force attacks with HTTP 429 lockouts and generic invalid credentials responses to block user enumeration.
- **Concurrency & Pessimistic-Free Consistency**: `@Version` optimistic locking on books, copies, members, and loans backed by a partial unique SQL index (`ux_loans_open_copy`) guaranteeing that concurrent borrow attempts yield exactly one 201 Created and 409 Conflict.
- **Soft Deletion & Historical Integrity**: Patron members are soft deleted (`deleted_at`), ensuring loan history is preserved forever while disallowing deletion if open loans exist.
- **Declarative Auditing**: Aspect-oriented auditing via `@Audited` executed inside the active transaction, with autonomous `REQUIRES_NEW` auditing for security events like login failures.
- **Comprehensive Quality Assurance**: Enforced JaCoCo line coverage >= 80% on all `*Service` classes, alongside a complete 14-test regression test suite running against real PostgreSQL containers via Testcontainers.
- **Interactive Web Portals**: Public catalog browsing with real-time copy availability, patron self-service dashboard (`/me`) with digital card activation and fine tracking, and an extensive administrative portal (`/admin`).

---

## 🛠 Tech Stack

### ☕ Backend REST API
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

### ⚛️ Frontend Web Application
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Next.js (App Router, Turbopack) | 16.4.x |
| UI Runtime | React & React DOM | 19.3.x |
| Styling | Tailwind CSS (Turbopack engine) | 4.x |
| Animations | GSAP & `@gsap/react` | 3.15.x |
| Server State | TanStack React Query | 5.x |
| Icons | Phosphor Icons (`@phosphor-icons/react`) | 2.1.x |
| Language | TypeScript | 5.x |
| Package Manager | npm | 10+ |

---

## 📂 Architecture & Project Structure

### Repository Structure
```text
LibraryMS fullstack/
├── frontend/               # Next.js 16 Web Application (React 19, Tailwind CSS v4)
│   ├── src/app/            # App Router pages ((site) catalog & patron portal, admin console)
│   ├── src/components/     # Reusable UI component library (Button, Modal, BookCover, etc.)
│   ├── src/lib/            # REST API client, query client, TypeScript definitions
│   └── package.json        # Frontend dependencies and npm scripts
├── src/                    # Spring Boot 3.5 Java backend source code
├── db/                     # Flyway migrations and development seed scripts
├── docker-compose.yml      # Docker orchestration for PostgreSQL 17 & API
├── pom.xml                 # Maven configuration for backend build & tests
└── .env.example            # Environment template for backend services
```

### Backend Package Organization (`com.libraryms`)
The backend is structured according to a clean **Layered Architecture (Package by Layer)** directly beneath `com.libraryms`:

```text
com.libraryms
├── LibraryManagementSystemApplication.java  # Spring Boot application entry point
│
├── enums/                                   # Domain enums (RoleName, CopyStatus, LoanStatusFilter)
├── config/                                  # Security, JWT, AOP Aspect, OpenAPI, Properties, Validators
├── controller/                              # HTTP REST controllers (Auth, User, Author, Book, Copy, Member, Loan, Audit)
├── dto/                                     # Immutable request/response DTO records & summary projections
├── exception/                               # Global exception handler, error codes, and ApiException hierarchy
├── mapper/                                  # Compile-time MapStruct mappers
├── model/                                   # JPA entities extending BaseEntity (User, Book, Loan, Member, etc.)
├── repository/                              # Spring Data JPA repositories with partial index & query specs
└── service/                                 # Transactional business services (Auth, Book, Loan, Member, etc.)
```

### 🎯 Purpose of Each Layer
| Package | Role & Architectural Responsibilities |
|---|---|
| **`controller/`** | Exposes HTTP REST endpoints under `/api/v1/*`. Handles HTTP verbs, path variables, query pagination, `@Valid` triggers, and `@PreAuthorize` security checks. Never accesses database entities or repositories directly. |
| **`service/`** | Implements core business logic and transactional boundaries (`@Transactional`). Coordinates repository calls, triggers `@Audited` operations, calculates loan fines, and prevents business rule violations. |
| **`repository/`** | Spring Data JPA interfaces interfacing directly with PostgreSQL. Encapsulates queries, partial unique index validations, and pessimistic/optimistic concurrency mechanisms. |
| **`model/`** | JPA database models mapped directly to PostgreSQL tables. Extends `BaseEntity` for optimistic locking (`@Version`) and automatic timestamp auditing. |
| **`mapper/`** | Compile-time MapStruct mappers (`ReportingPolicy.ERROR`) ensuring type-safe entity-to-DTO conversions and eliminating circular JSON references. |
| **`dto/`** | Immutable Java records defining public API request and response contracts. Prevents entity leakage to API consumers. |
| **`exception/`** | Unified error handling (`GlobalExceptionHandler`, `ApiException`, `ApiErrorResponse`). |
| **`config/`** | Spring Security, JWT authentication converters, AOP `@Audited` interceptors, custom validators, and application properties. |
| **`enums/`** | Shared domain enumerations across models and DTOs. |

---


## 🚀 Quick Start Guide

### Option A: Run Fullstack Stack with Docker Compose (Recommended)

Spins up the **Database**, **Spring Boot REST API**, and **Next.js Frontend** in coordinated containers with one command:

1. Copy the sample environment file in the project root:
   ```bash
   cp .env.example .env
   ```
2. Populate `.env` with a secure database password and a 32+ character `JWT_SECRET` (generate with `openssl rand -base64 48`).
3. Launch all services (`db`, `app`, and `frontend`):
   ```bash
   docker compose up --build -d
   ```
4. Verify backend health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
5. *(Recommended)* Load sample seed data for local development (32 books, copies, members, reader accounts, and loans):
   ```bash
   docker exec -i libraryms-db-1 sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < db/seed/dev-seed.sql
   ```
   > **Sample reader logins**: `amara`, `omar`, or `lina` with password `reading-room-2026`.
   > **Admin login**: Configured from `ADMIN_USERNAME` / `ADMIN_PASSWORD` in `.env`.

---

### Option B: Run Backend in Docker & Frontend Locally (Fast UI Development)

If you are modifying frontend UI and want instant Hot Module Replacement (HMR):

1. Start backend and database:
   ```bash
   docker compose up -d db app
   ```
2. Navigate to the `frontend/` directory and run Next.js:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

### Access the Applications

| Application / Service | URL | Description |
|---|---|---|
| **Frontend Web App** | [http://localhost:3000](http://localhost:3000) | Fullstack Next.js UI (Patron Portal & Admin Console) |
| **Backend REST API** | [http://localhost:8080](http://localhost:8080) | Spring Boot REST API root |
| **Swagger UI** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Interactive API exploration & testing |
| **OpenAPI Docs (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | OpenAPI 3.0 specification |
| **Health Check** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Spring Actuator health monitoring |

---

### 👥 Pre-Seeded Test Accounts & Logins



### 🗄️ Database Access & GUI Client Setup (DBeaver, TablePlus, pgAdmin)

To connect your favorite database GUI client to the PostgreSQL 17 container:

| Setting | Value |
|---|---|
| **Host** | `localhost` or `127.0.0.1` |
| **Port** | `5432` |
| **Database** | **`libraryms`** *(⚠️ Make sure to select `libraryms`, NOT `postgres`)* |
| **Username** | `libraryms` (or value of `DB_USERNAME` in `.env`) |
| **Password** | Value of `DB_PASSWORD` in `.env` |


---

## 💻 Frontend Development & Architecture

The frontend application (`frontend/`) is built with Next.js 16 (App Router), React 19, Tailwind CSS v4, TanStack React Query, and GSAP.

### Available Frontend Scripts

Run these commands from inside the `frontend/` directory:

| Command | Action |
|---|---|
| `npm run dev` | Starts the Next.js development server with hot-reloading on port 3000 |
| `npm run build` | Compiles and optimizes the application for production |
| `npm run start` | Starts the production server using the built assets |
| `npm run lint` | Runs ESLint to verify code quality and adherence to style rules |

### Frontend Portals & Routes

- **Public & Patron Portal (`frontend/src/app/(site)/`)**:
  - `/`: Landing page showcasing library metrics, featured catalog titles, and member benefits.
  - `/catalog`: Searchable catalog with real-time filters (genre, author) and physical copy availability.
  - `/books/[id]`: Book details, author biographies, ISBN metadata, and copy status list.
  - `/login` & `/register`: Patron authentication with automatic JWT access and refresh token management.
  - `/me`: Patron dashboard for digital library card activation, viewing active loans, tracking due dates and overdue fines, and browsing loan history.
- **Admin Management Console (`frontend/src/app/admin/`)**:
  - `/admin`: Overview dashboard displaying circulation KPIs and system shortcuts.
  - `/admin/books`: Create, edit, and manage catalog book entries.
  - `/admin/copies`: Physical copy inventory tracker (barcode lookup and status toggles: `AVAILABLE`, `ON_LOAN`, `LOST`).
  - `/admin/authors`: Author directory management.
  - `/admin/members`: Patron directory, library card status inspection, and safe deletion controls.
  - `/admin/loans`: Circulation desk to issue loans, process book returns, and settle overdue fines.
  - `/admin/audit`: Searchable audit log inspector recording security and entity operations.
  - `/admin/users`: User management and role administration (`ROLE_USER`, `ROLE_ADMIN`).

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

---


