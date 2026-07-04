package com.petrichor.sharedInventory.inventory;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import com.petrichor.sharedInventory.block.SharedInventoryChestBlock;
import com.petrichor.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
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

/**
 * 模组对象注册中心 — 统一注册所有 Item、Block、BlockEntity、ScreenHandler
 *
 * 注册顺序: Block → BlockItem → BlockEntity → ScreenHandler
 * 调用 registerModObjects() 触发类加载，完成所有静态注册
 */
public class ModObjects {
    //=== 物品注册 ===
    /** 共享背包: 不可堆叠、防火 */
    public static final Item SHARED_INVENTORY_BACKPACK =registerItems("shared_inventory_backpack",new SharedInventoryBackpack(new Item.Settings().maxCount(1).fireproof()));

    //=== 方块注册 ===
    /** 共享核心方块: 石头材质、需工具挖掘、高爆炸抗性 */
    public static final Block SHARED_INVENTORY_CHEST_BLOCK =registerBlocks("shared_inventory_chest_block",new SharedInventoryChestBlock(AbstractBlock.Settings.of(Material.STONE).requiresTool() .strength(1.5f,1200f)));
    /** 共享核心方块物品: 可堆叠64、防火 */
    public static final Item SHARED_INVENTORY_CHEST =registerItems("shared_inventory_chest_block",new BlockItem(SHARED_INVENTORY_CHEST_BLOCK,new Item.Settings().maxCount(64).fireproof()));
    /** 共享核心方块实体 */
    public static final BlockEntityType<SharedInventoryChestBlockEntity> SHARED_INVENTORY_CHEST_BLOCK_ENTITY = registerBlockEntity("shared_inventory_chest_block_entity", FabricBlockEntityTypeBuilder.create(SharedInventoryChestBlockEntity::create, SHARED_INVENTORY_CHEST_BLOCK).build());

    //=== ScreenHandler 注册 ===
    /** 共享存储界面处理器 */
    public static final ScreenHandlerType<SharedInventoryScreenHandler> SHARED_INVENTORY_SCREEN_HANDLER = Registry.register(Registry.SCREEN_HANDLER, new Identifier(SharedInventoryMod.MOD_ID, "shared_inventory_screen_handler"), new ScreenHandlerType<>(SharedInventoryScreenHandler::new));

    //=== 注册辅助方法 ===
    /** 注册物品到 Minecraft 注册表 */
    private static Item registerItems(String name, Item item){
        return Registry.register(Registry.ITEM,new Identifier(SharedInventoryMod.MOD_ID,name),item);

    }
    /** 注册方块到 Minecraft 注册表 */
    private static Block registerBlocks(String name, Block block){
        return Registry.register(Registry.BLOCK,new Identifier(SharedInventoryMod.MOD_ID,name),block);
    }
    /** 注册方块实体类型到 Minecraft 注册表 */
    private static <T extends BlockEntityType<?>> T registerBlockEntity(String name, T blockEntityType) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(SharedInventoryMod.MOD_ID, name), blockEntityType);
    }
    /** 触发类加载，完成所有静态注册 (方法体为空，注册在 static 初始化块中完成) */
    public static void registerModObjects(){

    }
}
