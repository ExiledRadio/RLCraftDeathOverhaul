package com.exiledradio.rlcraftdeathpenalty;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Optional, off by default: keeps some or all of a player's items across death, so the
 * mod can stand alone in a pack with no gravestone or death-penalty mod of its own.
 *
 * <p><b>How it works.</b> {@code EntityPlayer.onDeath} fires {@code LivingDeathEvent}
 * and only afterwards calls {@code inventory.dropAllItems()}. Emptying the slots we
 * want to save during that event means vanilla never sees them and never drops them.
 * They ride across the respawn in the player's persisted NBT — the same tag the death
 * counter uses — and are put back on {@code PlayerRespawnEvent}.
 *
 * <p>Storing them in NBT rather than a static map is deliberate: a player who logs out
 * on the death screen, or a server that restarts while they are dead, would lose
 * everything held in memory.
 */
@Mod.EventBusSubscriber(modid = RLCraftDeathPenalty.MODID)
public class InventoryKeepHandler {

    /** Which of {@link InventoryPlayer}'s three lists a saved stack came from. */
    private static final int LIST_MAIN = 0;
    private static final int LIST_ARMOR = 1;
    private static final int LIST_OFFHAND = 2;

    private static final String NBT_LIST = "List";
    private static final String NBT_SLOT = "Slot";
    private static final String NBT_ITEM = "Item";

    // ------------------------------------------------------------------
    // Death: lift the saved slots out before vanilla drops anything
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!ModConfig.KEEP_INVENTORY) {
            return;
        }
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote || !isEligible(player)) {
            return;
        }

        InventoryPlayer inv = player.inventory;
        NBTTagList kept = new NBTTagList();

        stash(kept, LIST_MAIN, inv.mainInventory, player);
        stash(kept, LIST_ARMOR, inv.armorInventory, player);
        stash(kept, LIST_OFFHAND, inv.offHandInventory, player);

        if (kept.tagCount() > 0) {
            DeathPenaltyData.setKeptItems(player, kept);
        }

        if (ModConfig.KEEP_XP) {
            DeathPenaltyData.setKeptXp(player, player.experienceTotal);
        }

        RLCraftDeathPenalty.LOGGER.debug("Holding {} stack(s) across {}'s death",
                kept.tagCount(), player.getName());
    }

    private static void stash(NBTTagList kept, int listId, NonNullList<ItemStack> slots,
                              EntityPlayer player) {
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty() || !shouldKeep(player, listId, slot)) {
                continue;
            }
            // Curse of Vanishing destroys the item on death, and vanilla applies that
            // before dropping. Saving such an item would quietly defeat the curse.
            if (EnchantmentHelper.hasVanishingCurse(stack)) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(NBT_LIST, (byte) listId);
            entry.setInteger(NBT_SLOT, slot);
            entry.setTag(NBT_ITEM, stack.writeToNBT(new NBTTagCompound()));
            kept.appendTag(entry);
            slots.set(slot, ItemStack.EMPTY);
        }
    }

    private static boolean shouldKeep(EntityPlayer player, int listId, int slot) {
        switch (listId) {
            case LIST_ARMOR:
                return ModConfig.KEEP_ARMOR;
            case LIST_OFFHAND:
                return ModConfig.KEEP_OFFHAND;
            case LIST_MAIN:
                // The held item lives inside the hotbar range, so it has to be checked
                // first or KEEP_MAINHAND could never be told apart from KEEP_HOTBAR.
                if (slot == player.inventory.currentItem) {
                    return ModConfig.KEEP_MAINHAND;
                }
                return slot < InventoryPlayer.getHotbarSize()
                        ? ModConfig.KEEP_HOTBAR
                        : ModConfig.KEEP_MAIN_INVENTORY;
            default:
                return false;
        }
    }

    /**
     * Vanilla skips dropping entirely for spectators and when the {@code keepInventory}
     * gamerule is set. Taking items out of the inventory in either case would destroy
     * them — nothing would drop them and vanilla would copy the emptied inventory
     * across — so this mod must not touch the inventory at all in those situations.
     */
    private static boolean isEligible(EntityPlayer player) {
        return !player.isSpectator()
                && !player.world.getGameRules().getBoolean("keepInventory");
    }

    // ------------------------------------------------------------------
    // Respawn: hand it all back
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) {
            return;
        }

        // Deliberately not gated on ModConfig.KEEP_INVENTORY. If the option is switched
        // off while someone is sitting on the death screen, their items still have to
        // come back — the alternative is silently eating them.
        if (DeathPenaltyData.hasKeptItems(player)) {
            restore(player, DeathPenaltyData.getKeptItems(player));
            DeathPenaltyData.clearKeptItems(player);
        }

        if (DeathPenaltyData.hasKeptXp(player)) {
            int xp = DeathPenaltyData.getKeptXp(player);
            DeathPenaltyData.clearKeptXp(player);
            if (xp > 0) {
                player.addExperience(xp);
            }
        }
    }

    private static void restore(EntityPlayer player, NBTTagList kept) {
        InventoryPlayer inv = player.inventory;
        int returned = 0;

        for (int i = 0; i < kept.tagCount(); i++) {
            NBTTagCompound entry = kept.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(entry.getCompoundTag(NBT_ITEM));
            if (stack.isEmpty()) {
                continue;
            }
            NonNullList<ItemStack> slots = listFor(inv, entry.getByte(NBT_LIST));
            int slot = entry.getInteger(NBT_SLOT);

            if (slots != null && slot >= 0 && slot < slots.size() && slots.get(slot).isEmpty()) {
                slots.set(slot, stack);
            } else if (!inv.addItemStackToInventory(stack)) {
                // Original slot taken and the inventory is full — better on the floor
                // at their feet than gone.
                player.dropItem(stack, false);
            }
            returned++;
        }

        if (player instanceof EntityPlayerMP) {
            // Respawn sends the client a fresh inventory before this runs, so the
            // restored stacks need pushing out or they stay invisible until relog.
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
        RLCraftDeathPenalty.LOGGER.debug("Returned {} stack(s) to {}", returned, player.getName());
    }

    private static NonNullList<ItemStack> listFor(InventoryPlayer inv, int listId) {
        switch (listId) {
            case LIST_MAIN:
                return inv.mainInventory;
            case LIST_ARMOR:
                return inv.armorInventory;
            case LIST_OFFHAND:
                return inv.offHandInventory;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------
    // Experience
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!ModConfig.KEEP_INVENTORY || !ModConfig.KEEP_XP) {
            return;
        }
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !isEligible(player)) {
            return;
        }
        // The amount was already banked at death; dropping it too would duplicate it.
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------

    /** Mods that also decide what survives a death. Running two of them is the problem. */
    private static final String[] CONFLICTING_MODS = {
            "corpsecomplex", "tombstone", "corail_tombstone", "gravestone",
            "universalgraves", "everlastingabilities", "deathquotes",
    };

    /** @return the modid of a loaded death-drops mod, or null if there is none */
    public static String findConflictingMod() {
        for (String modid : CONFLICTING_MODS) {
            if (Loader.isModLoaded(modid)) {
                return modid;
            }
        }
        return null;
    }

    /**
     * Two mods both deciding what survives a death is the single most likely way for
     * this feature to lose or duplicate someone's items, so say so loudly rather than
     * leaving it to be discovered the hard way.
     *
     * <p>Worth being blunt about the symptom as well as the risk: if the other mod is
     * already keeping the same slots, toggling {@code KEEP_INVENTORY} changes nothing
     * you can see, and the natural conclusion is that the setting is broken.
     */
    public static String buildConflictWarning() {
        if (!ModConfig.KEEP_INVENTORY) {
            return null;
        }
        String conflict = findConflictingMod();
        if (conflict == null) {
            return null;
        }
        // Deliberately worded as "check", not "conflict". This only knows the other mod
        // is installed, not how it is configured — one set to keep nothing is perfectly
        // fine to run alongside, and crying wolf at those users would train them to
        // ignore the message that actually matters.
        return "KEEP_INVENTORY is on and '" + conflict + "' is also installed. Check that it is "
                + "not set to keep items too. If it is, only one of them should be - two mods "
                + "saving one inventory risks losing or duplicating items, and whichever runs "
                + "second appears to do nothing, which makes these settings look broken.";
    }

    public static void logCompatibilityWarnings() {
        String warning = buildConflictWarning();
        if (warning != null) {
            RLCraftDeathPenalty.LOGGER.warn(warning);
        }
    }
}
