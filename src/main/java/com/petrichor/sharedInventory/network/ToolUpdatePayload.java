package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 工具切换网络包 — 客户端发送工具类型切换请求到服务端
 *
 * 流程: 客户端点击工具按钮 → 发送 ToolUpdatePayload → 服务端重建 Slot 并同步
 * 编码: 仅包含一个 int (ToolType ordinal)
 */
public record ToolUpdatePayload(int toolOrdinal) implements CustomPayload {
    public static final Id<ToolUpdatePayload> ID = new Id<>(com.petrichor.sharedInventory.SharedInventoryMod.TOOL_UPDATE_ID);
    public static final PacketCodec<RegistryByteBuf, ToolUpdatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ToolUpdatePayload::toolOrdinal,
            ToolUpdatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public ToolType getToolType() {
        ToolType[] values = ToolType.VALUES;
        return toolOrdinal() >= 0 && toolOrdinal() < values.length ? values[toolOrdinal()] : ToolType.CRAFTING;
    }

    /** 服务端处理: 更新 ScreenHandler 的 activeTool 并重建 Slot，然后同步到客户端 */
    public static void handle(ToolUpdatePayload payload, ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof SharedInventoryScreenHandler handler) {
            handler.setActiveTool(payload.getToolType());
            handler.sendContentUpdates();
        }
    }
}
