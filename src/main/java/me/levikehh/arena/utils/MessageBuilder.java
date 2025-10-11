package me.levikehh.arena.utils;

import org.bukkit.Location;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class MessageBuilder {
    public enum LocationFormat {
        SHORT, // 10, 10, 10
        DEFAULT, // x: 10, y: 10, z: 10
    }

    private TextComponent message;

    public MessageBuilder(String message) {
        this.message = new TextComponent(message);
    }

    public MessageBuilder() {
        this("");
    }

    public MessageBuilder addSuccess(String message) {
        TextComponent messageComponent = new TextComponent(message);
        messageComponent.setColor(ChatColor.GREEN);
        
        this.message.addExtra(messageComponent);
        return this;
    }

    public MessageBuilder addError(String message) {
        TextComponent messageComponent = new TextComponent(message);
        messageComponent.setColor(ChatColor.RED);

        this.message.addExtra(messageComponent);
        return this;
    }

    public MessageBuilder addVariable(String message) {
        TextComponent messageComponent = new TextComponent(message);
        messageComponent.setColor(ChatColor.GOLD);

        this.message.addExtra(messageComponent);
        return this;
    }

    public MessageBuilder addClickable(String message, ClickEvent action, HoverEvent hover) {
        TextComponent messageComponent = new TextComponent("[" + message + "]");
        
        messageComponent.setColor(ChatColor.AQUA);
        messageComponent.setClickEvent(action);
        messageComponent.setHoverEvent(hover);
        
        this.message.addExtra(messageComponent);
        
        return this;
    }

    public MessageBuilder addLocation(LocationFormat format, Location location) {
        TextComponent messageComponent = new TextComponent("");
        messageComponent.setColor(ChatColor.GREEN);

        switch (format) {
            case SHORT: {
                TextComponent xText = new TextComponent(String.format("%.2f", location.getX()));
                xText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(xText);
                messageComponent.addExtra(", ");
                
                TextComponent yText = new TextComponent(String.format("%.2f", location.getY()));
                yText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(yText);
                messageComponent.addExtra(", ");

                TextComponent zText = new TextComponent(String.format("%.2f", location.getZ()));
                zText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(zText);
                break;
            }
            case DEFAULT:
            default: {
                messageComponent.addExtra("x: ");

                TextComponent xText = new TextComponent(String.format("%.2f", location.getX()));
                xText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(xText);
                messageComponent.addExtra(", y: ");
                
                TextComponent yText = new TextComponent(String.format("%.2f", location.getY()));
                yText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(yText);
                messageComponent.addExtra(", z: ");

                TextComponent zText = new TextComponent(String.format("%.2f", location.getZ()));
                zText.setColor(ChatColor.GOLD);

                messageComponent.addExtra(zText);
                break;
            }
        }

        this.message.addExtra(messageComponent);
        return this;
    }

    public TextComponent build() {
        return this.message;
    }

    public static TextComponent internalError() {
        TextComponent errorMessage = new TextComponent("An unexpected error occured, please try again later.");
        errorMessage.setColor(ChatColor.GRAY);

        return errorMessage;
    }
}
