package com.umut.sharedInventory;

import com.umut.sharedInventory.inventory.ModObjects;
import com.umut.sharedInventory.network.BackpackKeybind;
import com.umut.sharedInventory.screen.SharedInventoryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

/**
 * 客户端入口 — Fabric ClientModInitializer 实现
 *
 * 注册内容:
 *   1. ScreenHandler → Screen 映射 (共享存储界面渲染)
 *   2. 背包快捷键绑定 (穿戴后按快捷键打开背包)
 */
public class SharedInventoryModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册共享存储界面的 Screen 渲染器
        ScreenRegistry.register(ModObjects.SHARED_INVENTORY_SCREEN_HANDLER, SharedInventoryScreen::new);

        // 注册快捷键 (默认按键打开穿戴的背包)
        BackpackKeybind.register();
    }
}
