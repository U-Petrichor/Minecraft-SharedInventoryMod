package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

/**
 * 锻造台数据 — 从 PrivateInventory 中提取的独立锻造台存储模块
 *
 * 槽位布局 (smithingStack):
 *   [0] 锻造模板
 *   [1] 材料 (下界合金锭等)
 *   [2] 输入物品 (钻石装备)
 *   [3] 输出 (锻造结果)
 *
 * 注意: 此类仅负责数据存储和 NBT 持久化，不包含锻造合成逻辑
 * 锻造的实际合成由原版 SmithingScreenHandler 处理，此处仅作为物品暂存
 */
public class SmithingData {
    /** 槽位: [0]模板 [1]材料 [2]输入 [3]输出 */
    private final DefaultedList<ItemStack> smithingStack = DefaultedList.ofSize(4, ItemStack.EMPTY);

    public DefaultedList<ItemStack> getSmithingStack() { return this.smithingStack; }

    /** 将 smithingStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getSmithingInventory() {
        return new Inventory() {
            @Override public int size() { return smithingStack.size(); }
            @Override public boolean isEmpty() { return smithingStack.stream().allMatch(ItemStack::isEmpty); }
            @Override public ItemStack getStack(int slot) { return smithingStack.get(slot); }
            @Override public ItemStack removeStack(int slot, int amount) { return net.minecraft.inventory.Inventories.splitStack(smithingStack, slot, amount); }
            @Override public ItemStack removeStack(int slot) { return net.minecraft.inventory.Inventories.removeStack(smithingStack, slot); }
            @Override public void setStack(int slot, ItemStack stack) { smithingStack.set(slot, stack); }
            @Override public void markDirty() {}
            @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
            @Override public void clear() { smithingStack.clear(); }
        };
    }

    /** 从 NBT 读取锻造台数据 */
    public void readNbt(NbtCompound nbt) {
        NbtList items = nbt.getList("SmithingItems", 10);
        this.smithingStack.clear();
        for (int i = 0; i < items.size(); ++i) {
            NbtCompound itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot");
            if (slot >= 0 && slot < this.smithingStack.size()) {
                this.smithingStack.set(slot, ItemStack.fromNbt(itemTag));
            }
        }
    }

    /** 将锻造台数据写入 NBT */
    public void writeNbt(NbtCompound nbt) {
        NbtList items = new NbtList();
        for (int i = 0; i < this.smithingStack.size(); ++i) {
            ItemStack stack = this.smithingStack.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                stack.writeNbt(itemTag);
                items.add(itemTag);
            }
        }
        nbt.put("SmithingItems", items);
    }
}
