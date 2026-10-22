package com.dansplugins.timescale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockAccumulatorTest {

    /** Runs server ticks from {@code start} until world time reaches {@code end}; returns the server ticks taken. */
    private static long serverTicksToReach(ClockAccumulator clock, long start, long end) {
        long time = start;
        long serverTicks = 0;
        while (time < end) {
            time += clock.ticksToAdvance(time);
            serverTicks++;
        }
        return serverTicks;
    }

    @Test
    void ratioOfOneAdvancesOneTickPerServerTick() {
        ClockAccumulator clock = new ClockAccumulator(1.0, 1.0);
        for (long time = 0; time < ClockAccumulator.TICKS_PER_DAY; time++) {
            assertEquals(1L, clock.ticksToAdvance(time));
        }
    }

    @Test
    void ratioOfThreeAdvancesOnceEveryThreeServerTicks() {
        ClockAccumulator clock = new ClockAccumulator(3.0, 3.0);
        assertEquals(0L, clock.ticksToAdvance(0));
        assertEquals(0L, clock.ticksToAdvance(0));
        assertEquals(1L, clock.ticksToAdvance(0));
    }

    @Test
    void ratioBelowOneAdvancesSeveralTicksPerServerTick() {
        ClockAccumulator clock = new ClockAccumulator(0.5, 0.5);
        assertEquals(2L, clock.ticksToAdvance(0));
    }

    @Test
    void ratioOfThreeGivesAOneHourCycle() {
        ClockAccumulator clock = new ClockAccumulator(3.0, 3.0);
        long serverTicks = serverTicksToReach(clock, 0, ClockAccumulator.TICKS_PER_DAY);
        // One hour at 20 server ticks per second; allow one tick of floating-point slack.
        assertEquals(72000L, serverTicks, 1.0);
    }

    @Test
    void dayAndNightUseTheirOwnRatios() {
        ClockAccumulator clock = new ClockAccumulator(4.0, 1.0);
        long dayTicks = serverTicksToReach(clock, 0, ClockAccumulator.NIGHT_START);
        long nightTicks = serverTicksToReach(clock, ClockAccumulator.NIGHT_START, ClockAccumulator.TICKS_PER_DAY);
        assertEquals(48000L, dayTicks, 1.0);
        assertEquals(12000L, nightTicks, 1.0);
    }

    @Test
    void ratioSwitchesAtNightStart() {
        ClockAccumulator clock = new ClockAccumulator(1000.0, 1.0);
        assertEquals(0L, clock.ticksToAdvance(ClockAccumulator.NIGHT_START - 1));
        // The day tick left a tiny remainder; a night tick at ratio 1.0 still yields exactly one.
        assertEquals(1L, clock.ticksToAdvance(ClockAccumulator.NIGHT_START));
    }

    @Test
    void fractionalRemainderCarriesAcrossPhases() {
        ClockAccumulator clock = new ClockAccumulator(2.0, 2.0);
        assertEquals(0L, clock.ticksToAdvance(ClockAccumulator.NIGHT_START - 1));
        assertEquals(1L, clock.ticksToAdvance(ClockAccumulator.NIGHT_START));
    }

    @Test
    void isNightUsesTimeOfDayOfAbsoluteWorldTime() {
        assertFalse(ClockAccumulator.isNight(0));
        assertFalse(ClockAccumulator.isNight(11999));
        assertTrue(ClockAccumulator.isNight(12000));
        assertTrue(ClockAccumulator.isNight(23999));
        assertFalse(ClockAccumulator.isNight(24000));
        assertTrue(ClockAccumulator.isNight(5 * 24000L + 13000));
        assertTrue(ClockAccumulator.isNight(-1));
    }

    @Test
    void exposesConfiguredRatios() {
        ClockAccumulator clock = new ClockAccumulator(3.0, 1.5);
        assertEquals(3.0, clock.getDayRatio());
        assertEquals(1.5, clock.getNightRatio());
    }

    @Test
    void rejectsNonPositiveOrNonFiniteRatios() {
        assertThrows(IllegalArgumentException.class, () -> new ClockAccumulator(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new ClockAccumulator(-1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new ClockAccumulator(1.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ClockAccumulator(1.0, Double.POSITIVE_INFINITY));
    }
}
