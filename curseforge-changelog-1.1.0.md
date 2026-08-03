## Changed

### Death drops now despawn after 15 minutes instead of never

`NO_DROP_DESPAWN` is replaced by **`DROP_DESPAWN_MINUTES`**, which defaults to **15**.

Never despawning was the wrong default. Five minutes is too short to fight your way back in a pack this size, but forever means every death pile nobody collects stays loaded as entities, and on a busy server that adds up. Fifteen minutes is a real grace period without leaving litter in the world permanently.

| Value | Behaviour |
|---|---|
| `15` | Default. Fifteen minutes to get back to your death pile. |
| any number | That many minutes, up to a week. |
| `0` | Leave drops alone entirely — vanilla timing, or whatever another mod has already set. |
| `-1` | Never despawn, the old behaviour. Watch your entity count on a server. |

**If you had `NO_DROP_DESPAWN` set to false**, you are migrated to `0` automatically and the mod carries on leaving your drops alone. Everyone else picks up the new 15 minute default. To keep drops forever, set `-1`.

## Fixed

Settings the mod no longer uses are now removed from the config file rather than sitting there looking editable. Forge never prunes properties once a mod stops reading them, so `NO_DROP_DESPAWN` would otherwise have stayed behind after this rename doing nothing.

## Install

Download `RLCraftDeathOverhaul-1.1.0.jar` below and drop it in your `mods` folder. Delete the older jar first — two versions in the same folder will fail to load.

Existing configs and worlds carry over. Nothing else changed.
