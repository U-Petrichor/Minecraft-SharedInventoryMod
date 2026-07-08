package com.umut.sharedInventory.network;

import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.inventory.PrivateInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class PageUpdatePacket {
    private final int newPage;

    public PageUpdatePacket(int newPage) {
        this.newPage = newPage;
    }

    public static void encode(PageUpdatePacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.newPage);
    }

    public static PageUpdatePacket decode(PacketByteBuf buf) {
        return new PageUpdatePacket(buf.readInt());
    }

    public static void handle(PageUpdatePacket packet, ServerPlayerEntity player) {
        if (player instanceof SharedInventoryPlayerEntity sharedInventoryPlayerEntity) {
            PrivateInventory inv = sharedInventoryPlayerEntity.shared$getPrivateInventory();
            int page = Math.max(1, Math.min(packet.newPage, inv.getPrivateStackMaxPage()));
            inv.setCurrentPage(page);
            player.currentScreenHandler.sendContentUpdates();
        }
    }
}
