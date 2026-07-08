package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 标签操作网络包
 * action: 0 = 设置标签, 1 = 跳转到标签页
 */
public class LabelUpdatePacket {
    /** 操作类型常量 */
    private static final int ACTION_SET_LABEL = 0;
    private static final int ACTION_JUMP_TO_LABEL = 1;
    /** 页码范围上限 */
    private static final int MAX_PAGE = 24;

    private final int action;
    /** 目标页码 (设置标签时使用) */
    private final int page;
    /** 标签文本 */
    private final String label;

    public LabelUpdatePacket(int action, int page, String label) {
        this.action = action;
        this.page = page;
        this.label = label;
    }

    public static void encode(LabelUpdatePacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.action);
        buf.writeInt(packet.page);
        buf.writeString(packet.label != null ? packet.label : "");
    }

    public static LabelUpdatePacket decode(PacketByteBuf buf) {
        int action = buf.readInt();
        int page = buf.readInt();
        String label = buf.readString(64);
        return new LabelUpdatePacket(action, page, label);
    }

    public static void handle(LabelUpdatePacket packet, ServerPlayerEntity player) {
        if (packet.action < ACTION_SET_LABEL || packet.action > ACTION_JUMP_TO_LABEL) return;
        if (packet.page < 1 || packet.page > MAX_PAGE) return;
        if (packet.label == null) return;
        if (player instanceof SharedInventoryPlayerEntity sharedInventoryPlayerEntity) {
            PrivateInventory inv = sharedInventoryPlayerEntity.shared$getPrivateInventory();
            switch (packet.action) {
                case ACTION_SET_LABEL:
                    inv.setPageLabel(packet.page, packet.label);
                    break;
                case ACTION_JUMP_TO_LABEL:
                    int targetPage = inv.findPageByLabel(packet.label);
                    if (targetPage > 0) {
                        inv.setCurrentPage(targetPage);
                    }
                    break;
            }
        }
    }
}
