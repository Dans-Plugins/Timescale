package com.dansplugins.timescale;

import org.bukkit.GameRule;
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

        GameRule<Boolean> daylightCycleRule = BukkitWorldClock.findDaylightCycleRule();
        if (daylightCycleRule == null) {
            // Without the gamerule, vanilla would keep moving the clock alongside this plugin.
            getLogger().severe("Could not find the daylight-cycle gamerule (tried "
                    + BukkitWorldClock.DAYLIGHT_CYCLE_RULE_NAMES + "); disabling Timescale");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        clockTask = new ClockTask(() -> BukkitWorldClock.scaledWorlds(getServer().getWorlds(), daylightCycleRule),
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
