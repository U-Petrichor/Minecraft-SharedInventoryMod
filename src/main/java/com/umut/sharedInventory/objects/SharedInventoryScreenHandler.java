package com.umut.sharedInventory.objects;

import com.mojang.datafixers.util.Pair;
import com.umut.sharedInventory.SharedInventoryMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

import java.util.Optional;


public class SharedInventoryScreenHandler extends AbstractRecipeScreenHandler<CraftingInventory> {

    //这部分是为了完成privateInventory部分所必须的变量
    private final PlayerInventory playerInventory;
    private final Inventory inventory;
    private boolean firstTime=true;
    private final SharedInventoryPlayerEntity shardInventoryPlayerEntity;

    //这一段是照抄的PlayerScreenHandler里面的盔甲部分，官方写的很完善，直接用就好
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

    //
    private final CraftingInventory craftingInput = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory craftingResult = new CraftingResultInventory();


    public SharedInventoryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory,new SimpleInventory(16));
    }

    // 这个构造器是在服务器的 BlockEntity 中被调用的，无需先调用其他构造器，服务器知道容器的物品栏
    // 并直接将其作为参数传入。然后物品栏在客户端完成同步。
    public SharedInventoryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModObjects.SHARED_INVENTORY_SCREEN_HANDLER, syncId);
        checkSize(inventory, 16);
        this.playerInventory=playerInventory;
        this.inventory=inventory;
        this.shardInventoryPlayerEntity =(SharedInventoryPlayerEntity)playerInventory.player;
        // 玩家开启时，一些物品栏有自定义的逻辑。
        inventory.onOpen(playerInventory.player);
        updateAllSlot();
        firstTime=false;

    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    // Shift + 玩家物品栏槽位
    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    private void updateAllSlot(){
        this.slots.clear();

        //privateInventory
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory(),   j + i * 9, 7 + j * 18,  19 + i * 18));
            }
        }

        //publicInventory
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new Slot(inventory, i*4+j, 173 + j * 18,  177 + i * 18));
            }
        }

        //物品栏
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 7 + j * 18, 177 + i * 18));
            }
        }

        //快捷栏
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 7 + i * 18, 232));
        }


        //这个是工作台合成结果的格子
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInput, this.craftingResult, 0, 232, 37));

        //这个是工作台合成地方的3*3格子
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.craftingInput, j + i * 3, 173 + j * 18, 19 + i * 18));
            }
        }

        //这里是玩家盔甲栏
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot equipmentSlot = EQUIPMENT_SLOT_ORDER[i];
            this.addSlot(new Slot(playerInventory, 39 - i, 173, 91 + i * 18) {
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
                    return !itemStack.isEmpty() && !playerEntity.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack) ? false : super.canTakeItems(playerEntity);
                }

                @Override
                public Pair<Identifier, Identifier> getBackgroundSprite() {
                    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_ARMOR_SLOT_TEXTURES[equipmentSlot.getEntitySlotId()]);
                }
            });
        }
        //这个是玩家副手，也就是左手
        this.addSlot(new Slot(playerInventory, 40, 193, 145) {
            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT);
            }
        });
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if(firstTime)
            return super.addSlot(slot);
        else{
            slot.id = this.slots.size();
            this.slots.add(slot);
        }
        return slot;
    }

    public void onPrevious_PageButtonClicked() {
        if(getCurrentPage()>=2&&getCurrentPage()<=getMaxPage()){
            setCurrentPage(getCurrentPage()-1);
            //updatePrivateSlot(getCurrentPage());

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(getCurrentPage());
            ClientPlayNetworking.send(SharedInventoryMod.PAGE_UPDATE_ID, buf);
        }
    }

    public void onNext_PageButtonClicked() {
        if(getCurrentPage()>=1&&getCurrentPage()<=getMaxPage()-1){
            setCurrentPage(getCurrentPage()+1);

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(getCurrentPage());
            ClientPlayNetworking.send(SharedInventoryMod.PAGE_UPDATE_ID, buf);
        }
    }

    public void onCurrentButtonClicked(int page) {
        setCurrentPage(page);

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(getCurrentPage());
        ClientPlayNetworking.send(SharedInventoryMod.PAGE_UPDATE_ID, buf);
    }


    private int getCurrentPage(){
        return this.shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().getCurrentPage();
    }

    private int getMaxPage(){
        return this.shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().getPrivateStackMaxPage();
    }

    private void setCurrentPage(int page){
        shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().setCurrentPage(page);
    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        this.craftingResult.clear();
        if (!player.world.isClient) {
            this.dropInventory(player, this.craftingInput);
        }
    }

    //这里往上基本都是处理页面逻辑和翻页功能实现的方法
    //这个地方往下是工作台部分
    @Override
    public void onContentChanged(Inventory inventory) {
        updateResult(this, this.playerInventory.player.world, this.playerInventory.player, this.craftingInput, this.craftingResult);
    }

    private void updateResult(
            ScreenHandler handler, World world, PlayerEntity player, CraftingInventory craftingInventory, CraftingResultInventory resultInventory
    ) {
        if (!world.isClient) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
            ItemStack itemStack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);
            if (optional.isPresent()) {
                CraftingRecipe craftingRecipe = (CraftingRecipe)optional.get();
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
        return 8;
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
