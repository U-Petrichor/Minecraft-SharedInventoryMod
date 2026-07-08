package com.umut.sharedInventory.inventory;

import com.umut.sharedInventory.SharedInventoryMod;
import com.umut.sharedInventory.item.SharedInventoryBackpack;
import com.umut.sharedInventory.block.SharedInventoryChestBlock;
import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.umut.sharedInventory.screen.SharedInventoryScreenHandler;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModObjects {
    public static final Item SHARED_INVENTORY_BACKPACK = registerItem("shared_inventory_backpack", new SharedInventoryBackpack(new Item.Settings().maxCount(1).fireproof()));

    public static final Block SHARED_INVENTORY_CHEST_BLOCK = registerBlock("shared_inventory_chest_block", new SharedInventoryChestBlock(AbstractBlock.Settings.of(Material.STONE).requiresTool().strength(1.5f, 1200f).nonOpaque()));
    public static final Item SHARED_INVENTORY_CHEST = registerItem("shared_inventory_chest_block", new BlockItem(SHARED_INVENTORY_CHEST_BLOCK, new Item.Settings().maxCount(64).fireproof()));
    public static final BlockEntityType<SharedInventoryChestBlockEntity> SHARED_INVENTORY_CHEST_BLOCK_ENTITY = registerBlockEntity("shared_inventory_chest_block_entity", FabricBlockEntityTypeBuilder.create(SharedInventoryChestBlockEntity::create, SHARED_INVENTORY_CHEST_BLOCK).build());

    public static final ScreenHandlerType<SharedInventoryScreenHandler> SHARED_INVENTORY_SCREEN_HANDLER = Registry.register(Registry.SCREEN_HANDLER, new Identifier(SharedInventoryMod.MOD_ID, "shared_inventory_screen_handler"), new ScreenHandlerType<>(SharedInventoryScreenHandler::new));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(SharedInventoryMod.MOD_ID, name), item);
    }
    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registry.BLOCK, new Identifier(SharedInventoryMod.MOD_ID, name), block);
    }
    private static <T extends BlockEntityType<?>> T registerBlockEntity(String name, T blockEntityType) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(SharedInventoryMod.MOD_ID, name), blockEntityType);
    }
    public static void registerModObjects() {
    }
}
