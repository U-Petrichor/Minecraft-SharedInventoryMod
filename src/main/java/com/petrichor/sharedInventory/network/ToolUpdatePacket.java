package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 工具切换网络包 — 客户端发送工具类型切换请求到服务端
 *
 * 流程: 客户端点击工具按钮 → 发送 ToolUpdatePacket → 服务端重建 Slot 并同步
 * 编码: 仅包含一个 int (ToolType ordinal)
 */
public class ToolUpdatePacket {
    private final ToolType toolType;

    public ToolUpdatePacket(ToolType toolType) {
        this.toolType = toolType;
    }

    public static void encode(ToolUpdatePacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.toolType.ordinal());
    }

    public static ToolUpdatePacket decode(PacketByteBuf buf) {
        int ordinal = buf.readInt();
        ToolType[] values = ToolType.VALUES;
        return new ToolUpdatePacket(ordinal >= 0 && ordinal < values.length ? values[ordinal] : ToolType.CRAFTING);
    }

    /** 服务端处理: 更新 ScreenHandler 的 activeTool 并重建 Slot，然后同步到客户端 */
    public static void handle(ToolUpdatePacket packet, ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof SharedInventoryScreenHandler handler) {
            handler.setActiveTool(packet.toolType);
            handler.sendContentUpdates();
        }
    }
}
