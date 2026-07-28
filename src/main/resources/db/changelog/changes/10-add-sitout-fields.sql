-- 10-add-sitout-fields.sql: Sit-out / I'm Back feature
ALTER TABLE poker_tables ADD COLUMN IF NOT EXISTS sit_out_timeout_minutes INT NOT NULL DEFAULT 8
    CONSTRAINT sit_out_timeout_range CHECK (sit_out_timeout_minutes >= 5 AND sit_out_timeout_minutes <= 60);

ALTER TABLE table_seats ADD COLUMN IF NOT EXISTS sit_out_until TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE table_seats ADD COLUMN IF NOT EXISTS wait_for_bb BOOLEAN NOT NULL DEFAULT FALSE;