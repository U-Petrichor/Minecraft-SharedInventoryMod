package com.umut.sharedInventory.inventory;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;

public class FurnaceLogic {
    private static final int DEFAULT_COOK_TIME = 200;
    private static Map<Item, Integer> fuelTimeMap;

    private final DefaultedList<ItemStack> furnaceStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final DefaultedListInventory furnaceInventory = new DefaultedListInventory(furnaceStack);
    private int burnTime;
    private int fuelTime;
    private int cookTime;
    private int totalCookTime;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return FurnaceLogic.this.burnTime;
                case 1: return FurnaceLogic.this.fuelTime;
                case 2: return FurnaceLogic.this.cookTime;
                case 3: return FurnaceLogic.this.totalCookTime;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: FurnaceLogic.this.burnTime = value; break;
                case 1: FurnaceLogic.this.fuelTime = value; break;
                case 2: FurnaceLogic.this.cookTime = value; break;
                case 3: FurnaceLogic.this.totalCookTime = value; break;
            }
        }

        @Override
        public int size() { return 4; }
    };

    public void tick(World world) {
        boolean wasBurning = isBurning();
        boolean isDirty = false;

        if (isBurning()) {
            --this.burnTime;
        }

        if (world != null && !world.isClient) {
            ItemStack fuelStack = this.furnaceStack.get(1);
            if (isBurning() || (!fuelStack.isEmpty() && !this.furnaceStack.get(0).isEmpty())) {
                RecipeType<SmeltingRecipe> recipeType = RecipeType.SMELTING;
                Optional<SmeltingRecipe> recipe = world.getRecipeManager().getFirstMatch(recipeType, this.furnaceInventory, world);

                if (!isBurning() && canAcceptRecipeOutput(recipe.orElse(null))) {
                    this.burnTime = getFuelTime(fuelStack);
                    this.fuelTime = this.burnTime;

                    if (isBurning()) {
                        isDirty = true;
                        if (!fuelStack.isEmpty()) {
                            Item item = fuelStack.getItem();
                            fuelStack.decrement(1);
                            if (fuelStack.isEmpty()) {
                                Item remainder = item.getRecipeRemainder();
                                this.furnaceStack.set(1, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
                            }
                        }
                    }
                }

                if (isBurning() && canAcceptRecipeOutput(recipe.orElse(null))) {
                    if (this.totalCookTime == 0) {
                        this.totalCookTime = getCookTime(world, recipeType, this.furnaceInventory);
                    }
                    ++this.cookTime;
                    if (this.cookTime == this.totalCookTime) {
                        this.cookTime = 0;
                        this.totalCookTime = getCookTime(world, recipeType, this.furnaceInventory);
                        if (craftRecipe(recipe.orElse(null))) {
                            isDirty = true;
                        }
                    }
                } else {
                    this.cookTime = 0;
                }
            } else if (!isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
            }

            if (wasBurning != isBurning()) {
                isDirty = true;
            }
        }
    }

    public boolean isBurning() { return this.burnTime > 0; }

    public DefaultedList<ItemStack> getFurnaceStack() { return this.furnaceStack; }

    public PropertyDelegate getPropertyDelegate() { return this.propertyDelegate; }

    public Inventory getFurnaceInventory() { return this.furnaceInventory; }

    public void setDirtyCallback(Runnable callback) {
        this.furnaceInventory.setDirtyCallback(callback);
    }

    public void readNbt(NbtCompound nbt) {
        this.burnTime = nbt.getShort("BurnTime");
        this.cookTime = nbt.getShort("CookTime");
        this.totalCookTime = nbt.getShort("CookTimeTotal");
        DefaultedListInventory.readFromNbt(this.furnaceStack, nbt, "FurnaceItems");
        this.fuelTime = getFuelTime(this.furnaceStack.get(1));
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putShort("BurnTime", (short) this.burnTime);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.totalCookTime);
        DefaultedListInventory.writeToNbt(this.furnaceStack, nbt, "FurnaceItems");
    }

    private boolean canAcceptRecipeOutput(SmeltingRecipe recipe) {
        if (!this.furnaceStack.get(0).isEmpty() && recipe != null) {
            ItemStack result = recipe.getOutput();
            if (result.isEmpty()) return false;
            ItemStack outputStack = this.furnaceStack.get(2);
            if (outputStack.isEmpty()) return true;
            if (!outputStack.isItemEqualIgnoreDamage(result)) return false;
            return outputStack.getCount() + result.getCount() <= outputStack.getMaxCount();
        }
        return false;
    }

    private boolean craftRecipe(SmeltingRecipe recipe) {
        if (recipe != null && canAcceptRecipeOutput(recipe)) {
            ItemStack outputStack = this.furnaceStack.get(2);
            ItemStack resultStack = recipe.getOutput();
            if (outputStack.isEmpty()) {
                this.furnaceStack.set(2, resultStack.copy());
            } else if (outputStack.isOf(resultStack.getItem())) {
                outputStack.increment(resultStack.getCount());
            }
            this.furnaceStack.get(0).decrement(1);
            return true;
        }
        return false;
    }

    private int getCookTime(World world, RecipeType<SmeltingRecipe> recipeType, Inventory inventory) {
        return world.getRecipeManager().getFirstMatch(recipeType, inventory, world).map(SmeltingRecipe::getCookTime).orElse(DEFAULT_COOK_TIME);
    }

    private int getFuelTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        if (fuelTimeMap == null) {
            fuelTimeMap = AbstractFurnaceBlockEntity.createFuelTimeMap();
        }
        return fuelTimeMap.getOrDefault(fuel.getItem(), 0);
    }
}
