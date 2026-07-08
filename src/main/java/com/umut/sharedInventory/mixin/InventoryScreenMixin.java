package com.umut.sharedInventory.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    private static final int BACKPACK_SLOT_INDEX = 46;
    private static Item backpackItem;

    static {
        backpackItem = Registries.ITEM.get(new Identifier("shared_inventory_mod", "shared_inventory_backpack"));
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void renderBackpackSlotIcon(MatrixStack matrices, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        PlayerScreenHandler handler = self.getScreenHandler();

        if (handler.slots.size() <= BACKPACK_SLOT_INDEX) return;

        Slot backpackSlot = handler.slots.get(BACKPACK_SLOT_INDEX);
        if (!backpackSlot.hasStack()) {
            if (backpackItem != Items.AIR) {
                ItemStack icon = new ItemStack(backpackItem);
                HandledScreenAccessor accessor = (HandledScreenAccessor) this;
                int guiX = accessor.getX();
                int guiY = accessor.getY();
                ItemRenderer itemRenderer = net.minecraft.client.MinecraftClient.getInstance().getItemRenderer();
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
                itemRenderer.renderInGui(icon, guiX + 77, guiY + 44);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
            }
        }
    }
}
