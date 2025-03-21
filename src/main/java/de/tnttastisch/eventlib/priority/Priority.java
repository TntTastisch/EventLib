package de.tnttastisch.eventlib.priority;

/**
 * Defines priority levels for event handlers.
 */
public enum Priority {
    LOWEST(-64), LOW(-32), NORMAL(0), HIGH(32), HIGHEST(64);

    private final byte value;

    Priority(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }

    public boolean isHigherThan(Priority other) {
        return this.value > other.value;
    }

    public boolean isLowerThan(Priority other) {
        return this.value < other.value;
    }
}
