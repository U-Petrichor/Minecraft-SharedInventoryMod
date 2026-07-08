package com.umut.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

/**
 * 铁砧数据 — 从 PrivateInventory 中提取的独立铁砧存储模块
 *
 * 槽位布局 (anvilStack):
 *   [0] 输入1 (待修理/重命名物品)
 *   [1] 输入2 (牺牲物品/材料)
 *   [2] 输出 (结果)
 *
 * 注意: 此类负责数据存储和 NBT 持久化，铁砧合成逻辑在 SharedInventoryScreenHandler 中实现
 */
public class AnvilData {
    /** 槽位: [0]输入1 [1]输入2 [2]输出 */
    private final DefaultedList<ItemStack> anvilStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    /** 缓存的 Inventory 包装器 */
    private final DefaultedListInventory anvilInventory = new DefaultedListInventory(anvilStack);
    /** 重命名文本 */
    private String renameText = "";
    /** 修理费用 (经验等级) */
    private int repairCost;

    public DefaultedList<ItemStack> getAnvilStack() { return this.anvilStack; }

    /** 将 anvilStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getAnvilInventory() {
        return this.anvilInventory;
    }

    /** 设置 markDirty 回调，转发到 DefaultedListInventory */
    public void setDirtyCallback(Runnable callback) {
        this.anvilInventory.setDirtyCallback(callback);
    }

    public String getRenameText() { return this.renameText; }

    public void setRenameText(String text) { this.renameText = text != null ? text : ""; }

    public int getRepairCost() { return this.repairCost; }

    public void setRepairCost(int cost) { this.repairCost = cost; }

    /** 从 NBT 读取铁砧数据 */
    public void readNbt(NbtCompound nbt) {
        this.renameText = nbt.getString("AnvilRenameText");
        this.repairCost = nbt.getInt("AnvilRepairCost");
        DefaultedListInventory.readFromNbt(this.anvilStack, nbt, "AnvilItems");
    }

    /** 将铁砧数据写入 NBT */
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("AnvilRenameText", this.renameText);
        nbt.putInt("AnvilRepairCost", this.repairCost);
        DefaultedListInventory.writeToNbt(this.anvilStack, nbt, "AnvilItems");
    }
}
