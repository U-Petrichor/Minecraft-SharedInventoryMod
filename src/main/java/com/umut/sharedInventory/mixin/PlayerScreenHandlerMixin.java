package com.umut.sharedInventory.mixin;

import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.item.BackpackSlotInventory;
import com.umut.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin {

    @Unique
    private static final int BACKPACK_SLOT_INDEX = 46;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBackpackSlot(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
        if (owner instanceof SharedInventoryPlayerEntity sharedPlayer) {
            BackpackSlotInventory backpackInv = new BackpackSlotInventory(sharedPlayer);
            ScreenHandlerAccessor accessor = (ScreenHandlerAccessor) this;
            accessor.callAddSlot(new Slot(backpackInv, 0, 77, 44) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.getItem() instanceof SharedInventoryBackpack;
                }

                @Override
                public int getMaxItemCount() {
                    return 1;
                }
            });
        }
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void handleBackpackTransfer(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        PlayerScreenHandler self = (PlayerScreenHandler) (Object) this;
        if (index < 0 || index >= self.slots.size()) return;

        Slot slot = self.slots.get(index);
        if (index == BACKPACK_SLOT_INDEX) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) return;
            ItemStack copy = stack.copy();
            if (!((ScreenHandlerAccessor) this).callInsertItem(stack, 9, 45, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
            } else {
                if (stack.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    slot.markDirty();
                }
                cir.setReturnValue(copy);
            }
        } else if (slot.hasStack() && slot.getStack().getItem() instanceof SharedInventoryBackpack) {
            ItemStack stack = slot.getStack();
            ItemStack copy = stack.copy();
            if (BACKPACK_SLOT_INDEX < self.slots.size()) {
                Slot backpackSlot = self.slots.get(BACKPACK_SLOT_INDEX);
                if (!backpackSlot.hasStack() && backpackSlot.canInsert(stack)) {
                    backpackSlot.setStack(stack.split(1));
                    backpackSlot.markDirty();
                    if (stack.isEmpty()) {
                        slot.setStack(ItemStack.EMPTY);
                    } else {
                        slot.markDirty();
                    }
                    cir.setReturnValue(copy);
                }
            }
        }
    }
}
