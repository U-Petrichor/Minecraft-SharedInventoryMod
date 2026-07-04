package com.petrichor.sharedInventory.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerAccessor {

    @Invoker("addSlot")
    Slot callAddSlot(Slot slot);

    @Invoker("insertItem")
    boolean callInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
}
