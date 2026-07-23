ALTER TABLE economy_cards
    ADD COLUMN IF NOT EXISTS card_creation_number INTEGER NOT NULL DEFAULT 0;

WITH numbered_cards AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY created_at, id)::INTEGER AS creation_number
      FROM economy_cards
     WHERE card_creation_number = 0
)
UPDATE economy_cards cards
   SET card_creation_number = numbered_cards.creation_number,
       custom_name = CASE
           WHEN cards.custom_name IS NULL OR cards.custom_name = ''
               THEN numbered_cards.creation_number || '-' || TO_CHAR(cards.created_at::DATE, 'DD-MM-YYYY')
           ELSE cards.custom_name
       END
  FROM numbered_cards
 WHERE cards.id = numbered_cards.id;

ALTER TABLE economy_cards
    ADD CONSTRAINT economy_cards_creation_number_non_negative CHECK (card_creation_number >= 0);

CREATE UNIQUE INDEX IF NOT EXISTS economy_cards_account_creation_number_idx
    ON economy_cards(account_id, card_creation_number)
    WHERE card_creation_number > 0;
