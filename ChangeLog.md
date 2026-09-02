# Shared Inventory Mod - 版本变更记录

## Minecraft 1.21.4（相对 1.21.3）

本版本主要是适配 Minecraft 1.21.4 的客户端资源与界面 API。游戏逻辑基本延续 1.21.3，但资源加载规则发生了关键变化。

### 版本移植内容

- 将 `Slot.getBackgroundSprite()` 适配为 1.21.4 的 `Identifier` 返回类型。
- 为背包和存储核心增加 1.21.4 新增的 `assets/shared_inventory_mod/items/*.json` 物品模型定义入口。
- 背包物品定义继续引用原有的 3D 背包模型；模型文件和 PNG 贴图与 1.21.2、1.21.3 完全相同。
- 玩家背部的背包渲染使用明确的物品展示变换。
- 为存储核心方块模型补充 `particle` 纹理。
- 更新 Minecraft、Yarn Mapping 和 Fabric API 依赖版本。

### 本次修复的问题

- 修复物品定义曾错误指向二维物品栏模型，导致玩家背上的背包变成平面或出现严重模型异常的问题。
- 修复缺少 `items/*.json` 入口时，背包和存储核心显示缺失模型的问题。
- 修复存储核心 BlockItem 缺少 `item.shared_inventory_mod.shared_inventory_chest_block` 翻译键，名称显示为翻译键或显示异常的问题。
- 修复盔甲栏和副手栏仍引用旧版 `item/empty_armor_slot_*` 路径的问题。1.21.4 的槽位图标已经迁移至 `minecraft:container/slot/*` GUI Sprite。
- 修复存储核心方块模型缺少粒子纹理而产生的资源加载警告。

### 为什么这些问题只在 1.21.4 明显出现

1.21.4 改用了新的物品模型定义入口，并迁移了 GUI 槽位 Sprite。直接复制 1.21.3 的资源与路径虽然能够通过编译，但运行时会出现缺失模型、错误模型或错误槽位贴图。

### 构建环境

- Minecraft：1.21.4
- Yarn Mappings：1.21.4+build.8
- Fabric API：0.119.4+1.21.4
- Java：21
