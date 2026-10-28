package com.dansplugins.timescale;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.function.Supplier;

/**
 * Turns off {@code doDaylightCycle} on worlds loaded after Timescale enables.
 *
 * <p>{@link ClockTask} advances every loaded overworld-type world, so a world loaded later (by a
 * world-management plugin, or reloaded from disk with the gamerule back on) would otherwise have
 * its clock moved by both vanilla and Timescale.
 */
public final class WorldLoadListener implements Listener {

    private final GameRule<Boolean> daylightCycleRule;

    public WorldLoadListener(GameRule<Boolean> daylightCycleRule) {
        this.daylightCycleRule = daylightCycleRule;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        // Only the gamerule is set here, so the sleeping-percentage rule is not needed.
        disableDaylightCycle(world.getEnvironment(), () -> new BukkitWorldClock(world, daylightCycleRule, null));
    }

    /**
     * Turns off the daylight cycle of a newly loaded world if Timescale drives its clock. The clock is
     * supplied lazily so worlds Timescale leaves alone are never wrapped.
     */
    static void disableDaylightCycle(World.Environment environment, Supplier<? extends WorldClock> clock) {
        if (BukkitWorldClock.isScaled(environment)) {
            clock.get().setDaylightCycle(false);
        }
    }
}
