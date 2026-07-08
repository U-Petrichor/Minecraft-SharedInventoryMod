package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 标签操作网络包
 * action: 0 = 设置标签, 1 = 跳转到标签页
 */
public record LabelUpdatePayload(int action, int page, String label) implements CustomPayload {
    private static final int ACTION_SET_LABEL = 0;
    private static final int ACTION_JUMP_TO_LABEL = 1;
    private static final int MAX_PAGE = 24;

    public static final Id<LabelUpdatePayload> ID = new Id<>(com.petrichor.sharedInventory.SharedInventoryMod.LABEL_UPDATE_ID);
    public static final PacketCodec<PacketByteBuf, LabelUpdatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, LabelUpdatePayload::action,
            PacketCodecs.VAR_INT, LabelUpdatePayload::page,
            PacketCodecs.STRING, LabelUpdatePayload::label,
            LabelUpdatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(LabelUpdatePayload payload, ServerPlayerEntity player) {
        if (payload.action() < ACTION_SET_LABEL || payload.action() > ACTION_JUMP_TO_LABEL) return;
        if (payload.page() < 1 || payload.page() > MAX_PAGE) return;
        if (payload.label() == null) return;
        if (player instanceof SharedInventoryPlayerEntity sharedInventoryPlayerEntity) {
            PrivateInventory inv = sharedInventoryPlayerEntity.shared$getPrivateInventory();
            switch (payload.action()) {
                case ACTION_SET_LABEL:
                    inv.setPageLabel(payload.page(), payload.label());
                    break;
                case ACTION_JUMP_TO_LABEL:
                    int targetPage = inv.findPageByLabel(payload.label());
                    if (targetPage > 0) {
                        inv.setCurrentPage(targetPage);
                    }
                    break;
            }
        }
    }
}
