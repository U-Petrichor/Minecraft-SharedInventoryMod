package com.petrichor.sharedInventory.mixin;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
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

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void writePrivateInventory(WriteView view, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        RegistryWrapper.WrapperLookup registryLookup = self.getRegistryManager();
        NbtCompound nbt = new NbtCompound();

        nbt.put("sharedInventoryPrivateInventory", this.sharedInventory$playerPrivateInventory.toNbtList(registryLookup));
        this.sharedInventory$playerPrivateInventory.writeFurnaceNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeLabelsToNbt(nbt);
        this.sharedInventory$playerPrivateInventory.writeBrewingNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeAnvilNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.writeSmithingNbt(nbt, registryLookup);

        if (!this.sharedInventory$backpackStack.isEmpty()) {
            // 1.21.2: ItemStack.encode 变为 toNbt
            view.put("sharedInventoryBackpack", ItemStack.CODEC, this.sharedInventory$backpackStack);
        }
        view.put("sharedInventoryData", NbtCompound.CODEC, nbt);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void readPrivateInventory(ReadView view, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        RegistryWrapper.WrapperLookup registryLookup = self.getRegistryManager();
        NbtCompound nbt = view.read("sharedInventoryData", NbtCompound.CODEC).orElse(new NbtCompound());

        if (nbt.contains("sharedInventoryPrivateInventory")) {
            this.sharedInventory$playerPrivateInventory.readNbtList(nbt.getListOrEmpty("sharedInventoryPrivateInventory"), registryLookup);
        }
        this.sharedInventory$playerPrivateInventory.readFurnaceNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readLabelsFromNbt(nbt);
        this.sharedInventory$playerPrivateInventory.readBrewingNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readAnvilNbt(nbt, registryLookup);
        this.sharedInventory$playerPrivateInventory.readSmithingNbt(nbt, registryLookup);

        this.sharedInventory$backpackStack =
                view.read("sharedInventoryBackpack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (!self.getWorld().isClient) {
            if (!this.sharedInventory$playerPrivateInventory.hasDirtyCallback()) {
                this.sharedInventory$playerPrivateInventory.setDirtyCallback(() -> {
                    SharedInventoryMod.LOGGER.debug("PrivateInventory marked dirty for player");
                });
            }
            this.sharedInventory$playerPrivateInventory.tick(self.getWorld());

            // 同步 PropertyDelegate 到客户端 — 如果玩家打开了 SharedInventoryScreenHandler
            if (self.currentScreenHandler instanceof com.petrichor.sharedInventory.screen.SharedInventoryScreenHandler handler) {
                handler.sendContentUpdates();
            }
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
