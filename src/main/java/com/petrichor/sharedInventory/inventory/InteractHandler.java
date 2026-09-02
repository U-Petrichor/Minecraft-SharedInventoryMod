package com.petrichor.sharedInventory.inventory;

import com.petrichor.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * 交互事件处理器 — 处理背包与共享核心的绑定
 * 当玩家手持共享背包右键点击共享核心方块时，
 * 将共享核心的坐标写入背包的 NBT，完成绑定。
 * 绑定后背包才能通过 NBT 坐标找到对应的 BlockEntity 来存取公共物品。
 */
public class InteractHandler {

    /** 注册右键方块事件监听 */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 只在服务端处理
            if (world.isClient()) return ActionResult.PASS;

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