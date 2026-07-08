package com.umut.sharedInventory.mixin;

import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class SharedInventoryServerPlayerEntityMixin {

    @Inject(
            method = "copyFrom",
            at = @At("HEAD")
    )
    public void keepSharedInventoryPrivateInventory(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci){
        if (oldPlayer instanceof SharedInventoryPlayerEntity oldShared
                && this instanceof SharedInventoryPlayerEntity newShared) {
            newShared.shared$setPrivateInventory(oldShared.shared$getPrivateInventory());
            newShared.shared$setBackpackStack(oldShared.shared$getBackpackStack());
        }
    }
}
