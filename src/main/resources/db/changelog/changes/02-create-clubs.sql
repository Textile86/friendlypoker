--liquibase formatted sql

--changeset friendlypoker:02-create-clubs
CREATE TABLE clubs (
                       id          BIGSERIAL PRIMARY KEY,
                       name        VARCHAR(100) NOT NULL,
                       description TEXT,
                       owner_id    BIGINT NOT NULL REFERENCES users(id),
                       created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE club_members (
                              id         BIGSERIAL PRIMARY KEY,
                              club_id    BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                              user_id    BIGINT NOT NULL REFERENCES users(id),
                              role       VARCHAR(20) NOT NULL,
                              joined_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                              UNIQUE(club_id, user_id)
);