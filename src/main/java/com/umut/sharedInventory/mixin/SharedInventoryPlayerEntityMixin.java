package com.umut.sharedInventory.mixin;

import com.umut.sharedInventory.SharedInventoryMod;
import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.inventory.PrivateInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class SharedInventoryPlayerEntityMixin implements SharedInventoryPlayerEntity {
    @Unique
    protected PrivateInventory sharedInventory$playerPrivateInventory = new PrivateInventory();

    @Unique
    protected ItemStack sharedInventory$backpackStack = ItemStack.EMPTY;

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writePrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("sharedInventoryPrivateInventory", this.sharedInventory$playerPrivateInventory.toNbtList());
        this.sharedInventory$playerPrivateInventory.writeFurnaceNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeLabelsToNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeBrewingNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeAnvilNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeSmithingNbt(nbt);

        if (!this.sharedInventory$backpackStack.isEmpty()) {
            NbtCompound backpackNbt = new NbtCompound();
            this.sharedInventory$backpackStack.writeNbt(backpackNbt);
            nbt.put("sharedInventoryBackpack", backpackNbt);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readPrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("sharedInventoryPrivateInventory", 9)) {
            this.sharedInventory$playerPrivateInventory.readNbtList(nbt.getList("sharedInventoryPrivateInventory", 10));
        }
        this.sharedInventory$playerPrivateInventory.readFurnaceNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readLabelsFromNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readBrewingNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readAnvilNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readSmithingNbt(nbt);

        if (nbt.contains("sharedInventoryBackpack", 10)) {
            this.sharedInventory$backpackStack = ItemStack.fromNbt(nbt.getCompound("sharedInventoryBackpack"));
        } else {
            this.sharedInventory$backpackStack = ItemStack.EMPTY;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (!((PlayerEntity)(Object)this).getWorld().isClient) {
            if (!this.sharedInventory$playerPrivateInventory.hasDirtyCallback()) {
                this.sharedInventory$playerPrivateInventory.setDirtyCallback(() -> {
                    SharedInventoryMod.LOGGER.debug("PrivateInventory marked dirty for player");
                });
            }
            this.sharedInventory$playerPrivateInventory.tick(((PlayerEntity)(Object)this).getWorld());
        }
    }

    @Override
    public PrivateInventory shared$getPrivateInventory() {
        return this.sharedInventory$playerPrivateInventory;
    }

    @Override
    public void shared$setPrivateInventory(PrivateInventory privateInventory) {
        this.sharedInventory$playerPrivateInventory = privateInventory;
    }

    @Override
    public ItemStack shared$getBackpackStack() {
        return this.sharedInventory$backpackStack;
    }

    @Override
    public void shared$setBackpackStack(ItemStack stack) {
        this.sharedInventory$backpackStack = stack == null ? ItemStack.EMPTY : stack;
    }
}
