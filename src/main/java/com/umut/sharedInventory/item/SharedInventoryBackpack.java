package com.umut.sharedInventory.item;

import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
import com.umut.sharedInventory.inventory.SharedCoreStorageState;
import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.client.item.TooltipContext;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

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
                    sharedPlayer.shared$setBackpackStack(stack.copy());
                    stack.setCount(0);
                    return TypedActionResult.success(stack, world.isClient());
                }
                user.openHandledScreen(this);
            }
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
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putUuid("linkedCoreId", coreId);
            nbt.putLong("linkedBlockEntityPos", blockEntity.getPos().asLong());
            nbt.putString("linkedDimension", blockEntity.getWorld().getRegistryKey().getValue().toString());
        }
    }

    @Nullable
    public static BackpackInventory createLinkedInventory(ItemStack stack, MinecraftServer server) {
        if (server == null) return null;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return null;
        SharedCoreStorageState storageState = SharedCoreStorageState.get(server);
        if (nbt.containsUuid("linkedCoreId")) {
            UUID coreId = nbt.getUuid("linkedCoreId");
            if (storageState.contains(coreId)) return new BackpackInventory(storageState, coreId);
        }
        SharedInventoryChestBlockEntity blockEntity = readLinkedBlockEntity(stack, server);
        if (blockEntity == null) return null;
        UUID coreId = blockEntity.getOrCreateCoreId();
        if (coreId == null) return null;
        stack.getOrCreateNbt().putUuid("linkedCoreId", coreId);
        return new BackpackInventory(storageState, coreId);
    }

    @Nullable
    public static SharedInventoryChestBlockEntity readLinkedBlockEntity(ItemStack stack, MinecraftServer server) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("linkedBlockEntityPos")) return null;
        BlockPos pos = BlockPos.fromLong(nbt.getLong("linkedBlockEntityPos"));

        if (nbt.contains("linkedDimension")) {
            try {
                Identifier dimId = new Identifier(nbt.getString("linkedDimension"));
                RegistryKey<World> dimKey = RegistryKey.of(Registry.WORLD_KEY, dimId);
                ServerWorld world = server.getWorld(dimKey);
                if (world != null) {
                    world.getChunk(pos);
                    if (world.getBlockEntity(pos) instanceof SharedInventoryChestBlockEntity) {
                        return (SharedInventoryChestBlockEntity) world.getBlockEntity(pos);
                    }
                }
            } catch (net.minecraft.util.InvalidIdentifierException ignored) {
            }
        }

        for (ServerWorld world : server.getWorlds()) {
            world.getChunk(pos);
            if (world.getBlockEntity(pos) instanceof SharedInventoryChestBlockEntity) {
                return (SharedInventoryChestBlockEntity) world.getBlockEntity(pos);
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
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.shared_inventory_mod.shared_inventory_backpack.tooltip"));
    }
}
