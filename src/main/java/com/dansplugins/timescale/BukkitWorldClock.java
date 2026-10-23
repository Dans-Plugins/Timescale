package com.dansplugins.timescale;

import org.bukkit.GameRule;
import org.bukkit.World;

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

    private final World world;
    private final GameRule<Boolean> daylightCycleRule;

    public BukkitWorldClock(World world, GameRule<Boolean> daylightCycleRule) {
        this.world = world;
        this.daylightCycleRule = daylightCycleRule;
    }

    /**
     * Returns whether Timescale drives the clock of a world in {@code environment}. Only the
     * overworld has a day cycle; the nether and the end are left alone.
     */
    public static boolean isScaled(World.Environment environment) {
        return environment == World.Environment.NORMAL;
    }

    /** Wraps every world whose clock Timescale drives. */
    public static List<WorldClock> scaledWorlds(List<World> worlds, GameRule<Boolean> daylightCycleRule) {
        List<WorldClock> clocks = new ArrayList<>();
        for (World world : worlds) {
            if (isScaled(world.getEnvironment())) {
                clocks.add(new BukkitWorldClock(world, daylightCycleRule));
            }
        }
        return clocks;
    }

    /** Returns this server's daylight-cycle gamerule, or {@code null} if none of the known names resolves. */
    @SuppressWarnings("unchecked")
    public static GameRule<Boolean> findDaylightCycleRule() {
        GameRule<?> rule = findFirst(DAYLIGHT_CYCLE_RULE_NAMES, BukkitWorldClock::gameRuleByName);
        if (rule == null || invokeGameRuleMethod("getType", rule) != Boolean.class) {
            return null;
        }
        return (GameRule<Boolean>) rule;
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
}
