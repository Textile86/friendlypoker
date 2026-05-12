--liquibase formatted sql

--changeset friendlypoker:05-add-chips-to-seats
ALTER TABLE table_seats ADD COLUMN chips INTEGER NOT NULL DEFAULT 0;