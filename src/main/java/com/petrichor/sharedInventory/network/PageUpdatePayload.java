package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 翻页网络包 — 客户端发送翻页请求到服务端
 *
 * 流程: 客户端点击翻页按钮 → 发送 PageUpdatePayload → 服务端更新 PrivateInventory.currentPage
 * 编码: 仅包含一个 int (目标页码)
 */
public record PageUpdatePayload(int newPage) implements CustomPayload {
    public static final Id<PageUpdatePayload> ID = new Id<>(com.petrichor.sharedInventory.SharedInventoryMod.PAGE_UPDATE_ID);
    public static final PacketCodec<RegistryByteBuf, PageUpdatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PageUpdatePayload::newPage,
            PageUpdatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    /** 服务端处理: 更新玩家的私人背包当前页码 (范围校验: 1 ~ MAX_PAGE)，然后同步槽位到客户端 */
    public static void handle(PageUpdatePayload payload, ServerPlayerEntity player) {
        if (player instanceof SharedInventoryPlayerEntity sharedInventoryPlayerEntity) {
            PrivateInventory inv = sharedInventoryPlayerEntity.shared$getPrivateInventory();
            int page = Math.max(1, Math.min(payload.newPage(), inv.getPrivateStackMaxPage()));
            inv.setCurrentPage(page);
            player.currentScreenHandler.sendContentUpdates();
        }
    }
}
