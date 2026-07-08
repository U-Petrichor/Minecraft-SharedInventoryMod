package com.umut.sharedInventory.inventory;

import net.minecraft.item.ItemStack;

/**
 * Mixin 接口 — 由 SharedInventoryPlayerEntityMixin 实现
 *
 * 通过 Mixin 注入到 PlayerEntity，使每个玩家实例都能持有 PrivateInventory 和背包装备。
 */
public interface SharedInventoryPlayerEntity {
    /** 获取该玩家的私人背包 */
    PrivateInventory shared$getPrivateInventory();
    /** 设置该玩家的私人背包 (用于死亡保留数据: copyFrom) */
    void shared$setPrivateInventory(PrivateInventory privateInventory);
    /** 获取该玩家装备的背包物品 */
    ItemStack shared$getBackpackStack();
    /** 设置该玩家装备的背包物品 */
    void shared$setBackpackStack(ItemStack stack);
}
