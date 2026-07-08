package com.umut.sharedInventory;

import com.umut.sharedInventory.inventory.ModObjects;
import com.umut.sharedInventory.network.BackpackKeybind;
import com.umut.sharedInventory.screen.SharedInventoryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SharedInventoryModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModObjects.SHARED_INVENTORY_SCREEN_HANDLER, SharedInventoryScreen::new);
        BackpackKeybind.register();
    }
}
