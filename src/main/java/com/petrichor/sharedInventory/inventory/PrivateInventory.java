package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 私人背包 — 玩家私有的扩展存储系统
 *
 * 职责:
 *   1. 分页物品存储: 60 槽位/页 × 24 页 = 1440 槽位
 *   2. 页签标签系统: 为每页设置名称，支持按标签跳转
 *   3. 组合根: 持有 FurnaceLogic / BrewingLogic / AnvilData / SmithingData 实例，
 *      通过委托方法暴露给 ScreenHandler，保持公开 API 不变
 *
 * 数据来源: 通过 Mixin (SharedInventoryPlayerEntityMixin) 注入到 PlayerEntity 中，
 * 每个玩家拥有独立的 PrivateInventory 实例，跟随玩家 NBT 持久化
 *
 * 注意: 实现 Inventory 接口而非继承 SimpleInventory，
 * 因为 SimpleInventory 的内部存储无法满足分页偏移访问的需求
 */
public class PrivateInventory implements Inventory {
    /** 每页槽位数 (6行 × 10列) */
    private static final int PER_PAGE_SIZE = 60;
    /** 最大页数 */
    private static final int MAX_PAGE = 24;
    /** 总槽位数 */
    private static final int STACK_SIZE = PER_PAGE_SIZE * MAX_PAGE;
    /** 全部物品 (跨页连续存储，通过 currentPage 计算偏移) */
    private final DefaultedList<ItemStack> privateStack = DefaultedList.ofSize(STACK_SIZE, ItemStack.EMPTY);
    /** 当前显示的页码 (1-based) */
    private int currentPage = 1;
    /** 页签标签: 页码 → 标签名 */
    private final Map<Integer, String> pageLabels = new HashMap<>();
    /** 熔炉逻辑 */
    private final FurnaceLogic furnaceLogic = new FurnaceLogic();
    /** 酿造逻辑 */
    private final BrewingLogic brewingLogic = new BrewingLogic();
    /** 铁砧数据 */
    private final AnvilData anvilData = new AnvilData();
    /** 锻造台数据 */
    private final SmithingData smithingData = new SmithingData();
    /** markDirty 回调，由 SharedInventoryPlayerEntityMixin 设置，用于触发玩家数据保存 */
    private Runnable dirtyCallback;

    public PrivateInventory() {}
    public int getCurrentPage() { return this.currentPage; }
    public void setCurrentPage(int page) {
        if (page < 1 || page > MAX_PAGE) return;
        this.currentPage = page;
    }
    public int getPrivateStackMaxPage() { return MAX_PAGE; }

    /** 设置 markDirty 回调 (由 Mixin 在注入时调用)，同时转发到子模块 */
    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback;
        this.furnaceLogic.setDirtyCallback(callback);
        this.brewingLogic.setDirtyCallback(callback);
        this.anvilData.setDirtyCallback(callback);
        this.smithingData.setDirtyCallback(callback);
    }
    /** 检查 markDirty 回调是否已设置 */
    public boolean hasDirtyCallback() { return this.dirtyCallback != null; }

    // === Inventory 接口实现 ===

    @Override
    public int size() { return PER_PAGE_SIZE; }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PER_PAGE_SIZE; i++) {
            if (!getStack(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        int index = getPrivateStackStartIndex(slot);
        return index >= 0 ? privateStack.get(index) : ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        int index = getPrivateStackStartIndex(slot);
        if (index >= 0) privateStack.set(index, stack);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        int index = getPrivateStackStartIndex(slot);
        if (index < 0) return ItemStack.EMPTY;
        return Inventories.splitStack(privateStack, index, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        int index = getPrivateStackStartIndex(slot);
        if (index < 0) return ItemStack.EMPTY;
        ItemStack stack = privateStack.get(index);
        privateStack.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void markDirty() {
        if (dirtyCallback != null) dirtyCallback.run();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override
    public void clear() {
        int start = (currentPage - 1) * PER_PAGE_SIZE;
        for (int i = start; i < start + PER_PAGE_SIZE; i++) {
            privateStack.set(i, ItemStack.EMPTY);
        }
    }

    // === 标签系统 ===

    /** 设置页签标签，空值则移除 */
    public void setPageLabel(int page, String label) {
        if (page < 1 || page > MAX_PAGE) return;
        if (label == null || label.isEmpty()) { pageLabels.remove(page); } else { pageLabels.put(page, label); }
    }
    public String getPageLabel(int page) { return pageLabels.getOrDefault(page, ""); }
    public Map<Integer, String> getAllLabels() { return new HashMap<>(pageLabels); }
    /** 按标签查找页码: 先精确匹配，再模糊匹配，未找到返回 -1 */
    public int findPageByLabel(String label) {
        if (label == null || label.isEmpty()) return -1;
        for (Map.Entry<Integer, String> entry : pageLabels.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(label)) return entry.getKey();
        }
        for (Map.Entry<Integer, String> entry : pageLabels.entrySet()) {
            if (entry.getValue().toLowerCase().contains(label.toLowerCase())) return entry.getKey();
        }
        return -1;
    }
    public void readLabelsFromNbt(NbtCompound nbt) {
        pageLabels.clear();
        if (nbt.contains("PageLabels", 10)) {
            NbtCompound labelsNbt = nbt.getCompound("PageLabels");
            for (String key : labelsNbt.getKeys()) {
                try { int page = Integer.parseInt(key); if (page >= 1 && page <= MAX_PAGE) { pageLabels.put(page, labelsNbt.getString(key)); } } catch (NumberFormatException ignored) {}
            }
        }
    }
    public void writeLabelsToNbt(NbtCompound nbt) {
        NbtCompound labelsNbt = new NbtCompound();
        for (Map.Entry<Integer, String> entry : pageLabels.entrySet()) { labelsNbt.putString(String.valueOf(entry.getKey()), entry.getValue()); }
        nbt.put("PageLabels", labelsNbt);
    }

    // === 物品存取 (基于当前页偏移) ===

    /** 将页面内 slot 转换为 privateStack 的绝对索引 */
    public int getPrivateStackStartIndex(int slot) { if (slot < 0 || slot >= PER_PAGE_SIZE) return -1; return slot + (currentPage - 1) * PER_PAGE_SIZE; }
    /** 从 NbtList 读取全部物品 (跨页连续存储) */
    public void readNbtList(NbtList nbtList, RegistryWrapper.WrapperLookup registryLookup) {
        privateStack.clear();
        for (int i = 0; i < STACK_SIZE; i++) { NbtCompound itemTag = nbtList.getCompound(i); int slot = itemTag.getShort("Slot") & 0xFFFF; if (slot >= 0 && slot < STACK_SIZE) { this.privateStack.set(slot, ItemStack.fromNbtOrEmpty(registryLookup, itemTag)); } }
    }
    /** 将全部物品写入 NbtList (非空物品) */
    public NbtList toNbtList(RegistryWrapper.WrapperLookup registryLookup) {
        NbtList nbtList = new NbtList();
        for (int slot = 0; slot < STACK_SIZE; slot++) { ItemStack stack = this.privateStack.get(slot); if (!stack.isEmpty()) { NbtCompound itemTag = new NbtCompound(); itemTag.putShort("Slot", (short) slot); itemTag.put("id", stack.encode(registryLookup)); nbtList.add(itemTag); } }
        return nbtList;
    }
    // === NBT 序列化 ===

    // === 每刻更新 ===

    /** 驱动熔炉和酿造的 tick 逻辑 */
    public void tick(World world) { furnaceLogic.tick(world); brewingLogic.tick(world); }

    // === 熔炉委托方法 (暴露给 ScreenHandler) ===
    public FurnaceLogic getFurnaceLogic() { return furnaceLogic; }
    public DefaultedList<ItemStack> getFurnaceStack() { return furnaceLogic.getFurnaceStack(); }
    public PropertyDelegate getPropertyDelegate() { return furnaceLogic.getPropertyDelegate(); }
    public Inventory getFurnaceInventory() { return furnaceLogic.getFurnaceInventory(); }
    public boolean isBurning() { return furnaceLogic.isBurning(); }
    public void readFurnaceNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { furnaceLogic.readNbt(nbt, registryLookup); }
    public void writeFurnaceNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { furnaceLogic.writeNbt(nbt, registryLookup); }

    // === 酿造委托方法 ===
    public BrewingLogic getBrewingLogic() { return brewingLogic; }
    public DefaultedList<ItemStack> getBrewingStack() { return brewingLogic.getBrewingStack(); }
    public Inventory getBrewingInventory() { return brewingLogic.getBrewingInventory(); }
    public PropertyDelegate getBrewingPropertyDelegate() { return brewingLogic.getPropertyDelegate(); }
    public void readBrewingNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { brewingLogic.readNbt(nbt, registryLookup); }
    public void writeBrewingNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { brewingLogic.writeNbt(nbt, registryLookup); }

    // === 铁砧委托方法 ===
    public AnvilData getAnvilData() { return anvilData; }
    public DefaultedList<ItemStack> getAnvilStack() { return anvilData.getAnvilStack(); }
    public Inventory getAnvilInventory() { return anvilData.getAnvilInventory(); }
    public String getAnvilRenameText() { return anvilData.getRenameText(); }
    public void setAnvilRenameText(String text) { anvilData.setRenameText(text); }
    public void readAnvilNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { anvilData.readNbt(nbt, registryLookup); }
    public void writeAnvilNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { anvilData.writeNbt(nbt, registryLookup); }

    // === 锻造台委托方法 ===
    public SmithingData getSmithingData() { return smithingData; }
    public DefaultedList<ItemStack> getSmithingStack() { return smithingData.getSmithingStack(); }
    public Inventory getSmithingInventory() { return smithingData.getSmithingInventory(); }
    public void readSmithingNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { smithingData.readNbt(nbt, registryLookup); }
    public void writeSmithingNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) { smithingData.writeNbt(nbt, registryLookup); }
}
