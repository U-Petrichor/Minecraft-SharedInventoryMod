package com.umut.sharedInventory.objects;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class SharedInventoryBackpack extends Item implements Inventory, NamedScreenHandlerFactory {

    //首先讲一下这个类是干什么的,首先这个类既负责打开背包界面,还负责存储绑定的共享核心

    public SharedInventoryBackpack(Settings settings) {
        super(settings);
    }

    private Text playerName=Text.of("Someone");
    private final int publicStackSize=16;
    //下面的这两个就是负责绑定和存储有关共享核心的信息的
    private SharedInventoryChestBlockEntity linkedBlockEntity;
    private BlockPos linkedBlockEntityPos;
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(hand==Hand.MAIN_HAND){
            playerName=user.getDisplayName();
            user.openHandledScreen(this);
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    //这个是创建UI界面的核心函数，在这里会判断有没有核心,如果没有绑定核心则不允许使用
    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        // 先读，这样是为了保证箱子被扣掉时能及时反馈给玩家，就不会因为别人偷偷扣掉箱子导致白白存入物品，以至于下次进入游戏时丢失物品
        readLinkedBlock(player.getStackInHand(Hand.MAIN_HAND), player.getServer());
        if (linkedBlockEntity == null) {
            // 如果读取后仍然为null，那就说明物品丢失

            player.sendMessage(Text.translatable("message.shared_inventory_mod.shared_inventory_backpack.message1"), true);
            return null;

        }
        //如果说上面没有返回就说明有绑定，可以去打开窗口
        return new SharedInventoryScreenHandler(syncId, inv, this);

    }

    //判断玩家存入物品时存入的位置是公共的还是私有的，以此处理正确的索引位置,原本privateStack是在infinityBackpack里面,所以搞了这么个东西方便调用,结果后面发现如果privateStack在这里的话根本实现不了我要的功能,所以重写了一遍模组,但这个东西还是有那么一点点用所以没删掉
    private boolean isPublicStack(int slot){
        boolean isPublic = true;
        boolean notPublic = false;
        return slot>=0&&slot<this.publicStackSize? isPublic : notPublic;
    }

    //返回正确的容量，为后续增加新的内容做铺垫
    @Override
    public int size() {
        return this.publicStackSize;
    }

    //这部分是获取以及放入物品时的逻辑,理论上这里的slot传入值总是0到16
    @Override
    public ItemStack getStack(int slot) {
        //先判断是不是publicStack,是的话直接调用绑定的方块的publicStack
        if(isPublicStack(slot))
            return this.linkedBlockEntity.publicStack.get(slot);
        //以上走不通就返回空物品
        if(slot>16||slot<0)
            System.err.println("在[SharedInventoryBackpack]的[getStack]方法里出现了一些意料之外的错误,请记录一下此bug的出现原因并且寻找作者Umut_o_O寻求帮助");
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(isPublicStack(slot)){
            this.linkedBlockEntity.publicStack.set(slot, stack);
            this.linkedBlockEntity.markDirty();
        }
        //手上拿着东西并且总和超过最大值，只存最大值
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack itemStack;
        itemStack = Inventories.splitStack(this.linkedBlockEntity.publicStack, slot, amount);
        if (!itemStack.isEmpty()) {
            this.markDirty();
        }
        return itemStack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if(slot>=0&&slot<this.publicStackSize)
            return Inventories.removeStack(this.linkedBlockEntity.publicStack, slot);
        return ItemStack.EMPTY;
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < publicStackSize; i++) {
            if (!getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    //双击选取箱子内所有相同物品
    public ItemStack removeItem(Item item, int count) {
        ItemStack itemStack = new ItemStack(item, 0);
        for (int i = this.publicStackSize - 1; i >= 0; i--) {
            ItemStack itemStack2 = this.getStack(i);
            if (itemStack2.getItem().equals(item)) {
                int j = count - itemStack.getCount();
                ItemStack itemStack3 = itemStack2.split(j);
                itemStack.increment(itemStack3.getCount());
                if (itemStack.getCount() == count) {
                    break;
                }
            }
        }
        if (!itemStack.isEmpty()) {
            this.markDirty();
        }
        return itemStack;
    }

    //清除物品栏
    @Override
    public void clear() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    //这个是有关于物品注释的
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        //注释语句
        tooltip.add(Text.translatable("item.shared_inventory_mod.shared_inventory_backpack.tooltip"));
        // 这样写可以改变字体颜色
        // tooltip.add(new TranslatableText("item.infinity_mod.infinity_backpack.tooltip").formatted(Formatting.RED));
    }

    @Override
    public Text getDisplayName() {
        return this.playerName;
    }

    public void linkToChest(ItemStack stack, SharedInventoryChestBlockEntity blockEntity) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (blockEntity != null) {
            this.linkedBlockEntity = blockEntity;
            this.linkedBlockEntityPos = blockEntity.getPos();
            nbt.putLong("linkedBlockEntityPos", blockEntity.getPos().asLong());
        }
    }
    //这两个是关于公共部分存储的，保证每次打开都可以自动的去绑定上一次最后绑定的那一个共享核心
    private void readLinkedBlock(ItemStack stack, MinecraftServer server) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("linkedBlockEntityPos")) {
            linkedBlockEntityPos = BlockPos.fromLong(nbt.getLong("linkedBlockEntityPos"));
        }
        for (ServerWorld world : server.getWorlds()) {
            if (linkedBlockEntityPos != null) {
                if (world.getBlockEntity(linkedBlockEntityPos) instanceof SharedInventoryChestBlockEntity) {
                    linkedBlockEntity = (SharedInventoryChestBlockEntity) world.getBlockEntity(linkedBlockEntityPos);
                    return; // 找到就直接返回，不需要继续找
                }
            }
        }
        // 如果遍历完所有维度都没找到
        linkedBlockEntity = null;
    }

}

