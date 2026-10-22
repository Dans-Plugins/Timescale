package com.dansplugins.timescale;

/**
 * Turns a day ratio and a night ratio into whole world-time ticks to add per server tick.
 *
 * <p>A ratio of {@code r} advances the clock by {@code 1 / r} ticks per server tick, so {@code 1.0}
 * is vanilla speed and {@code 3.0} makes that half of the day three times longer. The fractional
 * part is carried forward between calls so the sun moves smoothly instead of stepping.
 *
 * <p>Day is time of day {@code [0, 12000)} and night is {@code [12000, 24000)}, the two halves of
 * the vanilla cycle, so equal ratios scale the whole cycle uniformly. The ratio is chosen from the
 * time of day at the start of each call.
 *
 * <p>Has no Bukkit dependencies so it can be unit-tested without a server.
 */
public final class ClockAccumulator {

    public static final long TICKS_PER_DAY = 24000L;
    public static final long NIGHT_START = 12000L;

    private final double dayRatio;
    private final double nightRatio;
    private double remainder;

    public ClockAccumulator(double dayRatio, double nightRatio) {
        this.dayRatio = requireValidRatio(dayRatio, "dayRatio");
        this.nightRatio = requireValidRatio(nightRatio, "nightRatio");
    }

    public double getDayRatio() {
        return dayRatio;
    }

    public double getNightRatio() {
        return nightRatio;
    }

    /**
     * Returns whether {@code worldTime} falls in the night half of the cycle.
     * Accepts absolute (full) world time; only the time of day is considered.
     */
    public static boolean isNight(long worldTime) {
        return Math.floorMod(worldTime, TICKS_PER_DAY) >= NIGHT_START;
    }

    /**
     * Advances the accumulator by one server tick and returns how many whole world-time ticks
     * to add, given the current world time.
     */
    public long ticksToAdvance(long worldTime) {
        double ratio = isNight(worldTime) ? nightRatio : dayRatio;
        remainder += 1.0 / ratio;
        long whole = (long) Math.floor(remainder);
        remainder -= whole;
        return whole;
    }

    private static double requireValidRatio(double ratio, String name) {
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            throw new IllegalArgumentException(name + " must be a positive, finite number but was " + ratio);
        }
        return ratio;
    }
}
