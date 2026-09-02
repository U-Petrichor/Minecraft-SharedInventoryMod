package com.petrichor.sharedInventory.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin Accessor — 暴露 HandledScreen 的 GUI 坐标 (x, y)
 *
 * x/y 是 protected 字段，通过 @Accessor 暴露为 public，
 * 供 InventoryScreenMixin 获取背包槽位的屏幕坐标以渲染半透明图标
 */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    int getX();

    @Accessor("y")
    int getY();
}
