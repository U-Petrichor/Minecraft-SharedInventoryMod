package com.umut.sharedInventory;

import com.umut.sharedInventory.objects.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
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

	public static final Identifier STATE_UPDATE_ID = new Identifier("umut", "inventory_state_update");

	public static final ItemGroup SHARED_INVENTORY_GROUP= FabricItemGroupBuilder.create(new Identifier("umut","shared_inventory_mod"))
			.icon(()->new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK))
			.appendItems(stacks->{
				stacks.add(new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK));
				stacks.add(new ItemStack(ModObjects.SHARED_INVENTORY_CHEST));
			})
			.build();

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Shared Inventory!");
		ModObjects.registerModObjects();

		interactHandler.register();

		ServerPlayNetworking.registerGlobalReceiver(
				PAGE_UPDATE_ID,
				(server, player, handler, buf, responseSender) -> {
					int newPage = buf.readInt();
					server.execute(() -> PageUpdatePacket.handle(new PageUpdatePacket(newPage), player));
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				STATE_UPDATE_ID,
				(server, player, handler, buf, responseSender) -> {
					int newPage = buf.readInt();
					int newSearchIndex = buf.readInt();
					server.execute(() -> InventoryStateUpdatePacket.handle(new InventoryStateUpdatePacket(newPage, newSearchIndex), player));
				}
		);
	}
}