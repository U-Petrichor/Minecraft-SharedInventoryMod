package com.petrichor.sharedInventory.item;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * 背包装备槽位的 Inventory 实现 — 桥接 Slot 系统与 SharedInventoryPlayerEntity 的背包物品存取
 *
 * 单槽位 Inventory，slot 0 对应玩家背包装备位。
 * 由 PlayerScreenHandlerMixin 添加到 PlayerScreenHandler 的第 47 个槽位（index 46）。
 */
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
