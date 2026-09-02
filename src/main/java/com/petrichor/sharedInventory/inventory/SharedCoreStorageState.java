package com.petrichor.sharedInventory.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 世界级共享核心存储。每个核心 UUID 对应一份独立库存。 */
public class SharedCoreStorageState extends PersistentState {
    public static final int INVENTORY_SIZE = 16;

    private static final String SAVE_KEY = "shared_inventory_mod_core_storage";

    private static final Codec<SerializedCore> CORE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Uuids.CODEC.fieldOf("id").forGetter(core -> core.id),
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(core -> core.items),
                    Codec.STRING.optionalFieldOf("dimension", "").forGetter(core -> core.dimension),
                    Codec.LONG.optionalFieldOf("position", 0L).forGetter(core -> core.position)
            ).apply(instance, SerializedCore::new)
    );

    private static final Codec<SharedCoreStorageState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CORE_CODEC.listOf().fieldOf("cores")
                            .forGetter(SharedCoreStorageState::serializeCores)
            ).apply(instance, SharedCoreStorageState::new)
    );

    private static final PersistentStateType<SharedCoreStorageState> TYPE =
            new PersistentStateType<>(SAVE_KEY, SharedCoreStorageState::new, CODEC, null);

    private final Map<UUID, CoreData> cores = new HashMap<>();

    public SharedCoreStorageState() {
    }

    private SharedCoreStorageState(List<SerializedCore> serializedCores) {
        for (SerializedCore serialized : serializedCores) {
            CoreData data = new CoreData();
            for (int i = 0; i < Math.min(INVENTORY_SIZE, serialized.items.size()); i++) {
                data.items.set(i, serialized.items.get(i).copy());
            }
            data.dimension = serialized.dimension;
            data.position = serialized.position;
            cores.put(serialized.id, data);
        }
    }

    public static SharedCoreStorageState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }

    private List<SerializedCore> serializeCores() {
        List<SerializedCore> serialized = new ArrayList<>(cores.size());
        for (Map.Entry<UUID, CoreData> entry : cores.entrySet()) {
            List<ItemStack> items = new ArrayList<>(INVENTORY_SIZE);
            for (ItemStack stack : entry.getValue().items) {
                items.add(stack.copy());
            }
            serialized.add(new SerializedCore(
                    entry.getKey(), items, entry.getValue().dimension, entry.getValue().position));
        }
        return serialized;
    }

    public void ensureCore(UUID coreId, ServerWorld world, BlockPos pos,
                           DefaultedList<ItemStack> legacyItems) {
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

    private record SerializedCore(UUID id, List<ItemStack> items, String dimension, long position) {
    }
}

