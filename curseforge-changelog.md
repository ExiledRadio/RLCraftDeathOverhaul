Initial release.

Dying costs you hearts off your maximum health instead of your whole inventory. Works out of the box with no setup.

## On death

- **Kept:** armour, hotbar, mainhand, offhand, Baubles (which covers the Tool Belt), and your Wearable Backpack with its contents.
- **Dropped:** your main inventory, the 27 non-hotbar slots.
- **Dropped items never despawn.** Vanilla deletes them after 5 minutes; now the pile waits indefinitely. RLCraft's Return Scroll gets you back to it.
- **10% durability** comes off every damageable item you kept. It never breaks anything — an item that would be destroyed stops at 1 durability.
- **One heart** comes off your max health, down to a floor. Hearts return only through Scaling Health heart containers.

## Heart floor

`MIN_HEARTS` defaults to `10`, matching Scaling Health's starting health, so the hearts you start with can never be taken and only hearts added with heart containers are at risk. Dying early costs nothing; spending a heart container becomes a decision.

## Grace period

`DEATHS_PER_PENALTY` (default `1`) sets how many deaths it takes to be charged. Set it to `3` for two free deaths before the third bites. Scaling Health already has flat per-death health loss and a health floor; the grace period is the part it lacks.

## Config

20 settings in `config/rlcraftdeathoverhaul.cfg` or **Mods → RLCraft Death Overhaul → Config**, all read live with no restart. Covers heart cost, the floor, exempt dimensions and damage types, per-slot item keeping, durability cost, drop despawning, and chat messages.

`/deathoverhaul status` shows where you stand. `reset` and `sethearts` require op; `sethearts` is how you give hearts back. Aliased to `/dov` and `/dp`.

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847+
- **Scaling Health** — required.

Baubles and Wearable Backpacks are optional and their settings are ignored when absent.

## Compatibility

If another mod already keeps items on death — Corpse Complex, a gravestone mod — turn one of the two off, or set `KEEP_INVENTORY=false`. Two mods saving one inventory can duplicate or lose items, and the log warns if one is detected. Out of the box there is no clash. The vanilla `keepInventory` gamerule takes precedence over this mod entirely.

## Install

Download `RLCraftDeathOverhaul-1.0.0.jar` below and drop it in your `mods` folder.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams, or with the author of Scaling Health. Licensed MIT.
