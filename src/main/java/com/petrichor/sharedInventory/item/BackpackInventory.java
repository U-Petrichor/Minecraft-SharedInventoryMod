package com.petrichor.sharedInventory.item;

import com.petrichor.sharedInventory.block.SharedInventoryChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * 每次打开背包时创建的 Inventory 包装器
 *
 * 关键设计: SharedInventoryBackpack 是 Item 单例，不能持有可变状态 (并发 Bug)。
 * 因此每次玩家打开背包时，从 NBT 解析出 BlockEntity 引用，创建一个新的 BackpackInventory
 * 来代理 BlockEntity 的 publicStack。关闭界面后此对象即被丢弃。
 *
 * 数据流向: BackpackInventory → SharedInventoryChestBlockEntity.publicStack → NBT 持久化
 */
public class BackpackInventory implements Inventory {
    /** 公共背包槽位数 (4×4) */
    private static final int PUBLIC_STACK_SIZE = 16;
    /** 关联的共享核心方块实体，所有物品操作委托给它 */
    private final SharedInventoryChestBlockEntity linkedBlockEntity;

    public BackpackInventory(SharedInventoryChestBlockEntity linkedBlockEntity) {
        this.linkedBlockEntity = linkedBlockEntity;
    }

    @Override
    public int size() { return PUBLIC_STACK_SIZE; }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < PUBLIC_STACK_SIZE)
            return this.linkedBlockEntity.publicStack.get(slot);
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < PUBLIC_STACK_SIZE) {
            this.linkedBlockEntity.publicStack.set(slot, stack);
            this.linkedBlockEntity.markDirty();
        }
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.linkedBlockEntity.publicStack, slot, amount);
        if (!result.isEmpty()) this.markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot >= 0 && slot < PUBLIC_STACK_SIZE)
            return Inventories.removeStack(this.linkedBlockEntity.publicStack, slot);
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
    public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override
    public void clear() {}
}
