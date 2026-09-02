package com.petrichor.sharedInventory.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FuelRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Optional;

public class FurnaceLogic {
    private static final int DEFAULT_COOK_TIME = 200;

    private final DefaultedList<ItemStack> furnaceStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final DefaultedListInventory furnaceInventory = new DefaultedListInventory(furnaceStack);
    private int burnTime;
    private int fuelTime;
    private int cookTime;
    private int totalCookTime;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FurnaceLogic.this.burnTime;
                case 1 -> FurnaceLogic.this.fuelTime;
                case 2 -> FurnaceLogic.this.cookTime;
                case 3 -> FurnaceLogic.this.totalCookTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> FurnaceLogic.this.burnTime = value;
                case 1 -> FurnaceLogic.this.fuelTime = value;
                case 2 -> FurnaceLogic.this.cookTime = value;
                case 3 -> FurnaceLogic.this.totalCookTime = value;
            }
        }

        @Override
        public int size() { return 4; }
    };

    private FuelRegistry fuelRegistry;

    public void tick(World world) {
        boolean wasBurning = isBurning();

        if (isBurning()) {
            --this.burnTime;
        }

        if (world != null && !world.isClient() && world.getServer() != null) {
            if (fuelRegistry == null) {
                fuelRegistry = FuelRegistry.createDefault(world.getRegistryManager(), world.getServer().getSaveProperties().getEnabledFeatures());
            }

            ItemStack fuelStack = this.furnaceStack.get(1);
            ItemStack inputStack = this.furnaceStack.get(0);
            if (isBurning() || (!fuelStack.isEmpty() && !inputStack.isEmpty())) {
                SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(inputStack);
                Optional<RecipeEntry<SmeltingRecipe>> recipeEntry = world.getServer().getRecipeManager().getFirstMatch(RecipeType.SMELTING, recipeInput, world);
                SmeltingRecipe recipe = recipeEntry.map(RecipeEntry::value).orElse(null);

                if (!isBurning() && canAcceptRecipeOutput(recipe, recipeInput, world)) {
                    this.burnTime = getFuelTime(fuelStack);
                    this.fuelTime = this.burnTime;

                    if (isBurning()) {
                        furnaceInventory.markDirty();
                        if (!fuelStack.isEmpty()) {
                            ItemStack remainder = fuelStack.getItem().getRecipeRemainder();
                            fuelStack.decrement(1);
                            if (fuelStack.isEmpty()) {
                                this.furnaceStack.set(1, remainder.isEmpty() ? ItemStack.EMPTY : remainder.copy());
                            }
                        }
                    }
                }

                if (isBurning() && canAcceptRecipeOutput(recipe, recipeInput, world)) {
                    if (this.totalCookTime == 0) {
                        this.totalCookTime = getCookTime(recipe);
                    }
                    ++this.cookTime;
                    if (this.cookTime == this.totalCookTime) {
                        this.cookTime = 0;
                        this.totalCookTime = getCookTime(recipe);
                        if (craftRecipe(recipe, recipeInput, world)) {
                            furnaceInventory.markDirty();
                        }
                    }
                } else {
                    this.cookTime = 0;
                }
            } else if (!isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
            }

            if (wasBurning != isBurning()) {
                furnaceInventory.markDirty();
            }
        }
    }

    public boolean isBurning() { return this.burnTime > 0; }

    public DefaultedList<ItemStack> getFurnaceStack() { return this.furnaceStack; }

    public PropertyDelegate getPropertyDelegate() { return this.propertyDelegate; }

    public Inventory getFurnaceInventory() {
        return this.furnaceInventory;
    }

    public void setDirtyCallback(Runnable callback) {
        this.furnaceInventory.setDirtyCallback(callback);
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        this.burnTime = nbt.getShort("BurnTime").orElse((short) 0);
        this.cookTime = nbt.getShort("CookTime").orElse((short) 0);
        this.totalCookTime = nbt.getShort("CookTimeTotal").orElse((short) 0);
        DefaultedListInventory.readFromNbt(this.furnaceStack, nbt, "FurnaceItems", registryLookup);
        this.fuelTime = getFuelTime(this.furnaceStack.get(1));
    }

    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putShort("BurnTime", (short) this.burnTime);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.totalCookTime);
        DefaultedListInventory.writeToNbt(this.furnaceStack, nbt, "FurnaceItems", registryLookup);
    }

    private boolean canAcceptRecipeOutput(SmeltingRecipe recipe, SingleStackRecipeInput recipeInput, World world) {
        if (!this.furnaceStack.get(0).isEmpty() && recipe != null) {
            ItemStack result = recipe.craft(recipeInput, world.getRegistryManager());
            if (result.isEmpty()) return false;
            ItemStack outputStack = this.furnaceStack.get(2);
            if (outputStack.isEmpty()) return true;
            if (!ItemStack.areItemsAndComponentsEqual(outputStack, result)) return false;
            return outputStack.getCount() + result.getCount() <= outputStack.getMaxCount();
        }
        return false;
    }

    private boolean craftRecipe(SmeltingRecipe recipe, SingleStackRecipeInput recipeInput, World world) {
        if (recipe != null && canAcceptRecipeOutput(recipe, recipeInput, world)) {
            ItemStack outputStack = this.furnaceStack.get(2);
            ItemStack resultStack = recipe.craft(recipeInput, world.getRegistryManager());
            if (outputStack.isEmpty()) {
                this.furnaceStack.set(2, resultStack.copy());
            } else if (outputStack.isOf(resultStack.getItem())) {
                outputStack.increment(resultStack.getCount());
            }
            this.furnaceStack.getFirst().decrement(1);
            return true;
        }
        return false;
    }

    private int getCookTime(SmeltingRecipe recipe) {
        return recipe != null ? recipe.getCookingTime() : DEFAULT_COOK_TIME;
    }

    private int getFuelTime(ItemStack fuel) {
        if (fuel.isEmpty() || fuelRegistry == null) return 0;
        return fuelRegistry.getFuelTicks(fuel);
    }
}