package com.dansplugins.timescale;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimescaleConfigTest {

    private static TimescaleConfig parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return TimescaleConfig.from(config);
    }

    @Test
    void readsDayAndNightRatiosSeparately() throws Exception {
        TimescaleConfig config = parse("day-ratio: 3.0\nnight-ratio: 1.5\n");
        assertEquals(3.0, config.getDayRatio());
        assertEquals(1.5, config.getNightRatio());
        assertTrue(config.getWarnings().isEmpty(), config.getWarnings().toString());
    }

    @Test
    void acceptsWholeNumberRatios() throws Exception {
        TimescaleConfig config = parse("day-ratio: 4\nnight-ratio: 2\n");
        assertEquals(4.0, config.getDayRatio());
        assertEquals(2.0, config.getNightRatio());
        assertTrue(config.getWarnings().isEmpty(), config.getWarnings().toString());
    }

    @Test
    void missingRatioFallsBackToVanillaWithWarning() throws Exception {
        TimescaleConfig config = parse("day-ratio: 3.0\n");
        assertEquals(3.0, config.getDayRatio());
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getNightRatio());
        assertEquals(1, config.getWarnings().size());
        assertTrue(config.getWarnings().get(0).contains("night-ratio"), config.getWarnings().get(0));
    }

    @Test
    void missingRatioWarnsEvenWithTheBundledConfigAsDefaults() throws Exception {
        // JavaPlugin.getConfig() attaches the bundled config.yml as defaults, so a removed key
        // must not be silently filled in from them without the warning.
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("day-ratio: 3.0\n");
        config.setDefaults(bundledConfig());
        TimescaleConfig parsed = TimescaleConfig.from(config);
        assertEquals(3.0, parsed.getDayRatio());
        assertEquals(TimescaleConfig.DEFAULT_RATIO, parsed.getNightRatio());
        assertEquals(1, parsed.getWarnings().size(), parsed.getWarnings().toString());
        assertTrue(parsed.getWarnings().get(0).contains("night-ratio is not set"), parsed.getWarnings().get(0));
    }

    @Test
    void nonNumericRatioFallsBackToVanillaWithWarning() throws Exception {
        TimescaleConfig config = parse("day-ratio: slow\nnight-ratio: 2.0\n");
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getDayRatio());
        assertEquals(2.0, config.getNightRatio());
        assertEquals(1, config.getWarnings().size());
        assertTrue(config.getWarnings().get(0).contains("day-ratio"), config.getWarnings().get(0));
    }

    @Test
    void quotedRatioIsNotANumberAndFallsBackToVanillaWithWarning() throws Exception {
        TimescaleConfig config = parse("day-ratio: \"3.0\"\nnight-ratio: 2.0\n");
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getDayRatio());
        assertEquals(2.0, config.getNightRatio());
        assertEquals(1, config.getWarnings().size());
        assertTrue(config.getWarnings().get(0).contains("day-ratio"), config.getWarnings().get(0));
    }

    @Test
    void nonPositiveOrNonFiniteRatiosFallBackToVanillaWithWarnings() throws Exception {
        TimescaleConfig config = parse("day-ratio: 0\nnight-ratio: .nan\n");
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getDayRatio());
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getNightRatio());
        assertEquals(2, config.getWarnings().size());

        TimescaleConfig negative = parse("day-ratio: -2.0\nnight-ratio: .inf\n");
        assertEquals(TimescaleConfig.DEFAULT_RATIO, negative.getDayRatio());
        assertEquals(TimescaleConfig.DEFAULT_RATIO, negative.getNightRatio());
        assertEquals(2, negative.getWarnings().size());
    }

    @Test
    void ratioBelowTheMinimumFallsBackToVanillaWithWarning() throws Exception {
        TimescaleConfig config = parse("day-ratio: 1.0e-15\nnight-ratio: 0.0000416\n");
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getDayRatio());
        assertEquals(TimescaleConfig.DEFAULT_RATIO, config.getNightRatio());
        assertEquals(2, config.getWarnings().size());
        assertTrue(config.getWarnings().get(0).contains("day-ratio"), config.getWarnings().get(0));
        assertTrue(config.getWarnings().get(0).contains(ClockAccumulator.RATIO_RULE), config.getWarnings().get(0));
    }

    @Test
    void smallestDocumentedRatioIsKept() throws Exception {
        TimescaleConfig config = parse("day-ratio: 0.0000417\nnight-ratio: 1.0\n");
        assertEquals(0.0000417, config.getDayRatio());
        assertTrue(config.getWarnings().isEmpty(), config.getWarnings().toString());
    }

    @Test
    void writtenRatiosReadBackAfterASaveAndReload() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("day-ratio: 1.0\nnight-ratio: 1.0\n");
        TimescaleConfig.write(config, 4.0, 0.5);
        TimescaleConfig reloaded = parse(config.saveToString());
        assertEquals(4.0, reloaded.getDayRatio());
        assertEquals(0.5, reloaded.getNightRatio());
        assertTrue(reloaded.getWarnings().isEmpty(), reloaded.getWarnings().toString());
    }

    @Test
    void bundledDefaultConfigIsVanillaSpeedWithoutWarnings() throws IOException {
        TimescaleConfig config = TimescaleConfig.from(bundledConfig());
        assertEquals(1.0, config.getDayRatio());
        assertEquals(1.0, config.getNightRatio());
        assertTrue(config.getWarnings().isEmpty(), config.getWarnings().toString());
    }

    private static YamlConfiguration bundledConfig() throws IOException {
        try (InputStream in = TimescaleConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(in, "config.yml must be on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
