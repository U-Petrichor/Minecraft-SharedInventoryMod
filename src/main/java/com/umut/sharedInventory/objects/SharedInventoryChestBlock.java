package com.umut.sharedInventory.objects;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SharedInventoryChestBlock extends BlockWithEntity {
    public SharedInventoryChestBlock(Settings settings) {
        super(settings);
    }
    //要在以后给这个箱子弄材质，记得别忘了
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SharedInventoryChestBlockEntity(ModObjects.SHARED_INVENTORY_CHEST_BLOCK_ENTITY, pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // 从 BlockWithEntity 继承的默认值为 INVISIBLE，所以这里需要进行改变！
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return super.createScreenHandlerFactory(state, world, pos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // 这里会调用 BlockWithEntity 的 createScreenHandlerFactory 方法，会将返回的方块实体强转为
            // 一个 namedScreenHandlerFactory。如果你的方块没有继承 BlockWithEntity，那就需要单独实现 createScreenHandlerFactory。
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null) {
                // 这个调用会让服务器请求客户端开启合适的 ScreenHandler
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }
}
