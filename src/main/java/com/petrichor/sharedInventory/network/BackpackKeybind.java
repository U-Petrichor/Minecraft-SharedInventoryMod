package com.petrichor.sharedInventory.network;

import com.petrichor.sharedInventory.SharedInventoryMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

/**
 * 背包快捷键 — 客户端注册 B 键打开穿戴的背包
 *
 * 流程: 玩家按 B 键 → 检查当前无界面打开 → 发送空包到 OPEN_BACKPACK_ID 通道
 * → 服务端 OpenBackpackPacket 处理 → 检查胸甲栏是否穿戴背包 → 打开共享存储界面
 */
public class BackpackKeybind {

    /** 快捷键绑定实例 */
    private static KeyBinding openBackpackKey;

    /** 注册快捷键 (默认 B 键) 和每 tick 检测 */
    public static void register() {
        openBackpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shared_inventory_mod.open_backpack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.gameplay"
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
