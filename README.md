# WorldBorderExpander

A Spigot plugin for **Minecraft 1.21** that gives players a GUI shop to spend XP levels expanding the world border.

---

## Features

- **GUI Shop** — clean inventory UI with tier items, lore descriptions, and color-coded affordability
- **XP Level Cost** — spends player XP levels (not points), fully configurable per tier
- **Confirm Screen** — optional second-screen confirmation before spending XP (toggleable)
- **Smooth Expansion** — border grows with a 3-second animated transition
- **Max Size Guard** — configurable hard cap; purchases that exceed it are blocked
- **Server Broadcast** — announces expansions to all online players (customizable/disableable)
- **Live Reload** — `/bordershop reload` reloads config without restarting

---

## Installation

1. Build with Maven: `mvn clean package`
2. Copy `target/WorldBorderExpander-1.0.0.jar` into your server's `plugins/` folder
3. Start or reload the server — `plugins/WorldBorderExpander/config.yml` is generated
4. Set your world's border to a small starting size (the plugin only expands, never sets)
5. Run `/bordershop` in-game

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/bordershop` | Open the border shop GUI | `worldborderexpander.use` |
| `/bordershop reload` | Reload config.yml | `worldborderexpander.reload` |

**Aliases:** `/bs`, `/borderbuy`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `worldborderexpander.use` | `true` (all players) | Open and use the shop |
| `worldborderexpander.reload` | `op` | Reload the config |

---

## Configuration (`config.yml`)

```yaml
world: world                  # Which world's border to expand
max-border-size: 10000        # Hard cap in blocks (diameter)
broadcast-message: "..."      # Set to "" to disable broadcasts

gui:
  title: "§8⚙ §6World Border Shop"
  rows: 3                     # 1–6 rows
  confirm-purchase: true      # Show confirmation screen

tiers:
  small:
    name: "Small Expansion"
    xp-cost: 5
    expansion-blocks: 50
    icon: OAK_SAPLING         # Any valid Bukkit Material name
    description: "A modest push outward."
  # Add as many tiers as you like...
```

### Broadcast Variables

| Variable | Replaced with |
|---|---|
| `{player}` | Player's name |
| `{size}` | New border size |
| `{expansion}` | Blocks added this purchase |
| `{tier}` | Tier display name |

---

## Compatibility

- **Spigot / Paper 1.21** (api-version 1.21)
- Java 21+
- No external dependencies

---

## Notes

- The plugin does **not** set the initial border size — configure that separately with `/worldborder set <size>`.
- XP **levels** are deducted (not raw XP points), so a purchase of 5 costs the player 5 whole levels.
- The border expansion uses Bukkit's built-in `WorldBorder#setSize(double, long)` for the smooth animation.
