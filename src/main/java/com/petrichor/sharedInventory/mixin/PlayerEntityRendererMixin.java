package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.client.BackpackRenderState;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 PlayerEntityRenderer.updateRenderState — 复制背包数据到渲染状态
 *
 * 1.21.3 架构变更:
 *   - 渲染数据从实体复制到 EntityRenderState
 *   - 需要在 updateRenderState 中提取背包物品栈
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void copyBackpackToRenderState(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof SharedInventoryPlayerEntity sharedPlayer && state instanceof BackpackRenderState backpackState) {
            backpackState.setBackpackStack(sharedPlayer.shared$getBackpackStack());
        }
    }
}