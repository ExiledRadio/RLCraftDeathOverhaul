package com.exiledradio.rlcraftdeathoverhaul;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The whole mechanic.
 *
 * <p>The work is split across the death and the respawn because each half needs
 * something only available at that moment: {@link LivingDeathEvent} is the only place
 * the {@code DamageSource} exists, and {@code PlayerRespawnEvent} is the only place
 * the new player entity exists to have its health written. The two halves talk to each
 * other through {@link DeathPenaltyData}, which rides along in the player's persisted
 * NBT across the respawn.
 *
 * <p><b>On the two respawn handlers.</b> Scaling Health subtracts its own
 * {@code Health Lost On Death} during {@code PlayerRespawnEvent} at default priority.
 * Rather than depend on the pack author zeroing that setting, this mod snapshots the
 * player's max health at {@link EventPriority#HIGHEST} (before Scaling Health touches
 * it) and writes the final value at {@link EventPriority#LOWEST} (after). Whatever
 * Scaling Health did in between is overwritten, so the two mods can never both charge
 * for the same death.
 */
@Mod.EventBusSubscriber(modid = RLCraftDeathOverhaul.MODID)
public class DeathPenaltyHandler {

    /**
     * Max health in HP as it stood before Scaling Health's respawn handler ran, keyed by
     * player UUID. Written at HIGHEST priority and consumed at LOWEST within the same
     * event dispatch, so entries never outlive a single respawn.
     */
    private static final Map<UUID, Float> PRE_RESPAWN_MAX_HEALTH = new ConcurrentHashMap<UUID, Float>();

    private static final String PREFIX = TextFormatting.DARK_RED + "[Death Overhaul] " + TextFormatting.RESET;

    // ------------------------------------------------------------------
    // Death: decide whether this death counts, and bank it for the respawn
    // ------------------------------------------------------------------

    /**
     * LOWEST priority so that anything able to cancel the death — totems, second-chance
     * mods, and RLCraft's own revival effects — has already had its say. A cancelled
     * event never reaches a handler that has not opted into receiving cancelled events,
     * so a player who is saved from dying is never charged.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote) {
            return;
        }

        if (!countsAsPenaltyDeath(player, event.getSource())) {
            return;
        }

        DeathPenaltyData.incrementTotalDeaths(player);
        int deaths = DeathPenaltyData.incrementDeathsSincePenalty(player);
        DeathPenaltyData.setDeathPending(player, true);
        RLCraftDeathOverhaul.LOGGER.debug("{} now has {} death(s) toward a penalty of {}",
                player.getName(), deaths, ModConfig.DEATHS_PER_PENALTY);
    }

    /**
     * Whether a death should cost the player anything at all.
     *
     * <p>Shared with {@link InventoryKeepHandler} rather than duplicated, because
     * DROP_EVERYTHING_AT_MIN_HEALTH decides what happens to items using the same answer.
     * A death that costs no hearts because it was exempt must not cost items either —
     * there is nothing being traded, so nothing should be charged on either side.
     *
     * <p>Written as a pure function of the player and damage source so the two handlers
     * can call it from opposite event priorities and still agree.
     */
    public static boolean countsAsPenaltyDeath(EntityPlayer player, DamageSource source) {
        String damageType = source == null ? "unknown" : source.getDamageType();
        int dimension = player.world.provider.getDimension();
        RLCraftDeathOverhaul.LOGGER.debug("{} died - damage type '{}', dimension {}",
                player.getName(), damageType, dimension);

        if (!ModConfig.COUNT_CREATIVE_DEATHS && (player.isCreative() || player.isSpectator())) {
            RLCraftDeathOverhaul.LOGGER.debug("Ignoring death of {}: creative/spectator",
                    player.getName());
            return false;
        }
        if (ModConfig.isDimensionExempt(dimension)) {
            RLCraftDeathOverhaul.LOGGER.debug("Ignoring death of {}: dimension {} is exempt",
                    player.getName(), dimension);
            return false;
        }
        if (ModConfig.isDamageTypeExempt(damageType)) {
            RLCraftDeathOverhaul.LOGGER.debug("Ignoring death of {}: damage type '{}' is exempt",
                    player.getName(), damageType);
            return false;
        }
        return true;
    }

    /**
     * Whether this death is the one that gets charged, asked while the counter has not
     * been incremented for it yet.
     *
     * <p>{@link InventoryKeepHandler} runs at {@code HIGHEST} and this handler at
     * {@code LOWEST}, so at the moment the item decision is made the increment has not
     * happened — hence the {@code + 1}. Without this, a grace period would apply to
     * hearts but not to items, and a death advertised as free would still empty your
     * inventory.
     */
    public static boolean willChargeThisDeath(EntityPlayer player) {
        return DeathPenaltyData.getDeathsSincePenalty(player) + 1 >= ModConfig.DEATHS_PER_PENALTY;
    }

    /**
     * True when the player has no health left to pay with — their maximum is already at
     * or below the floor, so a penalty would take nothing.
     */
    public static boolean isAtHealthFloor(EntityPlayer player) {
        float maxHp = ScalingHealthBridge.getMaxHealth(player);
        return maxHp >= 0.0F && maxHp <= ModConfig.getMinHealthHp() + 0.001F;
    }

    // ------------------------------------------------------------------
    // Respawn: snapshot before Scaling Health, settle up after it
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawnPre(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) {
            return;
        }
        float maxHealth = ScalingHealthBridge.getMaxHealth(player);
        if (maxHealth > 0.0F) {
            PRE_RESPAWN_MAX_HEALTH.put(player.getUniqueID(), maxHealth);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawnPost(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) {
            return;
        }

        Float snapshot = PRE_RESPAWN_MAX_HEALTH.remove(player.getUniqueID());
        float currentMaxHp = ScalingHealthBridge.getMaxHealth(player);
        if (currentMaxHp < 0.0F) {
            // Scaling Health has no data for this player — nothing to do, and nothing
            // it could have subtracted either.
            return;
        }
        // Without a snapshot (the HIGHEST handler somehow did not run) the best available
        // baseline is what Scaling Health left behind. Worst case that means its own
        // subtraction stands for this one respawn, which is still not a double charge.
        float baselineHp = snapshot == null ? currentMaxHp : snapshot.floatValue();

        boolean deathPending = DeathPenaltyData.isDeathPending(player);
        DeathPenaltyData.setDeathPending(player, false);

        // Returning from The End through the exit portal fires this event without a death.
        // The pending flag already excludes it, but check explicitly so the intent is not
        // resting on an implementation detail of another mod.
        if (!deathPending || event.isEndConquered()) {
            restoreIfChanged(player, baselineHp, currentMaxHp);
            return;
        }

        int deaths = DeathPenaltyData.getDeathsSincePenalty(player);
        if (deaths < ModConfig.DEATHS_PER_PENALTY) {
            restoreIfChanged(player, baselineHp, currentMaxHp);
            announceProgress(player, deaths);
            return;
        }

        // Only spend the banked deaths if the charge actually went through — otherwise a
        // misconfigured Scaling Health would quietly eat the counter every respawn while
        // the player never loses anything.
        if (applyPenalty(player, baselineHp, currentMaxHp) && ModConfig.RESET_COUNTER_ON_PENALTY) {
            DeathPenaltyData.setDeathsSincePenalty(player, 0);
        }

        // Consumed last, once the message that depends on it has been sent.
        DeathPenaltyData.clearDroppedEverything(player);
    }

    /**
     * Puts max health back to {@code baselineHp}, undoing any subtraction Scaling Health
     * made during this same respawn. Writing is skipped when the value already matches,
     * so the ordinary case (Scaling Health's own loss set to 0) costs nothing and sends
     * no sync packet.
     */
    private static void restoreIfChanged(EntityPlayer player, float baselineHp, float currentMaxHp) {
        if (Math.abs(baselineHp - currentMaxHp) > 0.001F) {
            RLCraftDeathOverhaul.LOGGER.debug(
                    "Undoing Scaling Health's own death loss for {}: {} -> {} HP",
                    player.getName(), currentMaxHp, baselineHp);
            ScalingHealthBridge.setMaxHealth(player, baselineHp);
            healToFull(player);
        }
    }

    /** @return true if the penalty was settled — including the no-op case of already being at the floor */
    private static boolean applyPenalty(EntityPlayer player, float baselineHp, float currentMaxHp) {
        float minHp = ModConfig.getMinHealthHp();
        float lossHp = ModConfig.getHeartsLostHp();

        // A player already at or under the floor keeps what they have. Notably this never
        // *raises* anyone: someone sitting below the floor because MIN_HEARTS was recently
        // increased is left alone rather than handed free hearts.
        float targetHp = baselineHp <= minHp ? baselineHp : Math.max(baselineHp - lossHp, minHp);
        float actualLossHp = baselineHp - targetHp;

        if (Math.abs(targetHp - currentMaxHp) > 0.001F) {
            if (!ScalingHealthBridge.setMaxHealth(player, targetHp)) {
                RLCraftDeathOverhaul.LOGGER.warn(
                        "Scaling Health rejected the health change for {} - is \"Allow Modified "
                                + "Health\" set to false?", player.getName());
                return false;
            }
            healToFull(player);
        }

        if (actualLossHp > 0.0F) {
            DeathPenaltyData.addTotalHeartsLost(player, actualLossHp / 2.0F);
        }

        RLCraftDeathOverhaul.LOGGER.debug("Penalty for {}: {} -> {} HP (lost {})",
                player.getName(), baselineHp, targetHp, actualLossHp);

        announcePenalty(player, actualLossHp, targetHp, minHp);
        return true;
    }

    /**
     * Scaling Health heals the player to full during its own respawn handler, using the
     * max health it believed in at the time. Since this mod changes that value afterwards,
     * the heal has to be redone against the corrected maximum.
     */
    private static void healToFull(EntityPlayer player) {
        player.setHealth(player.getMaxHealth());
    }

    // ------------------------------------------------------------------
    // Sleeping
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!ModConfig.RESET_COUNTER_ON_SLEEP) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        // Only a night actually slept through should wipe the slate, and `updateWorld`
        // is the flag that says so — which is not what its name suggests. Vanilla has
        // exactly two call sites: WorldServer.wakeAllPlayers passes
        // wakeUpPlayer(false, false, true) at dawn, and NetHandlerPlayServer passes
        // wakeUpPlayer(false, true, true) when the player clicks "leave bed". Both leave
        // wakeImmediately false and shouldSetSpawn true, so updateWorld is the only one
        // of the three that tells them apart.
        if (player == null || player.world.isRemote || event.updateWorld()) {
            return;
        }
        int deaths = DeathPenaltyData.getDeathsSincePenalty(player);
        if (deaths <= 0) {
            return;
        }
        DeathPenaltyData.setDeathsSincePenalty(player, 0);
        if (ModConfig.ANNOUNCE_PROGRESS) {
            send(player, new TextComponentString(PREFIX + TextFormatting.GREEN
                    + "A full night's rest clears your record. "
                    + TextFormatting.GRAY + "(" + deaths + " pending death"
                    + (deaths == 1 ? "" : "s") + " forgiven)"));
        }
    }

    // ------------------------------------------------------------------
    // Player-facing messages
    // ------------------------------------------------------------------

    private static void announcePenalty(EntityPlayer player, float actualLossHp,
                                        float targetHp, float minHp) {
        if (!ModConfig.ANNOUNCE_PENALTY) {
            return;
        }

        // Charged in items rather than hearts, so "cost you nothing" would be a lie.
        // Announced from here rather than from InventoryKeepHandler because that runs at
        // NORMAL priority and this at LOWEST — messaging from both produced two
        // contradictory lines, one after the other.
        if (DeathPenaltyData.didDropEverything(player)) {
            send(player, new TextComponentString(PREFIX + TextFormatting.RED
                    + "You had no hearts left to lose, so this death cost you everything you "
                    + "were carrying. " + TextFormatting.GRAY
                    + "Raise your maximum health to protect your gear again."));
            return;
        }

        if (actualLossHp <= 0.0F) {
            send(player, new TextComponentString(PREFIX + TextFormatting.YELLOW
                    + "You are already at the minimum of " + TextFormatting.WHITE
                    + formatHearts(minHp) + TextFormatting.YELLOW
                    + " hearts - this death cost you nothing."));
            return;
        }

        ITextComponent message = new TextComponentString(PREFIX + TextFormatting.RED
                + "You lost " + TextFormatting.WHITE + formatHearts(actualLossHp)
                + TextFormatting.RED + " heart" + (actualLossHp == 2.0F ? "" : "s")
                + ". Max health is now " + TextFormatting.WHITE + formatHearts(targetHp)
                + TextFormatting.RED + " hearts."
                + (targetHp <= minHp ? TextFormatting.GRAY + " (the minimum)" : ""));
        send(player, message);

        if (ModConfig.BROADCAST_PENALTY_TO_SERVER && player.getServer() != null) {
            player.getServer().getPlayerList().sendMessage(new TextComponentString(
                    PREFIX + TextFormatting.GRAY + player.getName() + " lost "
                            + formatHearts(actualLossHp) + " heart"
                            + (actualLossHp == 2.0F ? "" : "s") + " on death, and is down to "
                            + formatHearts(targetHp) + "."));
        }
    }

    private static void announceProgress(EntityPlayer player, int deaths) {
        if (!ModConfig.ANNOUNCE_PROGRESS) {
            return;
        }
        int remaining = ModConfig.DEATHS_PER_PENALTY - deaths;
        if (remaining <= 0) {
            return;
        }
        send(player, new TextComponentString(PREFIX + TextFormatting.YELLOW
                + "That one was free. " + TextFormatting.WHITE + remaining
                + TextFormatting.YELLOW + " more death" + (remaining == 1 ? "" : "s")
                + " and you lose " + TextFormatting.WHITE
                + formatHearts(ModConfig.getHeartsLostHp()) + TextFormatting.YELLOW
                + " heart" + (ModConfig.HEARTS_LOST_PER_PENALTY == 1.0F ? "" : "s") + "."));
    }

    private static void send(EntityPlayer player, ITextComponent message) {
        if (player instanceof EntityPlayerMP) {
            player.sendMessage(message);
        }
    }

    /** Renders a raw HP value as hearts, dropping the decimal on whole numbers. */
    public static String formatHearts(float hp) {
        float hearts = hp / 2.0F;
        if (hearts == Math.floor(hearts)) {
            return String.valueOf((int) hearts);
        }
        return String.format(Locale.ROOT, "%.1f", hearts);
    }
}
