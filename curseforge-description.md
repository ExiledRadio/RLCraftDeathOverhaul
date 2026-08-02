# RLCraft Death Overhaul

Changes what dying costs you. Instead of losing your whole inventory, you keep your gear and pay in hearts off your maximum health.

Works out of the box. No config editing, no second mod to set up.

---

## What it does

**Keeps your equipped gear.** Armour, hotbar, mainhand, offhand, Baubles, and your Wearable Backpack with its contents. Your Tool Belt too, since it sits in a Baubles slot.

**Drops your main inventory.** The 27 non-hotbar slots. Your loot and materials are still at risk.

**Your dropped items never despawn.** Vanilla deletes them after 5 minutes. In a pack this size that is rarely enough time to fight your way back, and it is one of the most common ways to lose a run's worth of loot. Now the pile waits for you indefinitely. RLCraft already gives you Corpse Complex's Return Scroll to get there.

**Charges 10% durability** on every damageable item you kept, so surviving with your gear still costs repair materials. It never breaks anything - an item that would be destroyed stops at 1 durability.

**Takes a heart off your max health.** Permanently, down to a floor. Hearts come back only through Scaling Health heart containers.

![Dying at the floor costs nothing](https://media.forgecdn.net/attachments/1836/873/deathmessage-png.png)

---

## Why not just use the keepInventory gamerule?

Because it is all or nothing. `keepInventory` keeps *everything* - gear, loot, XP - so death stops costing anything at all, and people end up using it as fast travel. That is usually the complaint that leads them to turn it back off and go back to losing entire runs.

This mod is the middle setting the gamerule cannot express:

| | keepInventory | This mod |
|---|---|---|
| Equipped gear | Kept | Kept |
| Main inventory | Kept | **Dropped** |
| Items on the ground | None | **Never despawn** |
| Durability cost | None | 10% on kept items |
| Max health | Unchanged | **One heart, down to a floor** |

You keep enough to survive the walk back, and you lose enough to make the walk worth taking.

If you genuinely do want the gamerule, turn it on — this mod detects it and leaves your inventory alone rather than fighting it. The heart cost still applies.

---

## The heart floor

`MIN_HEARTS` defaults to **10**, the same as Scaling Health's starting health.

So the hearts you start with can never be taken, and only hearts you added with heart containers are at risk. Two consequences:

- Dying early costs nothing, so new players are not dug into a hole.
- Every heart container becomes a decision, because using one puts it permanently at risk.

If you change Scaling Health's `Starting Health`, change `MIN_HEARTS` to match.

---

## Grace period

`DEATHS_PER_PENALTY` sets how many deaths it takes to be charged. Default `1`, so every death costs. Set it to `3` and you get two free deaths before the third takes a heart.

Scaling Health already has flat per-death health loss and a minimum-health floor. The grace period is the part it does not have.

---

## Config

`config/rlcraftdeathoverhaul.cfg`, or in-game via **Mods → RLCraft Death Overhaul → Config**. Health values are in whole hearts, and every setting is read live - no restart needed.

![In-game config screen](https://media.forgecdn.net/attachments/1836/874/config-png.png)

| Setting | Default | Effect |
|---|---|---|
| `HEARTS_LOST_PER_PENALTY` | `1.0` | Hearts removed per penalty. Accepts halves. `0` disables health loss. |
| `DEATHS_PER_PENALTY` | `1` | Deaths needed to trigger a penalty. |
| `MIN_HEARTS` | `10.0` | Health floor. |
| `RESET_COUNTER_ON_PENALTY` | `true` | Grace period repeats rather than being one-time. |
| `RESET_COUNTER_ON_SLEEP` | `false` | Sleeping clears pending deaths. Does not refund hearts. |
| `COUNT_CREATIVE_DEATHS` | `false` | Whether creative and spectator deaths count. |
| `ANNOUNCE_PENALTY` | `true` | Chat message when charged. |
| `ANNOUNCE_PROGRESS` | `true` | Chat message showing deaths remaining. |
| `BROADCAST_PENALTY_TO_SERVER` | `false` | Announce penalties to everyone. |
| `EXEMPT_DIMENSIONS` | *(empty)* | Dimension IDs where dying is free. |
| `EXEMPT_DAMAGE_TYPES` | *(empty)* | Damage types that do not count. |
| `ENABLE_ITEM_KEEPING` | `true` | Master switch for keeping items. |
| `KEEP_ARMOR` / `KEEP_HOTBAR` / `KEEP_MAINHAND` / `KEEP_OFFHAND` | `true` | Equipped slots. |
| `KEEP_BAUBLES` | `true` | Baubles, and the Tool Belt with them. |
| `KEEP_WEARABLE_BACKPACK` | `true` | Backpack and contents. |
| `KEEP_MAIN_INVENTORY` | `false` | The 27 loot slots. |
| `KEEP_XP` | `false` | Keep experience instead of dropping it. |
| `DURABILITY_LOSS_ON_KEPT_ITEMS` | `0.10` | Durability charged on kept items. |
| `NO_DROP_DESPAWN` | `true` | Dropped items never despawn. |

`EXEMPT_DAMAGE_TYPES` matches Minecraft's internal damage name (`fall`, `lava`, `cactus`), not the chat death message. Set your log level to debug and the mod prints the damage type of every death it sees.

---

## Commands

| Command | Permission | Effect |
|---|---|---|
| `/deathoverhaul status` | anyone | Max health, floor, deaths banked, lifetime totals. |
| `/deathoverhaul status <player>` | op | Same, for another player. |
| `/deathoverhaul reset <player>` | op | Clears counters. Does not refund hearts. |
| `/deathoverhaul sethearts <player> <hearts>` | op | Sets max health. This is how you give hearts back. |

Aliased to `/dov` and `/dp`.

---

## Compatibility

**If another mod already keeps items on death** - Corpse Complex, a gravestone mod - turn one of the two off. Two mods saving the same inventory can duplicate or lose items. This mod warns in the log if it detects one. Out of the box there is no clash: Corpse Complex ships with RLCraft but has its Inventory Module disabled.

To hand item handling back to your pack entirely, set `ENABLE_ITEM_KEEPING=false`.

**The vanilla `keepInventory` gamerule takes precedence.** With it on, this mod does not touch your inventory at all.

---

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847+
- **[Scaling Health](https://www.curseforge.com/minecraft/mc-mods/scaling-health)** - required. Forge will not load this mod without it.

Baubles and Wearable Backpacks are optional. Their settings are ignored when those mods are absent.

Built for RLCraft and RLCraft Dregora, but nothing in it is pack-specific - it works on any 1.12.2 pack with Scaling Health.

---

## Links

- **Discord:** [discord.gg/kxQvMDJBTN](https://discord.gg/kxQvMDJBTN)
- **Issues:** [GitHub issue tracker](https://github.com/ExiledRadio/RLCraftDeathOverhaul/issues)
- **Source:** [github.com/ExiledRadio/RLCraftDeathOverhaul](https://github.com/ExiledRadio/RLCraftDeathOverhaul) - MIT

*Unofficial addon. Not affiliated with the RLCraft or RLCraft Dregora teams, or with the author of Scaling Health.*
