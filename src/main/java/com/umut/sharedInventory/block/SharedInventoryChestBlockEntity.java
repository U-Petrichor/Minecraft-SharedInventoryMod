package com.umut.sharedInventory.block;

import com.umut.sharedInventory.inventory.DefaultedListInventory;
import com.umut.sharedInventory.inventory.ModObjects;
import com.umut.sharedInventory.item.BackpackInventory;
import com.umut.sharedInventory.screen.SharedInventoryScreenHandler;
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

public class SharedInventoryChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    private static final int PUBLIC_STACK_SIZE = 16;
    public final DefaultedList<ItemStack> publicStack = DefaultedList.ofSize(PUBLIC_STACK_SIZE, ItemStack.EMPTY);

    public SharedInventoryChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    public static SharedInventoryChestBlockEntity create(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(pos, state);
    }

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
