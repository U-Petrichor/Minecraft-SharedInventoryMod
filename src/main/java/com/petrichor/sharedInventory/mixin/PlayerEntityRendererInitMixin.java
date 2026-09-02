package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.client.BackpackFeatureRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 PlayerEntityRenderer 构造函数 — 注册背包渲染特性
 *
 * 在 PlayerEntityRenderer 构造完成后，通过 LivingEntityRendererAccessor
 * 调用 addFeature() 添加 BackpackFeatureRenderer
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererInitMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBackpackFeature(CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<?, PlayerEntityRenderState, PlayerEntityModel> renderer =
                (LivingEntityRenderer<?, PlayerEntityRenderState, PlayerEntityModel>) (Object) this;
        ((LivingEntityRendererAccessor) (Object) this).callAddFeature(
                new BackpackFeatureRenderer(renderer)
        );
    }
}