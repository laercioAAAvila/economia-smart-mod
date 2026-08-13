ALTER TABLE economy_private_property_members
    ADD COLUMN IF NOT EXISTS permission_mask INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS player_name VARCHAR(16) NULL;

ALTER TABLE economy_private_property_members
    DROP CONSTRAINT IF EXISTS economy_private_property_members_permission_mask_valid;

ALTER TABLE economy_private_property_members
    ADD CONSTRAINT economy_private_property_members_permission_mask_valid
        CHECK (permission_mask >= 0 AND permission_mask <= 7);

ALTER TABLE economy_claim_invoices DROP CONSTRAINT IF EXISTS economy_claim_invoices_type_valid;
ALTER TABLE economy_claim_invoices
    ADD CONSTRAINT economy_claim_invoices_type_valid
        CHECK (invoice_type IN ('LAND', 'ANCHOR', 'SALE', 'BUNDLE'));

CREATE TABLE IF NOT EXISTS economy_claim_invoice_bundle_items (
    bundle_invoice_id UUID NOT NULL REFERENCES economy_claim_invoices(id) ON DELETE CASCADE,
    child_invoice_id UUID NOT NULL UNIQUE REFERENCES economy_claim_invoices(id),
    PRIMARY KEY (bundle_invoice_id, child_invoice_id)
);
