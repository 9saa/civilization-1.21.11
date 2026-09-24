package com.example.civ;

public enum CivRank {
    OWNER(4),
    SECOND_IN_COMMAND(3),
    MANAGER(2),
    GUARD(1),
    MEMBER(0);

    private final int power;

    CivRank(int power) { this.power = power; }

    public int getPower() { return power; }

    public boolean isAtLeast(CivRank other) {
        return this.power >= other.power;
    }

    /** Next rank up when promoting. OWNER is terminal. */
    public CivRank next() {
        return switch (this) {
            case MEMBER -> GUARD;
            case GUARD -> MANAGER;
            case MANAGER -> SECOND_IN_COMMAND;
            case SECOND_IN_COMMAND, OWNER -> OWNER;
        };
    }
}
