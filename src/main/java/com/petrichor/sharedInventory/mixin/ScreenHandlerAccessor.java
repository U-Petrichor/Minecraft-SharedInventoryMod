package com.petrichor.sharedInventory.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin Accessor — 暴露 ScreenHandler 的内部方法和字段
 *
 * addSlot() / insertItem(): protected 方法，供 PlayerScreenHandlerMixin 使用
 * trackedStacks / previousTrackedStacks: private 字段，供 SharedInventoryScreenHandler 重建 Slot 时同步清理
 */
@Mixin(ScreenHandler.class)
public interface ScreenHandlerAccessor {

    @Invoker("addSlot")
    Slot callAddSlot(Slot slot);

    @Invoker("insertItem")
    boolean callInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);

    @Accessor("trackedStacks")
    DefaultedList<ItemStack> getTrackedStacks();

    @Accessor("previousTrackedStacks")
    DefaultedList<ItemStack> getPreviousTrackedStacks();
}
