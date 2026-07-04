package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 翻页网络包 — 客户端发送翻页请求到服务端
 *
 * 流程: 客户端点击翻页按钮 → 发送 PageUpdatePacket → 服务端更新 PrivateInventory.currentPage
 * 编码: 仅包含一个 int (目标页码)
 */
public class PageUpdatePacket {
    /** 目标页码 (1-based) */
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

    /** 服务端处理: 更新玩家的私人背包当前页码 */
    public static void handle(PageUpdatePacket packet, ServerPlayerEntity player) {
        // 确保 inventory 是经过 Mixin 改造后的对象
        if (player instanceof SharedInventoryPlayerEntity sharedInventoryPlayerEntity) {
            sharedInventoryPlayerEntity.shared$getPrivateInventory().setCurrentPage((packet.newPage));
        }
    }
}
