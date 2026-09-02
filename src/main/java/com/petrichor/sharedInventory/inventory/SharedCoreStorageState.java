package com.petrichor.sharedInventory.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 世界级共享核心存储。
 *
 * 每个核心使用独立 UUID 作为键，因此不同核心的库存相互隔离。数据统一保存在主世界的
 * PersistentState 中，使远程背包不必依赖核心区块保持加载。
 */
public class SharedCoreStorageState extends PersistentState {
    public static final int INVENTORY_SIZE = 16;

    private static final String SAVE_KEY = "shared_inventory_mod_core_storage";
    private static final String CORES_KEY = "Cores";
    private static final String CORE_ID_KEY = "CoreId";
    private static final String ITEMS_KEY = "Items";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String POSITION_KEY = "Position";

    private final Map<UUID, CoreData> cores = new HashMap<>();

    /** 跨维度共享同一份状态，因此固定使用主世界的 PersistentStateManager。 */
    public static SharedCoreStorageState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                SharedCoreStorageState::fromNbt,
                SharedCoreStorageState::new,
                SAVE_KEY
        );
    }

    public static SharedCoreStorageState fromNbt(NbtCompound nbt) {
        SharedCoreStorageState state = new SharedCoreStorageState();
        NbtList coresNbt = nbt.getList(CORES_KEY, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < coresNbt.size(); i++) {
            NbtCompound coreNbt = coresNbt.getCompound(i);
            if (!coreNbt.containsUuid(CORE_ID_KEY)) continue;

            UUID coreId = coreNbt.getUuid(CORE_ID_KEY);
            CoreData data = new CoreData();
            DefaultedListInventory.readFromNbt(data.items, coreNbt, ITEMS_KEY);
            data.dimension = coreNbt.getString(DIMENSION_KEY);
            data.position = coreNbt.getLong(POSITION_KEY);
            state.cores.put(coreId, data);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList coresNbt = new NbtList();
        for (Map.Entry<UUID, CoreData> entry : cores.entrySet()) {
            NbtCompound coreNbt = new NbtCompound();
            coreNbt.putUuid(CORE_ID_KEY, entry.getKey());
            DefaultedListInventory.writeToNbt(entry.getValue().items, coreNbt, ITEMS_KEY);
            coreNbt.putString(DIMENSION_KEY, entry.getValue().dimension);
            coreNbt.putLong(POSITION_KEY, entry.getValue().position);
            coresNbt.add(coreNbt);
        }
        nbt.put(CORES_KEY, coresNbt);
        return nbt;
    }

    /**
     * 注册或刷新核心信息。仅在首次注册时复制旧版 BlockEntity 中的库存。
     */
    public void ensureCore(UUID coreId, ServerWorld world, BlockPos pos, DefaultedList<ItemStack> legacyItems) {
        CoreData data = cores.get(coreId);
        boolean changed = false;

        if (data == null) {
            data = new CoreData();
            for (int i = 0; i < Math.min(INVENTORY_SIZE, legacyItems.size()); i++) {
                data.items.set(i, legacyItems.get(i).copy());
            }
            cores.put(coreId, data);
            changed = true;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        long position = pos.asLong();
        if (!Objects.equals(data.dimension, dimension) || data.position != position) {
            data.dimension = dimension;
            data.position = position;
            changed = true;
        }

        if (changed) markDirty();
    }

    public boolean contains(UUID coreId) {
        return cores.containsKey(coreId);
    }

    @Nullable
    public DefaultedList<ItemStack> getItems(UUID coreId) {
        CoreData data = cores.get(coreId);
        return data == null ? null : data.items;
    }

    /** 删除核心记录并返回其物品列表，供方块被破坏时掉落。 */
    @Nullable
    public DefaultedList<ItemStack> removeCore(UUID coreId) {
        CoreData removed = cores.remove(coreId);
        if (removed == null) return null;
        markDirty();
        return removed.items;
    }

    private static final class CoreData {
        private final DefaultedList<ItemStack> items =
                DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        private String dimension = "";
        private long position;
    }
}
