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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 共享核心方块实体 — 持有核心身份，物品数据存储在世界级 PersistentState 中。
 *
 * 每个核心拥有唯一 UUID，所有绑定此 UUID 的背包访问同一份库存。
 * legacyPublicStack 只用于把旧版本 BlockEntity 中的 PublicItems 自动迁移到新存储。
 */
public class SharedInventoryChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    /** 核心身份；旧世界中的核心会在首次访问时生成。 */
    private UUID coreId;
    private final DefaultedList<ItemStack> legacyPublicStack =
            DefaultedList.ofSize(SharedCoreStorageState.INVENTORY_SIZE, ItemStack.EMPTY);

    public SharedInventoryChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    public static SharedInventoryChestBlockEntity create(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(pos, state);
    }

    /** 注册核心并完成旧库存迁移。仅允许在服务端调用。 */
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
        if (id == null) return null;
        return new BackpackInventory(SharedCoreStorageState.get(serverWorld.getServer()), id);
    }

    /** 核心被真正破坏时删除其世界级记录；区块卸载不会调用此方法。 */
    public DefaultedList<ItemStack> removeStorage() {
        if (this.world instanceof ServerWorld serverWorld && coreId != null) {
            DefaultedList<ItemStack> removed =
                    SharedCoreStorageState.get(serverWorld.getServer()).removeCore(coreId);
            if (removed != null) return removed;
        }
        return legacyPublicStack;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("CoreId")) {
            coreId = nbt.getUuid("CoreId");
        }
        DefaultedListInventory.readFromNbt(this.legacyPublicStack, nbt, "PublicItems");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (coreId != null) {
            nbt.putUuid("CoreId", coreId);
        }
        if (!legacyPublicStack.isEmpty()) {
            DefaultedListInventory.writeToNbt(this.legacyPublicStack, nbt, "PublicItems");
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
