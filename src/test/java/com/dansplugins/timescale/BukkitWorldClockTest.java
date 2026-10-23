package com.dansplugins.timescale;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitWorldClockTest {

    @Test
    void onlyTheOverworldIsScaled() {
        assertTrue(BukkitWorldClock.isScaled(World.Environment.NORMAL));
        assertFalse(BukkitWorldClock.isScaled(World.Environment.NETHER));
        assertFalse(BukkitWorldClock.isScaled(World.Environment.THE_END));
    }
}
