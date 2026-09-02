package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.item.BackpackInventory;
import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;

/**
 * 打开背包网络包 — 服务端处理客户端按 B 键打开背包的请求
 *
 * 流程: 客户端按键 → 发送 OPEN_BACKPACK_ID 包 → 服务端查找玩家装备的背包 →
 * 读取背包绑定的 SharedInventoryChestBlockEntity → 打开 SharedInventoryScreenHandler
 * 若未装备背包或未绑定共享核心，返回提示消息
 */
public class OpenBackpackPacket {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SharedInventoryMod.OPEN_BACKPACK_ID,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> handleOpenBackpack(player));
                }
        );
    }

    private static void handleOpenBackpack(PlayerEntity player) {
        ItemStack backpackStack = SharedInventoryBackpack.findBackpackStack(player);

        if (backpackStack == null || !(backpackStack.getItem() instanceof SharedInventoryBackpack)) {
            player.sendMessage(new TranslatableText("message.shared_inventory_mod.backpack.not_equipped"), true);
            return;
        }

        BackpackInventory inventory = SharedInventoryBackpack.createLinkedInventory(
                backpackStack, player.getServer());

        if (inventory == null) {
            player.sendMessage(new TranslatableText("message.shared_inventory_mod.shared_inventory_backpack.message1"), true);
            return;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
                @Override
                public net.minecraft.text.Text getDisplayName() {
                    return new TranslatableText("item.shared_inventory_mod.shared_inventory_backpack");
                }

                @Override
                public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity player) {
                    return new SharedInventoryScreenHandler(syncId, inv, inventory);
                }
            });
        }
    }
}
