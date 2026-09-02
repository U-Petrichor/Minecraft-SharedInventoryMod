package com.petrichor.sharedInventory.block;

import com.petrichor.sharedInventory.inventory.DefaultedListInventory;
import com.petrichor.sharedInventory.inventory.ModObjects;
import com.petrichor.sharedInventory.inventory.SharedCoreStorageState;
import com.petrichor.sharedInventory.item.BackpackInventory;
import com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 共享核心方块实体 — 持有公共背包数据 (4×4 = 16 格)
 * publicStack 是所有绑定此核心的背包共享的存储空间，
 * 通过 BackpackInventory 包装后传给 ScreenHandler。
 * 数据通过 NBT 持久化在方块实体中。
 */
public class SharedInventoryChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    private UUID coreId;
    private final DefaultedList<ItemStack> legacyPublicStack =
            DefaultedList.ofSize(SharedCoreStorageState.INVENTORY_SIZE, ItemStack.EMPTY);

    public SharedInventoryChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    public static SharedInventoryChestBlockEntity create(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(pos, state);
    }

    @Nullable
    public UUID getOrCreateCoreId() {
        if (!(this.world instanceof ServerWorld serverWorld)) return coreId;
        if (coreId == null) {
            coreId = UUID.randomUUID();
            markDirty();
        }
        SharedCoreStorageState storageState = SharedCoreStorageState.get(serverWorld.getServer());
        storageState.ensureCore(coreId, serverWorld, pos, legacyPublicStack);
        if (!legacyPublicStack.isEmpty()) {
            legacyPublicStack.clear();
            markDirty();
        }
        return coreId;
    }

    @Nullable
    public BackpackInventory createInventory() {
        if (!(this.world instanceof ServerWorld serverWorld)) return null;
        UUID id = getOrCreateCoreId();
        return id == null ? null
                : new BackpackInventory(SharedCoreStorageState.get(serverWorld.getServer()), id);
    }

    public DefaultedList<ItemStack> removeStorage() {
        if (this.world instanceof ServerWorld serverWorld && coreId != null) {
            DefaultedList<ItemStack> removed =
                    SharedCoreStorageState.get(serverWorld.getServer()).removeCore(coreId);
            if (removed != null) return removed;
        }
        return legacyPublicStack;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        coreId = view.read("CoreId", Uuids.CODEC).orElse(null);
        DefaultedListInventory.readFromData(
                this.legacyPublicStack, view.getReadView("PublicItems"));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (coreId != null) view.put("CoreId", Uuids.CODEC, coreId);
        if (!legacyPublicStack.isEmpty()) {
            DefaultedListInventory.writeToData(
                    this.legacyPublicStack, view.get("PublicItems"));
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.shared_inventory_mod.shared_inventory_chest_block");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        BackpackInventory inventory = createInventory();
        return inventory == null ? null : new SharedInventoryScreenHandler(syncId, inv, inventory);
    }
}
