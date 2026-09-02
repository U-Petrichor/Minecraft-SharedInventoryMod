package com.umut.sharedInventory.item;

import com.umut.sharedInventory.inventory.SharedCoreStorageState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.UUID;

public class BackpackInventory implements Inventory {
    private static final int PUBLIC_STACK_SIZE = SharedCoreStorageState.INVENTORY_SIZE;
    private final SharedCoreStorageState storageState;
    private final UUID coreId;

    public BackpackInventory(SharedCoreStorageState storageState, UUID coreId) {
        this.storageState = storageState;
        this.coreId = coreId;
    }

    @Override
    public int size() { return PUBLIC_STACK_SIZE; }

    private DefaultedList<ItemStack> getItems() {
        return storageState.getItems(coreId);
    }

    @Override
    public ItemStack getStack(int slot) {
        DefaultedList<ItemStack> items = getItems();
        if (items != null && slot >= 0 && slot < PUBLIC_STACK_SIZE)
            return items.get(slot);
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        DefaultedList<ItemStack> items = getItems();
        if (items == null || slot < 0 || slot >= PUBLIC_STACK_SIZE) return;
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        items.set(slot, stack);
        storageState.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        DefaultedList<ItemStack> items = getItems();
        if (items == null || slot < 0 || slot >= PUBLIC_STACK_SIZE) return ItemStack.EMPTY;
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) storageState.markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        DefaultedList<ItemStack> items = getItems();
        if (items != null && slot >= 0 && slot < PUBLIC_STACK_SIZE) {
            ItemStack result = Inventories.removeStack(items, slot);
            if (!result.isEmpty()) storageState.markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void markDirty() { storageState.markDirty(); }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PUBLIC_STACK_SIZE; i++) {
            if (!getStack(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return storageState.contains(coreId); }

    @Override
    public void clear() {
        DefaultedList<ItemStack> items = getItems();
        if (items == null) return;
        items.clear();
        storageState.markDirty();
    }
}
