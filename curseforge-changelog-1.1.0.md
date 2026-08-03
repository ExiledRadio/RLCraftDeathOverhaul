## Death drops now despawn after 15 minutes instead of never

`NO_DROP_DESPAWN` is replaced by **`DROP_DESPAWN_MINUTES`**, default **15**.

Never despawning was the wrong default. Five minutes is too short to fight your way back in a pack this size, but forever means every death pile nobody collects stays loaded, which adds up on a server.

| Value | Behaviour |
|---|---|
| `15` | Default. Fifteen minutes to get back to your pile. |
| any number | That many minutes, up to a week. |
| `0` | Leave drops alone — vanilla timing, or whatever another mod set. |
| `-1` | Never despawn, the old behaviour. |

**If you had `NO_DROP_DESPAWN` off**, you are moved to `0` automatically and drops are still left alone. Everyone else gets the new 15 minute default.

## New: pay in items when you run out of hearts

**`DROP_EVERYTHING_AT_MIN_HEALTH`**, off by default.

Once you are at the `MIN_HEARTS` minimum, a death currently costs nothing at all — there is no health left to take and your gear is kept anyway. Turn this on and the trade runs both ways: hearts while you have them, your whole inventory once you don't. Heart containers become the thing that buys your gear protection back.

**Check `MIN_HEARTS` first.** It defaults to 10, the same as Scaling Health's starting health, so a new player is standing on the minimum from their first spawn and would drop everything on every death until their first heart container. Set `MIN_HEARTS` below starting health if you want a buffer to spend first.

A death that costs no hearts costs no items either, so exempt deaths and the free ones inside a `DEATHS_PER_PENALTY` grace period leave your inventory alone.

## Also

Settings the mod no longer uses are now cleared out of your config file instead of sitting there looking editable.

## Install

Download `RLCraftDeathOverhaul-1.1.0.jar` below and drop it in your `mods` folder. **Delete the older jar first** — two versions in the same folder will fail to load.

Existing configs and worlds carry over.
