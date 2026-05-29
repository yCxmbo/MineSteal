package me.ycxmbo.minesteal.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a deathbanned player is revived.
 */
public class PlayerReviveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player revived;
    private final @Nullable Player reviver;
    private final boolean adminRevive;

    public PlayerReviveEvent(Player revived, @Nullable Player reviver, boolean adminRevive) {
        this.revived = revived;
        this.reviver = reviver;
        this.adminRevive = adminRevive;
    }

    public Player getRevived()             { return revived; }
    public @Nullable Player getReviver()   { return reviver; }
    public boolean isAdminRevive()         { return adminRevive; }

    @Override public boolean isCancelled()          { return cancelled; }
    @Override public void setCancelled(boolean c)   { this.cancelled = c; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
