package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * 酿造逻辑 — 从 PrivateInventory 中提取的独立酿造模块
 *
 * 槽位布局 (brewingStack):
 *   [0-2] 三个药水瓶槽位
 *   [3]   酿造材料
 *   [4]   烈焰粉 (燃料)
 *
 * 酿造流程: tick() 每游戏刻调用 → 消耗烈焰粉 → 酿造 400 tick → 通过 BrewingRecipeRegistry 验证配方后产出
 * 修复说明: canBrew() 使用 BrewingRecipeRegistry.craft() 验证配方合法性，而非仅检查药水瓶是否存在
 */
public class BrewingLogic {
    /** 槽位: [0-2]药水瓶 [3]材料 [4]烈焰粉 */
    private final DefaultedList<ItemStack> brewingStack = DefaultedList.ofSize(5, ItemStack.EMPTY);
    /** 当前剩余酿造时间 (酿造一次 400 tick) */
    private int brewTime;
    /** 剩余烈焰粉份数 (每份可酿造一次) */
    private int brewFuel;

    /** 每游戏刻执行: 推进酿造倒计时，完成时产出药水并消耗烈焰粉 */
    public void tick(World world) {
        if (world == null || world.isClient) return;
        ItemStack ingredient = this.brewingStack.get(3);
        if (this.brewTime > 0) {
            if (ingredient.isEmpty() || !canBrew()) {
                this.brewTime = 0;
                return;
            }
            --this.brewTime;
            if (this.brewTime == 0) {
                craftBrew();
                if (this.brewFuel > 0) {
                    --this.brewFuel;
                }
            }
        } else {
            if (!ingredient.isEmpty() && this.brewFuel > 0 && canBrew()) {
                this.brewTime = 400;
            }
        }
    }

    public DefaultedList<ItemStack> getBrewingStack() { return this.brewingStack; }

    /** 将 brewingStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getBrewingInventory() {
        return new Inventory() {
            @Override public int size() { return brewingStack.size(); }
            @Override public boolean isEmpty() { return brewingStack.stream().allMatch(ItemStack::isEmpty); }
            @Override public ItemStack getStack(int slot) { return brewingStack.get(slot); }
            @Override public ItemStack removeStack(int slot, int amount) { return net.minecraft.inventory.Inventories.splitStack(brewingStack, slot, amount); }
            @Override public ItemStack removeStack(int slot) { return net.minecraft.inventory.Inventories.removeStack(brewingStack, slot); }
            @Override public void setStack(int slot, ItemStack stack) { brewingStack.set(slot, stack); }
            @Override public void markDirty() {}
            @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
            @Override public void clear() { brewingStack.clear(); }
        };
    }

    /** 从 NBT 读取酿造状态 */
    public void readNbt(NbtCompound nbt) {
        this.brewTime = nbt.getShort("BrewTime");
        this.brewFuel = nbt.getShort("BrewFuel");
        NbtList items = nbt.getList("BrewingItems", 10);
        this.brewingStack.clear();
        for (int i = 0; i < items.size(); ++i) {
            NbtCompound itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot");
            if (slot >= 0 && slot < this.brewingStack.size()) {
                this.brewingStack.set(slot, ItemStack.fromNbt(itemTag));
            }
        }
    }

    /** 将酿造状态写入 NBT */
    public void writeNbt(NbtCompound nbt) {
        nbt.putShort("BrewTime", (short) this.brewTime);
        nbt.putShort("BrewFuel", (short) this.brewFuel);
        NbtList items = new NbtList();
        for (int i = 0; i < this.brewingStack.size(); ++i) {
            ItemStack stack = this.brewingStack.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                stack.writeNbt(itemTag);
                items.add(itemTag);
            }
        }
        nbt.put("BrewingItems", items);
    }

    /** 检查是否可以酿造: 至少一个药水瓶槽位有合法配方结果 */
    private boolean canBrew() {
        ItemStack ingredient = this.brewingStack.get(3);
        if (ingredient.isEmpty()) return false;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty()) {
                ItemStack result = BrewingRecipeRegistry.craft(ingredient, bottle);
                if (!result.isEmpty()) return true;
            }
        }
        return false;
    }

    /** 执行酿造: 用材料转换药水瓶，消耗材料 (有残留物则替换) */
    private void craftBrew() {
        ItemStack ingredient = this.brewingStack.get(3);
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty()) {
                ItemStack result = BrewingRecipeRegistry.craft(ingredient, bottle);
                if (!result.isEmpty()) {
                    this.brewingStack.set(i, result);
                }
            }
        }
        if (ingredient.getItem().hasRecipeRemainder()) {
            this.brewingStack.set(3, new ItemStack(ingredient.getItem().getRecipeRemainder()));
        } else {
            ingredient.decrement(1);
        }
    }
}
