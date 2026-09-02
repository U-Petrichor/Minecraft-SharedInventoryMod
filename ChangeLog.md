# Shared Inventory Mod - 版本变更记录

## Minecraft 1.21.9（相对 1.21.8）

本版本以可用的 1.21.8 完整实现为功能基线，适配 1.21.9 的重大 API 变化。

### 版本移植内容

- 更新至 Minecraft 1.21.9、Yarn Mappings 1.21.9+build.1 和 Fabric API 0.134.1+1.21.9。
- 保留 1.21.8 的共享存储、私人分页背包、工作站、背包装备槽功能。

### 1.21.9 API 适配

- `Entity.getWorld()` → `Entity.getEntityWorld()`
- `World.isClient` 字段 → `World.isClient()` 方法
- `Entity.getServer()` 移除，改用 `context.server()` 或 `world.getServer()`
- `ServerPlayNetworking.registerGlobalReceiver` 的 handler 改用 `context.server()` 获取服务器实例
- `FeatureRenderer.render` 签名变更，`VertexConsumerProvider` → `OrderedRenderCommandQueue`
- `ItemRenderer.renderItem` 签名完全改变，暂时禁用 3D 背包渲染
- `KeyBinding` 构造函数的 Category 参数从 String 改为 `KeyBinding.Category`

### 已知问题

- 3D 背包渲染暂时禁用，等待新 ItemRenderState API 文档完善后再实现

### 验证结果

- Java 21 完整 Gradle 构建成功。
