package com.exiledradio.rlcraftdeathoverhaul.compat;

import net.mcft.copy.backpacks.api.BackpackHelper;
import net.mcft.copy.backpacks.api.IBackpack;
import net.mcft.copy.backpacks.api.IBackpackData;
import net.mcft.copy.backpacks.api.IBackpackType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Wearable Backpacks support, isolated so its classes only load once
 * {@code Loader.isModLoaded("wearablebackpacks")} has passed.
 *
 * <p>A backpack is two separate things: the item itself and an {@link IBackpackData}
 * holding its contents. Both have to survive, so both are serialised — the data through
 * the {@code INBTSerializable} it already implements, and rebuilt on the far side with
 * {@link IBackpackType#createBackpackData}.
 */
public final class BackpackCompat {

    private BackpackCompat() {
    }

    private static final String NBT_ITEM = "Item";
    private static final String NBT_DATA = "Data";

    /**
     * Wearable Backpacks can be configured to wear the backpack in the chest armour
     * slot instead of its own. In that mode the stack is a normal armour item and
     * KEEP_ARMOR already handles it — stashing it here as well would duplicate it.
     */
    public static boolean handledAsArmor() {
        return BackpackHelper.equipAsChestArmor;
    }

    /** @return the serialised backpack, or null if the player had none equipped */
    public static NBTTagCompound stash(EntityPlayer player) {
        if (handledAsArmor()) {
            return null;
        }
        IBackpack backpack = BackpackHelper.getBackpack(player);
        if (backpack == null) {
            return null;
        }
        ItemStack stack = backpack.getStack();
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        NBTTagCompound saved = new NBTTagCompound();
        saved.setTag(NBT_ITEM, stack.writeToNBT(new NBTTagCompound()));

        IBackpackData data = backpack.getData();
        if (data != null) {
            NBTBase serialised = data.serializeNBT();
            if (serialised != null) {
                saved.setTag(NBT_DATA, serialised);
            }
        }

        // Unequipping is what stops the mod's own death handler dropping it.
        BackpackHelper.setEquippedBackpack(player, ItemStack.EMPTY, null);
        return saved;
    }

    /** @return true if a backpack was put back on the player */
    public static boolean restore(EntityPlayer player, NBTTagCompound saved) {
        if (saved == null || handledAsArmor()) {
            return false;
        }
        ItemStack stack = new ItemStack(saved.getCompoundTag(NBT_ITEM));
        if (stack.isEmpty()) {
            return false;
        }

        IBackpackData data = null;
        if (saved.hasKey(NBT_DATA)) {
            IBackpackType type = BackpackHelper.getBackpackType(stack);
            if (type != null) {
                data = type.createBackpackData(stack);
                if (data != null) {
                    data.deserializeNBT(saved.getTag(NBT_DATA));
                }
            }
        }

        BackpackHelper.setEquippedBackpack(player, stack, data);
        return true;
    }
}
