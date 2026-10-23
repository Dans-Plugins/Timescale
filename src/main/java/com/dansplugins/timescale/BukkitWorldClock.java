package com.dansplugins.timescale;

import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapts a Bukkit {@link World} to {@link WorldClock}.
 */
public final class BukkitWorldClock implements WorldClock {

    private final World world;

    public BukkitWorldClock(World world) {
        this.world = world;
    }

    /**
     * Returns whether Timescale drives the clock of a world in {@code environment}. Only the
     * overworld has a day cycle; the nether and the end are left alone.
     */
    public static boolean isScaled(World.Environment environment) {
        return environment == World.Environment.NORMAL;
    }

    /** Wraps every world whose clock Timescale drives. */
    public static List<WorldClock> scaledWorlds(List<World> worlds) {
        List<WorldClock> clocks = new ArrayList<>();
        for (World world : worlds) {
            if (isScaled(world.getEnvironment())) {
                clocks.add(new BukkitWorldClock(world));
            }
        }
        return clocks;
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
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enabled);
    }
}
