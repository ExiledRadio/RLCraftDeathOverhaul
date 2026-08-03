package com.exiledradio.rlcraftdeathoverhaul;

import com.exiledradio.rlcraftdeathoverhaul.compat.BackpackCompat;
import com.exiledradio.rlcraftdeathoverhaul.compat.BaublesCompat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Keeps your gear through a death so that hearts, not your inventory, are the price of
 * dying. On by default — RLCraft ships with every keep-item option turned off, and a
 * mod whose whole premise is "you pay in hearts instead" is useless if the player still
 * has to go and configure something else before that becomes true.
 *
 * <p><b>How it works.</b> {@code EntityPlayer.onDeath} fires {@code LivingDeathEvent}
 * and only afterwards calls {@code inventory.dropAllItems()}. Emptying the slots we want
 * to save during that event means vanilla never sees them and never drops them. They
 * ride across the respawn in the player's persisted NBT — the same tag the death counter
 * uses — and go back on {@code PlayerRespawnEvent}.
 *
 * <p>Storing them in NBT rather than a static map is deliberate: a player who logs out
 * on the death screen, or a server that restarts while they are dead, would lose
 * everything held only in memory.
 */
@Mod.EventBusSubscriber(modid = RLCraftDeathOverhaul.MODID)
public class InventoryKeepHandler {

    private static final String MODID_BAUBLES = "baubles";
    private static final String MODID_BACKPACKS = "wearablebackpacks";

    /** Which of {@link InventoryPlayer}'s three lists a saved stack came from. */
    private static final int LIST_MAIN = 0;
    private static final int LIST_ARMOR = 1;
    private static final int LIST_OFFHAND = 2;

    private static final int TICKS_PER_MINUTE = 20 * 60;

    private static final String NBT_LIST = "List";
    private static final String NBT_SLOT = "Slot";
    private static final String NBT_ITEM = "Item";

    // ------------------------------------------------------------------
    // Death: lift the saved slots out before vanilla drops anything
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!ModConfig.ENABLE_ITEM_KEEPING || !(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote || !isEligible(player)) {
            return;
        }

        // No health left to pay with, so the bill is settled in items instead. Keeping
        // nothing here means vanilla drops the lot, exactly as if the mod were absent.
        // Gated on the death actually counting: an exempt death charges no hearts, so it
        // must not charge items either.
        if (ModConfig.DROP_EVERYTHING_AT_MIN_HEALTH
                && DeathPenaltyHandler.isAtHealthFloor(player)
                && DeathPenaltyHandler.countsAsPenaltyDeath(player, event.getSource())
                && DeathPenaltyHandler.willChargeThisDeath(player)) {
            DeathPenaltyData.setDroppedEverything(player, true);
            RLCraftDeathOverhaul.LOGGER.debug(
                    "{} died at the health floor - dropping everything", player.getName());
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

        if (ModConfig.KEEP_BAUBLES && Loader.isModLoaded(MODID_BAUBLES)) {
            NBTTagList baubles = BaublesCompat.stash(player);
            if (baubles.tagCount() > 0) {
                DeathPenaltyData.setKeptBaubles(player, baubles);
            }
        }

        if (ModConfig.KEEP_WEARABLE_BACKPACK && Loader.isModLoaded(MODID_BACKPACKS)) {
            NBTTagCompound backpack = BackpackCompat.stash(player);
            if (backpack != null) {
                DeathPenaltyData.setKeptBackpack(player, backpack);
            }
        }

        if (ModConfig.KEEP_XP) {
            DeathPenaltyData.setKeptXp(player, player.experienceTotal);
        }
    }

    private static void stash(NBTTagList kept, int listId, NonNullList<ItemStack> slots,
                              EntityPlayer player) {
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty() || !shouldKeep(player, listId, slot)) {
                continue;
            }
            // Curse of Vanishing destroys the item on death, and vanilla applies that
            // just before dropping. Saving such an item would quietly defeat the curse.
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
     * them — nothing would drop them, and vanilla would copy the emptied inventory
     * across — so the inventory must not be touched at all in those situations.
     */
    private static boolean isEligible(EntityPlayer player) {
        return !player.isSpectator()
                && !player.world.getGameRules().getBoolean("keepInventory");
    }

    // ------------------------------------------------------------------
    // Drops: stop the pile you did lose from evaporating
    // ------------------------------------------------------------------

    /**
     * Forge captures everything a dying player drops and hands it over here before the
     * items are spawned, which is the one place to reach all of them at once.
     */
    @SubscribeEvent
    public static void onPlayerDrops(PlayerDropsEvent event) {
        int minutes = ModConfig.DROP_DESPAWN_MINUTES;
        if (minutes == 0 || event.getEntityPlayer() == null
                || event.getEntityPlayer().world.isRemote) {
            return;
        }

        // Forge reads `lifespan` per item rather than using the fixed 6000-tick vanilla
        // lifetime, and its despawn test is a plain `age >= lifespan` with no sentinel
        // for "never" — so forever has to be spelled as a value age cannot reach.
        int lifespan = minutes < 0 ? Integer.MAX_VALUE : minutes * TICKS_PER_MINUTE;

        for (EntityItem item : event.getDrops()) {
            if (item != null) {
                item.lifespan = lifespan;
            }
        }
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

        // Deliberately not gated on ModConfig.ENABLE_ITEM_KEEPING. If the option is switched
        // off while someone is sitting on the death screen, their gear still has to come
        // back — the alternative is silently eating it.
        boolean any = false;

        if (DeathPenaltyData.hasKeptItems(player)) {
            restoreInventory(player, DeathPenaltyData.getKeptItems(player));
            any = true;
        }

        if (DeathPenaltyData.hasKeptBaubles(player) && Loader.isModLoaded(MODID_BAUBLES)) {
            BaublesCompat.restore(player, DeathPenaltyData.getKeptBaubles(player));
            any = true;
        }

        if (DeathPenaltyData.hasKeptBackpack(player) && Loader.isModLoaded(MODID_BACKPACKS)) {
            BackpackCompat.restore(player, DeathPenaltyData.getKeptBackpack(player));
            any = true;
        }

        if (DeathPenaltyData.hasKeptXp(player)) {
            int xp = DeathPenaltyData.getKeptXp(player);
            if (xp > 0) {
                player.addExperience(xp);
            }
            any = true;
        }

        // Why the gear is gone is announced by DeathPenaltyHandler, which runs after this
        // and owns every penalty message, so the player cannot be told two different
        // things about the same death. The flag it reads is cleared there too.
        DeathPenaltyData.clearKept(player);

        if (any && player instanceof EntityPlayerMP) {
            // Respawn sends the client a fresh inventory before this runs, so the
            // restored stacks need pushing out or they stay invisible until relog.
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }

    private static void restoreInventory(EntityPlayer player, NBTTagList kept) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < kept.tagCount(); i++) {
            NBTTagCompound entry = kept.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(entry.getCompoundTag(NBT_ITEM));
            if (stack.isEmpty()) {
                continue;
            }
            applyDurabilityLoss(stack);

            NonNullList<ItemStack> slots = listFor(inv, entry.getByte(NBT_LIST));
            int slot = entry.getInteger(NBT_SLOT);
            if (slots != null && slot >= 0 && slot < slots.size() && slots.get(slot).isEmpty()) {
                slots.set(slot, stack);
            } else if (!inv.addItemStackToInventory(stack)) {
                // Original slot taken and the inventory full — better at their feet
                // than gone.
                player.dropItem(stack, false);
            }
        }
    }

    /**
     * The cost of keeping your gear. Applied on the way back rather than at death so it
     * lands once, on exactly the items that actually survived.
     *
     * <p>Never breaks anything: an item that would be destroyed is left on its last
     * point of durability instead. Losing a fully-repaired weapon to a fall would be a
     * far harsher penalty than this setting advertises.
     */
    private static void applyDurabilityLoss(ItemStack stack) {
        float fraction = ModConfig.DURABILITY_LOSS_ON_KEPT_ITEMS;
        if (fraction <= 0.0F || !stack.isItemStackDamageable()) {
            return;
        }
        int loss = Math.max(1, Math.round(stack.getMaxDamage() * fraction));
        int damage = Math.min(stack.getItemDamage() + loss, stack.getMaxDamage() - 1);
        stack.setItemDamage(Math.max(0, damage));
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
        if (!ModConfig.ENABLE_ITEM_KEEPING || !ModConfig.KEEP_XP
                || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !isEligible(player)) {
            return;
        }
        // The amount was banked at death; dropping it as well would duplicate it.
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------

    /** Mods that also decide what survives a death. Running two of them is the problem. */
    private static final String[] CONFLICTING_MODS = {
            "corpsecomplex", "tombstone", "corail_tombstone", "gravestone", "universalgraves",
    };

    /**
     * Two mods both deciding what survives a death can duplicate or lose items, and this
     * mod now keeps things by default — so anyone whose pack already handles drops needs
     * telling. Deliberately worded as "check", not "conflict": this only knows the other
     * mod is installed, not how it is set up, and one configured to keep nothing is
     * perfectly fine alongside.
     */
    public static void logCompatibilityWarnings() {
        if (!ModConfig.ENABLE_ITEM_KEEPING) {
            return;
        }
        for (String modid : CONFLICTING_MODS) {
            if (Loader.isModLoaded(modid)) {
                RLCraftDeathOverhaul.LOGGER.warn(
                        "ENABLE_ITEM_KEEPING is on and '{}' is also installed. Check it is not set to "
                                + "keep items as well - only one mod should be. Two mods saving one "
                                + "inventory can duplicate or lose items, and whichever runs second "
                                + "looks like it is doing nothing.", modid);
            }
        }
    }
}
