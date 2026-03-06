package com.umut.sharedInventory.mixin;

import com.umut.sharedInventory.objects.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.objects.SharedInventoryPlayerPrivateInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (PlayerEntity.class)
public class SharedInventoryPlayerEntityMixin implements SharedInventoryPlayerEntity{
    @Unique
    protected SharedInventoryPlayerPrivateInventory sharedInventory$playerPrivateInventory = new SharedInventoryPlayerPrivateInventory();
    @Unique
    protected int sharedInventory$searchIndex = 0;


    @Inject(
            method = "writeCustomDataToNbt",
            at = @At("TAIL")
    )
    private void writePrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("sharedInventoryPrivateInventory", this.sharedInventory$playerPrivateInventory.toNbtList());
        nbt.putInt("sharedInventorySearchIndex", this.sharedInventory$searchIndex);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readPrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("sharedInventoryPrivateInventory", 9)) {
            this.sharedInventory$playerPrivateInventory.readNbtList(nbt.getList("sharedInventoryPrivateInventory", 10));
        }
        if (nbt.contains("sharedInventorySearchIndex")) {
            this.sharedInventory$searchIndex = nbt.getInt("sharedInventorySearchIndex");
        }
    }



    @Override
    public SharedInventoryPlayerPrivateInventory shared_inventory1_18_2$getPrivateInventory() {
        return this.sharedInventory$playerPrivateInventory;
    }

    @Override
    public void shared_inventory1_18_2$setPrivateInventory(SharedInventoryPlayerPrivateInventory privateInventory){
        this.sharedInventory$playerPrivateInventory=privateInventory;
    }

    @Override
    public int shared_inventory1_18_2$getSearchIndex() {
        return this.sharedInventory$searchIndex;
    }

    @Override
    public void shared_inventory1_18_2$setSearchIndex(int index) {
        this.sharedInventory$searchIndex = index;
    }


}
