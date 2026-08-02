# RLCraft Death Penalty

**Death should cost you something. It shouldn't cost you everything.**

You know both sides of this. Vanilla RLCraft drops your entire inventory in a lava lake you can't get back to, and you lose four hours. So you flip on `keepInventory` - and three days later death means nothing, you're throwing yourself off cliffs as a fast-travel system, and the game has quietly stopped being RLCraft.

This mod picks a third option. **Dying takes hearts off your maximum health.** Permanently, down to a floor you set. Your gear is somebody else's problem - this mod only handles the part that actually makes you careful.

---

## The floor is the whole idea

`MIN_HEARTS` defaults to **10** - exactly what Scaling Health starts you with.

That means **the hearts you were born with can never be taken away, and only the ones you added with heart containers are ever at stake.**

Two things fall out of that, and both are the point:

- **New players aren't punished for learning.** The deaths you're going to have in your first few hours - the dragon you misjudged, the first night, the cave with the bad idea in it - cost you nothing at all. You are never dug into a hole you can't climb out of.
- **Every heart container becomes a decision.** Using one puts it permanently at risk. The question stops being *"can I craft another heart?"* and becomes *"do I trust myself with what's in front of me enough to bank this now?"*

The penalty only grows teeth once you've chosen to give it teeth. Nobody gets punished for being new; everybody gets to gamble once they're not.

![Dying at the floor costs nothing](https://media.forgecdn.net/attachments/1836/873/deathmessage-png.png)

*Dying while you're still at the floor tells you so, and costs you nothing.*

---

## Deaths before it bites

Scaling Health already has a flat "lose a heart every death" setting. If that's all you want, use it and skip this mod - genuinely, you don't need this.

What it has no concept of is a **grace period**, and that's the part this mod adds. Set `DEATHS_PER_PENALTY` to `3` and you get two free deaths; the third one takes the hearts. That turns death from a per-incident tax into a budget - the difference between *"I'm afraid to leave base"* and *"I have two mistakes left, spend them well."*

You get told where you stand. Respawn without being charged and the game tells you how many you have left. Respawn on the one that counts and it tells you what it cost.

Optionally, sleeping through a full night wipes your pending deaths (`RESET_COUNTER_ON_SLEEP`). Hearts already lost stay lost - a night's rest forgives the record, not the damage.

---

## Configuration

`config/rlcraftdeathpenalty.cfg`, or in-game via **Mods → RLCraft Death Penalty → Config**. Everything is in **whole hearts**, and every setting is read live - no restart, ever.

![In-game config screen](https://media.forgecdn.net/attachments/1836/874/config-png.png)

| Setting | Default | What it does |
|---|---|---|
| `HEARTS_LOST_PER_PENALTY` | `1.0` | Hearts removed per penalty. `0.5` for a half. `0` tracks deaths without charging. |
| `DEATHS_PER_PENALTY` | `1` | Deaths needed to trigger one penalty. |
| `MIN_HEARTS` | `10.0` | The floor. See above - this is the setting that defines the mod. |
| `RESET_COUNTER_ON_PENALTY` | `true` | Grace period repeats. `false` makes it one-time-only. |
| `RESET_COUNTER_ON_SLEEP` | `false` | A full night's sleep forgives pending deaths. |
| `COUNT_CREATIVE_DEATHS` | `false` | Whether creative and spectator deaths count. |
| `ANNOUNCE_PENALTY` | `true` | Tell the player when they're charged. |
| `ANNOUNCE_PROGRESS` | `true` | Tell them how many deaths remain when they aren't. |
| `BROADCAST_PENALTY_TO_SERVER` | `false` | Announce penalties to the whole server. |
| `EXEMPT_DIMENSIONS` | *(empty)* | Dimension IDs where dying is free. |
| `EXEMPT_DAMAGE_TYPES` | *(empty)* | Damage types that don't count - `fall`, `lava`, `cactus`, … |
| `KEEP_INVENTORY` | `true` | Master switch for keeping items. |
| `KEEP_ARMOR` / `KEEP_HOTBAR` / `KEEP_MAINHAND` / `KEEP_OFFHAND` | `true` | Your equipped kit. |
| `KEEP_BAUBLES` | `true` | Baubles, and the Tool Belt with them. |
| `KEEP_WEARABLE_BACKPACK` | `true` | Backpack and contents. |
| `KEEP_MAIN_INVENTORY` | **`false`** | Your 27 loot slots - the thing you're meant to lose. |
| `KEEP_XP` | `false` | Keep experience instead of dropping it. |
| `DURABILITY_LOSS_ON_KEPT_ITEMS` | `0.10` | Durability charged on what you keep. |
| `NO_DROP_DESPAWN` | `true` | What you did drop never despawns. |

`EXEMPT_DAMAGE_TYPES` matches Minecraft's internal damage name, **not** the death message in chat. If you don't know a modded one, set your log to debug - every death this mod sees is logged with its damage type.

---

## Commands

| Command | Who | |
|---|---|---|
| `/deathpenalty status` | anyone | Your max health, the floor, deaths banked, lifetime totals. |
| `/deathpenalty status <player>` | op | The same, for someone else. |
| `/deathpenalty reset <player>` | op | Clears counters. Does not refund hearts. |
| `/deathpenalty sethearts <player> <hearts>` | op | Sets max health directly - the way to hand hearts back. |

Aliased to `/dp`.

---

## You keep your gear. Install and play.

**No setup. No second mod to configure.** Drop it in and it already works the way it's meant to.

That matters, because RLCraft ships with every keep-item option switched off. A mod whose whole premise is *"you pay in hearts instead of items"* would be worse than useless out of the box - you'd lose your inventory **and** your hearts. So keeping your gear is on by default.

**On death you keep:**

- **Armour, hotbar, mainhand and offhand** - your entire equipped kit
- **Baubles** - rings, amulets, belts. This covers your **Tool Belt** too, since it lives in a Baubles slot
- **Your Wearable Backpack**, everything inside it included

**You still drop your main inventory** - the 27 non-hotbar slots. That's the one default deliberately left off. Your loot and your materials are what makes a death hurt *right now*; the hearts are what makes it hurt *later*.

Two settings keep that from being a free ride:

- **10% durability** comes off every damageable item you kept, so walking away with your gear still costs you repair materials. It can never break anything - an item that would be destroyed stops at one point of durability.
- **Your death pile never despawns.** Vanilla deletes dropped items after five minutes, which in a pack this size is rarely enough time to fight your way back. Now it waits. RLCraft already enables Corpse Complex's Return Scroll, so going to get it is a real option.

### Prefer to do it yourself?

Set `KEEP_INVENTORY=false` and the mod won't touch your inventory at all - drops go back to whatever your pack already does, and hearts stay the only thing this mod changes. Or leave it on and flip the individual `KEEP_*` options to taste.

**Already running Corpse Complex or a gravestone mod?** Turn off one of the two. Two mods saving the same inventory can duplicate or lose items. Out of the box there's no clash - Corpse Complex ships with RLCraft but has its Inventory Module disabled - and this mod warns in the log if it spots one.

**The vanilla `keepInventory` gamerule always wins.** With it on, this mod won't touch your inventory no matter what the config says.

---

## Requirements

- **Minecraft 1.12.2**, Forge **14.23.5.2847** or newer
- **[Scaling Health](https://www.curseforge.com/minecraft/mc-mods/scaling-health)** - a hard dependency, not optional

Scaling Health is what owns player max health, so this mod is built on top of it rather than fighting it. Forge will refuse to load this mod without it rather than misbehave.

Built for **RLCraft** and **RLCraft Dregora**, but there's nothing pack-specific in it - it works on any 1.12.2 pack with Scaling Health.

**A note if you already use Scaling Health's own `Health Lost On Death`:** you can leave it however you like. While this mod is installed it takes over health-on-death completely and overrides that setting on respawn, so the two can never both charge you for the same death. Setting it to `0` just saves confusion.

---

## Feedback

Feedback is welcome, especially on whether the defaults feel right - the balance between the floor, the cost and the grace period is the whole design, and it's the thing most worth arguing about.

- **Discord:** **[discord.gg/kxQvMDJBTN](https://discord.gg/kxQvMDJBTN)** - best place to reach me, or find me directly as `exiledradio`
- **Bug reports:** the [GitHub issue tracker](https://github.com/ExiledRadio/RLCraftDeathPenalty/issues)
- Or leave a comment on this page

---

## Source & license

Source: **https://github.com/ExiledRadio/RLCraftDeathPenalty**

Licensed **MIT** - fork it, modify it, bundle it in your modpack. Just keep the copyright notice.

*Unofficial addon. Not affiliated with or endorsed by the RLCraft or RLCraft Dregora teams, or by the author of Scaling Health. This mod bundles none of their code or assets.*
