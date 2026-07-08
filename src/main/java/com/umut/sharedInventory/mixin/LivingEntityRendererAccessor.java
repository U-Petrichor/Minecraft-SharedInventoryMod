package com.umut.sharedInventory.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin Accessor — 暴露 LivingEntityRenderer.addFeature() 方法
 *
 * addFeature() 是 protected 方法，定义在 LivingEntityRenderer (PlayerEntityRenderer 的父类) 中。
 * 通过 @Invoker 暴露为 public，供 PlayerEntityRendererMixin 注册 BackpackFeatureRenderer
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    /** 调用 LivingEntityRenderer.addFeature() 添加渲染特性 */
    @Invoker("addFeature")
    boolean callAddFeature(FeatureRenderer<?, ?> featureRenderer);
}
