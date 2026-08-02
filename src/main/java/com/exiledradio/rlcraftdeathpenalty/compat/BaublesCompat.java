package com.exiledradio.rlcraftdeathpenalty.compat;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Baubles support, isolated in its own class so the JVM only ever loads it — and the
 * Baubles classes it references — after {@code Loader.isModLoaded("baubles")} has
 * passed. Touching any of this without Baubles present would be a NoClassDefFoundError.
 *
 * <p>This also covers the Tool Belt. Whenever Baubles is installed, Tool Belt's own
 * {@code BeltFinderBaubles} puts the belt in a Baubles slot, so it is stashed and
 * restored here along with everything else. A separate Tool Belt integration would find
 * the same stack a second time and duplicate it on restore.
 */
public final class BaublesCompat {

    private BaublesCompat() {
    }

    private static final String NBT_SLOT = "Slot";
    private static final String NBT_ITEM = "Item";

    /**
     * Takes every equipped bauble off the player and returns them serialised.
     * Emptying the slots is what stops Baubles' own death handler dropping them.
     */
    public static NBTTagList stash(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        NBTTagList kept = new NBTTagList();
        if (handler == null) {
            return kept;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(NBT_SLOT, slot);
            entry.setTag(NBT_ITEM, stack.writeToNBT(new NBTTagCompound()));
            kept.appendTag(entry);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return kept;
    }

    /** @return how many baubles were put back */
    public static int restore(EntityPlayer player, NBTTagList kept) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return 0;
        }
        int restored = 0;
        for (int i = 0; i < kept.tagCount(); i++) {
            NBTTagCompound entry = kept.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(entry.getCompoundTag(NBT_ITEM));
            int slot = entry.getInteger(NBT_SLOT);
            if (stack.isEmpty() || slot < 0 || slot >= handler.getSlots()) {
                continue;
            }
            if (handler.getStackInSlot(slot).isEmpty()) {
                handler.setStackInSlot(slot, stack);
            } else {
                player.dropItem(stack, false);
            }
            restored++;
        }
        return restored;
    }
}
