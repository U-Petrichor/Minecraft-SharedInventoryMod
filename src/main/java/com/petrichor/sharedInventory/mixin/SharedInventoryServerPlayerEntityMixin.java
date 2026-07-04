package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 ServerPlayerEntity — 死亡/维度切换时保留私人背包数据
 *
 * 注入 copyFrom (HEAD): 在旧玩家数据复制到新玩家之前，
 * 将旧玩家的 PrivateInventory 直接赋给新玩家，避免数据丢失。
 * 这确保了玩家死亡重生或从末地返回时，私人背包物品不丢失。
 */
@Mixin(ServerPlayerEntity.class)
public class SharedInventoryServerPlayerEntityMixin  {

    /** 旧玩家实体 (用于获取旧背包数据) */
    @Unique
    SharedInventoryPlayerEntity oldPlayerEntity;
    /** 新玩家实体 (用于设置新背包数据) */
    @Unique
    SharedInventoryPlayerEntity newPlayerEntity;

    /** 在 copyFrom 执行前，将旧玩家的私人背包迁移到新玩家 */
    @Inject(
            method = "copyFrom",
            at = @At("HEAD")
    )
    public void keepSharedInventoryPrivateInventory(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci){
        if(oldPlayer instanceof SharedInventoryPlayerEntity)
            oldPlayerEntity=(SharedInventoryPlayerEntity)oldPlayer;
        if(this instanceof SharedInventoryPlayerEntity)
            newPlayerEntity=(SharedInventoryPlayerEntity)this;
        newPlayerEntity.shared$setPrivateInventory(oldPlayerEntity.shared$getPrivateInventory());
        newPlayerEntity.shared$setBackpackStack(oldPlayerEntity.shared$getBackpackStack());

    }
}
