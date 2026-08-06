## `KEEP_CURSED_ITEMS` now covers Curse of Possession only, and defaults to `ALWAYS`

Curse of Vanishing is no longer touched by this setting. It is vanilla, players expect it to work, and it goes back to destroying its item as it always did.

The setting is now about **Curse of Possession** — RLCraft's curse that destroys an item on death.

| Setting | Effect |
|---|---|
| `ALWAYS` | **New default.** The item stays in your inventory no matter what, in any slot, including a death that costs you everything. |
| `WITH_GEAR` | Kept with the gear in slots you keep, destroyed when that gear is lost. |
| `NEVER` | The curse works exactly as it would without this mod. |

`ALWAYS` is the default because a possessed item is the only thing in the mod you can lose permanently. Everything else you drop is lying on the ground waiting for you.

**Worth knowing:** there is no "drops on the ground and survives" outcome for a possessed item. Destroying items once they are on the ground *is* the curse — so the item either stays in your inventory or it is gone. `ALWAYS` is the only setting that never loses one.

## Install

Download `RLCraftDeathOverhaul-1.3.1.jar` below and drop it in your `mods` folder. **Delete the older jar first** — two versions in the same folder will fail to load.

Existing configs and worlds carry over.
