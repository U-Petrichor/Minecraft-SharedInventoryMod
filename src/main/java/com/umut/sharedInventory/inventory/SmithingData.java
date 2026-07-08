package com.umut.sharedInventory.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
 * 注意: 此类负责数据存储和 NBT 持久化，锻造合成逻辑在 SharedInventoryScreenHandler 中实现
 */
public class SmithingData {
    /** 槽位: [0]模板 [1]材料 [2]输入 [3]输出 */
    private final DefaultedList<ItemStack> smithingStack = DefaultedList.ofSize(4, ItemStack.EMPTY);
    /** 缓存的 Inventory 包装器 */
    private final DefaultedListInventory smithingInventory = new DefaultedListInventory(smithingStack);

    public DefaultedList<ItemStack> getSmithingStack() { return this.smithingStack; }

    /** 将 smithingStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getSmithingInventory() {
        return this.smithingInventory;
    }

    /** 设置 markDirty 回调，转发到 DefaultedListInventory */
    public void setDirtyCallback(Runnable callback) {
        this.smithingInventory.setDirtyCallback(callback);
    }

    /** 从 NBT 读取锻造台数据 */
    public void readNbt(NbtCompound nbt) {
        DefaultedListInventory.readFromNbt(this.smithingStack, nbt, "SmithingItems");
    }

    /** 将锻造台数据写入 NBT */
    public void writeNbt(NbtCompound nbt) {
        DefaultedListInventory.writeToNbt(this.smithingStack, nbt, "SmithingItems");
    }
}
