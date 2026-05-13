# Multiplayer Boss Fight

A lightweight Minecraft mod that dynamically scales boss attributes based on the number of nearby players — without any performance overhead.

## Features

*   Adjusts boss stats (health, damage, etc.) when players join or leave the fight.
*   Synchronization only happens when a boss enters or exits a player's client — no constant polling or lag.
*   Fully configurable per boss type (or boss tag) via a simple JSON file.

## How It Works

When a player approaches a boss, the mod recalculates the boss's attributes according to the configured multipliers. When a player leaves, the stats update again. This event‑driven approach means **no performance impact** during normal gameplay.

## Configuration

Example `config/multiplayer_boss_fight/config.json`:

```json
[
  {
    "entities": ["#forge:bosses", "minecraft:wither_boss"],
    "attribute_modifiers": [
      {
        "attribute": "minecraft:generic.max_health",
        "operation": "MULTIPLY_TOTAL",
        "value": 0.4,
        "max_limit": 4
      },
      {
        "attribute": "minecraft:generic.attack_damage",
        "operation": "MULTIPLY_TOTAL",
        "value": 0.1,
        "max_limit": 4
      }
    ]
  },
  {
    "entities": ["minecraft:ender_dragon"],
    "attribute_modifiers": [
      {
        "attribute": "minecraft:generic.max_health",
        "operation": "MULTIPLY_TOTAL",
        "value": 0.25,
        "max_limit": 4
      },
      {
        "attribute": "minecraft:generic.attack_damage",
        "operation": "MULTIPLY_TOTAL",
        "value": 0.1,
        "max_limit": 4
      }
    ]
  }
]
```
## Parameter Explanation

*   **`entities`**

    List of boss entity IDs. You can use tags (prefixed with `#`).

*   **`attribute_modifiers`**

    A list of attribute modifications, each containing:

    *   `attribute` – Attribute to modify (e.g. `generic.max_health`).
    *   `operation` – `ADDITION`, `MULTIPLY_BASE`, or `MULTIPLY_TOTAL`.
    *   `value` – Multiplier **per player** (e.g., `0.4` = +40% per player).
    *   `max_limit` – Upper limit of players that affect the boss. Additional players will not increase stats further.

## Compatibility

*   Works in multiplayer (dedicated server & LAN).
*   Optional in client installation (pure server‑side mod).
*   Safe to add/remove mid‑game.

***

**Enjoy balanced boss fights for any group size!**