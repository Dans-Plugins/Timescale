package com.dansplugins.timescale;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The day and night ratios read from {@code config.yml}.
 *
 * <p>A missing, non-numeric, non-positive or non-finite ratio falls back to {@link #DEFAULT_RATIO}
 * (vanilla speed) and records a warning, so a typo slows nothing rather than stopping the plugin.
 */
public final class TimescaleConfig {

    public static final String DAY_RATIO_KEY = "day-ratio";
    public static final String NIGHT_RATIO_KEY = "night-ratio";
    public static final double DEFAULT_RATIO = 1.0;

    private final double dayRatio;
    private final double nightRatio;
    private final List<String> warnings;

    private TimescaleConfig(double dayRatio, double nightRatio, List<String> warnings) {
        this.dayRatio = dayRatio;
        this.nightRatio = nightRatio;
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public static TimescaleConfig from(ConfigurationSection section) {
        List<String> warnings = new ArrayList<>();
        double dayRatio = readRatio(section, DAY_RATIO_KEY, warnings);
        double nightRatio = readRatio(section, NIGHT_RATIO_KEY, warnings);
        return new TimescaleConfig(dayRatio, nightRatio, warnings);
    }

    /** Writes both ratios to {@code section} under the keys {@link #from} reads. */
    public static void write(ConfigurationSection section, double dayRatio, double nightRatio) {
        section.set(DAY_RATIO_KEY, dayRatio);
        section.set(NIGHT_RATIO_KEY, nightRatio);
    }

    public double getDayRatio() {
        return dayRatio;
    }

    public double getNightRatio() {
        return nightRatio;
    }

    /** Problems found while reading the config, one message per ratio that fell back to the default. */
    public List<String> getWarnings() {
        return warnings;
    }

    private static double readRatio(ConfigurationSection section, String key, List<String> warnings) {
        Object value = section.get(key);
        if (value == null) {
            warnings.add(key + " is not set; using " + DEFAULT_RATIO);
            return DEFAULT_RATIO;
        }
        if (!(value instanceof Number)) {
            warnings.add(key + " must be a number but was '" + value + "'; using " + DEFAULT_RATIO);
            return DEFAULT_RATIO;
        }
        double ratio = ((Number) value).doubleValue();
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            warnings.add(key + " must be a positive, finite number but was " + ratio + "; using " + DEFAULT_RATIO);
            return DEFAULT_RATIO;
        }
        return ratio;
    }
}
