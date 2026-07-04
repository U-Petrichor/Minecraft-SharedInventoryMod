package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

/**
 * 铁砧数据 — 从 PrivateInventory 中提取的独立铁砧存储模块
 *
 * 槽位布局 (anvilStack):
 *   [0] 输入1 (待修理/重命名物品)
 *   [1] 输入2 (牺牲物品/材料)
 *   [2] 输出 (结果)
 *
 * 注意: 此类仅负责数据存储和 NBT 持久化，不包含铁砧合成逻辑
 * 铁砧的实际合成由原版 AnvilScreenHandler 处理，此处仅作为物品暂存
 */
public class AnvilData {
    /** 槽位: [0]输入1 [1]输入2 [2]输出 */
    private final DefaultedList<ItemStack> anvilStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    /** 重命名文本 */
    private String renameText = "";

    public DefaultedList<ItemStack> getAnvilStack() { return this.anvilStack; }

    /** 将 anvilStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getAnvilInventory() {
        return new Inventory() {
            @Override public int size() { return anvilStack.size(); }
            @Override public boolean isEmpty() { return anvilStack.stream().allMatch(ItemStack::isEmpty); }
            @Override public ItemStack getStack(int slot) { return anvilStack.get(slot); }
            @Override public ItemStack removeStack(int slot, int amount) { return net.minecraft.inventory.Inventories.splitStack(anvilStack, slot, amount); }
            @Override public ItemStack removeStack(int slot) { return net.minecraft.inventory.Inventories.removeStack(anvilStack, slot); }
            @Override public void setStack(int slot, ItemStack stack) { anvilStack.set(slot, stack); }
            @Override public void markDirty() {}
            @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
            @Override public void clear() { anvilStack.clear(); }
        };
    }

    public String getRenameText() { return this.renameText; }

    public void setRenameText(String text) { this.renameText = text != null ? text : ""; }

    /** 从 NBT 读取铁砧数据 */
    public void readNbt(NbtCompound nbt) {
        this.renameText = nbt.getString("AnvilRenameText");
        NbtList items = nbt.getList("AnvilItems", 10);
        this.anvilStack.clear();
        for (int i = 0; i < items.size(); ++i) {
            NbtCompound itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot");
            if (slot >= 0 && slot < this.anvilStack.size()) {
                this.anvilStack.set(slot, ItemStack.fromNbt(itemTag));
            }
        }
    }

    /** 将铁砧数据写入 NBT */
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("AnvilRenameText", this.renameText);
        NbtList items = new NbtList();
        for (int i = 0; i < this.anvilStack.size(); ++i) {
            ItemStack stack = this.anvilStack.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                stack.writeNbt(itemTag);
                items.add(itemTag);
            }
        }
        nbt.put("AnvilItems", items);
    }
}
