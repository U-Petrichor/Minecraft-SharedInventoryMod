package com.petrichor.sharedInventory.item;

import com.petrichor.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.petrichor.sharedInventory.inventory.SharedCoreStorageState;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 共享背包物品 — 核心交互入口
 *
 * 交互流程:
 *   1. 首次右键: 将背包装备到专属槽位 (通过 SharedInventoryPlayerEntity)
 *   2. 再次右键 (手持已装备的背包): 打开共享存储界面
 *   3. 绑定: 手持背包右键共享核心方块 → 将方块坐标写入背包 NBT
 *   4. 快捷键: 按 B 键 → OpenBackpackPayload 处理 → 打开共享存储界面
 */
public class SharedInventoryBackpack extends Item implements NamedScreenHandlerFactory {

    public SharedInventoryBackpack(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand == Hand.MAIN_HAND) {
            if (user instanceof SharedInventoryPlayerEntity sharedPlayer) {
                ItemStack currentBackpack = sharedPlayer.shared$getBackpackStack();
                if (currentBackpack.isEmpty()) {
                    // 首次使用: 装备到专属槽位
                    sharedPlayer.shared$setBackpackStack(stack.copy());
                    stack.setCount(0);
                    return TypedActionResult.success(stack, world.isClient());
                }
                // 已装备背包: 打开共享存储界面
                user.openHandledScreen(this);
            }
            // Mixin 未生效时不执行任何操作
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ItemStack backpackStack = findBackpackStack(player);
        if (backpackStack == null) return null;
        BackpackInventory inventory = createLinkedInventory(backpackStack, player.getServer());
        if (inventory == null) {
            player.sendMessage(Text.translatable("message.shared_inventory_mod.shared_inventory_backpack.message1"), true);
            return null;
        }
        return new SharedInventoryScreenHandler(syncId, inv, inventory);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("item.shared_inventory_mod.shared_inventory_backpack");
    }

    public void linkToChest(ItemStack stack, SharedInventoryChestBlockEntity blockEntity) {
        if (blockEntity != null && blockEntity.getWorld() != null) {
            UUID coreId = blockEntity.getOrCreateCoreId();
            if (coreId == null) return;
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
                nbt.putUuid("linkedCoreId", coreId);
                nbt.putLong("linkedBlockEntityPos", blockEntity.getPos().asLong());
                nbt.putString("linkedDimension", blockEntity.getWorld().getRegistryKey().getValue().toString());
            });
        }
    }

    @Nullable
    public static BackpackInventory createLinkedInventory(ItemStack stack, MinecraftServer server) {
        if (server == null) return null;
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        NbtCompound nbt = customData.copyNbt();
        SharedCoreStorageState storageState = SharedCoreStorageState.get(server);
        if (nbt.containsUuid("linkedCoreId")) {
            UUID coreId = nbt.getUuid("linkedCoreId");
            if (storageState.contains(coreId)) return new BackpackInventory(storageState, coreId);
        }
        SharedInventoryChestBlockEntity blockEntity = readLinkedBlockEntity(stack, server);
        if (blockEntity == null) return null;
        UUID coreId = blockEntity.getOrCreateCoreId();
        if (coreId == null) return null;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack,
                data -> data.putUuid("linkedCoreId", coreId));
        return new BackpackInventory(storageState, coreId);
    }

    @Nullable
    public static SharedInventoryChestBlockEntity readLinkedBlockEntity(ItemStack stack, MinecraftServer server) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        NbtCompound nbt = customData.copyNbt();
        if (!nbt.contains("linkedBlockEntityPos")) return null;
        BlockPos pos = BlockPos.fromLong(nbt.getLong("linkedBlockEntityPos"));

        // 优先按维度精确查找 (新格式)
        if (nbt.contains("linkedDimension")) {
            try {
                Identifier dimId = Identifier.of(nbt.getString("linkedDimension"));
                RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimId);
                ServerWorld world = server.getWorld(dimKey);
                if (world != null) {
                    world.getChunk(pos);
                    if (world.getBlockEntity(pos) instanceof SharedInventoryChestBlockEntity be) {
                        return be;
                    }
                }
            } catch (net.minecraft.util.InvalidIdentifierException ignored) {
                // 损坏或过时的 NBT 数据，回退到遍历维度
            }
        }

        // 回退: 遍历所有维度 (兼容无维度信息的旧数据)
        for (ServerWorld world : server.getWorlds()) {
            world.getChunk(pos);
            if (world.getBlockEntity(pos) instanceof SharedInventoryChestBlockEntity be) {
                return be;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack findBackpackStack(PlayerEntity player) {
        if (player instanceof SharedInventoryPlayerEntity sharedPlayer) {
            ItemStack worn = sharedPlayer.shared$getBackpackStack();
            if (!worn.isEmpty() && worn.getItem() instanceof SharedInventoryBackpack) return worn;
        }
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        if (mainHand.getItem() instanceof SharedInventoryBackpack) return mainHand;
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        if (offHand.getItem() instanceof SharedInventoryBackpack) return offHand;
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.shared_inventory_mod.shared_inventory_backpack.tooltip"));
    }
}
