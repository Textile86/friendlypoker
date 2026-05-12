package com.friendlypoker.dto;

import com.friendlypoker.engine.domain.event.GameEvent;

public record GameEventView(String type, Object data) {
        public static GameEventView from(GameEvent event) {
            return new GameEventView(
                    event.getClass().getSimpleName(),
                    event
            );
        }
}
