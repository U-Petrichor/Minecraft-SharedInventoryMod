package com.petrichor.sharedInventory.screen;

import com.mojang.datafixers.util.Pair;
import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.inventory.ModObjects;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import com.petrichor.sharedInventory.network.PageUpdatePacket;
import com.petrichor.sharedInventory.network.LabelUpdatePacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
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

    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private boolean firstTime = true;
    private final SharedInventoryPlayerEntity sharedInventoryPlayerEntity;

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
        this.sharedInventoryPlayerEntity = (SharedInventoryPlayerEntity) playerInventory.player;

        this.addProperties(this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getPropertyDelegate());

        inventory.onOpen(playerInventory.player);
        rebuildSlots();
        firstTime = false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

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
        this.slots.clear();

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
                    return !itemStack.isEmpty() && !playerEntity.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack)
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
        // 合成结果 (285,198)
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInput, this.craftingResult, 0, 285, 198));
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
        // 烈焰粉 (223,180)
        this.addSlot(new Slot(brewingInventory, 4, 223, 180) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BLAZE_POWDER);
            }
        });
        // 材料 (241,180)
        this.addSlot(new Slot(brewingInventory, 3, 241, 180));
        // 3 个药水瓶 (223,216), (241,216), (259,216)
        for (int i = 0; i < 3; i++) {
            this.addSlot(new Slot(brewingInventory, i, 223 + i * 18, 216) {
                @Override
                public int getMaxItemCount() {
                    return 1;
                }
            });
        }
    }

    private void addAnvilSlots() {
        Inventory anvilInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getAnvilInventory();
        // 输入1 (223,180)
        this.addSlot(new Slot(anvilInventory, 0, 223, 180));
        // 输入2 (241,180)
        this.addSlot(new Slot(anvilInventory, 1, 241, 180));
        // 输出 (277,198)
        this.addSlot(new Slot(anvilInventory, 2, 277, 198) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
    }

    private void addSmithingSlots() {
        Inventory smithingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getSmithingInventory();
        // 模板 (223,180)
        this.addSlot(new Slot(smithingInventory, 0, 223, 180) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });
        // 材料 (241,180)
        this.addSlot(new Slot(smithingInventory, 1, 241, 180));
        // 输入 (223,198)
        this.addSlot(new Slot(smithingInventory, 2, 223, 198));
        // 输出 (277,198)
        this.addSlot(new Slot(smithingInventory, 3, 277, 198) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (firstTime)
            return super.addSlot(slot);
        else {
            slot.id = this.slots.size();
            this.slots.add(slot);
        }
        return slot;
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
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(getCurrentPage());
        ClientPlayNetworking.send(SharedInventoryMod.PAGE_UPDATE_ID, buf);
    }

    // === 标签逻辑 ===

    public void setLabel(int page, String label) {
        sharedInventoryPlayerEntity.shared$getPrivateInventory().setPageLabel(page, label);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        LabelUpdatePacket.encode(new LabelUpdatePacket(0, page, label), buf);
        ClientPlayNetworking.send(SharedInventoryMod.LABEL_UPDATE_ID, buf);
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

    // === Shift+点击 ===

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
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
    public void close(PlayerEntity player) {
        super.close(player);
        this.craftingResult.clear();
        if (!player.world.isClient) {
            this.dropInventory(player, this.craftingInput);
        }
    }

    // === 合成台逻辑 ===

    @Override
    public void onContentChanged(Inventory inventory) {
        updateResult(this, this.playerInventory.player.world, this.playerInventory.player, this.craftingInput, this.craftingResult);
    }

    private void updateResult(
            ScreenHandler handler, World world, PlayerEntity player, CraftingInventory craftingInventory, CraftingResultInventory resultInventory
    ) {
        if (!world.isClient) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
            ItemStack itemStack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);
            if (optional.isPresent()) {
                CraftingRecipe craftingRecipe = optional.get();
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory);
                }
            }

            resultInventory.setStack(0, itemStack);
            handler.setPreviousTrackedSlot(0, itemStack);
            serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
        }
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
    public boolean matches(Recipe<? super CraftingInventory> recipe) {
        return recipe.matches(this.craftingInput, this.playerInventory.player.world);
    }

    @Override
    public int getCraftingResultSlotIndex() {
        return 0;
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
        return 10;
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
