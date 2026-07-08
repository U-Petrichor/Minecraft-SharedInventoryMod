package com.umut.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class PrivateInventory implements Inventory {
    private static final int PER_PAGE_SIZE = 60;
    private static final int MAX_PAGE = 24;
    private static final int STACK_SIZE = PER_PAGE_SIZE * MAX_PAGE;
    private final DefaultedList<ItemStack> privateStack = DefaultedList.ofSize(STACK_SIZE, ItemStack.EMPTY);
    private int currentPage = 1;
    private final Map<Integer, String> pageLabels = new HashMap<>();
    private final FurnaceLogic furnaceLogic = new FurnaceLogic();
    private final BrewingLogic brewingLogic = new BrewingLogic();
    private final AnvilData anvilData = new AnvilData();
    private final SmithingData smithingData = new SmithingData();
    private Runnable dirtyCallback;

    public PrivateInventory() {}
    public int getCurrentPage() { return this.currentPage; }
    public void setCurrentPage(int page) {
        if (page < 1 || page > MAX_PAGE) return;
        this.currentPage = page;
    }
    public int getPrivateStackMaxPage() { return MAX_PAGE; }

    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback;
        this.furnaceLogic.setDirtyCallback(callback);
        this.brewingLogic.setDirtyCallback(callback);
        this.anvilData.setDirtyCallback(callback);
        this.smithingData.setDirtyCallback(callback);
    }
    public boolean hasDirtyCallback() { return this.dirtyCallback != null; }

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

    public void setPageLabel(int page, String label) {
        if (page < 1 || page > MAX_PAGE) return;
        if (label == null || label.isEmpty()) { pageLabels.remove(page); } else { pageLabels.put(page, label); }
    }
    public String getPageLabel(int page) { return pageLabels.getOrDefault(page, ""); }
    public Map<Integer, String> getAllLabels() { return new HashMap<>(pageLabels); }
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

    public int getPrivateStackStartIndex(int slot) { if (slot < 0 || slot >= PER_PAGE_SIZE) return -1; return slot + (currentPage - 1) * PER_PAGE_SIZE; }
    public void readNbtList(NbtList nbtList) {
        privateStack.clear();
        for (int i = 0; i < STACK_SIZE; i++) { NbtCompound itemTag = nbtList.getCompound(i); int slot = itemTag.getShort("Slot") & 0xFFFF; if (slot >= 0 && slot < STACK_SIZE) { this.privateStack.set(slot, ItemStack.fromNbt(itemTag)); } }
    }
    public NbtList toNbtList() {
        NbtList nbtList = new NbtList();
        for (int slot = 0; slot < STACK_SIZE; slot++) { ItemStack stack = this.privateStack.get(slot); if (!stack.isEmpty()) { NbtCompound itemTag = new NbtCompound(); itemTag.putShort("Slot", (short) slot); stack.writeNbt(itemTag); nbtList.add(itemTag); } }
        return nbtList;
    }

    public void tick(World world) { furnaceLogic.tick(world); brewingLogic.tick(world); }

    public FurnaceLogic getFurnaceLogic() { return furnaceLogic; }
    public DefaultedList<ItemStack> getFurnaceStack() { return furnaceLogic.getFurnaceStack(); }
    public PropertyDelegate getPropertyDelegate() { return furnaceLogic.getPropertyDelegate(); }
    public Inventory getFurnaceInventory() { return furnaceLogic.getFurnaceInventory(); }
    public boolean isBurning() { return furnaceLogic.isBurning(); }
    public void readFurnaceNbt(NbtCompound nbt) { furnaceLogic.readNbt(nbt); }
    public void writeFurnaceNbt(NbtCompound nbt) { furnaceLogic.writeNbt(nbt); }

    public BrewingLogic getBrewingLogic() { return brewingLogic; }
    public DefaultedList<ItemStack> getBrewingStack() { return brewingLogic.getBrewingStack(); }
    public Inventory getBrewingInventory() { return brewingLogic.getBrewingInventory(); }
    public PropertyDelegate getBrewingPropertyDelegate() { return brewingLogic.getPropertyDelegate(); }
    public void readBrewingNbt(NbtCompound nbt) { brewingLogic.readNbt(nbt); }
    public void writeBrewingNbt(NbtCompound nbt) { brewingLogic.writeNbt(nbt); }

    public AnvilData getAnvilData() { return anvilData; }
    public DefaultedList<ItemStack> getAnvilStack() { return anvilData.getAnvilStack(); }
    public Inventory getAnvilInventory() { return anvilData.getAnvilInventory(); }
    public String getAnvilRenameText() { return anvilData.getRenameText(); }
    public void setAnvilRenameText(String text) { anvilData.setRenameText(text); }
    public void readAnvilNbt(NbtCompound nbt) { anvilData.readNbt(nbt); }
    public void writeAnvilNbt(NbtCompound nbt) { anvilData.writeNbt(nbt); }

    public SmithingData getSmithingData() { return smithingData; }
    public DefaultedList<ItemStack> getSmithingStack() { return smithingData.getSmithingStack(); }
    public Inventory getSmithingInventory() { return smithingData.getSmithingInventory(); }
    public void readSmithingNbt(NbtCompound nbt) { smithingData.readNbt(nbt); }
    public void writeSmithingNbt(NbtCompound nbt) { smithingData.writeNbt(nbt); }
}
