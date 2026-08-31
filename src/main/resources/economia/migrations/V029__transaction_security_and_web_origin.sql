ALTER TABLE economy_transactions
    ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS origin VARCHAR(16) NOT NULL DEFAULT 'MINECRAFT';

ALTER TABLE economy_transactions DROP CONSTRAINT IF EXISTS economy_transactions_origin_valid;
ALTER TABLE economy_transactions ADD CONSTRAINT economy_transactions_origin_valid
    CHECK (origin IN ('MINECRAFT', 'WEB', 'ADMIN', 'SYSTEM'));

CREATE INDEX IF NOT EXISTS economy_transactions_origin_created_idx
    ON economy_transactions(origin, created_at DESC);

ALTER TABLE economy_operations
    ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS result_payload TEXT NULL;

ALTER TABLE economy_operations DROP CONSTRAINT IF EXISTS economy_operations_state_valid;
ALTER TABLE economy_operations ADD CONSTRAINT economy_operations_state_valid CHECK (state IN (
    'CREATED','ITEMS_RESERVED','SQL_COMMITTED','ITEMS_DELIVERED','COMPLETED',
    'ROLLBACK_REQUIRED','RECONCILIATION_REQUIRED','ROLLED_BACK'
));
