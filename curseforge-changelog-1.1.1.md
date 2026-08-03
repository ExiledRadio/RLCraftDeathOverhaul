## Fixed: backpack contents scattering on death

`KEEP_WEARABLE_BACKPACK` kept the backpack itself but not what was inside it. The items dropped on the ground as if the setting were off.

RLCraft wears the backpack in a Baubles slot, and Wearable Backpacks stores the contents separately from the item. This mod was clearing the Baubles slot before saving those contents, and Wearable Backpacks treats a backpack that vanished from its slot as one that has been removed improperly — it emptied the contents out before they could be saved.

The contents are now saved first. Nothing to change in your config; if `KEEP_WEARABLE_BACKPACK` was already on, it now does what it said.

Thanks to the player who reported it.

## Install

Download `RLCraftDeathOverhaul-1.1.1.jar` below and drop it in your `mods` folder. **Delete the 1.1.0 jar first** — two versions in the same folder will fail to load.

Existing configs and worlds carry over.
