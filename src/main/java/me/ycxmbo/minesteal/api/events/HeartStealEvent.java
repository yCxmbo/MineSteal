package me.ycxmbo.minesteal.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when hearts are stolen from one player by another during PvP.
 */
public class HeartStealEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player killer;
    private final Player victim;
    private int killerGain;
    private int victimLoss;

    public HeartStealEvent(Player killer, Player victim, int killerGain, int victimLoss) {
        this.killer = killer;
        this.victim = victim;
        this.killerGain = killerGain;
        this.victimLoss = victimLoss;
    }

    public Player getKiller()              { return killer; }
    public Player getVictim()              { return victim; }
    public int getKillerGain()             { return killerGain; }
    public void setKillerGain(int g)       { this.killerGain = g; }
    public int getVictimLoss()             { return victimLoss; }
    public void setVictimLoss(int l)       { this.victimLoss = l; }

    @Override public boolean isCancelled()          { return cancelled; }
    @Override public void setCancelled(boolean c)   { this.cancelled = c; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
