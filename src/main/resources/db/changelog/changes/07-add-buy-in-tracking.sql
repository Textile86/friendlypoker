--liquibase formatted sql

--changeset friendlypoker:07-add-buy-in-tracking
ALTER TABLE table_seats ADD COLUMN total_buy_in INTEGER NOT NULL DEFAULT 0;
