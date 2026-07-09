package com.umut.sharedInventory.screen;

import com.mojang.datafixers.util.Pair;
import com.umut.sharedInventory.inventory.AnvilData;
import com.umut.sharedInventory.inventory.ModObjects;
import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.inventory.ToolType;
import com.umut.sharedInventory.inventory.PrivateInventory;
import com.umut.sharedInventory.mixin.ScreenHandlerAccessor;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
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


public class SharedInventoryScreenHandler extends AbstractRecipeScreenHandler<CraftingInventory> {

    public interface ClientCallback {
        void sendPageUpdate(int page);
        void sendLabelUpdate(int action, int page, String label);
    }

    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private final SharedInventoryPlayerEntity sharedInventoryPlayerEntity;
    private ClientCallback clientCallback;

    private ToolType activeTool = ToolType.CRAFTING;

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

    private final CraftingInventory craftingInput = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory craftingResult = new CraftingResultInventory();

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

    private void rebuildSlots() {
        if (!this.craftingInput.isEmpty() && !this.playerInventory.player.getWorld().isClient) {
            this.dropInventory(this.playerInventory.player, this.craftingInput);
        }
        this.craftingResult.clear();
        this.craftingInput.clear();

        this.slots.clear();
        ((ScreenHandlerAccessor) this).getTrackedStacks().clear();
        ((ScreenHandlerAccessor) this).getPreviousTrackedStacks().clear();

        privateSlotStart = this.slots.size();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 10; j++) {
                this.addSlot(new Slot(sharedInventoryPlayerEntity.shared$getPrivateInventory(),
                        j + i * 10, 22 + j * 18, 35 + i * 18));
            }
        }
        privateSlotEnd = this.slots.size();

        publicSlotStart = this.slots.size();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new Slot(inventory, i * 4 + j, 225 + j * 19, 36 + i * 19));
            }
        }
        publicSlotEnd = this.slots.size();

        toolSlotStart = this.slots.size();
        addToolSlots();
        toolSlotEnd = this.slots.size();

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

        playerInvSlotStart = this.slots.size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 22 + j * 18, 157 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 22 + i * 18, 213));
        }
        playerInvSlotEnd = this.slots.size();
    }

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
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.craftingInput, j + i * 3, 223 + j * 18, 180 + i * 18));
            }
        }
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInput, this.craftingResult, 0, 284, 198));
    }

    private void addFurnaceSlots() {
        Inventory furnaceInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getFurnaceInventory();
        this.addSlot(new Slot(furnaceInventory, 0, 223, 180));
        this.addSlot(new Slot(furnaceInventory, 1, 223, 216) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return AbstractFurnaceBlockEntity.canUseAsFuel(stack) || stack.isOf(Items.BUCKET);
            }
        });
        this.addSlot(new Slot(furnaceInventory, 2, 271, 198) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
    }

    private void addBrewingSlots() {
        Inventory brewingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingInventory();
        this.addSlot(new Slot(brewingInventory, 4, 223, 180) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BLAZE_POWDER);
            }
        });
        this.addSlot(new Slot(brewingInventory, 3, 260, 180) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return net.minecraft.recipe.BrewingRecipeRegistry.isValidIngredient(stack);
            }
        });
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
        this.addSlot(new Slot(anvilInventory, 0, 223, 198) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        this.addSlot(new Slot(anvilInventory, 1, 251, 198) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
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
        this.addSlot(new Slot(smithingInventory, 1, 242, 181) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
        this.addSlot(new Slot(smithingInventory, 2, 260, 181) {
            @Override
            public void markDirty() {
                super.markDirty();
                SharedInventoryScreenHandler.this.onContentChanged(this.inventory);
            }
        });
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

    public int getBrewTime() {
        return this.sharedInventoryPlayerEntity.shared$getPrivateInventory().getBrewingPropertyDelegate().get(0);
    }

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

            if (this.activeTool == ToolType.CRAFTING && invSlot == this.getCraftingResultSlotIndex()) {
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
                int movedCount = newStack.getCount() - originalStack.getCount();
                if (movedCount <= 0) {
                    return ItemStack.EMPTY;
                }
                ItemStack takenStack = newStack.copy();
                takenStack.setCount(movedCount);
                if (originalStack.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    slot.markDirty();
                }
                slot.onTakeItem(player, takenStack);
                return newStack;
            }
            if (invSlot >= playerInvSlotStart && invSlot < playerInvSlotEnd) {
                if (!this.insertItem(originalStack, toolSlotStart, toolSlotEnd, false)) {
                    if (!this.insertItem(originalStack, publicSlotStart, publicSlotEnd, false)) {
                        if (!this.insertItem(originalStack, privateSlotStart, privateSlotEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else if (invSlot >= privateSlotStart && invSlot < privateSlotEnd) {
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= publicSlotStart && invSlot < publicSlotEnd) {
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= toolSlotStart && invSlot < toolSlotEnd) {
                if (!this.insertItem(originalStack, playerInvSlotStart, playerInvSlotEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot >= armorSlotStart && invSlot < armorSlotEnd) {
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
            Optional<CraftingRecipe> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);
            if (optional.isPresent()) {
                CraftingRecipe craftingRecipe = optional.get();
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory);
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
        int priorWork = input1.getRepairCost();
        int renameCost = 0;
        ItemStack result = input1.copy();

        if (!input2.isEmpty()) {
            priorWork += input2.getRepairCost();
            boolean isBook = input2.isOf(Items.ENCHANTED_BOOK)
                    && !EnchantedBookItem.getEnchantmentNbt(input2).isEmpty();

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
                    finishAnvilResult(anvilData, anvilInventory, result, input1, input2, materialCost, priorWork, serverPlayer);
                    return;
                }
            }

            if (!isBook && !result.isOf(input2.getItem())) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }
            if (!isBook && result.isOf(input2.getItem()) && !result.isDamageable()) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }

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

            Map<Enchantment, Integer> resultEnchantments = EnchantmentHelper.get(result);
            Map<Enchantment, Integer> input2Enchantments = isBook
                    ? EnchantmentHelper.fromNbt(EnchantedBookItem.getEnchantmentNbt(input2))
                    : EnchantmentHelper.get(input2);

            boolean hasCompatible = false;
            boolean hasIncompatible = false;

            for (Map.Entry<Enchantment, Integer> entry : input2Enchantments.entrySet()) {
                Enchantment ench = entry.getKey();
                if (ench == null) continue;
                int newLevel = entry.getValue();
                int currentLevel = resultEnchantments.getOrDefault(ench, 0);

                if (currentLevel == newLevel) {
                    newLevel = Math.min(newLevel + 1, ench.getMaxLevel());
                } else {
                    newLevel = Math.max(newLevel, currentLevel);
                }

                boolean canApply = ench.isAcceptableItem(input1);
                if (serverPlayer.isCreative() && input1.isOf(Items.ENCHANTED_BOOK)) {
                    canApply = true;
                }

                for (Enchantment existing : resultEnchantments.keySet()) {
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
                resultEnchantments.put(ench, newLevel);

                int rarityCost = getRarityCost(ench.getRarity());
                if (isBook) {
                    rarityCost = Math.max(1, rarityCost / 2);
                }
                materialCost += rarityCost * newLevel;

                if (input1.getCount() > 1) {
                    materialCost = 40;
                }
            }

            if (hasIncompatible && !hasCompatible) {
                setAnvilOutput(anvilInventory, anvilData, ItemStack.EMPTY, 0);
                return;
            }

            EnchantmentHelper.set(resultEnchantments, result);
        }

        finishAnvilResult(anvilData, anvilInventory, result, input1, input2, materialCost, priorWork, serverPlayer);
    }

    private void finishAnvilResult(AnvilData anvilData, Inventory anvilInventory, ItemStack result, ItemStack input1, ItemStack input2, int materialCost, int priorWork, ServerPlayerEntity serverPlayer) {
        int renameCost = 0;

        String renameText = anvilData.getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            if (!renameText.equals(input1.getName().getString())) {
                renameCost = 1;
                materialCost += renameCost;
                result.setCustomName(net.minecraft.text.Text.of(renameText));
            }
        } else if (input1.hasCustomName()) {
            renameCost = 1;
            materialCost += renameCost;
            result.removeCustomName();
        }

        int totalCost = priorWork + materialCost;

        if (materialCost <= 0) {
            result = ItemStack.EMPTY;
        }

        if (renameCost == materialCost && renameCost > 0 && totalCost >= 40) {
            totalCost = 39;
        }

        if (totalCost >= 40 && !serverPlayer.isCreative()) {
            result = ItemStack.EMPTY;
        }

        if (!result.isEmpty()) {
            int newRepairCost = result.getRepairCost();
            if (!input2.isEmpty()) {
                newRepairCost = Math.max(newRepairCost, input2.getRepairCost());
            }
            if (renameCost != materialCost || renameCost == 0) {
                newRepairCost = getNextCost(newRepairCost);
            }
            result.setRepairCost(newRepairCost);
        }

        setAnvilOutput(anvilInventory, anvilData, result, totalCost >= 40 && !serverPlayer.isCreative() ? 0 : totalCost);
    }

    private void setAnvilOutput(Inventory anvilInventory, AnvilData anvilData, ItemStack result, int cost) {
        anvilInventory.setStack(2, result);
        anvilData.setRepairCost(cost);
        sendSlotUpdate(toolSlotStart + 2, result);
    }

    private static int getNextCost(int cost) {
        return cost * 2 + 1;
    }

    private static int getRarityCost(Enchantment.Rarity rarity) {
        switch (rarity) {
            case UNCOMMON: return 2;
            case RARE: return 4;
            case VERY_RARE: return 8;
            default: return 1;
        }
    }

    // === 锻造台逻辑 ===

    private void updateSmithingResult() {
        if (this.playerInventory.player.getWorld().isClient) return;
        World world = this.playerInventory.player.getWorld();
        Inventory smithingInventory = sharedInventoryPlayerEntity.shared$getPrivateInventory().getSmithingInventory();

        Optional<SmithingRecipe> optional = world.getRecipeManager().getFirstMatch(RecipeType.SMITHING, smithingInventory, world);
        ItemStack result = optional.map(recipe -> recipe.craft(smithingInventory)).orElse(ItemStack.EMPTY);

        smithingInventory.setStack(3, result);
        sendSlotUpdate(toolSlotStart + 3, result);
    }

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
    public boolean matches(Recipe<? super CraftingInventory> recipe) {
        return recipe.matches(this.craftingInput, this.playerInventory.player.getWorld());
    }

    @Override
    public int getCraftingResultSlotIndex() {
        // Result slot is after the 3x3 crafting input slots in this custom layout.
        return this.activeTool == ToolType.CRAFTING ? this.toolSlotStart + 9 : -1;
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
        return index != this.getCraftingResultSlotIndex();
    }
}
