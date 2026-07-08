package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.client.BackpackFeatureRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 PlayerEntityRenderer — 注册背包 3D 渲染特性
 *
 * 在 PlayerEntityRenderer 构造完成后 (RETURN)，通过 LivingEntityRendererAccessor
 * 调用 addFeature() 添加 BackpackFeatureRenderer，使穿着背包的玩家背上渲染物品模型。
 *
 * @SuppressWarnings("unchecked"): 将 (Object)this 强转为 FeatureRendererContext 是安全的，
 * 因为 PlayerEntityRenderer 必然是 FeatureRendererContext<AbstractClientPlayerEntity, ...> 的子类
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void addBackpackFeature(CallbackInfo ci) {
        FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context =
                (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) (Object) this;
        ((LivingEntityRendererAccessor) (Object) this).callAddFeature(
                new BackpackFeatureRenderer(context)
        );
    }
}
