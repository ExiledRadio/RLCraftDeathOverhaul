## New: curses no longer delete your gear

**`KEEP_CURSED_ITEMS`**, on by default.

Two curses destroy an item on death instead of letting it drop — **Curse of Vanishing** from vanilla, and **Curse of Possession** from So Many Enchantments, which RLCraft ships. Until now they still deleted your item even though everything else in that slot was being kept.

Now anything in a slot you are keeping survives both. The mod's whole point is that death costs you hearts rather than gear, and an item silently deleted was the loudest exception to that.

Set `KEEP_CURSED_ITEMS` to `false` if you would rather the curses still bite.

This only covers slots that are being kept anyway. A cursed item in your main inventory still drops while `KEEP_MAIN_INVENTORY` is off, and Curse of Possession still destroys it on the way down. Curse of Binding is unaffected — it stops you removing armour, it does not destroy anything.

## Also includes the 1.1.1 fix

`KEEP_WEARABLE_BACKPACK` was keeping the backpack but not what was inside it. Fixed — if you are coming straight from 1.1.0, you get that fix here too.

## Install

Download `RLCraftDeathOverhaul-1.2.0.jar` below and drop it in your `mods` folder. **Delete the older jar first** — two versions in the same folder will fail to load.

Existing configs and worlds carry over.
