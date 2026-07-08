package com.umut.sharedInventory.item;

import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class BackpackSlotInventory implements Inventory {

    private final SharedInventoryPlayerEntity sharedPlayer;

    public BackpackSlotInventory(SharedInventoryPlayerEntity sharedPlayer) {
        this.sharedPlayer = sharedPlayer;
    }

    @Override
    public int size() { return 1; }

    @Override
    public boolean isEmpty() {
        return sharedPlayer.shared$getBackpackStack().isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot == 0 ? sharedPlayer.shared$getBackpackStack() : ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            sharedPlayer.shared$setBackpackStack(stack);
        }
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack current = sharedPlayer.shared$getBackpackStack();
        if (current.isEmpty() || slot != 0) return ItemStack.EMPTY;
        ItemStack result = current.split(amount);
        if (current.isEmpty()) {
            sharedPlayer.shared$setBackpackStack(ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack current = sharedPlayer.shared$getBackpackStack();
        sharedPlayer.shared$setBackpackStack(ItemStack.EMPTY);
        return current;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override
    public void clear() {
        sharedPlayer.shared$setBackpackStack(ItemStack.EMPTY);
    }
}
