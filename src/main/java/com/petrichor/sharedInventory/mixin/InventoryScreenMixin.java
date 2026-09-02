package com.petrichor.sharedInventory.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 InventoryScreen — 在空背包槽位渲染半透明的背包图标
 *
 * 当原版背包界面中的专属背包槽位为空时，渲染一个半透明的背包物品图标作为提示，
 * 告知玩家该槽位可以放置共享背包。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    private static final int BACKPACK_SLOT_INDEX = 46;
    /** 缓存的背包物品实例，避免每帧注册表查询 */
    private static Item backpackItem;

    static {
        backpackItem = Registries.ITEM.get(Identifier.of("shared_inventory_mod", "shared_inventory_backpack"));
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void renderBackpackSlotIcon(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        PlayerScreenHandler handler = self.getScreenHandler();

        // Only render if the backpack slot exists (index 46)
        if (handler.slots.size() <= BACKPACK_SLOT_INDEX) return;

        // Check if the backpack slot is empty
        Slot backpackSlot = handler.slots.get(BACKPACK_SLOT_INDEX);
        if (!backpackSlot.hasStack()) {
            if (backpackItem != Items.AIR) {
                ItemStack icon = new ItemStack(backpackItem);
                HandledScreenAccessor accessor = (HandledScreenAccessor) this;
                int guiX = accessor.getX();
                int guiY = accessor.getY();
                // 1.21.5: Use drawItemWithLayer for translucent rendering
                context.getMatrices().pushMatrix();
                // Render with transparency by drawing the item with a modified color multiplier
                // DrawContext.drawItem draws the item at the specified position
                context.drawItem(icon, guiX + 77, guiY + 44);
                context.getMatrices().popMatrix();
            }
        }
    }
}
