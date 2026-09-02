package com.petrichor.sharedInventory.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class BrewingLogic {
    private final DefaultedList<ItemStack> brewingStack = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final DefaultedListInventory brewingInventory = new DefaultedListInventory(brewingStack);
    private int brewTime;
    private int brewFuel;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BrewingLogic.this.brewTime;
                case 1 -> BrewingLogic.this.brewFuel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BrewingLogic.this.brewTime = value;
                case 1 -> BrewingLogic.this.brewFuel = value;
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
            if (ingredient.isEmpty() || !canBrew(world)) {
                this.brewTime = 0;
                return;
            }
            --this.brewTime;
            if (this.brewTime == 0) {
                craftBrew(world);
                if (this.brewFuel > 0) {
                    --this.brewFuel;
                }
            }
        } else {
            if (!ingredient.isEmpty() && this.brewFuel > 0 && canBrew(world)) {
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

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        this.brewTime = nbt.getShort("BrewTime");
        this.brewFuel = nbt.getShort("BrewFuel");
        DefaultedListInventory.readFromNbt(this.brewingStack, nbt, "BrewingItems", registryLookup);
    }

    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putShort("BrewTime", (short) this.brewTime);
        nbt.putShort("BrewFuel", (short) this.brewFuel);
        DefaultedListInventory.writeToNbt(this.brewingStack, nbt, "BrewingItems", registryLookup);
    }

    private boolean canBrew(World world) {
        ItemStack ingredient = this.brewingStack.get(3);
        if (ingredient.isEmpty()) return false;
        BrewingRecipeRegistry registry = world.getBrewingRecipeRegistry();
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty() && registry.hasRecipe(bottle, ingredient)) {
                return true;
            }
        }
        return false;
    }

    private void craftBrew(World world) {
        ItemStack ingredient = this.brewingStack.get(3);
        BrewingRecipeRegistry registry = world.getBrewingRecipeRegistry();
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = this.brewingStack.get(i);
            if (!bottle.isEmpty() && registry.hasRecipe(bottle, ingredient)) {
                ItemStack result = registry.craft(ingredient, bottle);
                if (result != null && !result.isEmpty()) {
                    this.brewingStack.set(i, result);
                }
            }
        }
        ItemStack remainder = ingredient.getItem().getRecipeRemainder();
        if (!remainder.isEmpty()) {
            this.brewingStack.set(3, remainder.copy());
        } else {
            ingredient.decrement(1);
            if (ingredient.isEmpty()) {
                this.brewingStack.set(3, ItemStack.EMPTY);
            }
        }
    }
}