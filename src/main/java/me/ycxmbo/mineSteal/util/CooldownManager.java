package me.ycxmbo.mineSteal.util;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final ConfigManager cfg;

    private final Map<UUID, Long> consume = new HashMap<>();
    private final Map<UUID, Long> withdraw = new HashMap<>();
    private final Map<UUID, Long> requestRevive = new HashMap<>();

    public CooldownManager(MineSteal plugin, ConfigManager cfg) {
        this.cfg = cfg;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private long left(Map<UUID, Long> map, UUID id, int seconds) {
        Long last = map.get(id);
        if (last == null) return 0;
        long passMs = now() - last;
        long needMs = seconds * 1000L;
        if (passMs >= needMs) return 0;
        return (needMs - passMs + 999) / 1000; // ceil seconds left
    }

    // Consume
    public long leftConsume(UUID id) { return left(consume, id, cfg.cdConsume()); }
    public void markConsume(UUID id) { if (cfg.cdConsume() > 0) consume.put(id, now()); }

    // Withdraw
    public long leftWithdraw(UUID id) { return left(withdraw, id, cfg.cdWithdraw()); }
    public void markWithdraw(UUID id) { if (cfg.cdWithdraw() > 0) withdraw.put(id, now()); }

    // Request revive
    public long leftRequestRevive(UUID id) { return left(requestRevive, id, cfg.cdRequestRevive()); }
    public void markRequestRevive(UUID id) { if (cfg.cdRequestRevive() > 0) requestRevive.put(id, now()); }
}
