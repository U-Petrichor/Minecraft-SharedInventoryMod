package com.petrichor.sharedInventory;

import com.petrichor.sharedInventory.inventory.*;
import com.petrichor.sharedInventory.network.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 模组主入口 — Fabric ModInitializer 实现
 *
 * 初始化流程:
 *   1. 注册模组对象 (物品、方块、ScreenHandler)
 *   2. 注册交互事件 (背包绑定共享核心)
 *   3. 注册网络包接收器 (翻页、标签更新、打开背包、工具切换)
 */
public class SharedInventoryMod implements ModInitializer {
    /** 模组 ID，用于资源路径和注册表命名空间 */
    public static final String MOD_ID = "shared_inventory_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** 翻页网络包通道 */
    public static final Identifier PAGE_UPDATE_ID = new Identifier(MOD_ID, "page_update");
    /** 标签更新网络包通道 */
    public static final Identifier LABEL_UPDATE_ID = new Identifier(MOD_ID, "label_update");
    /** 打开背包网络包通道 (穿戴后按键触发) */
    public static final Identifier OPEN_BACKPACK_ID = new Identifier(MOD_ID, "open_backpack");
    /** 工具切换网络包通道 */
    public static final Identifier TOOL_UPDATE_ID = new Identifier(MOD_ID, "tool_update");

    public static final ItemGroup SHARED_INVENTORY_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "shared_inventory_mod"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK))
                    .displayName(Text.translatable("itemGroup.shared_inventory_mod.shared_inventory_mod"))
                    .entries((context, entries) -> {
                        entries.add(new ItemStack(ModObjects.SHARED_INVENTORY_BACKPACK));
                        entries.add(new ItemStack(ModObjects.SHARED_INVENTORY_CHEST));
                    })
                    .build()
    );

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
