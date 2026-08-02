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

## Keeping your items - you set this up, not the mod

**This mod never touches your inventory, and never edits another mod's config.** Install it and your death drops behave exactly as they already do - the heart cost simply layers on top. There is no keep-inventory option here, on purpose.

**Corpse Complex** already ships with RLCraft and does that job properly: per-slot control, durability costs, random drop chance, soulbinding, Baubles and toolbelt support. Shipping a worse copy of it would just mean two mods fighting over one inventory, which is how items go missing.

**Its Inventory Module is off by default, and that is the real reason death wipes you.** Open `config/corpsecomplex.cfg`, find the `inventory` block, and set:

```
B:"Enable Inventory Module"=true
B:"Keep Armor"=true
B:"Keep Hotbar"=true
B:"Keep Mainhand"=true
B:"Keep Offhand"=true
B:"Keep Main Inventory"=false
B:"Keep Baubles"=true
B:"Keep Toolbelt"=true
B:"Keep Wearable Backpack"=true
D:"Durability Loss on Kept Items"=0.1
B:"Limit Durability Loss"=true
I:"Drop Despawn Timer"=900
```

Restart the game afterwards. That gives you the shape this mod is built around:

- **You keep your kit** - armour, hotbar, both hands, baubles, toolbelt, backpack
- **You drop your haul** - the 27 main inventory slots, your loot and materials
- **Everything you kept takes 10% durability**, so a death stings immediately, and `Limit Durability Loss` means it can never destroy an item outright
- **Your drops sit there for 15 minutes** instead of 5, and RLCraft already enables Corpse Complex's Return Scroll, so running back is a real option

The hearts are the part that lasts.

### Turning it off again

Set any of those lines back to `false`, or set `Enable Inventory Module=false` to switch the whole lot off in one go and go back to vanilla drops.

**Want to lose nothing at all?** Turn on the vanilla `keepInventory` gamerule and leave Corpse Complex alone. This mod works fine that way - the hearts just become the only penalty. Gentler than intended, but a perfectly valid way to play it.

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
