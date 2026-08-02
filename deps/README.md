# deps/

Third-party mod jars this project compiles against. They are **compileOnly** — never
bundled into the built jar, and not ours to redistribute, so `deps/*.jar` is gitignored.

To build this project you need:

| File | Where to get it | Why |
|---|---|---|
| `ScalingHealth-1.12.2-1.3.42.jar` | [Scaling Health on CurseForge](https://www.curseforge.com/minecraft/mc-mods/scaling-health/files) — 1.12.2 build `1.3.42+147` | Hard dependency |
| `SilentLib-1.12.2-3.0.14.jar` | [Silent Lib on CurseForge](https://www.curseforge.com/minecraft/mc-mods/silent-lib/files) — build `3.0.14+168` | Scaling Health's `Config` extends its `ConfigBaseNew` |
| `Baubles-1.12-1.5.2-dev.jar` | [Baubles on CurseForge](https://www.curseforge.com/minecraft/mc-mods/baubles/files) — the **dev** jar | Soft dependency, `KEEP_BAUBLES` |
| `WearableBackpacks-1.12.2-3.2.6.jar` | [Wearable Backpacks on CurseForge](https://www.curseforge.com/minecraft/mc-mods/wearable-backpacks/files) | Soft dependency, `KEEP_WEARABLE_BACKPACK` |

The last two are **soft** — the mod builds and runs without either at runtime. Every call
into them is behind a `Loader.isModLoaded` check and lives in `compat/`, so their classes
are never loaded when the mod is absent. They are still required to *compile*.

The CurseForge download is named `ScalingHealth-1.12.2-1.3.42+147.jar`. **Rename it to
drop the `+147`** before dropping it in here — Gradle's `flatDir` resolver splits
artifact filenames on `-` and `+` to guess name/version, and the `+` makes it fail to
match the `compileOnly name: 'ScalingHealth-1.12.2-1.3.42'` coordinate in `build.gradle`.

Any 1.12.2 Scaling Health in the 1.3.x line should work — the three things this mod
touches (`SHPlayerDataHandler.get`, `PlayerData.getMaxHealth/setMaxHealth`, and
`Config.Player.Health`) have been stable across that series. Every one of those calls
is funnelled through `ScalingHealthBridge` so that if a future version does move them,
there is exactly one file to fix.

Scaling Health is declared `required-after:scalinghealth` in the `@Mod` annotation, so
Forge refuses to load this mod at all when it is absent — the direct compile-time
references can never resolve against a missing class at runtime.
