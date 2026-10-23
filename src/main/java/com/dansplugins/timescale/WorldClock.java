package com.dansplugins.timescale;

import java.util.UUID;

/**
 * The parts of a world's clock that {@link ClockTask} reads and writes.
 *
 * <p>Kept separate from {@link org.bukkit.World} so the clock logic can be unit-tested without a
 * server; {@link BukkitWorldClock} is the thin adapter used at runtime.
 */
public interface WorldClock {

    UUID getId();

    /** Absolute world time, including elapsed days. */
    long getFullTime();

    void setFullTime(long time);

    /** Sets the {@code doDaylightCycle} gamerule. */
    void setDaylightCycle(boolean enabled);
}
