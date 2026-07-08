package com.umut.sharedInventory.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ItemButtonWidget extends ButtonWidget {

    private static final int ICON_SIZE = 14;
    private static final float ICON_SCALE = (float) ICON_SIZE / 16;

    private final ItemStack icon;

    public ItemButtonWidget(int x, int y, int width, int height, Item iconItem, PressAction onPress) {
        super(x, y, width, height, Text.literal(""), onPress);
        this.icon = new ItemStack(iconItem);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);

        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        int iconX = this.x + (this.width - 16) / 2;
        int iconY = this.y + (this.height - 16) / 2;

        float brightness = this.isHovered() ? 1.0F : 0.85F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);

        itemRenderer.renderInGui(icon, iconX, iconY);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
