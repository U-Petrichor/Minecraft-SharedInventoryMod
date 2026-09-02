# Shared Inventory Mod - 版本变更记录

## Minecraft 1.21.3（相对 1.21.2）

本版本由 1.21.2 移植而来，主体玩法、背包模型和贴图资源没有变化，主要修改集中在版本依赖和客户端渲染兼容性。

### 版本移植内容

- 更新 Minecraft、Yarn Mapping、Fabric Loader 和 Fabric API 依赖版本。
- 延续 1.21.2 的玩家渲染状态架构：通过扩展 `PlayerEntityRenderState` 将已装备的背包传递给背包特性渲染器。
- 背包 3D 模型与 PNG 贴图直接沿用 1.21.2；经 SHA-256 和逐字节比较确认完全相同。
- 继续使用 1.21.3 及更早版本的传统 `models/item/*.json` 物品模型加载方式。

### 本次修复的问题

- 修复玩家背部渲染背包时使用 `ModelTransformationMode.NONE`，导致物品模型或贴图无法正确应用的问题。
- 将背部背包渲染改为明确的 `ModelTransformationMode.GUI` 展示变换。

### 与 1.21.4 的关键区别

1.21.3 不需要 `assets/<命名空间>/items/*.json`。该物品模型定义入口从 1.21.4 开始使用，因此不能把 1.21.4 的资源结构反向套用到本版本。

### 构建环境

- Minecraft：1.21.3
- Java：21
