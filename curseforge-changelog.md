Initial release.

Dying takes hearts off your maximum health instead of emptying your inventory. Lost hearts come back only through Scaling Health heart containers.

## The floor is the point

`MIN_HEARTS` defaults to **10** — deliberately the same as Scaling Health's starting health.

That means the hearts you start with can never be taken away, and only the hearts you added with heart containers are ever at risk. Dying while you're still learning the pack costs nothing, and every heart container becomes a decision instead of a free upgrade.

If you've changed Scaling Health's `Starting Health`, set `MIN_HEARTS` to match or the effect is lost.

## Included

- **`DEATHS_PER_PENALTY`** (default `1`) — how many deaths it takes to be charged. Set it to `3` for two free deaths before the third one bites. Scaling Health has no grace period of its own; this is the part it doesn't do.
- **`HEARTS_LOST_PER_PENALTY`** (default `1.0`) — hearts removed per penalty. Accepts halves. `0` tracks deaths without charging.
- **`MIN_HEARTS`** (default `10.0`) — the floor. See above.
- **`RESET_COUNTER_ON_PENALTY`** (default `true`) — whether the grace period repeats or is one-time-only.
- **`RESET_COUNTER_ON_SLEEP`** (default `false`) — a full night's sleep forgives pending deaths. Hearts already lost stay lost.
- **`EXEMPT_DIMENSIONS`** and **`EXEMPT_DAMAGE_TYPES`** — deaths that don't count at all. Damage types match Minecraft's internal name (`fall`, `lava`, `cactus`, …), not the chat death message.
- **`COUNT_CREATIVE_DEATHS`**, **`ANNOUNCE_PENALTY`**, **`ANNOUNCE_PROGRESS`**, **`BROADCAST_PENALTY_TO_SERVER`**.

Every setting is in whole hearts and read live — nothing here needs a restart.

## Commands

`/deathpenalty status` shows your max health, the floor, deaths banked and lifetime totals, and is available to everyone. `reset` and `sethearts` require op. Aliased to `/dp`.

`sethearts` is how you hand hearts back — `reset` only clears counters.

## If you already use Scaling Health's own death penalty

Leave `Health Lost On Death` however you like. While this mod is installed it takes over health-on-death completely and overrides Scaling Health's subtraction on respawn, so the two can never both charge you for one death. Setting it to `0` just saves confusion.

## Your inventory is untouched

That's deliberate, so you can pair this with whatever item rules you want. On RLCraft you already have **Corpse Complex** installed with its Inventory Module switched off — turning it on and keeping equipped gear while still dropping your main inventory gives the intended shape: you keep your kit, you drop your haul, and the hearts are the part that hurts.

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847+
- **[Scaling Health](https://www.curseforge.com/minecraft/mc-mods/scaling-health)** — a hard dependency. Forge will refuse to load this mod without it rather than misbehave.

Works on RLCraft and RLCraft Dregora, and on any other 1.12.2 pack with Scaling Health.

## Install

Download `RLCraftDeathPenalty-1.0.0.jar` below and drop it in your `mods` folder.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams, or with the author of Scaling Health. Licensed MIT.
