# Shared Inventory Mod - 版本变更记录

## Minecraft 1.21.7（相对 1.21.5）

本版本以可用的 1.21.5 完整实现为功能基线，并使用 1.21.7 Fabric 模板的依赖配置完成移植。由于现有 1.21.6 工程不可用，本次没有从 1.21.6 复制代码。

### 版本移植内容

- 更新至 Minecraft 1.21.7、Yarn Mappings 1.21.7+build.8 和 Fabric API 0.129.0+1.21.7。
- 保留 1.21.5 的共享存储、私人分页背包、工作站、背包装备槽与玩家背部 3D 模型功能。
- 保留 1.21.4 起使用的 `assets/<命名空间>/items/*.json` 物品模型定义格式。
- 保留正确的 GUI Sprite 槽位路径 `minecraft:container/slot/*`。

### 1.21.7 API 适配

- 方块实体持久化由 `readNbt/writeNbt` 迁移至 `ReadView/WriteView` 的 `readData/writeData`。
- 玩家数据 Mixin 注入点由旧 NBT 方法迁移至 `readCustomData/writeCustomData`。
- ItemStack 持久化改用 `ItemStack.CODEC` 和注册表感知的 NBT Ops。
- Inventory 物品列表持久化适配 `Inventories.readData/writeData`。
- GUI 纹理渲染从 `RenderLayer::getGuiTextured` 迁移至 `RenderPipelines.GUI_TEXTURED`。
- GUI 矩阵栈调用适配 `Matrix3x2fStack` 的 `pushMatrix/popMatrix`。
- 移除 1.21.7 已删除的全局 `RenderSystem.setShaderColor` 调用。
- GUI 文本颜色改用完整 ARGB 格式，避免旧版六位 RGB 被解释为 Alpha 为 0 而导致文字透明。

### 移植期间修复的问题

- 为 3D 背包模型补充 `particle` 纹理引用，消除 1.21.7 资源加载警告。
- 修复共享存储界面标题和页码在 1.21.7 中透明或显示异常的问题。
- 更新 `fabric.mod.json` 的 Minecraft 和 Fabric API 版本约束。
- 验证背包与存储核心的新格式物品模型入口能够被 1.21.7 正确加载。

### 验证结果

- Java 21 完整 Gradle 构建成功。
- Fabric 客户端能够启动并完成资源加载。
- 未发现 Mixin 注入失败、崩溃或缺失物品模型错误。
