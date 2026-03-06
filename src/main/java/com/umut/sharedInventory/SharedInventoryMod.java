package com.umut.sharedInventory;

import com.umut.sharedInventory.objects.ModObjects;
import com.umut.sharedInventory.objects.PageUpdatePacket;
import com.umut.sharedInventory.objects.interactHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SharedInventoryMod implements ModInitializer {
	public static final String MOD_ID = "shared_inventory_mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier PAGE_UPDATE_ID = new Identifier("umut", "shared_inventory_mod");
	public static final Identifier SET_CHEST_PASSWORD_PACKET_ID = new Identifier("umut", "shared_inventory_mod");

	public static final ItemGroup SHARED_INVENTORY_GROUP=  FabricItemGroup.builder(new Identifier("umut","shared_inventory_mod"))
			.icon(()->new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK))
			.build();

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Shared Inventory!");
		ModObjects.registerModObjects();
		interactHandler.register();
		ItemGroupEvents.modifyEntriesEvent(SHARED_INVENTORY_GROUP).register(content -> {
			content.add(ModObjects.SHARED_INVENTORY_BACKPACK);
			content.add(ModObjects.SHARED_INVENTORY_CHEST);
		});

		ServerPlayNetworking.registerGlobalReceiver(
				PAGE_UPDATE_ID,
				(server, player, handler, buf, responseSender) -> {
					int newPage = buf.readInt();
					server.execute(() -> PageUpdatePacket.handle(new PageUpdatePacket(newPage), player));
				}
		);
	}
}