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
 *
 * 当玩家手持共享背包右键点击共享核心方块时，
 * 将共享核心的 UUID 写入背包 NBT，完成绑定。
 * 坐标和维度仍会保留，用于旧存档迁移与诊断，但远程访问不再依赖核心区块加载。
 */
public class InteractHandler {

    /** 注册右键方块事件监听 */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 只在服务端处理
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
