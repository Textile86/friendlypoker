--liquibase formatted sql

--changeset friendlypoker:04-create-poker-tables
CREATE TABLE poker_tables (
                              id                   BIGSERIAL PRIMARY KEY,
                              club_id              BIGINT       NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                              name                 VARCHAR(100) NOT NULL,
                              small_blind          INT          NOT NULL,
                              big_blind            INT          NOT NULL,
                              min_players          INT          NOT NULL DEFAULT 2,
                              max_players          INT          NOT NULL DEFAULT 9,
                              starting_chips       INT          NOT NULL,
                              action_timeout_secs  INT          NOT NULL DEFAULT 30,
                              variant              VARCHAR(30)  NOT NULL DEFAULT 'TEXAS_HOLDEM',
                              status               VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
                              created_by           BIGINT       NOT NULL REFERENCES users(id),
                              created_at           TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE table_seats (
                             id          BIGSERIAL PRIMARY KEY,
                             table_id    BIGINT NOT NULL REFERENCES poker_tables(id) ON DELETE CASCADE,
                             user_id     BIGINT NOT NULL REFERENCES users(id),
                             seat_index  INT    NOT NULL,
                             joined_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                             UNIQUE(table_id, user_id),
                             UNIQUE(table_id, seat_index)
);