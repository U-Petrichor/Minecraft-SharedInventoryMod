package com.umut.sharedInventory.network;

import com.umut.sharedInventory.inventory.ToolType;
import com.umut.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

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

    public static void handle(ToolUpdatePacket packet, ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof SharedInventoryScreenHandler handler) {
            handler.setActiveTool(packet.toolType);
            handler.sendContentUpdates();
        }
    }
}
