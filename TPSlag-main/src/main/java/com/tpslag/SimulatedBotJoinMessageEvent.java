package com.tpslag;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SimulatedBotJoinMessageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String botName;
    private String joinMessage;
    private boolean cancelled;

    public SimulatedBotJoinMessageEvent(String botName, String joinMessage) {
        this.botName = botName;
        this.joinMessage = joinMessage;
    }

    public String getBotName() {
        return botName;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
