package me.ycxmbo.minesteal.database;

import java.util.UUID;

/**
 * Immutable snapshot of a player's persisted data.
 * Used by the database layer and managers.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private int hearts;
    private int kills;
    private int deaths;
    private int heartsStolen;
    private int currentStreak;
    private int bestStreak;
    private boolean deathbanned;
    private long firstJoin;
    private long lastSeen;

    public PlayerData(UUID uuid, String name, int hearts) {
        this.uuid = uuid;
        this.name = name;
        this.hearts = hearts;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }

    // Full constructor used by DB layer
    public PlayerData(UUID uuid, String name, int hearts, int kills, int deaths,
                      int heartsStolen, int currentStreak, int bestStreak,
                      boolean deathbanned, long firstJoin, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.hearts = hearts;
        this.kills = kills;
        this.deaths = deaths;
        this.heartsStolen = heartsStolen;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.deathbanned = deathbanned;
        this.firstJoin = firstJoin;
        this.lastSeen = lastSeen;
    }

    // ---- Getters / Setters ----

    public UUID getUuid()             { return uuid; }
    public String getName()           { return name; }
    public void setName(String n)     { this.name = n; }
    public int getHearts()            { return hearts; }
    public void setHearts(int h)      { this.hearts = h; }
    public int getKills()             { return kills; }
    public void setKills(int k)       { this.kills = k; }
    public int getDeaths()            { return deaths; }
    public void setDeaths(int d)      { this.deaths = d; }
    public int getHeartsStolen()      { return heartsStolen; }
    public void setHeartsStolen(int h){ this.heartsStolen = h; }
    public int getCurrentStreak()     { return currentStreak; }
    public void setCurrentStreak(int s){ this.currentStreak = s; }
    public int getBestStreak()        { return bestStreak; }
    public void setBestStreak(int s)  { this.bestStreak = s; }
    public boolean isDeathbanned()    { return deathbanned; }
    public void setDeathbanned(boolean b){ this.deathbanned = b; }
    public long getFirstJoin()        { return firstJoin; }
    public void setFirstJoin(long t)  { this.firstJoin = t; }
    public long getLastSeen()         { return lastSeen; }
    public void setLastSeen(long t)   { this.lastSeen = t; }

    public double getKdRatio() {
        return deaths == 0 ? (double) kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
