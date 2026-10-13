-- =====================================================================
-- V2: Add fine_settled_at to loans for payment / fine settlement tracking
-- =====================================================================
ALTER TABLE loans ADD COLUMN fine_settled_at TIMESTAMPTZ;

CREATE INDEX ix_loans_fine_settled_at ON loans (fine_settled_at);
