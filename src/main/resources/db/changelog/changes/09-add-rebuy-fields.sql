--liquibase formatted sql

--changeset friendlypoker:09-add-rebuy-fields
ALTER TABLE poker_tables
    ADD COLUMN IF NOT EXISTS rebuy_min       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rebuy_max       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rebuy_count_min INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rebuy_count_max INT NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS rebuy_unlimited BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE table_seats
    ADD COLUMN IF NOT EXISTS rebuy_count INT NOT NULL DEFAULT 0;