package com.friendlypoker.engine.domain.model.enums;

public enum PlayerStatus {
    WAITING,
    ACTIVE,
    FOLDED,
    ALL_IN,
    SITTING_OUT;

    public boolean isInHand() {
        return this == ACTIVE || this == ALL_IN;
    }

    public boolean canAnt() {
        return this == ACTIVE;
    }
}
