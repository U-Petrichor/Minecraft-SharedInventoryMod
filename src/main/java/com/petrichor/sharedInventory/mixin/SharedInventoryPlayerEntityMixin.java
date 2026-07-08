package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 PlayerEntity — 为每个玩家附加 PrivateInventory 和背包装备数据
 *
 * 注入点:
 *   - writeCustomDataToNbt (TAIL): 将私人背包、工作站数据、标签、背包装备写入玩家 NBT
 *   - readCustomDataFromNbt (TAIL): 从 NBT 恢复上述数据
 *   - tick (TAIL): 驱动熔炉和酿造的每刻逻辑 (仅服务端)
 *
 * 线程安全: MC 服务端主线程模型保证 tick 和 NBT 操作不会并发执行。
 * 所有字段使用 @Unique + sharedInventory$ 前缀避免名称冲突。
 */
@Mixin(PlayerEntity.class)
public class SharedInventoryPlayerEntityMixin implements SharedInventoryPlayerEntity {
    @Unique
    protected PrivateInventory sharedInventory$playerPrivateInventory = new PrivateInventory();

    @Unique
    protected ItemStack sharedInventory$backpackStack = ItemStack.EMPTY;

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writePrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        RegistryWrapper.WrapperLookup registryLookup = self.getRegistryManager();

        nbt.put("sharedInventoryPrivateInventory", this.sharedInventory$playerPrivateInventory.toNbtList(registryLookup));
        this.sharedInventory$playerPrivateInventory.writeFurnaceNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeLabelsToNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeBrewingNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeAnvilNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeSmithingNbt(nbt, registryLookup);

        if (!this.sharedInventory$backpackStack.isEmpty()) {
            nbt.put("sharedInventoryBackpack", this.sharedInventory$backpackStack.encode(registryLookup));
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readPrivateInventory(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        RegistryWrapper.WrapperLookup registryLookup = self.getRegistryManager();

        if (nbt.contains("sharedInventoryPrivateInventory", 9)) {
            this.sharedInventory$playerPrivateInventory.readNbtList(nbt.getList("sharedInventoryPrivateInventory", 10), registryLookup);
        }
        this.sharedInventory$playerPrivateInventory.readFurnaceNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readLabelsFromNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readBrewingNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readAnvilNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readSmithingNbt(nbt, registryLookup);

        if (nbt.contains("sharedInventoryBackpack", 10)) {
            this.sharedInventory$backpackStack = ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("sharedInventoryBackpack"));
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
