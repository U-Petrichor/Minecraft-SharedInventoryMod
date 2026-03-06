package com.umut.sharedInventory.mixin;

import com.umut.sharedInventory.objects.SharedInventoryPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class SharedInventoryServerPlayerEntityMixin  {

    @Unique
    SharedInventoryPlayerEntity oldPlayerEntity;
    @Unique
    SharedInventoryPlayerEntity newPlayerEntity;

    @Inject(
            method = "copyFrom",
            at = @At("HEAD")
    )
    public void keepSharedInventoryPrivateInventory(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci){
        if(oldPlayer instanceof SharedInventoryPlayerEntity)
            oldPlayerEntity=(SharedInventoryPlayerEntity)oldPlayer;
        if(this instanceof SharedInventoryPlayerEntity)
            newPlayerEntity=(SharedInventoryPlayerEntity)this;
        newPlayerEntity.shared_inventory1_18_2$setPrivateInventory(oldPlayerEntity.shared_inventory1_18_2$getPrivateInventory());

    }
}
