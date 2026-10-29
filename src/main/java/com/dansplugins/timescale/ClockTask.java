package com.dansplugins.timescale;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private double dayRatio;
    private double nightRatio;
    private final Map<UUID, ClockAccumulator> accumulators = new HashMap<>();
    /** Worlds whose night was skipped while the sleep threshold has stayed met since. */
    private final Set<UUID> skipped = new HashSet<>();

    public ClockTask(Supplier<? extends Iterable<? extends WorldClock>> worlds, double dayRatio, double nightRatio) {
        // Validates both ratios up front rather than on the first tick.
        new ClockAccumulator(dayRatio, nightRatio);
        this.worlds = worlds;
        this.dayRatio = dayRatio;
        this.nightRatio = nightRatio;
    }

    public double getDayRatio() {
        return dayRatio;
    }

    public double getNightRatio() {
        return nightRatio;
    }

    /**
     * Changes both ratios from the next tick on. Each world starts a fresh accumulator, dropping
     * less than one tick of carried-over remainder.
     *
     * @throws IllegalArgumentException if either ratio is not a positive, finite number
     */
    public void setRatios(double dayRatio, double nightRatio) {
        new ClockAccumulator(dayRatio, nightRatio);
        this.dayRatio = dayRatio;
        this.nightRatio = nightRatio;
        accumulators.clear();
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
            // Only the tick the threshold is first met skips, so sleepers still in bed afterwards
            // cannot skip a further day on every tick until they are woken.
            if (!world.isNightSkipped()) {
                skipped.remove(world.getId());
            } else if (skipped.add(world.getId())) {
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
