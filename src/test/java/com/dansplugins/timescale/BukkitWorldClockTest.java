package com.dansplugins.timescale;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
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
        assertFalse(BukkitWorldClock.isScaled(World.Environment.CUSTOM));
    }

    @Test
    void triesTheCurrentDaylightCycleNameBeforeTheLegacyOne() {
        assertEquals(List.of("advance_time", "doDaylightCycle"), BukkitWorldClock.DAYLIGHT_CYCLE_RULE_NAMES);
    }

    @Test
    void triesTheCurrentSleepingPercentageNameBeforeTheLegacyOne() {
        assertEquals(List.of("players_sleeping_percentage", "playersSleepingPercentage"),
                BukkitWorldClock.SLEEPING_PERCENTAGE_RULE_NAMES);
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

    @Test
    void scaledOnlyKeepsOverworldsInOrder() {
        Map<String, World.Environment> environments = Map.of(
                "world", World.Environment.NORMAL,
                "world_nether", World.Environment.NETHER,
                "second", World.Environment.NORMAL,
                "world_the_end", World.Environment.THE_END,
                "custom", World.Environment.CUSTOM);
        List<String> worlds = List.of("world", "world_nether", "second", "world_the_end", "custom");
        assertEquals(List.of("world", "second"), BukkitWorldClock.scaledOnly(worlds, environments::get));
    }

    @Test
    void scaledOnlyReturnsNothingWhenNoWorldIsAnOverworld() {
        assertEquals(List.of(), BukkitWorldClock.scaledOnly(List.of("nether"), world -> World.Environment.NETHER));
    }

    @Test
    void daylightCycleIsOnOnlyWhenTheGameruleIsTrue() {
        assertTrue(BukkitWorldClock.isOn(Boolean.TRUE));
        assertFalse(BukkitWorldClock.isOn(Boolean.FALSE));
        assertFalse(BukkitWorldClock.isOn(null));
    }

    @Test
    void spectatorsAreMarkedAsSpectators() {
        assertTrue(BukkitWorldClock.sleeperOf(GameMode.SPECTATOR, false, 0, false).spectator());
        assertFalse(BukkitWorldClock.sleeperOf(GameMode.SURVIVAL, false, 0, false).spectator());
        assertFalse(BukkitWorldClock.sleeperOf(GameMode.CREATIVE, false, 0, false).spectator());
        assertFalse(BukkitWorldClock.sleeperOf(GameMode.ADVENTURE, false, 0, false).spectator());
    }

    @Test
    void sleeperCarriesThePlayersSleepState() {
        assertEquals(new SleepRule.Sleeper(false, true, 42, false),
                BukkitWorldClock.sleeperOf(GameMode.SURVIVAL, true, 42, false));
        assertEquals(new SleepRule.Sleeper(false, false, 7, true),
                BukkitWorldClock.sleeperOf(GameMode.SURVIVAL, false, 7, true));
    }

    @Test
    void sleepingPercentageIsTheGameruleValue() {
        GameRule<Integer> rule = BukkitWorldClock.findSleepingPercentageRule();
        assertEquals(37, BukkitWorldClock.sleepingPercentage(rule, r -> 37));
        assertEquals(0, BukkitWorldClock.sleepingPercentage(rule, r -> 0));
    }

    @Test
    void sleepingPercentageFallsBackToEveryPlayerWhenTheRuleIsMissing() {
        assertEquals(SleepRule.DEFAULT_SLEEPING_PERCENTAGE, BukkitWorldClock.sleepingPercentage(null, r -> 37));
        assertEquals(100, SleepRule.DEFAULT_SLEEPING_PERCENTAGE);
    }

    @Test
    void sleepingPercentageFallsBackToEveryPlayerWhenTheWorldHasNoValue() {
        GameRule<Integer> rule = BukkitWorldClock.findSleepingPercentageRule();
        assertEquals(SleepRule.DEFAULT_SLEEPING_PERCENTAGE, BukkitWorldClock.sleepingPercentage(rule, r -> null));
    }
}
