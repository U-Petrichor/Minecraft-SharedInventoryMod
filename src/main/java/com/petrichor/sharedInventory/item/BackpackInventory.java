package com.petrichor.sharedInventory.item;

import com.petrichor.sharedInventory.inventory.SharedCoreStorageState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.UUID;

/**
 * 每次打开背包时创建的 Inventory 包装器
 *
 * 关键设计: SharedInventoryBackpack 是 Item 单例，不能持有可变状态 (并发 Bug)。
 * 因此每次玩家打开背包时，从 NBT 解析出核心 UUID，创建一个新的 BackpackInventory
 * 来代理世界级 PersistentState 中对应核心的物品列表。关闭界面后此对象即被丢弃。
 *
 * 数据流向: BackpackInventory → SharedCoreStorageState → world/data 持久化
 */
public class BackpackInventory implements Inventory {
    /** 公共背包槽位数 (4×4) */
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
