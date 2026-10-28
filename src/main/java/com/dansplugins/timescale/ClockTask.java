package com.dansplugins.timescale;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Advances world time once per server tick in place of the vanilla daylight cycle.
 *
 * <p>Each world keeps its own {@link ClockAccumulator} so fractional ticks carry over
 * independently. When enough players are asleep the clock jumps to the next morning, as vanilla
 * sleep would. Only world time and {@code doDaylightCycle} are touched; every other tick-driven
 * mechanic stays on real ticks.
 */
public final class ClockTask implements Runnable {

    private final Supplier<? extends Iterable<? extends WorldClock>> worlds;
    private final double dayRatio;
    private final double nightRatio;
    private final Map<UUID, ClockAccumulator> accumulators = new HashMap<>();

    public ClockTask(Supplier<? extends Iterable<? extends WorldClock>> worlds, double dayRatio, double nightRatio) {
        // Validates both ratios up front rather than on the first tick.
        new ClockAccumulator(dayRatio, nightRatio);
        this.worlds = worlds;
        this.dayRatio = dayRatio;
        this.nightRatio = nightRatio;
    }

    /** Turns off {@code doDaylightCycle} on every world so only this task moves the clock. */
    public void disableDaylightCycle() {
        setDaylightCycle(false);
    }

    /** Turns {@code doDaylightCycle} back on for every world, handing the clock back to vanilla. */
    public void restoreDaylightCycle() {
        setDaylightCycle(true);
    }

    @Override
    public void run() {
        for (WorldClock world : worlds.get()) {
            ClockAccumulator accumulator = accumulators.computeIfAbsent(world.getId(),
                    id -> new ClockAccumulator(dayRatio, nightRatio));
            long time = world.getFullTime();
            if (world.isNightSkipped()) {
                // Vanilla wakes the sleepers but, with the daylight cycle off, leaves the clock alone.
                world.setFullTime(SleepRule.nextMorning(time));
                continue;
            }
            long ticks = accumulator.ticksToAdvance(time);
            if (ticks > 0) {
                world.setFullTime(time + ticks);
            }
        }
    }

    private void setDaylightCycle(boolean enabled) {
        for (WorldClock world : worlds.get()) {
            world.setDaylightCycle(enabled);
        }
    }
}
