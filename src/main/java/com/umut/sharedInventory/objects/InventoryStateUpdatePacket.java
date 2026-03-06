package com.umut.sharedInventory.objects;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

public class InventoryStateUpdatePacket {
    private final int newPage;
    private final int newSearchIndex;

    public InventoryStateUpdatePacket(int newPage, int newSearchIndex) {
        this.newPage = newPage;
        this.newSearchIndex = newSearchIndex;
    }

    public static void encode(InventoryStateUpdatePacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.newPage);
        buf.writeInt(packet.newSearchIndex);
    }

    public static InventoryStateUpdatePacket decode(PacketByteBuf buf) {
        return new InventoryStateUpdatePacket(buf.readInt(), buf.readInt());
    }

    public static void handle(InventoryStateUpdatePacket packet, ServerPlayerEntity player) {
        if (player instanceof SharedInventoryPlayerEntity shardInventoryPlayerEntity) {
            shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().setCurrentPage(packet.newPage);
            shardInventoryPlayerEntity.shared_inventory1_18_2$setSearchIndex(packet.newSearchIndex);

            // Refresh slots
            ScreenHandler handler = player.currentScreenHandler;
            if (handler instanceof SharedInventoryScreenHandler sharedHandler) {
                sharedHandler.refreshPrivateSlots();
            }
        }
    }
}