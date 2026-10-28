package com.dansplugins.timescale;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClockTaskTest {

    /** In-memory world clock; there is no server in unit tests. */
    private static final class FakeWorldClock implements WorldClock {
        private final UUID id = UUID.randomUUID();
        long fullTime;
        Boolean daylightCycle;
        boolean nightSkipped;

        FakeWorldClock(long fullTime) {
            this.fullTime = fullTime;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public long getFullTime() {
            return fullTime;
        }

        @Override
        public void setFullTime(long time) {
            fullTime = time;
        }

        @Override
        public void setDaylightCycle(boolean enabled) {
            daylightCycle = enabled;
        }

        @Override
        public boolean isNightSkipped() {
            return nightSkipped;
        }
    }

    private static ClockTask taskFor(double dayRatio, double nightRatio, FakeWorldClock... worlds) {
        List<FakeWorldClock> list = Arrays.asList(worlds);
        return new ClockTask(() -> list, dayRatio, nightRatio);
    }

    private static void runTimes(ClockTask task, int times) {
        for (int i = 0; i < times; i++) {
            task.run();
        }
    }

    @Test
    void ratioOfOneAdvancesWorldTimeOneTickPerRun() {
        FakeWorldClock world = new FakeWorldClock(1000);
        runTimes(taskFor(1.0, 1.0, world), 5);
        assertEquals(1005L, world.fullTime);
    }

    @Test
    void dayRatioSlowsTheDayHalf() {
        FakeWorldClock world = new FakeWorldClock(0);
        runTimes(taskFor(4.0, 1.0, world), 400);
        assertEquals(100L, world.fullTime);
    }

    @Test
    void nightRatioAppliesDuringTheNightHalf() {
        FakeWorldClock world = new FakeWorldClock(ClockAccumulator.NIGHT_START);
        runTimes(taskFor(4.0, 2.0, world), 400);
        assertEquals(ClockAccumulator.NIGHT_START + 200L, world.fullTime);
    }

    @Test
    void preservesElapsedDaysInFullTime() {
        long start = 7 * ClockAccumulator.TICKS_PER_DAY + 500;
        FakeWorldClock world = new FakeWorldClock(start);
        taskFor(1.0, 1.0, world).run();
        assertEquals(start + 1, world.fullTime);
    }

    @Test
    void eachWorldCarriesItsOwnFractionalRemainder() {
        FakeWorldClock first = new FakeWorldClock(0);
        FakeWorldClock second = new FakeWorldClock(0);
        ClockTask task = taskFor(2.0, 2.0, first, second);
        task.run();
        assertEquals(0L, first.fullTime);
        assertEquals(0L, second.fullTime);
        task.run();
        assertEquals(1L, first.fullTime);
        assertEquals(1L, second.fullTime);
    }

    @Test
    void sleepingSkipsToTheNextMorning() {
        long start = 3 * ClockAccumulator.TICKS_PER_DAY + 15000;
        FakeWorldClock world = new FakeWorldClock(start);
        world.nightSkipped = true;
        taskFor(4.0, 4.0, world).run();
        assertEquals(4 * ClockAccumulator.TICKS_PER_DAY, world.fullTime);
    }

    @Test
    void sleepersStillInBedDoNotSkipAnotherDayEachTick() {
        FakeWorldClock world = new FakeWorldClock(13000);
        world.nightSkipped = true;
        ClockTask task = taskFor(1.0, 1.0, world);
        task.run();
        task.run();
        task.run();
        assertEquals(ClockAccumulator.TICKS_PER_DAY + 2, world.fullTime);
    }

    @Test
    void theNextNightCanBeSkippedOnceTheSleepersHaveWoken() {
        FakeWorldClock world = new FakeWorldClock(13000);
        world.nightSkipped = true;
        ClockTask task = taskFor(1.0, 1.0, world);
        task.run();
        world.nightSkipped = false;
        task.run();
        world.fullTime = ClockAccumulator.TICKS_PER_DAY + 13000;
        world.nightSkipped = true;
        task.run();
        assertEquals(2 * ClockAccumulator.TICKS_PER_DAY, world.fullTime);
    }

    @Test
    void sleepingInOneWorldLeavesTheOthersOnTheirRatio() {
        FakeWorldClock sleeping = new FakeWorldClock(13000);
        FakeWorldClock awake = new FakeWorldClock(13000);
        sleeping.nightSkipped = true;
        taskFor(1.0, 1.0, sleeping, awake).run();
        assertEquals(ClockAccumulator.TICKS_PER_DAY, sleeping.fullTime);
        assertEquals(13001L, awake.fullTime);
    }

    @Test
    void disableAndRestoreToggleDaylightCycle() {
        FakeWorldClock world = new FakeWorldClock(0);
        ClockTask task = taskFor(1.0, 1.0, world);
        assertNull(world.daylightCycle);
        task.disableDaylightCycle();
        assertEquals(Boolean.FALSE, world.daylightCycle);
        task.restoreDaylightCycle();
        assertEquals(Boolean.TRUE, world.daylightCycle);
    }

    @Test
    void rejectsInvalidRatios() {
        assertThrows(IllegalArgumentException.class, () -> taskFor(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> taskFor(1.0, Double.NaN));
    }
}
