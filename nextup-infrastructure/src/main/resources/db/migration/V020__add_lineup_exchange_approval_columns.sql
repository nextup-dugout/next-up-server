-- Add exchange approval process columns to lineup_submissions table
-- Supports the manager approval workflow: SUBMITTED → EXCHANGE_PENDING → EXCHANGED/EXCHANGE_REJECTED

ALTER TABLE lineup_submissions
    ADD COLUMN exchange_pending_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN exchange_rejection_reason VARCHAR(500),
    ADD COLUMN exchange_rejected_by_id BIGINT REFERENCES users(id);

-- Update status column to accommodate new enum values (EXCHANGE_PENDING, EXCHANGE_REJECTED)
-- PostgreSQL VARCHAR column already handles new enum values transparently
