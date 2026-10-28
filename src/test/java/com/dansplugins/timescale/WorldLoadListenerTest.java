package com.dansplugins.timescale;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldLoadListenerTest {

    /** Records the daylight-cycle gamerule; there is no server in unit tests. */
    private static final class FakeWorldClock implements WorldClock {
        Boolean daylightCycle;

        @Override
        public UUID getId() {
            return UUID.randomUUID();
        }

        @Override
        public long getFullTime() {
            return 0;
        }

        @Override
        public void setFullTime(long time) {
        }

        @Override
        public void setDaylightCycle(boolean enabled) {
            daylightCycle = enabled;
        }

        @Override
        public boolean isNightSkipped() {
            return false;
        }
    }

    @Test
    void turnsOffTheDaylightCycleOnALoadedOverworld() {
        FakeWorldClock world = new FakeWorldClock();
        WorldLoadListener.disableDaylightCycle(World.Environment.NORMAL, () -> world);
        assertEquals(Boolean.FALSE, world.daylightCycle);
    }

    @Test
    void reappliesTheGameruleWhenAWorldIsReloadedWithItOn() {
        FakeWorldClock world = new FakeWorldClock();
        world.daylightCycle = true;
        WorldLoadListener.disableDaylightCycle(World.Environment.NORMAL, () -> world);
        assertEquals(Boolean.FALSE, world.daylightCycle);
    }

    @Test
    void leavesTheNetherAndTheEndAlone() {
        FakeWorldClock nether = new FakeWorldClock();
        FakeWorldClock end = new FakeWorldClock();
        WorldLoadListener.disableDaylightCycle(World.Environment.NETHER, () -> nether);
        WorldLoadListener.disableDaylightCycle(World.Environment.THE_END, () -> end);
        assertNull(nether.daylightCycle);
        assertNull(end.daylightCycle);
    }
}
