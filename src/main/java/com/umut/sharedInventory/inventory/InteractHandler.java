package com.umut.sharedInventory.inventory;

import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.umut.sharedInventory.item.SharedInventoryBackpack;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class InteractHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            ItemStack stack = player.getStackInHand(hand);

            if (blockEntity instanceof SharedInventoryChestBlockEntity && stack.getItem() instanceof SharedInventoryBackpack) {
                ((SharedInventoryBackpack) stack.getItem()).linkToChest(stack, (SharedInventoryChestBlockEntity) blockEntity);
                player.sendMessage(Text.translatable("message.shared_inventory_mod.interactHandler"), false);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
