# Changelog

All notable changes to the Shared Inventory Mod will be documented in this file.

## [1.21.11] - 2026-07-16

### Changed
- Migrated from Minecraft 1.21.10 to 1.21.11
- Updated Fabric API to 0.141.4+1.21.11
- Updated Yarn mappings to 1.21.11+build.6

### Fixed
- **Text API**: Static methods on `Text` interface (`literal`, `translatable`, `of`) must now be called with fully qualified name `net.minecraft.text.Text.xxx()` due to compiler compatibility issues with Loom remapping
- **ItemButtonWidget**: Implemented `drawIcon()` method instead of overriding `renderWidget()` (which is now `final` in `PressableWidget`)

### API Changes (Minecraft 1.21.11)
- `PressableWidget.drawIcon(DrawContext, int, int, float)` - new abstract method
- `PressableWidget.renderWidget()` - now `final`, cannot be overridden

## [1.21.10] - 2026-07-16

### Changed
- Migrated from Minecraft 1.21.9 to 1.21.10
- Updated Fabric API to 0.138.4+1.21.10
- Updated Yarn mappings to 1.21.10+build.3

### Notes
- No breaking API changes from 1.21.9

## [1.21.9] - 2026-07-16

### Changed
- Migrated from Minecraft 1.21.8 to 1.21.9
- Updated Fabric API to 0.136.2+1.21.9
- Updated Yarn mappings to 1.21.9+build.4

### Fixed
- `entity.getWorld()` → `entity.getEntityWorld()`
- `world.isClient` (field) → `world.isClient()` (method)
- `context.player().getServer()` → `context.server()` in ServerPlayNetworking handlers
- `KeyBinding` constructor now requires `KeyBinding.Category.create(Identifier)` instead of `String`
- Mixin `PlayerEntityRenderer.updateRenderState` parameter changed from `AbstractClientPlayerEntity` to `PlayerLikeEntity`

### API Changes (Minecraft 1.21.9)
- `FeatureRenderer.render()` parameter changed from `VertexConsumerProvider` to `OrderedRenderCommandQueue`
- `ItemRenderer.renderItem()` signature completely changed (new ItemRenderState system)
- 3D backpack rendering temporarily disabled pending ItemRenderState API documentation

## [1.21.8] - 2026-07-16

### Changed
- Migrated from Minecraft 1.21.7 to 1.21.8

### Notes
- No breaking API changes from 1.21.7