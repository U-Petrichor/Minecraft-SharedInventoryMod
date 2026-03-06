package com.umut.sharedInventory.objects;

import com.umut.sharedInventory.SharedInventoryMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModObjects {
    //基础的背包,箱子,和箱子实体
    public static final Item SHARED_INVENTORY_BACKPACK =registerItems("shared_inventory_backpack",new SharedInventoryBackpack(new Item.Settings().maxCount(1).fireproof()));

    public static final Block SHARED_INVENTORY_CHEST_BLOCK =registerBlocks("shared_inventory_chest_block",new SharedInventoryChestBlock(AbstractBlock.Settings.of(Material.STONE).requiresTool() .strength(1.5f,1200f)));
    public static final Item SHARED_INVENTORY_CHEST =registerItems("shared_inventory_chest_block",new BlockItem(SHARED_INVENTORY_CHEST_BLOCK,new Item.Settings().maxCount(64).fireproof()));
    public static final BlockEntityType<SharedInventoryChestBlockEntity> SHARED_INVENTORY_CHEST_BLOCK_ENTITY = registerBlockEntity("shared_inventory_chest_block_entity", FabricBlockEntityTypeBuilder.create(SharedInventoryChestBlockEntity::create, SHARED_INVENTORY_CHEST_BLOCK).build());

    public static final ScreenHandlerType<SharedInventoryScreenHandler> SHARED_INVENTORY_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, new Identifier(SharedInventoryMod.MOD_ID, "shared_inventory_screen_handler"), new ScreenHandlerType<>(SharedInventoryScreenHandler::new, FeatureSet.empty()));

    //物品,方块,实体对应的注册方法
    private static Item registerItems(String name, Item item){
        return Registry.register(Registries.ITEM,new Identifier(SharedInventoryMod.MOD_ID,name),item);

    }
    private static Block registerBlocks(String name, Block block){
        return Registry.register(Registries.BLOCK,new Identifier(SharedInventoryMod.MOD_ID,name),block);
    }
    private static <T extends BlockEntityType<?>> T registerBlockEntity(String name, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(SharedInventoryMod.MOD_ID, name), blockEntityType);
    }
    public static void registerModObjects(){

    }
}
