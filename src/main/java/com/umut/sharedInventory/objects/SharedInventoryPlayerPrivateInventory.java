package com.umut.sharedInventory.objects;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;

public class SharedInventoryPlayerPrivateInventory extends SimpleInventory {

    private static final int STACK_SIZE = 1440;
    private static final int PER_PAGE_SIZE = 72;
    private static final int MAX_PAGE = 20;

    private final DefaultedList<ItemStack> privateStack = DefaultedList.ofSize(STACK_SIZE, ItemStack.EMPTY);
    private int currentPage = 1;

    public SharedInventoryPlayerPrivateInventory() {}

    public int findNextItemIndex(int startIndex, String searchText) {
        int totalSlots = STACK_SIZE;
        // Search from startIndex to end
        for (int i = startIndex; i < totalSlots; i++) {
            ItemStack stack = privateStack.get(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(searchText.toLowerCase())) {
                return i;
            }
        }
        // Search from 0 to startIndex - 1
        for (int i = 0; i < startIndex; i++) {
            ItemStack stack = privateStack.get(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(searchText.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }
    public int getCurrentPage(){
        return this.currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public int getPrivateStackMaxPage(){
        return MAX_PAGE;
    }

    public int getPrivateStackStartIndex(int slot){
        if(slot<0||slot>PER_PAGE_SIZE)
            return -1;
        return slot+(currentPage-1)*PER_PAGE_SIZE;
    }

    @Override
    public void readNbtList(NbtList nbtList) {
        privateStack.clear();
        for (int i = 0; i < STACK_SIZE; i++) {
            NbtCompound itemTag = nbtList.getCompound(i);
            //System.out.println("readNbtList通报，当前的i位置是"+i+"/1440");
            int slot = itemTag.getShort("Slot") & 0xFFFF; // 读取无符号short
            if (slot >= 0 && slot < STACK_SIZE) { // 确保槽位合法
                ItemStack stack = ItemStack.fromNbt(itemTag);
                //System.out.println("readNbtList通报，你当前slot读取的位置是"+slot+"/1440");
                this.privateStack.set(slot, stack);
            }
        }
    }

    @Override
    public NbtList toNbtList() {
        NbtList nbtList = new NbtList();
        for (int slot = 0; slot < STACK_SIZE; slot++) {
            //注意不能用这个类里面的getStack,因为存储时是要遍历整个箱子,而且玩家关闭的时候不一定停留在第一页,所以要保证从一开始去遍历,这个道理也同样适用于上面的readNbtList
            ItemStack stack = this.privateStack.get(slot);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putShort("Slot", (short) slot);
                stack.writeNbt(itemTag);
                nbtList.add(itemTag);
            }
        }
        return nbtList;
    }

    // 核心功能方法,根据当前页数去传递正确的物品
    public ItemStack getStack(int slot) {
        int index = getPrivateStackStartIndex(slot);
        return index >= 0 ? privateStack.get(index) : ItemStack.EMPTY;
    }
    public void setStack(int slot, ItemStack stack) {
        int index = getPrivateStackStartIndex(slot);
        if (index >= 0) privateStack.set(index, stack);

    }

    public ItemStack removeStack(int slot, int amount) {
        int index = getPrivateStackStartIndex(slot);
        if (index < 0) return ItemStack.EMPTY;

        ItemStack stack = privateStack.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        return stack.split(amount);
    }



}