package com.umut.sharedInventory.inventory;

import net.minecraft.item.ItemStack;

public interface SharedInventoryPlayerEntity {
    PrivateInventory shared$getPrivateInventory();
    void shared$setPrivateInventory(PrivateInventory privateInventory);
    ItemStack shared$getBackpackStack();
    void shared$setBackpackStack(ItemStack stack);
}
