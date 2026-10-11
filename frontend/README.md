# LibraryMS Frontend

A modern, responsive web application for the Library Management System (LibraryMS), built with **Next.js 16**, **React 19**, and **Tailwind CSS v4**.

---

## 🌟 Features

- **Public Book Catalog**: Browse library books with live search, genre filters, author badges, and real-time copy availability.
- **Patron Portal (`/me`)**:
  - Digital library card activation and status.
  - Active loan monitoring with remaining due days and live overdue fine calculations.
  - Historical borrow records.
- **Admin Management Console (`/admin`)**:
  - **Catalog Management**: Add, update, and manage books and physical inventory copies with barcode tracking.
  - **Author Management**: Author directory linked to catalog titles.
  - **Patron & Member Directory**: Member profiles, card issuance, and soft-delete safeguards.
  - **Circulation & Loans**: Issue loans, process returns, calculate late fines, and track copy statuses.
  - **Audit Logs**: Filterable timeline of all mutations and security events across the system.
  - **User Administration**: Role assignment (`ROLE_USER`, `ROLE_ADMIN`) and patron account linkages.
- **Polished UI & Interactions**: Dynamic GSAP micro-animations, Phosphor icons, accessible modals, and dark-mode friendly design tokens.

---

## 🛠 Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Framework** | Next.js (App Router, Turbopack) | 16.4.x |
| **UI Library** | React & React DOM | 19.3.x |
| **Language** | TypeScript | 5.x |
| **Styling** | Tailwind CSS (Turbopack engine) | 4.x |
| **Animation** | GSAP & `@gsap/react` | 3.15.x |
| **Server State** | TanStack React Query | 5.x |
| **Icons** | Phosphor Icons (`@phosphor-icons/react`) | 2.1.x |

---

## 📋 Prerequisites

- **Node.js**: v20 LTS or higher recommended
- **npm**: v10+ (or `pnpm` / `yarn`)
- **Backend API**: The Spring Boot REST API must be running (default: `http://localhost:8080`).

---

## 🚀 Getting Started

### 1. Navigate to the frontend directory
> **Important**: The frontend application lives inside `frontend/`. Ensure your terminal working directory is `frontend/`:

```bash
cd frontend
```

### 2. Install dependencies

```bash
npm install
```

### 3. Configure environment variables (optional)

By default, the frontend connects to `http://localhost:8080`. To customize the backend URL, create `.env.local`:

```bash
cp .env.example .env.local
```

Edit `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4. Run the development server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 🐳 Docker Deployment

You can build and run the frontend image independently or via the root Docker Compose stack:

### Run via Docker Compose (Root)
Spins up `db`, `app`, and `frontend`:
```bash
docker compose up --build -d
```

### Build & Run Frontend Standalone Container
```bash
# Build production image
docker build -t libraryms-frontend:latest .

# Run container
docker run -p 3000:3000 -e NEXT_PUBLIC_API_URL=http://localhost:8080 libraryms-frontend:latest
```

---

## 📜 Available Scripts

| Command | Description |
|---|---|
| `npm run dev` | Starts the Next.js development server with hot reloading on port 3000 |
| `npm run build` | Compiles and optimizes the production build |
| `npm run start` | Runs the compiled production server |
| `npm run lint` | Runs ESLint 9 to verify code standards |

---

## 📂 Project Structure

```text
frontend/
├── src/
│   ├── app/
│   │   ├── (site)/             # Public and patron pages
│   │   │   ├── page.tsx        # Landing page with featured collection & stats
│   │   │   ├── catalog/        # Catalog page with search, filters & book details
│   │   │   ├── books/[id]/     # Book detail page
│   │   │   ├── login/          # Patron & admin authentication
│   │   │   ├── register/       # Patron registration
│   │   │   └── me/             # Patron self-service dashboard & card management
│   │   ├── admin/              # Admin console (/admin/*)
│   │   │   ├── page.tsx        # Admin dashboard overview
│   │   │   ├── books/          # Book catalog management
│   │   │   ├── copies/         # Physical copy tracking & status toggle
│   │   │   ├── authors/        # Author CRUD
│   │   │   ├── members/        # Library card and member management
│   │   │   ├── loans/          # Circulation desk (issue, return, calculate fines)
│   │   │   ├── audit/          # System audit trail logs
│   │   │   └── users/          # User administration
│   │   ├── globals.css         # Global stylesheet & Tailwind CSS v4 setup
│   │   └── layout.tsx          # Root HTML layout & React Query provider
│   ├── components/             # Reusable UI component library (Button, Modal, BookCover, etc.)
│   └── lib/
│       ├── api.ts              # REST API client with auto token refresh
│       └── types.ts            # TypeScript definitions matching Spring Boot DTOs
├── public/                     # Static media and assets
├── package.json
└── tsconfig.json
```

---

## 🔐 Authentication & Session Handling

- **JWT Tokens**: Authenticated sessions communicate with Spring Boot via signed HS256 Bearer tokens and opaque refresh tokens.
- **Silent Refresh**: The API client automatically handles token rotation when requests encounter token expiration.
- **Demo Logins**: If you loaded the backend sample dataset (`db/seed/dev-seed.sql`):
  - **Patron Accounts**: `amara`, `omar`, or `lina` (Password: `reading-room-2026`)
  - **Admin Account**: Credentials configured via `ADMIN_USERNAME` and `ADMIN_PASSWORD` in root `.env`.
