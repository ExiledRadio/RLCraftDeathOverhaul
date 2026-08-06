## `KEEP_CURSED_ITEMS` is now three settings instead of on/off

Curse of Vanishing and Curse of Possession destroy an item on death rather than letting it drop. On/off was not enough control, because `DROP_EVERYTHING_AT_MIN_HEALTH` overrode it — if you were at the minimum, cursed items were destroyed no matter what this was set to.

| Setting | Effect |
|---|---|
| `ALWAYS` | The item survives no matter what, in any slot, including a death that costs you everything. |
| `WITH_GEAR` | Default, and how it worked before. Kept with the gear in slots you keep, lost when that gear is lost. |
| `NEVER` | The curses work exactly as they would without this mod. |

**`ALWAYS` is worth considering**, because cursed items are the only thing in the mod you can lose permanently. Everything else you drop lands on the ground and waits for you. A cursed item is destroyed outright.

Your existing setting carries over — `true` becomes `WITH_GEAR`, `false` becomes `NEVER`.

Curse of Binding is unaffected by all three; it stops you removing armour, it does not destroy anything. Baubles follow `KEEP_BAUBLES`.

## Install

Download `RLCraftDeathOverhaul-1.3.0.jar` below and drop it in your `mods` folder. **Delete the older jar first** — two versions in the same folder will fail to load.

Existing configs and worlds carry over.
