package com.umut.sharedInventory;

import com.umut.sharedInventory.item.*;
import com.umut.sharedInventory.inventory.*;
import com.umut.sharedInventory.network.*;
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

    public static final Identifier PAGE_UPDATE_ID = new Identifier(MOD_ID, "page_update");
    public static final Identifier LABEL_UPDATE_ID = new Identifier(MOD_ID, "label_update");
    public static final Identifier OPEN_BACKPACK_ID = new Identifier(MOD_ID, "open_backpack");
    public static final Identifier TOOL_UPDATE_ID = new Identifier(MOD_ID, "tool_update");

    public static final ItemGroup SHARED_INVENTORY_GROUP = FabricItemGroupBuilder.create(new Identifier(MOD_ID, "shared_inventory_mod"))
            .icon(() -> new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK))
            .appendItems(stacks -> {
                stacks.add(new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK));
                stacks.add(new ItemStack(ModObjects.SHARED_INVENTORY_CHEST));
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("Shared Inventory Mod initialized");
        ModObjects.registerModObjects();

        InteractHandler.register();

        OpenBackpackPacket.register();

        ServerPlayNetworking.registerGlobalReceiver(
                PAGE_UPDATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    int newPage = buf.readInt();
                    server.execute(() -> PageUpdatePacket.handle(new PageUpdatePacket(newPage), player));
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                LABEL_UPDATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    LabelUpdatePacket packet = LabelUpdatePacket.decode(buf);
                    server.execute(() -> LabelUpdatePacket.handle(packet, player));
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TOOL_UPDATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    ToolUpdatePacket packet = ToolUpdatePacket.decode(buf);
                    server.execute(() -> ToolUpdatePacket.handle(packet, player));
                }
        );
    }
}
