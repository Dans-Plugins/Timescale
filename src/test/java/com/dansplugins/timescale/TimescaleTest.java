package com.dansplugins.timescale;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimescaleTest {

    @Test
    void mainClassIsAJavaPlugin() {
        assertTrue(JavaPlugin.class.isAssignableFrom(Timescale.class));
    }

    @Test
    void pluginYmlPointsAtMainClass() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be on the classpath");
            String yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yml.contains("main: " + Timescale.class.getName()), yml);
            assertTrue(!yml.contains("${version}"), "version placeholder must be expanded");
        }
    }

    @Test
    void pluginYmlRegistersTheCommandAndPermissions() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be on the classpath");
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            assertNotNull(yml.getString("commands.timescale.usage"), "timescale needs a usage string");
            assertEquals("timescale.view", yml.getString("commands.timescale.permission"));
            assertEquals("true", yml.getString("permissions.timescale.view.default"));
            assertEquals("op", yml.getString("permissions." + TimescaleCommand.SET_PERMISSION + ".default"));
        }
    }
}
