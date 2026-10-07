-- =====================================================================
-- V1: initial LibraryMS schema.
-- Rules: BIGINT identity keys, every FK column indexed, business rules
-- enforced by NOT NULL / CHECK / UNIQUE in the database itself.
-- NOTE: never edit this file once applied; add V2__..., V3__... instead.
-- =====================================================================

-- Trigram indexes make case-insensitive "contains" filters index-assisted.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------
-- Roles (seeded: roles are reference data, NOT credentials)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('ROLE_USER', 'ROLE_ADMIN'))
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- ---------------------------------------------------------------------
-- Members (library patrons). Soft-deleted so loan history is kept forever.
-- ---------------------------------------------------------------------
CREATE TABLE members (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name  VARCHAR(200) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    phone      VARCHAR(32),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_members_full_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_members_email_format CHECK (position('@' IN email) > 1)
);

-- Case-insensitive uniqueness among active (non-deleted) members
CREATE UNIQUE INDEX ux_members_email ON members (lower(email)) WHERE deleted_at IS NULL;
CREATE INDEX ix_members_full_name_trgm ON members USING gin (lower(full_name) gin_trgm_ops);

-- ---------------------------------------------------------------------
-- Users (login accounts), optionally linked to one member
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    member_id     BIGINT REFERENCES members (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_users_email_format CHECK (position('@' IN email) > 1),
    -- Defence in depth: only BCrypt hashes can ever be stored, never plain text
    CONSTRAINT ck_users_password_is_bcrypt CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$')
);

CREATE UNIQUE INDEX ux_users_username ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));
-- A member can be linked to at most one user account (also indexes the FK)
CREATE UNIQUE INDEX ux_users_member_id ON users (member_id);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);
-- user_id is covered by the PK's leading column
CREATE INDEX ix_user_roles_role_id ON user_roles (role_id);

-- ---------------------------------------------------------------------
-- Refresh tokens (only a SHA-256 hash of the opaque token is stored)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------
-- Authors
-- ---------------------------------------------------------------------
CREATE TABLE authors (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    bio        TEXT,
    birth_date DATE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_authors_name_not_blank CHECK (btrim(name) <> '')
);

CREATE INDEX ix_authors_name_trgm ON authors USING gin (lower(name) gin_trgm_ops);

-- ---------------------------------------------------------------------
-- Books (ISBN stored normalized as ISBN-13 digits)
-- ---------------------------------------------------------------------
CREATE TABLE books (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(500) NOT NULL,
    isbn           VARCHAR(13)  NOT NULL,
    published_date DATE         NOT NULL,
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT ck_books_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT ck_books_isbn_format CHECK (isbn ~ '^97[89][0-9]{10}$')
);

CREATE INDEX ix_books_title_trgm ON books USING gin (lower(title) gin_trgm_ops);

-- Join rows are removed with either side; books/authors themselves never cascade.
CREATE TABLE book_authors (
    book_id   BIGINT NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES authors (id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE INDEX ix_book_authors_author_id ON book_authors (author_id);

CREATE TABLE book_genres (
    book_id BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    genre   VARCHAR(50) NOT NULL,
    PRIMARY KEY (book_id, genre),
    CONSTRAINT ck_book_genres_not_blank CHECK (btrim(genre) <> '')
);

CREATE INDEX ix_book_genres_genre ON book_genres (lower(genre));

-- ---------------------------------------------------------------------
-- Book copies (members borrow copies, not books)
-- ---------------------------------------------------------------------
CREATE TABLE book_copies (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    barcode    VARCHAR(64) NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_book_copies_barcode UNIQUE (barcode),
    CONSTRAINT ck_book_copies_barcode_not_blank CHECK (btrim(barcode) <> ''),
    CONSTRAINT ck_book_copies_status CHECK (status IN ('AVAILABLE', 'ON_LOAN', 'LOST'))
);

CREATE INDEX ix_book_copies_book_id ON book_copies (book_id);
CREATE INDEX ix_book_copies_status ON book_copies (status);

-- ---------------------------------------------------------------------
-- Loans (history kept forever: FKs RESTRICT deletion of copies/members)
-- ---------------------------------------------------------------------
CREATE TABLE loans (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    copy_id     BIGINT        NOT NULL REFERENCES book_copies (id) ON DELETE RESTRICT,
    member_id   BIGINT        NOT NULL REFERENCES members (id) ON DELETE RESTRICT,
    borrowed_at TIMESTAMPTZ   NOT NULL,
    due_date    TIMESTAMPTZ   NOT NULL,
    returned_at TIMESTAMPTZ,
    fine_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_loans_due_after_borrow CHECK (due_date >= borrowed_at),
    CONSTRAINT ck_loans_returned_after_borrow CHECK (returned_at IS NULL OR returned_at >= borrowed_at),
    CONSTRAINT ck_loans_fine_non_negative CHECK (fine_amount >= 0)
);

CREATE INDEX ix_loans_copy_id ON loans (copy_id);
CREATE INDEX ix_loans_member_id ON loans (member_id);
-- Backstop for concurrency: at most ONE open loan per copy
CREATE UNIQUE INDEX ux_loans_open_copy ON loans (copy_id) WHERE returned_at IS NULL;
CREATE INDEX ix_loans_open_due_date ON loans (due_date) WHERE returned_at IS NULL;

-- ---------------------------------------------------------------------
-- Audit log. user_id is nullable + SET NULL so deleting a user never fails;
-- username is a snapshot that survives the user's deletion.
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    username    VARCHAR(100) NOT NULL DEFAULT 'system',
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    operation   VARCHAR(32)  NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    details     TEXT
);

CREATE INDEX ix_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_occurred_at ON audit_logs (occurred_at DESC);
