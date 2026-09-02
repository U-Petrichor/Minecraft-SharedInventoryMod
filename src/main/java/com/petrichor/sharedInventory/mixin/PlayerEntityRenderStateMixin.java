package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.client.BackpackRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin 扩展 PlayerEntityRenderState — 添加背包物品栈字段
 *
 * 1.21.3 架构:
 *   - FeatureRenderer 接收 EntityRenderState 而非 Entity
 *   - 需要在渲染状态中存储要渲染的数据
 *
 * 通过实现 BackpackRenderState 接口，使 FeatureRenderer 可以访问背包数据
 */
@Mixin(net.minecraft.client.render.entity.state.PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements BackpackRenderState {

    @Unique
    private ItemStack shared_inventory$backpackStack = ItemStack.EMPTY;

    @Unique
    private final ItemRenderState shared_inventory$backpackItemRenderState = new ItemRenderState();

    @Override
    public ItemStack getBackpackStack() {
        return this.shared_inventory$backpackStack;
    }

    @Override
    public void setBackpackStack(ItemStack stack) {
        this.shared_inventory$backpackStack = stack;
    }

    @Override
    public ItemRenderState getBackpackItemRenderState() {
        return this.shared_inventory$backpackItemRenderState;
    }
}
