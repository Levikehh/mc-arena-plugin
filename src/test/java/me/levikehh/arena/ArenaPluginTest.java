package me.levikehh.arena;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import hu.nomindz.devkit.utils.Result;

public class ArenaPluginTest {
    static ServerMock server;
    static ArenaPlugin plugin;

    @BeforeAll
    public static void beforeAll() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ArenaPlugin.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void initialize() {
        // Test if server mocking works
        assertNotNull(server, "ServerMock can not be null after setUp");
        // Test if plugin loading works
        assertNotNull(plugin, "Plugin can not be null after setUp");
        assertTrue(plugin.isEnabled(), "Plugin should be enabled");
    }

    @Test
    public void test_listarenas() {
        SpyPlayerMock player = new SpyPlayerMock(server, "Tester");
        player.addAttachment(plugin, "arena.admin", true);

        boolean ok = player.performCommand("arena list");
        assertTrue(ok, "Could not execute /arena list command");

        assertTrue(player.nextMessage().contains("No arenas have been created yet!"),
                "Should trigger warning when no arenas been created yet");
    }

    @Test
    public void test_listarenas_with_data() {
        // Initialize custom PlayerMock to be able to catch bungee messages
        SpyPlayerMock player = new SpyPlayerMock(server, "Tester");
        player.addAttachment(plugin, "arena.admin", true);

        // Get server world
        World world = server.getWorld("world");

        // Create an arena
        Location arenaSpawn1 = new Location(world, 0, 0, 0);
        Location arenaSpawn2 = new Location(world, 10, 0, 10);
        Result<Void> createResult = plugin.getArenaManager().createArena("test", arenaSpawn1, arenaSpawn2);
        assertTrue(createResult.isSuccess(), "Could not create test arena");

        // Run list command
        boolean ok = player.performCommand("arena list");
        assertTrue(ok, "Could not execute /arena list command");

        // Check if arena count is correct
        String messageHeader = player.nextMessage();
        assertTrue(messageHeader.contains("1"),
                "It should add the arena count to the header");

        // Check if arena availability is correct
        String messageArenaDetails = player.nextBugeeMessage();
        assertTrue(messageArenaDetails.contains("[AVAILABLE]"), "The newly created arena should be available");
    }
}
