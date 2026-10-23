package com.dansplugins.timescale;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for Timescale. Turns off the vanilla daylight cycle and advances world time itself
 * at the configured day and night ratios. Scope is defined in MVP.md.
 */
public final class Timescale extends JavaPlugin {

    private ClockTask clockTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        TimescaleConfig config = TimescaleConfig.from(getConfig());
        for (String warning : config.getWarnings()) {
            getLogger().warning(warning);
        }

        clockTask = new ClockTask(() -> BukkitWorldClock.scaledWorlds(getServer().getWorlds()),
config.getDayRatio(), config.getNightRatio());
        clockTask.disableDaylightCycle();
        getServer().getScheduler().runTaskTimer(this, clockTask, 1L, 1L);
        getLogger().info("Day ratio " + config.getDayRatio() + ", night ratio " + config.getNightRatio());
    }

    @Override
    public void onDisable() {
        // Hand the clock back to vanilla so removing the plugin does not leave time frozen.
        if (clockTask != null) {
            clockTask.restoreDaylightCycle();
            clockTask = null;
        }
    }
}
