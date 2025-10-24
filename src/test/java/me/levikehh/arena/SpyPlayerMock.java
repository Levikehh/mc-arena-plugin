package me.levikehh.arena;

import java.util.ArrayDeque;
import java.util.Deque;

import org.bukkit.entity.Player;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class SpyPlayerMock extends PlayerMock {
    private final Deque<String> messages = new ArrayDeque<>();

    public SpyPlayerMock(ServerMock server, String name) {
        super(server, name);
    }

    @Override
    public Player.Spigot spigot() {
        return new SpySpigotMock();
    }

    public String nextBugeeMessage() {
        return messages.pollFirst();
    }

    private class SpySpigotMock extends Player.Spigot {
        @Override
        public void sendMessage(BaseComponent component) {
            messages.addLast(TextComponent.toLegacyText(component));
        }
        @Override
        public void sendMessage(BaseComponent... components) {
            messages.addLast(TextComponent.toLegacyText(components));
        }
    }
}
