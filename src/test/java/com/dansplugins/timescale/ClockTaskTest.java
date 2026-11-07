package com.dansplugins.timescale;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
            fullTimeWrites++;
        }

        int fullTimeWrites;
        int daylightCycleWrites;

        @Override
        public boolean isDaylightCycle() {
            return Boolean.TRUE.equals(daylightCycle);
        }

        @Override
        public void setDaylightCycle(boolean enabled) {
            daylightCycle = enabled;
            daylightCycleWrites++;
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
    void theClockIsOnlyWrittenWhenAWholeTickIsDue() {
        FakeWorldClock world = new FakeWorldClock(0);
        runTimes(taskFor(4.0, 4.0, world), 400);
        assertEquals(100L, world.fullTime);
        assertEquals(100, world.fullTimeWrites);
    }

    @Test
    void aTimeSetFromOutsideCarriesOnAtTheRatioOfTheNewTime() {
        FakeWorldClock world = new FakeWorldClock(0);
        ClockTask task = taskFor(4.0, 2.0, world);
        task.run();
        world.fullTime = ClockAccumulator.NIGHT_START;
        runTimes(task, 7);
        // The first run left 0.25 behind; seven night runs add 3.5, so three whole ticks are due.
        // Seven runs at the day ratio would only add 1.75, leaving two.
        assertEquals(ClockAccumulator.NIGHT_START + 3, world.fullTime);
    }

    @Test
    void worldsAreReadAgainOnEveryRun() {
        FakeWorldClock first = new FakeWorldClock(0);
        FakeWorldClock second = new FakeWorldClock(0);
        List<FakeWorldClock> loaded = new ArrayList<>(List.of(first));
        ClockTask task = new ClockTask(() -> loaded, 1.0, 1.0);
        task.run();

        loaded.add(second);
        task.run();
        assertEquals(2L, first.fullTime);
        assertEquals(1L, second.fullTime);

        loaded.remove(first);
        task.run();
        assertEquals(2L, first.fullTime);
        assertEquals(2L, second.fullTime);
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
    void changingTheRatiosDoesNotLetSleepersStillInBedSkipAgain() {
        FakeWorldClock world = new FakeWorldClock(13000);
        world.nightSkipped = true;
        ClockTask task = taskFor(1.0, 1.0, world);
        task.run();
        task.setRatios(2.0, 2.0);
        runTimes(task, 2);
        assertEquals(ClockAccumulator.TICKS_PER_DAY + 1, world.fullTime);
    }

    @Test
    void daylightCycleTurnedBackOnIsTurnedOffOnTheRunThatSkipsTheNight() {
        FakeWorldClock world = new FakeWorldClock(13000);
        ClockTask task = taskFor(1.0, 1.0, world);
        task.disableDaylightCycle();
        world.daylightCycle = true;
        world.nightSkipped = true;
        task.run();
        assertEquals(Boolean.FALSE, world.daylightCycle);
        assertEquals(ClockAccumulator.TICKS_PER_DAY, world.fullTime);
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
    void daylightCycleTurnedBackOnIsTurnedOffOnTheNextRun() {
        FakeWorldClock world = new FakeWorldClock(1000);
        ClockTask task = taskFor(1.0, 1.0, world);
        task.disableDaylightCycle();
        world.daylightCycle = true;
        task.run();
        assertEquals(Boolean.FALSE, world.daylightCycle);
        assertEquals(1001L, world.fullTime);
    }

    @Test
    void daylightCycleAlreadyOffIsNotRewrittenEachRun() {
        FakeWorldClock world = new FakeWorldClock(0);
        ClockTask task = taskFor(1.0, 1.0, world);
        task.disableDaylightCycle();
        runTimes(task, 5);
        assertEquals(1, world.daylightCycleWrites);
        assertEquals(Boolean.FALSE, world.daylightCycle);
    }

    @Test
    void setRatiosAppliesFromTheNextRun() {
        FakeWorldClock world = new FakeWorldClock(0);
        ClockTask task = taskFor(1.0, 1.0, world);
        runTimes(task, 10);
        assertEquals(10L, world.fullTime);
        task.setRatios(4.0, 2.0);
        assertEquals(4.0, task.getDayRatio());
        assertEquals(2.0, task.getNightRatio());
        runTimes(task, 400);
        assertEquals(110L, world.fullTime);
    }

    @Test
    void setRatiosRejectsInvalidRatiosAndKeepsTheOldOnes() {
        FakeWorldClock world = new FakeWorldClock(0);
        ClockTask task = taskFor(2.0, 3.0, world);
        assertThrows(IllegalArgumentException.class, () -> task.setRatios(-1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> task.setRatios(1.0, Double.POSITIVE_INFINITY));
        assertEquals(2.0, task.getDayRatio());
        assertEquals(3.0, task.getNightRatio());
    }

    @Test
    void rejectsInvalidRatios() {
        assertThrows(IllegalArgumentException.class, () -> taskFor(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> taskFor(1.0, Double.NaN));
    }
}
