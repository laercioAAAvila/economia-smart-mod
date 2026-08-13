ALTER TABLE economy_claim_invoice_bundle_items
    DROP CONSTRAINT IF EXISTS economy_claim_invoice_bundle_items_child_invoice_id_fkey;

ALTER TABLE economy_claim_invoice_bundle_items
    ADD CONSTRAINT economy_claim_invoice_bundle_items_child_invoice_id_fkey
        FOREIGN KEY (child_invoice_id) REFERENCES economy_claim_invoices(id) ON DELETE CASCADE;
