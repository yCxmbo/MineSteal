# 🍏 MineSteal — Lifesteal SMP Plugin (Minecraft 1.21.x)

**MineSteal** is a fully-featured **Lifesteal SMP** plugin for Minecraft **Paper/Spigot 1.21+**,  
designed for performance, customization, and modern server aesthetics.

---

## ✨ Features
- ❤️ **Lifesteal System** — Steal hearts from players when you kill them.
- 🩸 **Custom Heart Limits** — Configure minimum and maximum hearts.
- ⚔ **PvP Balanced** — Fully compatible with combat plugins.
- 📊 **Leaderboard** — Interactive, clickable leaderboard in chat & holograms.
- 👑 **Hologram Support** — Integrated with [DecentHolograms](https://www.spigotmc.org/resources/decent-holograms.96927/).
- ⌛ **Auto-Refresh** — Holograms & leaderboard update every few seconds.
- ⚙ **Highly Configurable** — Edit all messages, hologram lines, and formats in `config.yml`.
- 🛡 **LiteBans Support** — Uses UUID-based bans for punishments.
- 🔄 **Async I/O** — Non-blocking save/load for player data.

---

## 📥 Installation
1. Download the latest release from the [Releases](../../releases) page.
2. Place the `.jar` file into your server’s `plugins/` folder.
3. Restart your server.
4. Configure settings in `plugins/MineSteal/config.yml`.
5. Enjoy your Lifesteal SMP server!

---

## 📝 Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/hearts` | View your hearts | `minesteal.hearts` |
| `/hearts top` | Show top players | `minesteal.top` |
| `/hearts holo` | Create hologram leaderboard | `minesteal.holo` |
| `/minesteal reload` | Reload plugin configuration | `minesteal.reload` |

---

## 🔧 Configuration
Example `config.yml` hologram section:
```yaml
hologram:
  title: "&a&lTop Hearts"
  format: "&e%position%. &a%player% &7- &c%hearts% ❤"
  refresh-interval: 30 # seconds
