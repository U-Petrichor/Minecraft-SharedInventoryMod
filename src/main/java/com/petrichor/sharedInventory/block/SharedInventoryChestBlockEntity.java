package com.petrichor.sharedInventory.block;

import com.petrichor.sharedInventory.inventory.DefaultedListInventory;
import com.petrichor.sharedInventory.inventory.ModObjects;
import com.petrichor.sharedInventory.item.BackpackInventory;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * 共享核心方块实体 — 持有公共背包数据 (4×4 = 16 格)
 *
 * publicStack 是所有绑定此核心的背包共享的存储空间，
 * 通过 BackpackInventory 包装后传给 ScreenHandler。
 * 数据通过 NBT 持久化在方块实体中。
 */
public class SharedInventoryChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    /** 公共背包槽位数 (4×4) */
    private static final int PUBLIC_STACK_SIZE = 16;
    /** 公共物品列表，所有绑定此核心的玩家共享 */
    private final DefaultedList<ItemStack> publicStack = DefaultedList.ofSize(PUBLIC_STACK_SIZE, ItemStack.EMPTY);

    public SharedInventoryChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    public static SharedInventoryChestBlockEntity create(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(pos, state);
    }

    /**
     * 获取公共物品列表 (供 BackpackInventory 委托使用)
     * 警告: 返回可变的内部列表，直接修改不会触发 markDirty()。
     * 应通过 BackpackInventory 操作以确保数据持久化。
     */
    public DefaultedList<ItemStack> getPublicStack() { return this.publicStack; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        DefaultedListInventory.readFromNbt(this.publicStack, nbt, "PublicItems");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        DefaultedListInventory.writeToNbt(this.publicStack, nbt, "PublicItems");
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.shared_inventory_mod.shared_inventory_chest_block");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new SharedInventoryScreenHandler(syncId, inv, new BackpackInventory(this));
    }
}
