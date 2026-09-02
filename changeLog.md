# Shared Inventory Mod - Version Change Log

## 1.21.5 (相对于 1.21.4)

### 概述

Minecraft 1.21.5 包含重大API重构，主要涉及NBT系统和渲染系统。

### API 变化

#### 1. NBT Optional API 重构 (重大变化)

**变化描述：** NbtCompound 的所有 getter 方法现在返回 `Optional<T>` 而非直接返回值。

| 方法 | 1.21.4 返回类型 | 1.21.5 返回类型 |
|------|----------------|----------------|
| `getString(key)` | `String` | `Optional<String>` |
| `getInt(key)` | `int` | `Optional<Integer>` |
| `getShort(key)` | `short` | `Optional<Short>` |
| `getLong(key)` | `long` | `Optional<Long>` |
| `getCompound(key)` | `NbtCompound` | `Optional<NbtCompound>` |
| `getList(key)` | `NbtList` | `Optional<NbtList>` |

**修改前 (1.21.4):**
```java
String name = nbt.getString("name");
int value = nbt.getInt("value");
NbtCompound sub = nbt.getCompound("key");
if (nbt.contains("key", 10)) { ... }
```

**修改后 (1.21.5):**
```java
String name = nbt.getString("name").orElse("");
int value = nbt.getInt("value").orElse(0);
nbt.getCompound("key").ifPresent(sub -> { ... });
if (nbt.contains("key")) { ... }  // 不再需要类型参数
```

**影响文件：**
- `AnvilData.java` - getString/getInt
- `BrewingLogic.java` - getShort
- `FurnaceLogic.java` - getShort
- `DefaultedListInventory.java` - getCompound
- `PrivateInventory.java` - contains/getCompound/getString/getShort, NbtList遍历
- `SharedInventoryBackpack.java` - getLong/getString
- `SharedInventoryPlayerEntityMixin.java` - contains/getCompound/getList

#### 2. ModelPart.rotate() 方法移除

**变化描述：** `ModelPart.rotate(MatrixStack)` 方法被移除，需要手动使用 `Quaternionf` 应用旋转。

**修改前 (1.21.4):**
```java
ModelPart body = this.getContextModel().body;
body.rotate(matrices);
```

**修改后 (1.21.5):**
```java
import org.joml.Quaternionf;

ModelPart body = this.getContextModel().body;
matrices.multiply(new Quaternionf().rotationXYZ(body.pitch, body.yaw, body.roll));
```

**影响文件：**
- `BackpackFeatureRenderer.java`

#### 3. ModelTransformationMode 重命名为 ItemDisplayContext

**变化描述：** `net.minecraft.item.ModelTransformationMode` 被重命名为 `net.minecraft.item.ItemDisplayContext`。

**修改前 (1.21.4):**
```java
import net.minecraft.item.ModelTransformationMode;
client.getItemRenderer().renderItem(stack, ModelTransformationMode.NONE, light, overlay, matrices, vertexConsumers, world, seed);
```

**修改后 (1.21.5):**
```java
import net.minecraft.item.ItemDisplayContext;
client.getItemRenderer().renderItem(stack, ItemDisplayContext.NONE, light, overlay, matrices, vertexConsumers, world, seed);
```

**影响文件：**
- `BackpackFeatureRenderer.java`

#### 4. RenderSystem.enableBlend()/disableBlend() 移除

**变化描述：** `RenderSystem.enableBlend()` 和 `RenderSystem.disableBlend()` 方法被移除。现在应使用 `DrawContext` 的透明度渲染功能。

**修改前 (1.21.4):**
```java
RenderSystem.enableBlend();
RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
context.drawItem(icon, x, y);
RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
RenderSystem.disableBlend();
```

**修改后 (1.21.5):**
```java
context.getMatrices().push();
context.getMatrices().translate(0, 0, 300);
context.drawItem(icon, x, y);
context.getMatrices().pop();
```

**影响文件：**
- `InventoryScreenMixin.java`

#### 5. ScreenHandler.setPreviousTrackedSlot() 移除

**变化描述：** `ScreenHandler.setPreviousTrackedSlot(int, ItemStack)` 方法被移除。现在直接发送更新包即可。

**修改前 (1.21.4):**
```java
handler.setPreviousTrackedSlot(slotIndex, stack);
serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(...));
```

**修改后 (1.21.5):**
```java
// 直接发送更新包，无需调用 setPreviousTrackedSlot
serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(...));
```

**影响文件：**
- `SharedInventoryScreenHandler.java` (两处调用)

#### 6. ItemStack.fromNbtOrEmpty() 方法变化

**变化描述：** `ItemStack.fromNbtOrEmpty(registryLookup, nbt)` 方法签名变化，现在使用 `ItemStack.fromNbt()` 返回 `Optional<ItemStack>`。

**修改前 (1.21.4):**
```java
ItemStack stack = ItemStack.fromNbtOrEmpty(registryLookup, nbt);
```

**修改后 (1.21.5):**
```java
ItemStack stack = ItemStack.fromNbt(registryLookup, nbt).orElse(ItemStack.EMPTY);
```

**影响文件：**
- `PrivateInventory.java`
- `SharedInventoryPlayerEntityMixin.java`

#### 7. NbtCompound.contains() 签名变化

**变化描述：** `NbtCompound.contains(key, type)` 方法简化为单参数版本。

**修改前 (1.21.4):**
```java
if (nbt.contains("key", 10)) { ... }  // 10 = COMPOUND_TYPE
if (nbt.contains("key", 9)) { ... }   // 9 = LIST_TYPE
```

**修改后 (1.21.5):**
```java
if (nbt.contains("key")) { ... }
// 使用 Optional 的 ifPresent 处理类型安全
nbt.getCompound("key").ifPresent(compound -> { ... });
```

### 修改的文件汇总

| 文件 | 变化类型 | 涉及的API |
|------|----------|-----------|
| `AnvilData.java` | NBT Optional | getString, getInt |
| `BrewingLogic.java` | NBT Optional | getShort |
| `FurnaceLogic.java` | NBT Optional | getShort |
| `DefaultedListInventory.java` | NBT Optional | getCompound |
| `PrivateInventory.java` | NBT Optional | contains, getCompound, getString, getShort, NbtList遍历, fromNbt |
| `SharedInventoryBackpack.java` | NBT Optional | getLong, getString |
| `SharedInventoryPlayerEntityMixin.java` | NBT Optional | contains, getCompound, getList, fromNbt |
| `BackpackFeatureRenderer.java` | 渲染API | ModelPart.rotate, ItemDisplayContext |
| `InventoryScreenMixin.java` | 渲染API | RenderSystem.enableBlend |
| `SharedInventoryScreenHandler.java` | ScreenHandler | setPreviousTrackedSlot |

### 无需修改的文件

以下文件可直接从1.21.4复制，无需任何修改：
- 所有网络payload文件
- ModObjects.java
- SharedInventoryChestBlock.java
- SharedInventoryChestBlockEntity.java
- BackpackRenderState.java
- PlayerEntityRendererMixin.java
- 其他Mixin文件 (除InventoryScreenMixin外)
- 所有资源文件 (textures, lang, models, data)

### Gradle配置变化

```properties
# 1.21.4
minecraft_version=1.21.4
yarn_mappings=1.21.4+build.8
fabric_api_version=0.119.4+1.21.4

# 1.21.5
minecraft_version=1.21.5
yarn_mappings=1.21.5+build.1
fabric_api_version=0.128.2+1.21.5
```

### 编译验证

- 构建状态: ✅ 成功
- 输出文件: `shared_inventory-1.0.0.jar`

### 备注

1.21.5是Minecraft 1.21.x系列中API变化最大的一次更新，主要原因是NBT系统全面采用Optional模式，提高了类型安全性。这要求开发者在所有NBT读取操作中处理Optional返回值。

#### 从1.21.4继承的修复

**Identifier 命名空间问题：** 在1.21.4+中，`getBackgroundSprite()` 返回的 `Identifier` 必须包含完整的命名空间：

```java
// 正确 - 包含 minecraft 命名空间
public static final Identifier EMPTY_HELMET_SLOT_TEXTURE = Identifier.of("minecraft:item/empty_armor_slot_helmet");

// 错误 - 缺少命名空间，导致贴图加载失败（黑紫色方块）
public static final Identifier EMPTY_HELMET_SLOT_TEXTURE = Identifier.of("item/empty_armor_slot_helmet");
```

---

## 1.21.4 (相对于 1.21.3)

### API 变化

#### 1. Slot.getBackgroundSprite() 返回类型简化

**变化描述：** Minecraft 1.21.4 简化了 `Slot.getBackgroundSprite()` 方法的返回类型。

| 项目 | 1.21.3 | 1.21.4 |
|------|--------|--------|
| 返回类型 | `Pair<Identifier, Identifier>` | `Identifier` |
| 含义 | (atlas纹理, sprite路径) | 直接返回sprite路径 |

**修改前 (1.21.3):**
```java
import com.mojang.datafixers.util.Pair;

@Override
public Pair<Identifier, Identifier> getBackgroundSprite() {
    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_ARMOR_SLOT_TEXTURES[slotIndex]);
}
```

**修改后 (1.21.4):**
```java
// 不再需要 Pair import

@Override
public Identifier getBackgroundSprite() {
    return EMPTY_ARMOR_SLOT_TEXTURES[slotIndex];
}
```

**影响位置：**
- `SharedInventoryScreenHandler.java` 第257-260行 (护甲槽位)
- `SharedInventoryScreenHandler.java` 第263-267行 (副手槽位)

**说明：** 1.21.4不再需要显式指定 `BLOCK_ATLAS_TEXTURE`，sprite系统自动处理纹理atlas定位。

### 修改的文件

| 文件 | 变化类型 | 说明 |
|------|----------|------|
| `SharedInventoryScreenHandler.java` | API适配 | 修改getBackgroundSprite()返回类型，移除Pair import |

### 无需修改的文件

所有其他37个Java文件可直接复制使用，无需任何修改：
- 渲染相关文件 (BackpackFeatureRenderer, BackpackRenderState等)
- 逻辑相关文件 (FurnaceLogic, BrewingLogic等)
- 网络相关文件 (所有payload文件)
- Block/Item/BlockEntity文件
- 所有Mixin文件
- 所有资源文件

### Gradle配置变化

```properties
# 1.21.3
minecraft_version=1.21.3
yarn_mappings=1.21.3+build.2
fabric_api_version=0.114.1+1.21.3

# 1.21.4
minecraft_version=1.21.4
yarn_mappings=1.21.4+build.8
fabric_api_version=0.119.4+1.21.4
```

### 编译验证

- 构建状态: ✅ 成功
- 输出文件: `shared_inventory-1.0.0.jar` (225KB)