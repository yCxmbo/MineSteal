package me.ycxmbo.minesteal.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired just before a player is deathbanned (reaches minimum hearts and dies).
 * Cancelling prevents the deathban.
 */
public class PlayerDeathBanEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player victim;
    private final @Nullable Player killer;
    private String banReason;

    public PlayerDeathBanEvent(Player victim, @Nullable Player killer, String banReason) {
        this.victim = victim;
        this.killer = killer;
        this.banReason = banReason;
    }

    public Player getVictim()              { return victim; }
    public @Nullable Player getKiller()    { return killer; }
    public String getBanReason()           { return banReason; }
    public void setBanReason(String r)     { this.banReason = r; }

    @Override public boolean isCancelled()          { return cancelled; }
    @Override public void setCancelled(boolean c)   { this.cancelled = c; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
