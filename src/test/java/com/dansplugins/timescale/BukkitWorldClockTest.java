package com.dansplugins.timescale;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitWorldClockTest {

    @Test
    void onlyTheOverworldIsScaled() {
        assertTrue(BukkitWorldClock.isScaled(World.Environment.NORMAL));
        assertFalse(BukkitWorldClock.isScaled(World.Environment.NETHER));
        assertFalse(BukkitWorldClock.isScaled(World.Environment.THE_END));
    }

    @Test
    void triesTheCurrentDaylightCycleNameBeforeTheLegacyOne() {
        assertEquals(List.of("advance_time", "doDaylightCycle"), BukkitWorldClock.DAYLIGHT_CYCLE_RULE_NAMES);
    }

    @Test
    void findFirstReturnsTheFirstNameThatResolves() {
        Map<String, String> rules = Map.of("doDaylightCycle", "legacy", "advance_time", "current");
        assertEquals("current", BukkitWorldClock.findFirst(List.of("advance_time", "doDaylightCycle"), rules::get));
        assertEquals("legacy", BukkitWorldClock.findFirst(List.of("missing", "doDaylightCycle"), rules::get));
    }

    @Test
    void findFirstSkipsNamesTheLookupRejects() {
        String found = BukkitWorldClock.findFirst(List.of("doDaylightCycle", "advance_time"), name -> {
            if (!name.equals(name.toLowerCase())) {
                throw new IllegalArgumentException("malformed key " + name);
            }
            return name;
        });
        assertEquals("advance_time", found);
    }

    @Test
    void findFirstReturnsNullWhenNothingResolves() {
        assertNull(BukkitWorldClock.findFirst(List.of("a", "b"), name -> null));
    }

    @Test
    void findsTheDaylightCycleRuleOnTheCompileTimeApi() {
        // The test classpath is the 1.21.4 API, where the rule is still called doDaylightCycle.
        assertEquals("doDaylightCycle", BukkitWorldClock.findDaylightCycleRule().getName());
    }

    @Test
    void findsTheSleepingPercentageRuleOnTheCompileTimeApi() {
        assertEquals("playersSleepingPercentage", BukkitWorldClock.findSleepingPercentageRule().getName());
    }
}
