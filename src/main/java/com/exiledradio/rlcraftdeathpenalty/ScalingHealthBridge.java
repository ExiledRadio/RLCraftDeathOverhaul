package com.exiledradio.rlcraftdeathpenalty;

import net.minecraft.entity.player.EntityPlayer;
import net.silentchaos512.scalinghealth.config.Config;
import net.silentchaos512.scalinghealth.utils.SHPlayerDataHandler;

/**
 * Every point of contact with Scaling Health lives here, so that a future Scaling
 * Health version that moves things around has exactly one file to fix.
 *
 * <p>Health values on both sides of this boundary are in <em>half-hearts</em> (raw
 * Minecraft HP): 20 HP = 10 hearts. Scaling Health's own config is in half-hearts
 * too. This mod's config is in whole hearts because that is how players talk about
 * it, so {@link ModConfig} does the conversion and everything below this line is HP.
 */
public final class ScalingHealthBridge {

    private ScalingHealthBridge() {
    }

    /**
     * The hard floor Scaling Health itself clamps to inside {@code setMaxHealth} —
     * it will never let a player drop below one heart no matter what we ask for.
     */
    public static final float ABSOLUTE_MIN_HP = 2.0F;

    /**
     * Scaling Health's stored max health for this player, in HP.
     *
     * <p>This is deliberately not {@code player.getMaxHealth()}: that attribute also
     * carries modifiers from armor, potions and other mods, and writing it back would
     * bake those temporary bonuses into the player's permanent health.
     *
     * @return the player's tracked max health, or -1 if Scaling Health has no data yet
     */
    public static float getMaxHealth(EntityPlayer player) {
        SHPlayerDataHandler.PlayerData data = SHPlayerDataHandler.get(player);
        return data == null ? -1.0F : data.getMaxHealth();
    }

    /**
     * Writes the player's max health, in HP.
     *
     * <p>Scaling Health's own setter does all four things that need to happen — clamps
     * to [2, its configured cap], reapplies the health attribute modifier, saves to the
     * player's NBT, and sends the sync packet that redraws the client's heart bar — so
     * this mod must not duplicate any of them.
     *
     * @return false if Scaling Health refused the write (see {@link #isHealthModificationAllowed})
     */
    public static boolean setMaxHealth(EntityPlayer player, float hp) {
        SHPlayerDataHandler.PlayerData data = SHPlayerDataHandler.get(player);
        if (data == null || !isHealthModificationAllowed()) {
            return false;
        }
        data.setMaxHealth(hp);
        return true;
    }

    /**
     * When Scaling Health's "Allow Modified Health" is off, its {@code setMaxHealth}
     * returns immediately without doing anything. Checking up front lets this mod say
     * so in the log instead of silently appearing broken.
     */
    public static boolean isHealthModificationAllowed() {
        return Config.Player.Health.allowModify;
    }

    /** Scaling Health's own per-death health loss, in HP. This mod expects it to be 0. */
    public static int getScalingHealthLostOnDeath() {
        return Config.Player.Health.lostOnDeath;
    }

    /** Scaling Health's max health cap, in HP. 0 means uncapped. */
    public static int getScalingHealthMaxHealth() {
        return Config.Player.Health.maxHealth;
    }

    /** Scaling Health's own minimum health, in HP. */
    public static int getScalingHealthMinHealth() {
        return Config.Player.Health.minHealth;
    }

    /**
     * Logs the ways the two mods' configs can disagree. None of these are fatal — this
     * mod overrides Scaling Health at respawn regardless — but each one produces
     * behaviour the pack author probably did not intend, and they are miserable to
     * diagnose from in-game symptoms alone.
     */
    public static void logCompatibilityWarnings() {
        if (!isHealthModificationAllowed()) {
            RLCraftDeathPenalty.LOGGER.warn(
                    "Scaling Health's \"Allow Modified Health\" is false, so it will reject every "
                            + "health change. This mod cannot do anything until that is set to true "
                            + "(config/scalinghealth/main.cfg -> player -> health).");
        }

        int shLostOnDeath = getScalingHealthLostOnDeath();
        if (shLostOnDeath != 0) {
            RLCraftDeathPenalty.LOGGER.warn(
                    "Scaling Health's \"Health Lost On Death\" is {} half-hearts. This mod takes over "
                            + "death health loss entirely and undoes Scaling Health's subtraction on "
                            + "respawn, so that setting no longer does anything. Set it to 0 to avoid "
                            + "confusion.", shLostOnDeath);
        }

        int shMax = getScalingHealthMaxHealth();
        float configuredMinHp = ModConfig.getMinHealthHp();
        if (shMax > 0 && configuredMinHp > shMax) {
            RLCraftDeathPenalty.LOGGER.warn(
                    "MIN_HEARTS is {} half-hearts, which is above Scaling Health's max health cap of {}. "
                            + "Scaling Health will clamp players down to the cap, so the floor cannot be "
                            + "reached.", configuredMinHp, shMax);
        }
    }
}
