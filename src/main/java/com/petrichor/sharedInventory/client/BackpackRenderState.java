package com.petrichor.sharedInventory.client;

import net.minecraft.item.ItemStack;

/**
 * 背包渲染状态接口 — 扩展 PlayerEntityRenderState 以传递背包数据
 * 1.21.3 架构:
 *   - EntityRenderState 不再包含对原实体的引用
 *   - 需要通过 Mixin 扩展 PlayerEntityRenderState 并实现此接口
 *   - 在 updateRenderState 中复制背包物品栈到渲染状态
 */
public interface BackpackRenderState {
    ItemStack getBackpackStack();
    void setBackpackStack(ItemStack stack);
}