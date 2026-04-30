--liquibase formatted sql

--changeset friendlypoker:03-create-club-invites
CREATE TABLE club_invites (
                              id         BIGSERIAL PRIMARY KEY,
                              club_id    BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                              token      VARCHAR(36) NOT NULL UNIQUE,
                              created_by BIGINT NOT NULL REFERENCES users(id),
                              expires_at TIMESTAMP WITH TIME ZONE,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);