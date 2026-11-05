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
    /**
     * The smallest ratio accepted, which moves the clock a whole day per server tick. A smaller
     * ratio defeats the plugin, and one small enough overflows the world's full time.
     */
    public static final double MIN_RATIO = 1.0 / TICKS_PER_DAY;
    /** What a valid ratio is, worded to follow "must be" in a message. */
    public static final String RATIO_RULE = "a finite number of at least 1/24000 (about 0.0000417)";

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

    /** Returns whether {@code ratio} is finite and at least {@link #MIN_RATIO}. */
    public static boolean isValidRatio(double ratio) {
        return Double.isFinite(ratio) && ratio >= MIN_RATIO;
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
        if (!isValidRatio(ratio)) {
            throw new IllegalArgumentException(name + " must be " + RATIO_RULE + " but was " + ratio);
        }
        return ratio;
    }
}
