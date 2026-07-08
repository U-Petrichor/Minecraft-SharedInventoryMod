package com.umut.sharedInventory.item;

import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class BackpackInventory implements Inventory {
    private static final int PUBLIC_STACK_SIZE = 16;
    private final SharedInventoryChestBlockEntity linkedBlockEntity;

    public BackpackInventory(SharedInventoryChestBlockEntity linkedBlockEntity) {
        this.linkedBlockEntity = linkedBlockEntity;
    }

    @Override
    public int size() { return PUBLIC_STACK_SIZE; }

    public boolean isLinkedBlockEntityValid() {
        return this.linkedBlockEntity != null && !this.linkedBlockEntity.isRemoved();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < PUBLIC_STACK_SIZE)
            return this.linkedBlockEntity.getPublicStack().get(slot);
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= PUBLIC_STACK_SIZE) return;
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.linkedBlockEntity.getPublicStack().set(slot, stack);
        this.linkedBlockEntity.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= PUBLIC_STACK_SIZE) return ItemStack.EMPTY;
        ItemStack result = Inventories.splitStack(this.linkedBlockEntity.getPublicStack(), slot, amount);
        if (!result.isEmpty()) this.linkedBlockEntity.markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot >= 0 && slot < PUBLIC_STACK_SIZE) {
            ItemStack result = Inventories.removeStack(this.linkedBlockEntity.getPublicStack(), slot);
            if (!result.isEmpty()) this.linkedBlockEntity.markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void markDirty() { this.linkedBlockEntity.markDirty(); }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PUBLIC_STACK_SIZE; i++) {
            if (!getStack(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return isLinkedBlockEntityValid(); }

    @Override
    public void clear() {
        for (int i = 0; i < PUBLIC_STACK_SIZE; i++) {
            this.linkedBlockEntity.getPublicStack().set(i, ItemStack.EMPTY);
        }
        this.linkedBlockEntity.markDirty();
    }
}
