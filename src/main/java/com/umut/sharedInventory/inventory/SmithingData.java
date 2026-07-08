package com.umut.sharedInventory.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

public class SmithingData {
    private final DefaultedList<ItemStack> smithingStack = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private final DefaultedListInventory smithingInventory = new DefaultedListInventory(smithingStack);

    public DefaultedList<ItemStack> getSmithingStack() { return this.smithingStack; }

    public Inventory getSmithingInventory() { return this.smithingInventory; }

    public void setDirtyCallback(Runnable callback) {
        this.smithingInventory.setDirtyCallback(callback);
    }

    public void readNbt(NbtCompound nbt) {
        DefaultedListInventory.readFromNbt(this.smithingStack, nbt, "SmithingItems");
    }

    public void writeNbt(NbtCompound nbt) {
        DefaultedListInventory.writeToNbt(this.smithingStack, nbt, "SmithingItems");
    }
}
