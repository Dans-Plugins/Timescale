package com.dansplugins.timescale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimescaleCommandTest {

    /** In-memory ratios; records whether they were changed. */
    private static final class FakeRatios implements TimescaleCommand.Ratios {
        double dayRatio = 3.0;
        double nightRatio = 1.5;
        boolean set;

        @Override
        public double getDayRatio() {
            return dayRatio;
        }

        @Override
        public double getNightRatio() {
            return nightRatio;
        }

        @Override
        public void setRatios(double dayRatio, double nightRatio) {
            this.dayRatio = dayRatio;
            this.nightRatio = nightRatio;
            set = true;
        }
    }

    private static String run(FakeRatios ratios, boolean canSet, String... args) {
        return TimescaleCommand.execute(args, canSet, ratios);
    }

    @Test
    void noArgumentsShowsBothRatios() {
        FakeRatios ratios = new FakeRatios();
        String reply = run(ratios, false);
        assertTrue(reply.contains("Day ratio 3.0"), reply);
        assertTrue(reply.contains("night ratio 1.5"), reply);
        assertFalse(ratios.set);
    }

    @Test
    void setChangesBothRatios() {
        FakeRatios ratios = new FakeRatios();
        String reply = run(ratios, true, "set", "4", "0.5");
        assertTrue(ratios.set);
        assertEquals(4.0, ratios.dayRatio);
        assertEquals(0.5, ratios.nightRatio);
        assertTrue(reply.contains("4.0") && reply.contains("0.5"), reply);
    }

    @Test
    void setIsCaseInsensitive() {
        FakeRatios ratios = new FakeRatios();
        run(ratios, true, "SET", "2", "2");
        assertEquals(2.0, ratios.dayRatio);
    }

    @Test
    void setWithoutPermissionChangesNothing() {
        FakeRatios ratios = new FakeRatios();
        String reply = run(ratios, false, "set", "4", "1");
        assertFalse(ratios.set);
        assertTrue(reply.contains(TimescaleCommand.SET_PERMISSION), reply);
    }

    @Test
    void setWithWrongArgumentCountShowsUsage() {
        FakeRatios ratios = new FakeRatios();
        assertEquals(TimescaleCommand.SET_USAGE, run(ratios, true, "set", "4"));
        assertEquals(TimescaleCommand.SET_USAGE, run(ratios, true, "set", "4", "1", "2"));
        assertFalse(ratios.set);
    }

    @Test
    void unknownSubcommandShowsUsage() {
        FakeRatios ratios = new FakeRatios();
        assertEquals(TimescaleCommand.USAGE, run(ratios, true, "speed"));
        assertFalse(ratios.set);
    }

    @Test
    void nonNumericRatioIsRejectedNamingTheValue() {
        FakeRatios ratios = new FakeRatios();
        String reply = run(ratios, true, "set", "slow", "1");
        assertFalse(ratios.set);
        assertTrue(reply.startsWith("Day ratio"), reply);
        assertTrue(reply.contains("'slow'"), reply);
        assertTrue(reply.contains(TimescaleCommand.SET_USAGE), reply);
    }

    @Test
    void nonPositiveOrNonFiniteRatiosAreRejected() {
        FakeRatios ratios = new FakeRatios();
        for (String bad : new String[] {"0", "-1", "NaN", "Infinity"}) {
            String reply = run(ratios, true, "set", "1", bad);
            assertTrue(reply.startsWith("Night ratio"), reply);
            assertTrue(reply.contains("'" + bad + "'"), reply);
        }
        assertFalse(ratios.set);
        assertEquals(3.0, ratios.dayRatio);
    }
}
