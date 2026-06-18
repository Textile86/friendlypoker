--liquibase formatted sql

--changeset friendlypoker:08-add-paused-until-to-poker-tables
ALTER TABLE poker_tables ADD COLUMN paused_until TIMESTAMP WITH TIME ZONE;
