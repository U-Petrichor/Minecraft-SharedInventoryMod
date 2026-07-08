package com.petrichor.sharedInventory.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class BrewingLogic {
    /** 槽位: [0-2]药水瓶 [3]材料 [4]烈焰粉 */
    private final DefaultedList<ItemStack> brewingStack = DefaultedList.ofSize(5, ItemStack.EMPTY);
    /** 缓存的 Inventory 包装器，避免每次创建新对象 */
    private final DefaultedListInventory brewingInventory = new DefaultedListInventory(brewingStack);
    /** 当前剩余酿造时间 (酿造一次 400 tick) */
    private int brewTime;
    /** 剩余烈焰粉份数 (每份可酿造 20 次) */
    private int brewFuel;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return BrewingLogic.this.brewTime;
                case 1: return BrewingLogic.this.brewFuel;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: BrewingLogic.this.brewTime = value; break;
                case 1: BrewingLogic.this.brewFuel = value; break;
            }
        }

        @Override
        public int size() { return 2; }
    };

    public void tick(World world) {
        if (world == null || world.isClient) return;
        ItemStack ingredient = this.brewingStack.get(3);
        ItemStack blazePowder = this.brewingStack.get(4);

        if (this.brewFuel <= 0 && !blazePowder.isEmpty() && blazePowder.isOf(net.minecraft.item.Items.BLAZE_POWDER)) {
            this.brewFuel = 20;
            blazePowder.decrement(1);
            if (blazePowder.isEmpty()) {
                this.brewingStack.set(4, ItemStack.EMPTY);
            }
        }

        if (this.brewTime > 0) {
            if (ingredient.isEmpty() || !canBrew()) {
                this.brewTime = 0;
                return;
            }
            --this.brewTime;
            if (this.brewTime == 0) {
                craftBrew();
                if (this.brewFuel > 0) {
                    --this.brewFuel;
                }
            }
        } else {
            if (!ingredient.isEmpty() && this.brewFuel > 0 && canBrew()) {
                this.brewTime = 400;
            }
        }
    }

    public DefaultedList<ItemStack> getBrewingStack() { return this.brewingStack; }

    public PropertyDelegate getPropertyDelegate() { return this.propertyDelegate; }

    public Inventory getBrewingInventory() {
        return this.brewingInventory;
    }

    public void setDirtyCallback(Runnable callback) {
        this.brewingInventory.setDirtyCallback(callback);
    }

    public void readNbt(NbtCompound nbt) {
        this.brewTime = nbt.getShort("BrewTime");
        this.brewFuel = nbt.getShort("BrewFuel");
        DefaultedListInventory.readFromNbt(this.brewingStack, nbt, "BrewingItems");
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putShort("BrewTime", (short) this.brewTime);
        nbt.putShort("BrewFuel", (short) this.brewFuel);
        DefaultedListInventory.writeToNbt(this.brewingStack, nbt, "BrewingItems");
    }

    private boolean canBrew() {
        ItemStack ingredient = this.brewingStack.get(3);
        if (ingredient.isEmpty()) return false;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty() && BrewingRecipeRegistry.hasRecipe(bottle, ingredient)) {
                return true;
            }
        }
        return false;
    }

    private void craftBrew() {
        ItemStack ingredient = this.brewingStack.get(3);
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty() && BrewingRecipeRegistry.hasRecipe(bottle, ingredient)) {
                ItemStack result = BrewingRecipeRegistry.craft(ingredient, bottle);
                this.brewingStack.set(i, result);
            }
        }
        if (ingredient.getItem().hasRecipeRemainder()) {
            this.brewingStack.set(3, new ItemStack(ingredient.getItem().getRecipeRemainder()));
        } else {
            ingredient.decrement(1);
            if (ingredient.isEmpty()) {
                this.brewingStack.set(3, ItemStack.EMPTY);
            }
        }
    }
}
