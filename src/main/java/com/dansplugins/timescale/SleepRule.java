package com.dansplugins.timescale;

/**
 * Decides when sleeping players skip the night, and where the clock lands when they do.
 *
 * <p>With {@code doDaylightCycle} off, vanilla still wakes everyone up once enough players have
 * slept, but no longer moves the clock to morning. This mirrors the vanilla (CraftBukkit) sleep
 * check so Timescale can make that jump itself on the same tick:
 *
 * <ul>
 *     <li>spectators are not counted;</li>
 *     <li>a player marked as sleeping-ignored counts as asleep, but at least one player must be
 *     genuinely deeply asleep (in bed for {@value #DEEP_SLEEP_TICKS} ticks);</li>
 *     <li>the number needed is {@code max(1, ceil(players * percentage / 100))}, where
 *     {@code percentage} is the {@code playersSleepingPercentage} gamerule.</li>
 * </ul>
 *
 * <p>Has no Bukkit dependencies so it can be unit-tested without a server.
 */
public final class SleepRule {

    /** The vanilla default of the {@code playersSleepingPercentage} gamerule. */
    public static final int DEFAULT_SLEEPING_PERCENTAGE = 100;

    /** How long a player must have been in bed before vanilla counts them as deeply asleep. */
    public static final int DEEP_SLEEP_TICKS = 100;

    private SleepRule() {
    }

    /** Returns whether a player who is {@code sleeping} for {@code sleepTicks} is deeply asleep. */
    public static boolean isDeeplySleeping(boolean sleeping, int sleepTicks) {
        return sleeping && sleepTicks >= DEEP_SLEEP_TICKS;
    }

    /** The sleep state of one player in a world. */
    public record Sleeper(boolean spectator, boolean sleeping, int sleepTicks, boolean sleepingIgnored) {
    }

    /** Returns whether enough of {@code players} are asleep to skip the night. */
    public static boolean isNightSkipped(Iterable<Sleeper> players, int percentage) {
        int activePlayers = 0;
        int deepSleepers = 0;
        boolean anyDeepSleep = false;
        for (Sleeper player : players) {
            if (player.spectator()) {
                continue;
            }
            activePlayers++;
            boolean deeplySleeping = isDeeplySleeping(player.sleeping(), player.sleepTicks());
            anyDeepSleep |= deeplySleeping;
            if (deeplySleeping || player.sleepingIgnored()) {
                deepSleepers++;
            }
        }
        return isNightSkipped(activePlayers, deepSleepers, anyDeepSleep, percentage);
    }

    /**
     * Returns whether enough players are asleep to skip the night.
     *
     * @param activePlayers players in the world who are not spectators
     * @param deepSleepers  non-spectators who are deeply asleep or marked as sleeping-ignored
     * @param anyDeepSleep  whether at least one player is genuinely deeply asleep
     * @param percentage    the {@code playersSleepingPercentage} gamerule
     */
    public static boolean isNightSkipped(int activePlayers, int deepSleepers, boolean anyDeepSleep, int percentage) {
        if (activePlayers <= 0 || !anyDeepSleep) {
            return false;
        }
        int needed = Math.max(1, (int) Math.ceil(activePlayers * (double) percentage / 100.0));
        return deepSleepers >= needed;
    }

    /** Returns the start of the next day after {@code fullTime}, keeping elapsed days. */
    public static long nextMorning(long fullTime) {
        long next = fullTime + ClockAccumulator.TICKS_PER_DAY;
        return next - Math.floorMod(next, ClockAccumulator.TICKS_PER_DAY);
    }
}
