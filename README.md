# MineSteal — Lifesteal SMP Plugin (Minecraft 1.21+)

MineSteal is a configurable Lifesteal SMP plugin for Paper/Spigot 1.21+. It provides heart-stealing on kill, custom heart items and shards, a leaderboard (chat + holograms), and optional revive tokens with LiteBans integration.

## Features
- Lifesteal system with min/max hearts
- Heart item and shard item with crafting recipes
- Withdraw hearts into items (or shards)
- PvE shard/token drops (configurable per mob)
- Leaderboard with chat hover/click and DecentHolograms support
- Optional permanent deathban or spectator mode at min hearts
- Revive Token flow with configurable unban command
- Async I/O and single-file player storage

## Commands
- `/hearts` — Check hearts, open GUI, withdraw, admin add/remove/set, top, holo, reload
- `/revive <player>` — Revive a player (uses token when required)
- `/msgive <heart|shard|token> <player> [amount]` — Give items (for crates/admin)

See `src/main/resources/plugin.yml` for full command usage and permissions.

## Configuration
The default `config.yml` is documented and ready to use. Key areas:
- `settings.*`: base hearts and rules
- `heart_item` and `heart_shard`: names/lore and crafting
- `withdraw.*`: withdraw behavior
- `leaderboard.*` and `holograms.*`: chat and hologram settings
- `revive_token.*`: enable/permission/material/unban command
- `pve_drops.*`: per-entity drop tables

## Building
Requires Java 21 and Maven. Build with:

```
mvn clean package
```

The shaded jar will be in `target/`.
