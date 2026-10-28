package com.dansplugins.timescale;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Adapts a Bukkit {@link World} to {@link WorldClock}.
 */
public final class BukkitWorldClock implements WorldClock {

    /**
     * Names the daylight-cycle gamerule has gone by, newest first. Minecraft 1.21.11 renamed
     * {@code doDaylightCycle} to {@code advance_time}, and the {@code GameRule.DO_DAYLIGHT_CYCLE}
     * constant does not exist on every supported version, so the rule is looked up by name.
     */
    static final List<String> DAYLIGHT_CYCLE_RULE_NAMES = List.of("advance_time", "doDaylightCycle");

    /**
     * Names the sleeping-percentage gamerule may go by, newest first, looked up by name for the same
     * reason as {@link #DAYLIGHT_CYCLE_RULE_NAMES}.
     */
    static final List<String> SLEEPING_PERCENTAGE_RULE_NAMES =
            List.of("players_sleeping_percentage", "playersSleepingPercentage");

    private final World world;
    private final GameRule<Boolean> daylightCycleRule;
    private final GameRule<Integer> sleepingPercentageRule;

    /**
     * @param sleepingPercentageRule the sleeping-percentage gamerule, or {@code null} to use the
     *                               vanilla default of {@value SleepRule#DEFAULT_SLEEPING_PERCENTAGE}
     */
    public BukkitWorldClock(World world, GameRule<Boolean> daylightCycleRule, GameRule<Integer> sleepingPercentageRule) {
        this.world = world;
        this.daylightCycleRule = daylightCycleRule;
        this.sleepingPercentageRule = sleepingPercentageRule;
    }

    /**
     * Returns whether Timescale drives the clock of a world in {@code environment}. Only the
     * overworld has a day cycle; the nether and the end are left alone.
     */
    public static boolean isScaled(World.Environment environment) {
        return environment == World.Environment.NORMAL;
    }

    /** Wraps every world whose clock Timescale drives. */
    public static List<WorldClock> scaledWorlds(List<World> worlds, GameRule<Boolean> daylightCycleRule,
                                                GameRule<Integer> sleepingPercentageRule) {
        List<WorldClock> clocks = new ArrayList<>();
        for (World world : worlds) {
            if (isScaled(world.getEnvironment())) {
                clocks.add(new BukkitWorldClock(world, daylightCycleRule, sleepingPercentageRule));
            }
        }
        return clocks;
    }

    /** Returns this server's daylight-cycle gamerule, or {@code null} if none of the known names resolves. */
    public static GameRule<Boolean> findDaylightCycleRule() {
        return findRule(DAYLIGHT_CYCLE_RULE_NAMES, Boolean.class);
    }

    /** Returns this server's sleeping-percentage gamerule, or {@code null} if none of the known names resolves. */
    public static GameRule<Integer> findSleepingPercentageRule() {
        return findRule(SLEEPING_PERCENTAGE_RULE_NAMES, Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> GameRule<T> findRule(List<String> names, Class<T> type) {
        GameRule<?> rule = findFirst(names, BukkitWorldClock::gameRuleByName);
        if (rule == null || invokeGameRuleMethod("getType", rule) != type) {
            return null;
        }
        return (GameRule<T>) rule;
    }

    private static GameRule<?> gameRuleByName(String name) {
        return (GameRule<?>) invokeGameRuleMethod("getByName", null, name);
    }

    /**
     * Calls a {@link GameRule} method reflectively. {@code GameRule} is a class up to 1.21.4 and an
     * interface from 1.21.11, so a compiled call to any of its methods fails to link on one side or
     * the other; looking the method up at runtime works on both.
     */
    private static Object invokeGameRuleMethod(String method, GameRule<?> target, Object... args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        try {
            return GameRule.class.getMethod(method, types).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("GameRule." + method + " failed", e);
        }
    }

    /**
     * Returns the first non-null result of {@code lookup} over {@code names}, or {@code null}. A name
     * the lookup rejects as malformed is skipped, since older and newer servers accept different forms.
     */
    static <T> T findFirst(List<String> names, Function<String, T> lookup) {
        for (String name : names) {
            try {
                T found = lookup.apply(name);
                if (found != null) {
                    return found;
                }
            } catch (IllegalArgumentException e) {
                // Not a valid name on this server; try the next one.
            }
        }
        return null;
    }

    @Override
    public UUID getId() {
        return world.getUID();
    }

    @Override
    public long getFullTime() {
        return world.getFullTime();
    }

    @Override
    public void setFullTime(long time) {
        world.setFullTime(time);
    }

    @Override
    public void setDaylightCycle(boolean enabled) {
        world.setGameRule(daylightCycleRule, enabled);
    }

    @Override
    public boolean isNightSkipped() {
        int activePlayers = 0;
        int deepSleepers = 0;
        boolean anyDeepSleep = false;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            activePlayers++;
            boolean deeplySleeping = SleepRule.isDeeplySleeping(player.isSleeping(), player.getSleepTicks());
            anyDeepSleep |= deeplySleeping;
            if (deeplySleeping || player.isSleepingIgnored()) {
                deepSleepers++;
            }
        }
        return SleepRule.isNightSkipped(activePlayers, deepSleepers, anyDeepSleep, sleepingPercentage());
    }

    private int sleepingPercentage() {
        if (sleepingPercentageRule == null) {
            return SleepRule.DEFAULT_SLEEPING_PERCENTAGE;
        }
        Integer value = world.getGameRuleValue(sleepingPercentageRule);
        return value != null ? value : SleepRule.DEFAULT_SLEEPING_PERCENTAGE;
    }
}
