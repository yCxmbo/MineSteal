package me.ycxmbo.minesteal.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player's heart count changes for any reason.
 * Cancelling this event prevents the change and keeps the old value.
 */
public class HeartChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    public enum Cause {
        KILL, DEATH, WITHDRAW, DEPOSIT, GIVE, RECEIVE, ADMIN_SET, ADMIN_ADD,
        ADMIN_REMOVE, REGEN, CONSUME_ITEM, RESET
    }

    private final Player player;
    private final int oldHearts;
    private int newHearts;
    private final Cause cause;
    private final @Nullable Player source;

    public HeartChangeEvent(Player player, int oldHearts, int newHearts,
                            Cause cause, @Nullable Player source) {
        this.player = player;
        this.oldHearts = oldHearts;
        this.newHearts = newHearts;
        this.cause = cause;
        this.source = source;
    }

    public Player getPlayer()              { return player; }
    public int getOldHearts()              { return oldHearts; }
    public int getNewHearts()              { return newHearts; }
    public void setNewHearts(int hearts)   { this.newHearts = hearts; }
    public Cause getCause()                { return cause; }
    public @Nullable Player getSource()    { return source; }
    public int getDelta()                  { return newHearts - oldHearts; }

    @Override public boolean isCancelled()          { return cancelled; }
    @Override public void setCancelled(boolean c)   { this.cancelled = c; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
