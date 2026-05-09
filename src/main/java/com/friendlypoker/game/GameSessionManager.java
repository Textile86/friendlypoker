package com.friendlypoker.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private final ConcurrentHashMap<Long, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession getOrCreate(Long tableId, GameSession session) {
        return sessions.computeIfAbsent(tableId, id -> session);
    }

    public GameSession get(Long tableId) {
        return sessions.get(tableId);
    }

    public void remove(Long tableId) {
        sessions.remove(tableId);
    }
}
