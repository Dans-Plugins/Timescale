package com.dansplugins.timescale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepRuleTest {

    @Test
    void everyPlayerMustSleepAtTheDefaultPercentage() {
        assertFalse(SleepRule.isNightSkipped(3, 2, true, 100));
        assertTrue(SleepRule.isNightSkipped(3, 3, true, 100));
    }

    @Test
    void neededSleepersRoundUp() {
        // 50% of 3 players is 1.5, so 2 are needed.
        assertFalse(SleepRule.isNightSkipped(3, 1, true, 50));
        assertTrue(SleepRule.isNightSkipped(3, 2, true, 50));
    }

    @Test
    void atLeastOneSleeperIsAlwaysNeeded() {
        assertFalse(SleepRule.isNightSkipped(4, 0, true, 0));
        assertTrue(SleepRule.isNightSkipped(4, 1, true, 0));
    }

    @Test
    void aPercentageOverOneHundredNeverSkips() {
        assertFalse(SleepRule.isNightSkipped(2, 2, true, 101));
    }

    @Test
    void sleepingIgnoredPlayersAloneDoNotSkip() {
        // Two players, both marked sleeping-ignored, nobody actually in bed.
        assertFalse(SleepRule.isNightSkipped(2, 2, false, 100));
    }

    @Test
    void anEmptyWorldNeverSkips() {
        assertFalse(SleepRule.isNightSkipped(0, 0, false, 0));
    }

    @Test
    void aPlayerIsDeeplyAsleepAfterOneHundredTicksInBed() {
        assertFalse(SleepRule.isDeeplySleeping(true, 99));
        assertTrue(SleepRule.isDeeplySleeping(true, 100));
        assertFalse(SleepRule.isDeeplySleeping(false, 100));
    }

    @Test
    void nextMorningIsTheStartOfTheNextDay() {
        assertEquals(24000L, SleepRule.nextMorning(13000L));
        assertEquals(24000L, SleepRule.nextMorning(0L));
        assertEquals(5 * 24000L, SleepRule.nextMorning(4 * 24000L + 23999L));
    }
}
