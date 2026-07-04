package com.petrichor.sharedInventory.inventory;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * 熔炉逻辑 — 从 PrivateInventory 中提取的独立熔炉模块
 *
 * 槽位布局 (furnaceStack):
 *   [0] 输入 (待烧炼物品)
 *   [1] 燃料
 *   [2] 输出 (烧炼结果)
 *
 * 烧炼流程: tick() 每游戏刻调用 → 消耗燃料 → 累加 cookTime → 达到 totalCookTime 时产出结果
 * PropertyDelegate 用于 ScreenHandler 同步燃烧/烧炼进度到客户端
 */
public class FurnaceLogic {
    /** 槽位: [0]输入 [1]燃料 [2]输出 */
    private final DefaultedList<ItemStack> furnaceStack = DefaultedList.ofSize(3, ItemStack.EMPTY);
    /** 当前剩余燃烧时间 (每 tick -1) */
    private int burnTime;
    /** 当前燃料的总燃烧时间 (用于计算火焰进度) */
    private int fuelTime;
    /** 当前已烧炼时间 */
    private int cookTime;
    /** 当前配方的总烧炼时间 (默认 200 tick = 10 秒) */
    private int totalCookTime;

    /** 属性委托: 向客户端同步 burnTime/fuelTime/cookTime/totalCookTime */
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

    /** 每游戏刻执行: 消耗燃料、推进烧炼、产出结果 */
    public void tick(World world) {
        boolean wasBurning = isBurning();
        boolean isDirty = false;

        if (isBurning()) {
            --this.burnTime;
        }

        if (world != null && !world.isClient) {
            ItemStack fuelStack = this.furnaceStack.get(1);
            if (isBurning() || !fuelStack.isEmpty() && !this.furnaceStack.get(0).isEmpty()) {
                RecipeType<SmeltingRecipe> recipeType = RecipeType.SMELTING;
                SimpleInventory inventory = new SimpleInventory(this.furnaceStack.get(0));

                Optional<SmeltingRecipe> recipe = world.getRecipeManager().getFirstMatch(recipeType, inventory, world);

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
                    ++this.cookTime;
                    if (this.cookTime == this.totalCookTime) {
                        this.cookTime = 0;
                        this.totalCookTime = getCookTime(world, recipeType, inventory);
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

    /** 是否正在燃烧 (burnTime > 0) */
    public boolean isBurning() { return this.burnTime > 0; }

    public DefaultedList<ItemStack> getFurnaceStack() { return this.furnaceStack; }

    public PropertyDelegate getPropertyDelegate() { return this.propertyDelegate; }

    /** 将 furnaceStack 包装为 Inventory，供 Slot 系统使用 */
    public Inventory getFurnaceInventory() {
        return new Inventory() {
            @Override public int size() { return furnaceStack.size(); }
            @Override public boolean isEmpty() { for (ItemStack stack : furnaceStack) { if (!stack.isEmpty()) return false; } return true; }
            @Override public ItemStack getStack(int slot) { return furnaceStack.get(slot); }
            @Override public ItemStack removeStack(int slot, int amount) { return net.minecraft.inventory.Inventories.splitStack(furnaceStack, slot, amount); }
            @Override public ItemStack removeStack(int slot) { return net.minecraft.inventory.Inventories.removeStack(furnaceStack, slot); }
            @Override public void setStack(int slot, ItemStack stack) { furnaceStack.set(slot, stack); }
            @Override public void markDirty() {}
            @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
            @Override public void clear() { furnaceStack.clear(); }
        };
    }

    /** 从 NBT 读取熔炉状态 (存档加载时调用) */
    public void readNbt(NbtCompound nbt) {
        this.burnTime = nbt.getShort("BurnTime");
        this.cookTime = nbt.getShort("CookTime");
        this.totalCookTime = nbt.getShort("CookTimeTotal");
        this.fuelTime = getFuelTime(this.furnaceStack.get(1));

        NbtList furnaceItems = nbt.getList("FurnaceItems", 10);
        this.furnaceStack.clear();
        for (int i = 0; i < furnaceItems.size(); ++i) {
            NbtCompound itemTag = furnaceItems.getCompound(i);
            int slot = itemTag.getByte("Slot");
            if (slot >= 0 && slot < this.furnaceStack.size()) {
                this.furnaceStack.set(slot, ItemStack.fromNbt(itemTag));
            }
        }
    }

    /** 将熔炉状态写入 NBT (存档保存时调用) */
    public void writeNbt(NbtCompound nbt) {
        nbt.putShort("BurnTime", (short) this.burnTime);
        nbt.putShort("CookTime", (short) this.cookTime);
        nbt.putShort("CookTimeTotal", (short) this.totalCookTime);

        NbtList furnaceItems = new NbtList();
        for (int i = 0; i < this.furnaceStack.size(); ++i) {
            ItemStack stack = this.furnaceStack.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                stack.writeNbt(itemTag);
                furnaceItems.add(itemTag);
            }
        }
        nbt.put("FurnaceItems", furnaceItems);
    }

    /** 检查输出槽能否容纳配方结果 (物品相同且数量未超限) */
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

    /** 执行烧炼: 将结果放入输出槽，消耗一个输入物品 */
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

    /** 获取当前配方的烧炼时间，无配方时默认 200 tick */
    private int getCookTime(World world, RecipeType<SmeltingRecipe> recipeType, Inventory inventory) {
        return world.getRecipeManager().getFirstMatch(recipeType, inventory, world).map(SmeltingRecipe::getCookTime).orElse(200);
    }

    /** 查询燃料的燃烧时间 (使用原版燃料表) */
    private int getFuelTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        return AbstractFurnaceBlockEntity.createFuelTimeMap().getOrDefault(fuel.getItem(), 0);
    }
}
