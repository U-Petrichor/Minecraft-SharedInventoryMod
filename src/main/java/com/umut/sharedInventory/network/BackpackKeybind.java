package com.umut.sharedInventory.network;

import com.umut.sharedInventory.SharedInventoryMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public class BackpackKeybind {

    private static KeyBinding openBackpackKey;

    public static void register() {
        openBackpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shared_inventory_mod.open_backpack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.shared_inventory_mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBackpackKey.wasPressed()) {
                if (client.player != null && client.player.currentScreenHandler == client.player.playerScreenHandler) {
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    ClientPlayNetworking.send(SharedInventoryMod.OPEN_BACKPACK_ID, buf);
                }
            }
        });
    }
}
