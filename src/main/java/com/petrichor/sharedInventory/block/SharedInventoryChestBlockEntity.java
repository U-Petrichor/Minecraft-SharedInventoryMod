package com.petrichor.sharedInventory.block;

import com.petrichor.sharedInventory.inventory.ModObjects;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * 共享核心方块实体 — 持有公共背包数据 (4×4 = 16 格)
 *
 * publicStack 是所有绑定此核心的背包共享的存储空间，
 * 通过 BackpackInventory 包装后传给 ScreenHandler。
 * 数据通过 NBT 持久化在方块实体中。
 */
public class SharedInventoryChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    /** 公共背包槽位数 (4×4) */
    private final int publicStackSize=16;
    /** 公共物品列表，所有绑定此核心的玩家共享 */
    public final DefaultedList<ItemStack> publicStack = DefaultedList.ofSize(publicStackSize, ItemStack.EMPTY);


    //以下是原有的，上面的是inventory里面搬过来的，要慢慢地处理
    public SharedInventoryChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    public static SharedInventoryChestBlockEntity create(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        // 读取 publicStack
        if (nbt.contains("PublicItems", NbtElement.LIST_TYPE)) {
            NbtList publicItems = nbt.getList("PublicItems", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < publicItems.size(); i++) {
                NbtCompound itemTag = publicItems.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < this.publicStack.size()) {
                    this.publicStack.set(slot, ItemStack.fromNbt(itemTag));
                }
            }
        }
    }

    // 保存数据到 NBT
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // 写入 publicStack 到 "PublicItems" 键
        NbtList publicItems = new NbtList();
        for (int i = 0; i < this.publicStack.size(); i++) {
            ItemStack stack = this.publicStack.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putInt("Slot", i);
                stack.writeNbt(itemTag);
                publicItems.add(itemTag);
            }
        }
        nbt.put("PublicItems", publicItems);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText("block_entity.shared_inventory_mod.shared_inventory_chest_block_entity_title");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return null;
    }


}
