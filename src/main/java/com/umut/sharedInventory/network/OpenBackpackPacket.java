package com.umut.sharedInventory.network;

import com.umut.sharedInventory.SharedInventoryMod;
import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.item.BackpackInventory;
import com.umut.sharedInventory.item.SharedInventoryBackpack;
import com.umut.sharedInventory.screen.SharedInventoryScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
            player.sendMessage(Text.translatable("message.shared_inventory_mod.backpack.not_equipped"), true);
            return;
        }

        SharedInventoryChestBlockEntity blockEntity = SharedInventoryBackpack.readLinkedBlockEntity(
                backpackStack, player.getServer());

        if (blockEntity == null) {
            player.sendMessage(Text.translatable("message.shared_inventory_mod.shared_inventory_backpack.message1"), true);
            return;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
                @Override
                public net.minecraft.text.Text getDisplayName() {
                    return Text.translatable("item.shared_inventory_mod.shared_inventory_backpack");
                }

                @Override
                public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity player) {
                    return new SharedInventoryScreenHandler(syncId, inv, new BackpackInventory(blockEntity));
                }
            });
        }
    }
}
