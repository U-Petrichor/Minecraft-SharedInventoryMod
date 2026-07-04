package com.petrichor.sharedInventory;

import com.petrichor.sharedInventory.item.*;
import com.petrichor.sharedInventory.inventory.*;
import com.petrichor.sharedInventory.network.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 模组主入口 — Fabric ModInitializer 实现
 *
 * 初始化流程:
 *   1. 注册模组对象 (物品、方块、ScreenHandler)
 *   2. 注册交互事件 (背包绑定共享核心)
 *   3. 注册网络包接收器 (翻页、标签更新、打开背包)
 */
public class SharedInventoryMod implements ModInitializer {
    /** 模组 ID，用于资源路径和注册表命名空间 */
    public static final String MOD_ID = "shared_inventory_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** 翻页网络包通道 */
    public static final Identifier PAGE_UPDATE_ID = new Identifier("petrichor", "shared_inventory_mod");
    /** 标签更新网络包通道 */
    public static final Identifier LABEL_UPDATE_ID = new Identifier("petrichor", "shared_inventory_mod_label");
    /** 打开背包网络包通道 (穿戴后按键触发) */
    public static final Identifier OPEN_BACKPACK_ID = new Identifier("petrichor", "open_backpack");

	public static final ItemGroup SHARED_INVENTORY_GROUP= FabricItemGroupBuilder.create(new Identifier("petrichor","shared_inventory_mod"))
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
	}
}