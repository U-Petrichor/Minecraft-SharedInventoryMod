package com.petrichor.sharedInventory.screen;

import com.mojang.datafixers.util.Pair;
import com.petrichor.sharedInventory.inventory.AnvilData;
import com.petrichor.sharedInventory.inventory.ModObjects;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import com.petrichor.sharedInventory.mixin.ScreenHandlerAccessor;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;


/**
 * 共享存储界面处理器 — 管理所有 Slot 布局、翻页、Shift+点击、合成台逻辑
 *
 * 布局区域 (按 Slot 顺序):
 *   1. 私人背包 (6×10) — 通过 PrivateInventory 分页访问
 *   2. 公共背包 (4×4) — 通过 BackpackInventory 访问共享核心
 *   3. 工具区域 (动态) — 根据 activeTool 切换合成/熔炉/酿造/铁砧/锻造
 *   4. 盔甲 + 副手 (4+1)
 *   5. 玩家物品栏 (3×9) + 快捷栏 (9)
 *
 * 关键机制:
 *   - rebuildSlots(): 切换工具时完全重建 Slot，通过 firstTime 标志区分首次/后续构建
 *   - transferSlot(): Shift+点击物品转移逻辑
 *   - updateResult(): 合成台结果更新
 */
public class SharedInventoryScreenHandler extends AbstractRecipeScreenHandler<CraftingInventory> {

    /** 客户端回调接口，用于从 ScreenHandler 发送网络包而不直接依赖客户端 API */
    public interface ClientCallback {
        void sendPageUpdate(int page);
        void sendLabelUpdate(int action, int page, String label);
    }

    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private final SharedInventoryPlayerEntity sharedInventoryPlayerEntity;
    private ClientCallback clientCallback;

    // 当前选中的工具类型
    private ToolType activeTool = ToolType.CRAFTING;

    // 盔甲槽位纹理
    public static final Identifier EMPTY_HELMET_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_helmet");
    public static final Identifier EMPTY_CHESTPLATE_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_chestplate");
    public static final Identifier EMPTY_LEGGINGS_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_leggings");
    public static final Identifier EMPTY_BOOTS_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_boots");
    public static final Identifier EMPTY_OFFHAND_ARMOR_SLOT = new Identifier("item/empty_armor_slot_shield");
    private final Identifier[] EMPTY_ARMOR_SLOT_TEXTURES = new Identifier[]{
            EMPTY_BOOTS_SLOT_TEXTURE, EMPTY_LEGGINGS_SLOT_TEXTURE, EMPTY_CHESTPLATE_SLOT_TEXTURE, EMPTY_HELMET_SLOT_TEXTURE
    };
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // 合成台
    private final CraftingInventory craftingInput = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory craftingResult = new CraftingResultInventory();

    // Slot 区域的索引边界，用于 transferSlot
    private int privateSlotStart;
    private int privateSlotEnd;
    private int publicSlotStart;
    private int publicSlotEnd;
    private int toolSlotStart;
    private int toolSlotEnd;
    private int armorSlotStart;
    private int armorSlotEnd;
    private int playerInvSlotStart;
    private int playerInvSlotEnd;

    public SharedInventoryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(16));
    }

    public SharedInventoryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModObjects.SHARED_INVENTORY_SCREEN_HANDLER, syncId);
        checkSize(inventory, 16);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        if (!(playerInventory.player instanceof SharedInventoryPlayerEntity)) {
            throw new IllegalStateException("PlayerEntity must implement SharedInventoryPlayerEntity (Mixin not applied?)");
        }
        this.sharedInventoryPlayerEntity = (SharedInventoryPlayerEntity) playerInventory.player;

        this.addProperties(this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate());
        this.addProperties(this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingPropertyDelegate());

        inventory.onOpen(playerInventory.player);
        rebuildSlots();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public void setClientCallback(ClientCallback callback) { this.clientCallback = callback; }

    public ToolType getActiveTool() {
        return this.activeTool;
    }

    public void setActiveTool(ToolType tool) {
        if (this.activeTool == tool) return;
        this.activeTool = tool;
        rebuildSlots();
    }

    /**
     * 完全重建所有 Slot。布局顺序：
     * 1. 私人背包 (6×10)
     * 2. 公共背包 (4×4)
     * 3. 工具区域 (根据 activeTool 动态)
     * 4. 盔甲 + 副手
     * 5. 玩家物品栏 (3×9)
     * 6. 快捷栏 (9)
     */
    private void rebuildSlots() {
        // 切换工具前，将合成格中的物品退还给玩家
        if (!this.craftingInput.isEmpty() && !this.playerInventory.player.getWorld().isClient) {
            this.dropInventory(this.playerInventory.player, this.craftingInput);
        }
        this.craftingResult.clear();
        this.craftingInput.clear();

        this.slots.clear();
        ((ScreenHandlerAccessor) this).getTrackedStacks().clear();
        ((ScreenHandlerAccessor) this).getPreviousTrackedStacks().clear();

        // === 1. 私人背包 6×10 ===
        // 起始 (22,35)，列间距18，行间距18
        privateSlotStart = this.slots.size();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 10; j++) {
                this.addSlot(new Slot(sharedInventoryPlayerEntity.shared$getPrivateInventory(),
                        j + i * 10, 22 + j * 18, 35 + i * 18));
            }
        }
        privateSlotEnd = this.slots.size();

        // === 2. 公共背包 4×4 ===
        // 起始 (225,36)，列间距19，行间距19
        publicSlotStart = this.slots.size();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new Slot(inventory, i * 4 + j, 225 + j * 19, 36 + i * 19));
            }
        }
        publicSlotEnd = this.slots.size();

        // === 3. 工具区域 ===
        // 区域范围 (221,178) 到 (301,234)
        toolSlotStart = this.slots.size();
        addToolSlots();
        toolSlotEnd = this.slots.size();

        // === 4. 盔甲 + 副手 ===
        // 盔甲横向排列：(215,124), (233,124), (251,124), (269,124)
        // 副手：(290,124)
        armorSlotStart = this.slots.size();
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot equipmentSlot = EQUIPMENT_SLOT_ORDER[i];
            this.addSlot(new Slot(playerInventory, 39 - i, 215 + i * 18, 124) {
                @Override
                public int getMaxItemCount() {
                    return 1;
                }

                @Override
                public boolean canInsert(ItemStack stack) {
                    return equipmentSlot == MobEntity.getPreferredEquipmentSlot(stack);
                }

                @Override
                public boolean canTakeItems(PlayerEntity playerEntity) {
                    ItemStack itemStack = this.getStack();
                    return !itemStack.isEmpty() && !playerEntity.getAbilities().creativeMode && EnchantmentHelper.hasBindingCurse(itemStack)
                            ? false : super.canTakeItems(playerEntity);
                }

                @Override
                public Pair<Identifier, Identifier> getBackgroundSprite() {
                    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_ARMOR_SLOT_TEXTURES[equipmentSlot.getEntitySlotId()]);
                }
            });
        }
        this.addSlot(new Slot(playerInventory, 40, 290, 124) {
            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT);
            }
        });
        armorSlotEnd = this.slots.size();

        // === 5. 玩家物品栏 3×9 ===
        // 起始 (22,157)，列间距18
        playerInvSlotStart = this.slots.size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 22 + j * 18, 157 + i * 18));
            }
        }

        // === 6. 快捷栏 9 ===
        // 起始 (22,213)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 22 + i * 18, 213));
        }
        playerInvSlotEnd = this.slots.size();
    }

    /**
     * 根据 activeTool 添加对应的工具 Slot
     * 所有工具区域严格限制在 (221,178) 到 (301,234) 范围内
     */
    private void addToolSlots() {
        switch (activeTool) {
            case CRAFTING:
                addCraftingSlots();
                break;
            case FURNACE:
                addFurnaceSlots();
                break;
            case BREWING:
                addBrewingSlots();
                break;
            case ANVIL:
                addAnvilSlots();
                break;
            case SMITHING:
                addSmithingSlots();
                break;
        }
    }

    private void addCraftingSlots() {
        // 3×3 合成格 起始 (223,180)，间距18
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.craftingInput, j + i * 3, 223 + j * 18, 180 + i * 18));
            }
        }
        // 合成结果 (284,198)
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInput, this.craftingResult, 0, 284, 198));
    }

    private void addFurnaceSlots() {
        Inventory furnaceInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getFurnaceInventory();
        // 输入 (223,180)
        this.addSlot(new Slot(furnaceInventory, 0, 223, 180));
        // 燃料 (223,216)
        this.addSlot(new Slot(furnaceInventory, 1, 223, 216) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return AbstractFurnaceBlockEntity.canUseAsFuel(stack) || stack.isOf(Items.BUCKET);
            }
        });
        // 输出 (261,198)
        this.addSlot(new Slot(furnaceInventory, 2, 261, 198) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
    }

    private void addBrewingSlots() {
        Inventory brewingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingInventory();
        // 烈焰粉 (223,180) — overlay (2,2)
        this.addSlot(new Slot(brewingInventory, 4, 223, 180) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BLAZE_POWDER);
            }
        });
        // 材料 (260,180) — overlay (39,2)
        this.addSlot(new Slot(brewingInventory, 3, 260, 180) {
            @Override
            public boolean canInsert(ItemStack stack) {
                // 通过 EMPTY 静态实例检查是否为酿造材料
                return net.minecraft.recipe.BrewingRecipeRegistry.EMPTY.isValidIngredient(stack);
            }
        });
        // 3 个药水瓶 (238,212), (260,217), (282,212) — 只允许药水类物品
        for (int i = 0; i < 3; i++) {
            final int bottleSlot = i;
            int x = 238 + i * 22;
            int y = (i == 1) ? 217 : 212;
            this.addSlot(new Slot(brewingInventory, bottleSlot, x, y) {
                @Override
                public int getMaxItemCount() {
                    return 1;
                }

                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION)
                            || stack.isOf(Items.LINGERING_POTION);
                }
            });
        }
    }

    private void addAnvilSlots() {
        Inventory anvilInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getAnvilInventory();
        AnvilData anvilData = sharedInventoryPlayerEntity.shared$getPrivateInventory().getAnvilData();
        // 输入1 (223,198) — markDirty 时触发 onContentChanged
        this.addSlot(new Slot(anvilInventory, 0, 223, 198) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        // 输入2 (251,198) — markDirty 时触发 onContentChanged
        this.addSlot(new Slot(anvilInventory, 1, 251, 198) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        // 输出 (283,198) — 取出时消耗输入并扣除经验
        this.addSlot(new Slot(anvilInventory, 2, 283, 198) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                int cost = anvilData.getRepairCost();
                anvilInventory.setStack(0, ItemStack.EMPTY);
                anvilInventory.setStack(1, ItemStack.EMPTY);
                anvilData.setRepairCost(0);
                if (!player.getWorld().isClient && !player.isCreative()) {
                    player.addExperienceLevels(-cost);
                }
            }
        });
    }

    private void addSmithingSlots() {
        Inventory smithingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getSmithingInventory();
        // 模板 (224,181) — markDirty 时触发 onContentChanged
        this.addSlot(new Slot(smithingInventory, 0, 224, 181) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        // 材料 (242,181) — markDirty 时触发 onContentChanged
        this.addSlot(new Slot(smithingInventory, 1, 242, 181) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        // 输入 (260,181) — markDirty 时触发 onContentChanged
        this.addSlot(new Slot(smithingInventory, 2, 260, 181) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        // 输出 (283,216) — 取出时消耗模板、材料、输入
        this.addSlot(new Slot(smithingInventory, 3, 283, 216) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                smithingInventory.setStack(0, ItemStack.EMPTY);
                smithingInventory.setStack(1, ItemStack.EMPTY);
                smithingInventory.setStack(2, ItemStack.EMPTY);
            }
        });
    }

    // === 翻页逻辑 ===

    public void onPreviousPageButtonClicked() {
        if (getCurrentPage() >= 2 && getCurrentPage() <= getMaxPage()) {
            setCurrentPage(getCurrentPage() - 1);
            sendPageUpdatePacket();
        }
    }

    public void onNextPageButtonClicked() {
        if (getCurrentPage() >= 1 && getCurrentPage() <= getMaxPage() - 1) {
            setCurrentPage(getCurrentPage() + 1);
            sendPageUpdatePacket();
        }
    }

    public void onCurrentButtonClicked(int page) {
        setCurrentPage(page);
        sendPageUpdatePacket();
    }

    private void sendPageUpdatePacket() {
        if (clientCallback != null) {
            clientCallback.sendPageUpdate(getCurrentPage());
        }
    }

    // === 标签逻辑 ===

    public void setLabel(int page, String label) {
        sharedInventoryPlayerEntity.shared$getPrivateInventory().setPageLabel(page, label);
        if (clientCallback != null) {
            clientCallback.sendLabelUpdate(0, page, label);
        }
    }

    public void jumpToLabel(String label) {
        int page = sharedInventoryPlayerEntity.shared$getPrivateInventory().findPageByLabel(label);
        if (page > 0) {
            onCurrentButtonClicked(page);
        }
    }

    public Map<Integer, String> getAllLabels() {
        return sharedInventoryPlayerEntity.shared$getPrivateInventory().getAllLabels();
    }

    private int getCurrentPage() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getCurrentPage();
    }

    private int getMaxPage() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage();
    }

    private void setCurrentPage(int page) {
        sharedInventoryPlayerEntity.shared$getPrivateInventory().setCurrentPage(page);
    }

    // === 熔炉进度 ===

    public int getCookProgress() {
        int i = this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate().get(2);
        int j = this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate().get(3);
        return j != 0 && i != 0 ? i * 24 / j : 0;
    }

    public int getFuelProgress() {
        int i = this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate().get(1);
        if (i == 0) {
            i = 200;
        }
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate().get(0) * 13 / i;
    }

    public boolean isBurning() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate().get(0) > 0;
    }

    // === 酿造进度 ===

    /** 酿造进度 0-400，400=完成 */
    public int getBrewTime() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingPropertyDelegate().get(0);
    }

    /** 酿造燃料剩余份数 (每份 20 次酿造) */
    public int getBrewFuel() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingPropertyDelegate().get(1);
    }

    // === Shift+点击 ===

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot >= playerInvSlotStart && invSlot < playerInvSlotEnd) {
                // 从玩家物品栏/快捷栏 → 优先放入工具区，再公共背包，再私人背包
                if (!this.insertItem(originalStack, toolSlotStart, toolSlotEnd, false)) {
                    if (!this.insertItem(originalStack, publicSlotStart, publicSlotEnd, false)) {
                        if (!this.insertItem(originalStack, privateSlotStart, privateSlotEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else if (invSlot >= privateSlotStart && invSlot < privateSlotEnd) {
                // 从私人背包 → 玩家物品栏
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= publicSlotStart && invSlot < publicSlotEnd) {
                // 从公共背包 → 玩家物品栏
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= toolSlotStart && invSlot < toolSlotEnd) {
                // 从工具区 → 玩家物品栏
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= armorSlotStart && invSlot < armorSlotEnd) {
                // 从盔甲 → 玩家物品栏
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    // === 关闭时处理 ===

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.craftingResult.clear();
        if (!player.getWorld().isClient) {
            this.dropInventory(player, this.craftingInput);
        }
    }

    // === 合成台逻辑 ===

    @Override
    public void onContentChanged(Inventory inventory) {
        switch (this.activeTool) {
            case CRAFTING:
                updateResult(this, this.playerInventory.player.getWorld(), this.playerInventory.player, this.craftingInput, this.craftingResult);
                break;
            case ANVIL:
                updateAnvilResult();
                break;
            case SMITHING:
                updateSmithingResult();
                break;
        }
    }

    private void updateResult(
            ScreenHandler handler, World world, PlayerEntity player, CraftingInventory craftingInventory, CraftingResultInventory resultInventory
    ) {
        if (!world.isClient) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
            ItemStack itemStack = ItemStack.EMPTY;
            Optional<RecipeEntry<CraftingRecipe>> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);
            if (optional.isPresent()) {
                CraftingRecipe craftingRecipe = optional.get().value();
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, optional.get())) {
                    itemStack = craftingRecipe.craft(craftingInventory, world.getRegistryManager());
                }
            }

            int resultSlotIndex = this.toolSlotStart + 9;
            resultInventory.setStack(0, itemStack);
            handler.setPreviousTrackedSlot(resultSlotIndex, itemStack);
            serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), resultSlotIndex, itemStack));
        }
    }

    // === 铁砧逻辑 ===

    private void updateAnvilResult() {
        if (this.playerInventory.player.getWorld().isClient) return;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) this.playerInventory.player;
        AnvilData anvilData = sharedInventoryPlayerEntity.shared$getPrivateInventory().getAnvilData();
        Inventory anvilInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getAnvilInventory();
        ItemStack input1 = anvilInventory.getStack(0);
        ItemStack input2 = anvilInventory.getStack(1);

        if (input1.isEmpty()) {
            setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
            return;
        }

        int materialCost = 0;
        ItemStack result = input1.copy();
        int priorWork = result.getComponents().getOrDefault(DataComponentTypes.REPAIR_COST, 0);
        int renameCost = 0;

        if (!input2.isEmpty()) {
            priorWork += input2.getComponents().getOrDefault(DataComponentTypes.REPAIR_COST, 0);
            boolean isBook = input2.isOf(Items.ENCHANTED_BOOK)
                    && input2.contains(DataComponentTypes.STORED_ENCHANTMENTS);

            // 材料修复 (Item.canRepair)
            if (result.isDamageable() && result.getItem().canRepair(input1, input2)) {
                int damage = result.getDamage();
                int repairAmount = Math.min(damage, result.getMaxDamage() / 4);
                if (repairAmount > 0) {
                    int itemsUsed = 0;
                    while (repairAmount > 0 && itemsUsed < input2.getCount()) {
                        result.setDamage(result.getDamage() - repairAmount);
                        materialCost++;
                        repairAmount = Math.min(result.getDamage(), result.getMaxDamage() / 4);
                        itemsUsed++;
                    }
                    // 材料修复不走附魔合并，直接跳到重命名
                    finishAnvilResult(anvilData, anvilInventory, result, input1, input2, materialCost, priorWork, serverPlayer);
                    return;
                }
            }

            // 合并条件检查：不是书且不是同类物品 → 无效
            if (!isBook && !result.isOf(input2.getItem())) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }
            // 同类但不可损坏 → 无效
            if (!isBook && result.isOf(input2.getItem()) && !result.isDamageable()) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }

            // 耐久度合并 (同类可损坏物品)
            if (result.isDamageable() && !isBook) {
                int input1Durability = input1.getMaxDamage() - input1.getDamage();
                int input2Durability = input2.getMaxDamage() - input2.getDamage();
                int bonus = input2Durability * result.getMaxDamage() * 12 / 100;
                int totalDurability = input1Durability + bonus;
                int newDamage = result.getMaxDamage() - totalDurability;
                if (newDamage < 0) newDamage = 0;
                if (newDamage < result.getDamage()) {
                    result.setDamage(newDamage);
                    materialCost += 2;
                }
            }

            // 附魔合并
            ItemEnchantmentsComponent resultEnchantments = result.getEnchantments();
            ItemEnchantmentsComponent input2Enchantments = isBook
                    ? input2.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                    : input2.getEnchantments();

            boolean hasCompatible = false;
            boolean hasIncompatible = false;

            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(resultEnchantments);

            for (var entry : input2Enchantments.getEnchantmentsMap()) {
                Enchantment ench = entry.getKey().value();
                if (ench == null) continue;
                int newLevel = entry.getIntValue();
                int currentLevel = builder.getLevel(ench);

                if (currentLevel == newLevel) {
                    newLevel = Math.min(newLevel + 1, ench.getMaxLevel());
                } else {
                    newLevel = Math.max(newLevel, currentLevel);
                }

                // 检查附魔是否适用于该物品
                boolean canApply = ench.isAcceptableItem(input1);
                if (serverPlayer.getAbilities().creativeMode && input1.isOf(Items.ENCHANTED_BOOK)) {
                    canApply = true;
                }

                // 检查与已有附魔的兼容性
                for (var existingEntry : resultEnchantments.getEnchantments()) {
                    Enchantment existing = existingEntry.value();
                    if (existing != ench && !ench.canCombine(existing)) {
                        canApply = false;
                        materialCost++;
                    }
                }

                if (!canApply) {
                    hasIncompatible = true;
                    continue;
                }

                hasCompatible = true;
                builder.set(ench, newLevel);

                // 附魔费用: anvilCost × level (附魔书半价)
                int anvilCost = ench.getAnvilCost();
                if (isBook) {
                    anvilCost = Math.max(1, anvilCost / 2);
                }
                materialCost += anvilCost * newLevel;

                // 可堆叠物品强制太贵
                if (input1.getCount() > 1) {
                    materialCost = 40;
                }
            }

            // 所有附魔都不兼容
            if (hasIncompatible && !hasCompatible) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }

            EnchantmentHelper.set(result, builder.build());
        }

        finishAnvilResult(anvilData, anvilInventory, result, input1, input2, materialCost, priorWork, serverPlayer);
    }

    /** 铁砧重命名与最终费用计算 */
    private void finishAnvilResult(AnvilData anvilData, Inventory anvilInventory, ItemStack result, ItemStack input1, ItemStack input2, int materialCost, int priorWork, ServerPlayerEntity serverPlayer) {
        int renameCost = 0;

        // 重命名
        String renameText = anvilData.getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            if (!renameText.equals(input1.getName().getString())) {
                renameCost = 1;
                materialCost += renameCost;
                result.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.of(renameText));
            }
        } else if (input1.contains(DataComponentTypes.CUSTOM_NAME)) {
            renameCost = 1;
            materialCost += renameCost;
            result.remove(DataComponentTypes.CUSTOM_NAME);
        }

        int totalCost = priorWork + materialCost;

        // 无材料费用 → 空结果
        if (materialCost <= 0) {
            result = ItemStack.EMPTY;
        }

        // 仅重命名时，费用上限放宽到39
        if (renameCost == materialCost && renameCost > 0 && totalCost >= 40) {
            totalCost = 39;
        }

        // 太贵了
        if (totalCost >= 40 && !serverPlayer.getAbilities().creativeMode) {
            result = ItemStack.EMPTY;
        }

        // 设置结果的先前修复惩罚
        if (!result.isEmpty()) {
            int newRepairCost = result.getComponents().getOrDefault(DataComponentTypes.REPAIR_COST, 0);
            if (!input2.isEmpty()) {
                newRepairCost = Math.max(newRepairCost, input2.getComponents().getOrDefault(DataComponentTypes.REPAIR_COST, 0));
            }
            if (renameCost != materialCost || renameCost == 0) {
                newRepairCost = getNextCost(newRepairCost);
            }
            result.set(DataComponentTypes.REPAIR_COST, newRepairCost);
        }

        setAnvilOutput(anvilInventory, anvilData, result, totalCost >= 40 && !serverPlayer.getAbilities().creativeMode ? 0 : totalCost);
    }

    /** 设置铁砧输出槽并同步 */
    private void setAnvilOutput(Inventory anvilInventory, AnvilData anvilData, ItemStack result, int cost) {
        anvilInventory.setStack(2, result);
        anvilData.setRepairCost(cost);
        sendSlotUpdate(toolSlotStart + 2, result);
    }

    /** 铁砧先前修复惩罚递增: cost * 2 + 1 */
    private static int getNextCost(int cost) {
        return cost * 2 + 1;
    }

    // === 锻造台逻辑 ===

    private void updateSmithingResult() {
        if (this.playerInventory.player.getWorld().isClient) return;
        World world = this.playerInventory.player.getWorld();
        Inventory smithingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getSmithingInventory();

        Optional<RecipeEntry<SmithingRecipe>> optional = world.getRecipeManager().getFirstMatch(RecipeType.SMITHING, smithingInventory, world);
        ItemStack result = optional.map(entry -> entry.value().craft(smithingInventory, world.getRegistryManager())).orElse(ItemStack.EMPTY);

        smithingInventory.setStack(3, result);
        sendSlotUpdate(toolSlotStart + 3, result);
    }

    /** 向客户端发送指定槽位的更新包 */
    private void sendSlotUpdate(int handlerSlotIndex, ItemStack stack) {
        if (!(this.playerInventory.player instanceof ServerPlayerEntity serverPlayer)) return;
        this.setPreviousTrackedSlot(handlerSlotIndex, stack);
        serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(this.syncId, this.nextRevision(), handlerSlotIndex, stack));
    }

    @Override
    public void populateRecipeFinder(RecipeMatcher finder) {
        this.craftingInput.provideRecipeInputs(finder);
    }

    @Override
    public void clearCraftingSlots() {
        this.craftingResult.clear();
        this.craftingInput.clear();
    }

    @Override
    public boolean matches(RecipeEntry<? extends Recipe<CraftingInventory>> recipeEntry) {
        return recipeEntry.value().matches(this.craftingInput, this.playerInventory.player.getWorld());
    }

    @Override
    public int getCraftingResultSlotIndex() {
        return this.activeTool == ToolType.CRAFTING ? 0 : -1;
    }

    @Override
    public int getCraftingWidth() {
        return this.craftingInput.getWidth();
    }

    @Override
    public int getCraftingHeight() {
        return this.craftingInput.getHeight();
    }

    @Override
    public int getCraftingSlotCount() {
        return this.activeTool == ToolType.CRAFTING ? 10 : 0;
    }

    @Override
    public RecipeBookCategory getCategory() {
        return RecipeBookCategory.CRAFTING;
    }

    @Override
    public boolean canInsertIntoSlot(int index) {
        return false;
    }
}
