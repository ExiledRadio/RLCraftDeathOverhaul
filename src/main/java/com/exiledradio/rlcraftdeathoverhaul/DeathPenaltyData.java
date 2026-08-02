package com.exiledradio.rlcraftdeathoverhaul;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * The per-player death ledger, stored in the player's {@code PlayerPersisted} NBT tag.
 *
 * <p>That tag is the one piece of entity data vanilla explicitly copies from the old
 * player entity to the new one inside {@code EntityPlayer.copyFrom}, which is exactly
 * what this data has to survive: it is written during {@code LivingDeathEvent} on the
 * dying entity and read back during {@code PlayerRespawnEvent} on its replacement. It
 * survives regardless of the {@code keepInventory} gamerule, and needs no capability,
 * no {@code PlayerEvent.Clone} handler and no save/load code of its own.
 */
public final class DeathPenaltyData {

    private DeathPenaltyData() {
    }

    /** Our sub-compound inside PlayerPersisted, so we never collide with another mod's keys. */
    private static final String ROOT = RLCraftDeathOverhaul.MODID;

    /** Deaths accumulated since the last time a penalty was charged. */
    private static final String KEY_DEATHS = "deathsSincePenalty";
    /** Set at death, consumed at respawn — tells the two handlers a real death happened. */
    private static final String KEY_PENDING = "pendingDeath";
    /** Lifetime counters, for the /deathoverhaul status readout. */
    private static final String KEY_TOTAL_DEATHS = "totalDeaths";
    private static final String KEY_TOTAL_HEARTS_LOST = "totalHeartsLost";

    private static NBTTagCompound root(EntityPlayer player) {
        NBTTagCompound persisted = player.getEntityData()
                .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        // getCompoundTag returns a detached empty compound when the key is missing, so
        // both levels have to be written back rather than assumed to be live references.
        if (!player.getEntityData().hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        NBTTagCompound ours = persisted.getCompoundTag(ROOT);
        if (!persisted.hasKey(ROOT)) {
            persisted.setTag(ROOT, ours);
        }
        return ours;
    }

    public static int getDeathsSincePenalty(EntityPlayer player) {
        return root(player).getInteger(KEY_DEATHS);
    }

    public static void setDeathsSincePenalty(EntityPlayer player, int value) {
        root(player).setInteger(KEY_DEATHS, Math.max(0, value));
    }

    public static int incrementDeathsSincePenalty(EntityPlayer player) {
        int updated = getDeathsSincePenalty(player) + 1;
        setDeathsSincePenalty(player, updated);
        return updated;
    }

    public static boolean isDeathPending(EntityPlayer player) {
        return root(player).getBoolean(KEY_PENDING);
    }

    public static void setDeathPending(EntityPlayer player, boolean pending) {
        root(player).setBoolean(KEY_PENDING, pending);
    }

    public static int getTotalDeaths(EntityPlayer player) {
        return root(player).getInteger(KEY_TOTAL_DEATHS);
    }

    public static void incrementTotalDeaths(EntityPlayer player) {
        root(player).setInteger(KEY_TOTAL_DEATHS, getTotalDeaths(player) + 1);
    }

    /** Lifetime hearts lost to this mod, in whole hearts. */
    public static float getTotalHeartsLost(EntityPlayer player) {
        return root(player).getFloat(KEY_TOTAL_HEARTS_LOST);
    }

    public static void addTotalHeartsLost(EntityPlayer player, float hearts) {
        root(player).setFloat(KEY_TOTAL_HEARTS_LOST, getTotalHeartsLost(player) + hearts);
    }

    // --- Items held across the respawn while ENABLE_ITEM_KEEPING is on ---
    //
    // These ride in the same persisted tag as the counters rather than a static map, so
    // a server restart or a logout on the death screen cannot lose someone's gear.

    private static final String KEY_KEPT_ITEMS = "keptItems";
    private static final String KEY_KEPT_BAUBLES = "keptBaubles";
    private static final String KEY_KEPT_BACKPACK = "keptBackpack";
    private static final String KEY_KEPT_XP = "keptXp";

    private static final int NBT_COMPOUND = 10;

    public static void setKeptItems(EntityPlayer player, NBTTagList items) {
        root(player).setTag(KEY_KEPT_ITEMS, items);
    }

    public static NBTTagList getKeptItems(EntityPlayer player) {
        return root(player).getTagList(KEY_KEPT_ITEMS, NBT_COMPOUND);
    }

    public static boolean hasKeptItems(EntityPlayer player) {
        return root(player).hasKey(KEY_KEPT_ITEMS);
    }

    public static void setKeptBaubles(EntityPlayer player, NBTTagList baubles) {
        root(player).setTag(KEY_KEPT_BAUBLES, baubles);
    }

    public static NBTTagList getKeptBaubles(EntityPlayer player) {
        return root(player).getTagList(KEY_KEPT_BAUBLES, NBT_COMPOUND);
    }

    public static boolean hasKeptBaubles(EntityPlayer player) {
        return root(player).hasKey(KEY_KEPT_BAUBLES);
    }

    public static void setKeptBackpack(EntityPlayer player, NBTTagCompound backpack) {
        root(player).setTag(KEY_KEPT_BACKPACK, backpack);
    }

    public static NBTTagCompound getKeptBackpack(EntityPlayer player) {
        return root(player).getCompoundTag(KEY_KEPT_BACKPACK);
    }

    public static boolean hasKeptBackpack(EntityPlayer player) {
        return root(player).hasKey(KEY_KEPT_BACKPACK);
    }

    public static void setKeptXp(EntityPlayer player, int totalExperience) {
        root(player).setInteger(KEY_KEPT_XP, totalExperience);
    }

    public static int getKeptXp(EntityPlayer player) {
        return root(player).getInteger(KEY_KEPT_XP);
    }

    public static boolean hasKeptXp(EntityPlayer player) {
        return root(player).hasKey(KEY_KEPT_XP);
    }

    /** Wipes every stash key at once, once the contents have been handed back. */
    public static void clearKept(EntityPlayer player) {
        NBTTagCompound ours = root(player);
        ours.removeTag(KEY_KEPT_ITEMS);
        ours.removeTag(KEY_KEPT_BAUBLES);
        ours.removeTag(KEY_KEPT_BACKPACK);
        ours.removeTag(KEY_KEPT_XP);
    }

    /** Clears the ledger. Used by {@code /deathoverhaul reset}. */
    public static void reset(EntityPlayer player) {
        NBTTagCompound ours = root(player);
        ours.setInteger(KEY_DEATHS, 0);
        ours.setBoolean(KEY_PENDING, false);
        ours.setInteger(KEY_TOTAL_DEATHS, 0);
        ours.setFloat(KEY_TOTAL_HEARTS_LOST, 0.0F);
    }
}
