package com.umut.sharedInventory.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

public class AnvilData {
    private final DefaultedList<ItemStack> anvilStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final DefaultedListInventory anvilInventory = new DefaultedListInventory(anvilStack);
    private String renameText = "";
    private int repairCost;

    public DefaultedList<ItemStack> getAnvilStack() { return this.anvilStack; }

    public Inventory getAnvilInventory() { return this.anvilInventory; }

    public void setDirtyCallback(Runnable callback) {
        this.anvilInventory.setDirtyCallback(callback);
    }

    public String getRenameText() { return this.renameText; }

    public void setRenameText(String text) { this.renameText = text != null ? text : ""; }

    public int getRepairCost() { return this.repairCost; }

    public void setRepairCost(int cost) { this.repairCost = cost; }

    public void readNbt(NbtCompound nbt) {
        this.renameText = nbt.getString("AnvilRenameText");
        this.repairCost = nbt.getInt("AnvilRepairCost");
        DefaultedListInventory.readFromNbt(this.anvilStack, nbt, "AnvilItems");
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putString("AnvilRenameText", this.renameText);
        nbt.putInt("AnvilRepairCost", this.repairCost);
        DefaultedListInventory.writeToNbt(this.anvilStack, nbt, "AnvilItems");
    }
}
