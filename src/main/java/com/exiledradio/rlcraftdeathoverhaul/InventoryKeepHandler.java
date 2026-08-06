package com.exiledradio.rlcraftdeathoverhaul;

import com.exiledradio.rlcraftdeathoverhaul.compat.BackpackCompat;
import com.exiledradio.rlcraftdeathoverhaul.compat.BaublesCompat;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
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
        boolean dropEverything = ModConfig.DROP_EVERYTHING_AT_MIN_HEALTH
                && DeathPenaltyHandler.isAtHealthFloor(player)
                && DeathPenaltyHandler.countsAsPenaltyDeath(player, event.getSource())
                && DeathPenaltyHandler.willChargeThisDeath(player);

        if (dropEverything) {
            DeathPenaltyData.setDroppedEverything(player, true);
            RLCraftDeathOverhaul.LOGGER.debug(
                    "{} died at the health floor - dropping everything", player.getName());
            // Everything goes — except cursed items when they are set to survive
            // regardless. A dropped item can be walked back to; a cursed one is
            // destroyed outright, so it is the only thing here that is gone for good.
            if (!ModConfig.cursesKeptAlways()) {
                return;
            }
        }

        InventoryPlayer inv = player.inventory;
        NBTTagList kept = new NBTTagList();
        stash(kept, LIST_MAIN, inv.mainInventory, player, dropEverything);
        stash(kept, LIST_ARMOR, inv.armorInventory, player, dropEverything);
        stash(kept, LIST_OFFHAND, inv.offHandInventory, player, dropEverything);
        if (kept.tagCount() > 0) {
            DeathPenaltyData.setKeptItems(player, kept);
        }

        // The backpack MUST come before the baubles, not after.
        //
        // Wearable Backpacks tracks the equipped backpack in an IBackpack capability, and
        // in RLCraft it is worn in a Baubles slot ("Equip Backpack as Bauble"), so the
        // item lives in the Baubles inventory while the capability holds the stack and,
        // crucially, the contents. Emptying the Baubles slot first leaves the capability
        // pointing at a backpack that is no longer worn — the state its own
        // onLivingUpdate treats as a faulty removal — and the contents go with it.
        // Reading and clearing the capability first means Wearable Backpacks has nothing
        // left to react to by the time the Baubles slot is touched.
        if (ModConfig.KEEP_WEARABLE_BACKPACK && Loader.isModLoaded(MODID_BACKPACKS)) {
            NBTTagCompound backpack = BackpackCompat.stash(player);
            if (backpack != null) {
                DeathPenaltyData.setKeptBackpack(player, backpack);
                RLCraftDeathOverhaul.LOGGER.debug("Saved {}'s equipped backpack and its contents",
                        player.getName());
            } else {
                RLCraftDeathOverhaul.LOGGER.debug(
                        "{} had no equipped backpack to save - if they were wearing one, its "
                                + "contents are handled by whatever holds the item instead",
                        player.getName());
            }
        }

        if (ModConfig.KEEP_BAUBLES && Loader.isModLoaded(MODID_BAUBLES)) {
            NBTTagList baubles = BaublesCompat.stash(player);
            if (baubles.tagCount() > 0) {
                DeathPenaltyData.setKeptBaubles(player, baubles);
            }
        }

        if (ModConfig.KEEP_XP) {
            DeathPenaltyData.setKeptXp(player, player.experienceTotal);
        }
    }

    /**
     * @param cursedOnly true on a death that costs everything, where the only reason we
     *                   are here at all is that cursed items are set to survive it. In
     *                   that mode the slot rules are bypassed: ALWAYS means always.
     */
    private static void stash(NBTTagList kept, int listId, NonNullList<ItemStack> slots,
                              EntityPlayer player, boolean cursedOnly) {
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // Vanilla's own curse, left alone on purpose — it is destroyed by
            // destroyVanishingCursedItems moments from now and players expect that.
            if (EnchantmentHelper.hasVanishingCurse(stack)) {
                continue;
            }
            boolean possessed = hasPossessionCurse(stack);

            if (ModConfig.cursesKeptAlways() && possessed) {
                // Survives regardless of slot, and regardless of everything else going.
            } else if (cursedOnly) {
                continue;
            } else if (!shouldKeep(player, listId, slot)) {
                continue;
            } else if (possessed && ModConfig.cursesNeverKept()) {
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

    /**
     * Whether Curse of Possession (So Many Enchantments) is on this stack.
     *
     * <p>The curse destroys the {@code EntityItem} once it is lying in the world — So Many
     * Enchantments scans for it, and RLTweaker wraps that scan to run every few ticks per
     * dimension rather than every tick. Either way it only ever looks at items on the
     * ground, so the single way to save one is to never let it drop.
     *
     * <p>Curse of Vanishing is deliberately not covered here. It is vanilla, players
     * expect it to work, and {@code KEEP_CURSED_ITEMS} is about the RLCraft-specific
     * curse. Vanishing-cursed items are always left for
     * {@code EntityPlayer.destroyVanishingCursedItems()} to deal with.
     *
     * <p>Curse of Binding is unaffected: it stops you unequipping armour, it destroys
     * nothing.
     */
    private static boolean hasPossessionCurse(ItemStack stack) {
        Enchantment possession = getPossessionCurse();
        return possession != null
                && EnchantmentHelper.getEnchantmentLevel(possession, stack) > 0;
    }

    private static Enchantment possessionCurse;
    private static boolean possessionCurseResolved;

    /**
     * Curse of Possession, looked up from the enchantment registry so So Many Enchantments
     * is not needed to compile or to run.
     *
     * <p>Matched on the path case-insensitively on purpose: So Many Enchantments re-cased
     * its registry names between the version base RLCraft ships and the one Dregora does,
     * so a hard-coded id would silently miss on one of the two packs.
     */
    private static Enchantment getPossessionCurse() {
        if (!possessionCurseResolved) {
            possessionCurseResolved = true;
            for (Enchantment enchantment : Enchantment.REGISTRY) {
                ResourceLocation id = Enchantment.REGISTRY.getNameForObject(enchantment);
                if (id != null && "curseofpossession".equalsIgnoreCase(id.getPath())) {
                    possessionCurse = enchantment;
                    RLCraftDeathOverhaul.LOGGER.debug("Found Curse of Possession as {}", id);
                    break;
                }
            }
        }
        return possessionCurse;
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

        // Mirror of the death order: the Baubles slot is filled first, then the
        // capability is pointed at it. Doing it the other way round would leave the
        // capability referencing a backpack that is not in any slot yet, which is the
        // state Wearable Backpacks tears down as a faulty removal.
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
