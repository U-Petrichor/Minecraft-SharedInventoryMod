package com.umut.sharedInventory.item;

import com.umut.sharedInventory.block.SharedInventoryChestBlockEntity;
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
        SharedInventoryChestBlockEntity blockEntity = readLinkedBlockEntity(backpackStack, player.getServer());
        if (blockEntity == null) {
            player.sendMessage(Text.translatable("message.shared_inventory_mod.shared_inventory_backpack.message1"), true);
            return null;
        }
        return new SharedInventoryScreenHandler(syncId, inv, new BackpackInventory(blockEntity));
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("item.shared_inventory_mod.shared_inventory_backpack");
    }

    public void linkToChest(ItemStack stack, SharedInventoryChestBlockEntity blockEntity) {
        if (blockEntity != null && blockEntity.getWorld() != null) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putLong("linkedBlockEntityPos", blockEntity.getPos().asLong());
            nbt.putString("linkedDimension", blockEntity.getWorld().getRegistryKey().getValue().toString());
        }
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
                if (world != null && world.getBlockEntity(pos) instanceof SharedInventoryChestBlockEntity be) {
                    return be;
                }
            } catch (net.minecraft.util.InvalidIdentifierException ignored) {
            }
        }

        for (ServerWorld world : server.getWorlds()) {
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
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.shared_inventory_mod.shared_inventory_backpack.tooltip"));
    }
}
