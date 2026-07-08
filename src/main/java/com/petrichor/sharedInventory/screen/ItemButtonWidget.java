package com.petrichor.sharedInventory.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * 物品图标按钮 — 在按钮背景上渲染缩放的方块/物品图标，替代文字标签
 *
 * 用于工具切换栏: 合成台/熔炉/酿造台/铁砧/锻造台
 */
public class ItemButtonWidget extends ButtonWidget {

    private static final int ICON_SIZE = 14;
    private static final float ICON_SCALE = (float) ICON_SIZE / 16;

    private final ItemStack icon;

    public ItemButtonWidget(int x, int y, int width, int height, Item iconItem, PressAction onPress) {
        super(x, y, width, height, Text.of(""), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.icon = new ItemStack(iconItem);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // 绘制原版按钮背景
        super.renderButton(matrices, mouseX, mouseY, delta);

        // 在按钮中央绘制物品图标 (16px 原始大小)
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        int iconX = this.getX() + (this.width - 16) / 2;
        int iconY = this.getY() + (this.height - 16) / 2;

        float brightness = this.isHovered() ? 1.0F : 0.85F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);

        itemRenderer.renderInGui(matrices, icon, iconX, iconY);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
