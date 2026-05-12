--liquibase formatted sql

--changeset friendlypoker:06-create-hand-history
CREATE TABLE hand_history (
                              id           BIGSERIAL PRIMARY KEY,
                              table_id     BIGINT    NOT NULL REFERENCES poker_tables(id) ON DELETE CASCADE,
                              hand_number  BIGINT    NOT NULL,
                              winner_user_id BIGINT  NOT NULL REFERENCES users(id),
                              pot_amount   INT       NOT NULL,
                              played_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);