package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

/**
 * DefaultedList 的 Inventory 适配器
 *
 * 将 DefaultedList<ItemStack> 包装为 Inventory 接口，供 Slot 系统使用。
 * 消除了 FurnaceLogic / BrewingLogic / AnvilData / SmithingData 中的匿名 Inventory 重复。
 */
public class DefaultedListInventory implements Inventory {
    private final DefaultedList<ItemStack> stacks;
    private Runnable dirtyCallback;

    public DefaultedListInventory(DefaultedList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback;
    }

    @Override public int size() { return stacks.size(); }

    @Override public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        return stacks.get(slot);
    }

    @Override public ItemStack removeStack(int slot, int amount) { return Inventories.splitStack(stacks, slot, amount); }

    @Override public ItemStack removeStack(int slot) { return Inventories.removeStack(stacks, slot); }

    @Override public void setStack(int slot, ItemStack stack) { stacks.set(slot, stack); }

    @Override public void markDirty() {
        if (dirtyCallback != null) dirtyCallback.run();
    }

    @Override public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override public void clear() { stacks.clear(); }

    /** 从 NBT 读取物品列表 */
    public static void readFromNbt(DefaultedList<ItemStack> stacks, NbtCompound nbt, String key) {
        NbtList items = nbt.getList(key, 10);
        stacks.clear();
        for (int i = 0; i < items.size(); ++i) {
            NbtCompound itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot");
            if (slot >= 0 && slot < stacks.size()) {
                stacks.set(slot, ItemStack.fromNbt(itemTag));
            }
        }
    }

    /** 将物品列表写入 NBT */
    public static void writeToNbt(DefaultedList<ItemStack> stacks, NbtCompound nbt, String key) {
        NbtList items = new NbtList();
        for (int i = 0; i < stacks.size(); ++i) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                stack.writeNbt(itemTag);
                items.add(itemTag);
            }
        }
        nbt.put(key, items);
    }
}
