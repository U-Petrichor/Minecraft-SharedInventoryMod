package com.umut.sharedInventory.objects;

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

        // 确保 inventory 是经过 Mixin 改造后的对象
        if ( player instanceof SharedInventoryPlayerEntity) {
            SharedInventoryPlayerEntity shardInventoryPlayerEntity =(SharedInventoryPlayerEntity) player;
            shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().setCurrentPage((packet.newPage));
        } else {
            // 输出错误日志（可选）
            System.err.println("PlayerInventory 未实现 IinfinityModPlayerInventory，请检查 Mixin 是否生效！");
        }
    }
}
