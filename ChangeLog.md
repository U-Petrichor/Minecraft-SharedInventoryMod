# Shared Inventory Mod - 版本变更记录

## Minecraft 1.21.6（相对 1.21.5）

本版本以可用的 1.21.5 完整实现为功能基线，参考 1.21.7 的成功移植经验完成迁移。

### 版本移植内容

- 更新至 Minecraft 1.21.6、Yarn Mappings 1.21.6+build.1 和 Fabric API 0.128.2+1.21.6。
- 保留 1.21.5 的共享存储、私人分页背包、工作站、背包装备槽与玩家背部 3D 模型功能。

### 1.21.6 API 适配

- 方块实体持久化由 `readNbt/writeNbt` 迁移至 `ReadView/WriteView` 的 `readData/writeData`。
- 玩家数据 Mixin 注入点由旧 NBT 方法迁移至 `readCustomData/writeCustomData`。
- ItemStack 持久化改用 `ItemStack.CODEC` 和注册表感知的 NBT Ops。
- Inventory 物品列表持久化适配 `Inventories.readData/writeData`。
- GUI 纹理渲染从 `RenderLayer::getGuiTextured` 迁移至 `RenderPipelines.GUI_TEXTURED`。
- GUI 矩阵栈调用适配 `Matrix3x2fStack` 的 `pushMatrix/popMatrix`。
- 移除已删除的全局 `RenderSystem.setShaderColor` 调用。
- GUI 文本颜色改用完整 ARGB 格式，避免透明问题。

### 验证结果

- Java 21 完整 Gradle 构建成功。
- Fabric 客户端能够启动并完成资源加载。
- 未发现 Mixin 注入失败、崩溃或缺失物品模型错误。
