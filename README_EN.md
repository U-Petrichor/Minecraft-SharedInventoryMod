<div align="center">

# SharedInventoryMod · Shared Storage

<img src="./宣传图.png" alt="SharedInventoryMod Promo" width="100%"/>

A Fabric mod for Minecraft 1.20.1 that integrates a **mega private backpack**, **player-shared backpack**, and **portable workstations** into one interface.

One backpack, connecting all companions.

</div>

---

## ✨ Features

- 🎒 **Mega Private Backpack** — 60 slots/page × 24 pages = **1440 slots** of personal storage, with pagination and tab labels for quick navigation.
- 🔗 **Shared Core** — Place a block in the world as a "storage hub"; all bound backpacks share the same 4×4 inventory.
- 🧰 **Portable Workstations** — Switch between **Crafting Table / Furnace / Brewing Stand / Anvil / Smithing Table** right inside the backpack interface.
- 💀 **Keep on Death** — Private backpack and equipped backpack are retained after death and dimension travel.

---

## 🧊 Core Items

### Shared Core

A block entity that stores **public items**. Once a backpack is bound to a Shared Core, it can read and write the core's 4×4 = 16 shared slots.

**Crafting Recipe:**

```
Diamond  Ender Eye  Diamond
Diamond   Chest    Diamond
Iron Block Iron Block Iron Block
```

### Shared Inventory Backpack

The key to opening the integrated interface (Private Backpack + Shared Backpack + Workstations). **Right-click to equip to the chest slot**, then press <kbd>B</kbd> to open.

**Crafting Recipe:**

```
Leather  Stick  Leather
Leather Ender Eye Leather
Leather Leather Leather
```

---

## 📖 Usage

### 1. Bind to a Shared Core

Hold the **Shared Inventory Backpack** and right-click a placed **Shared Core** block. The backpack will record the core's coordinates, and the chat will display "Successfully linked to shared core!".

### 2. Equip the Backpack

Hold the backpack and **right-click** to equip it to the **dedicated backpack slot** (a hidden slot independent of the chest armor slot).

### 3. Open the Interface

While equipped, press <kbd>B</kbd> (default keybind, configurable in "Options → Controls → Key Binds → Shared Inventory") to open the integrated interface:

| Area | Description |
| --- | --- |
| **Private Backpack** (left 6×10) | Your personal storage, paginated (24 pages total) |
| **Shared Backpack** (right 4×4) | Shared among all players bound to the same core, synced in real-time |
| **Workstations** (bottom) | Crafting Table / Furnace / Brewing Stand / Anvil / Smithing Table, switch as needed |
| **Armor + Off-hand** | Same as vanilla equipment slots |
| **Player Inventory + Hotbar** | Same as vanilla |

### 4. Pagination & Labels

- "Back / Next" buttons: browse page by page.
- "Go" button: enter a page number or label name to jump directly; label names support exact/fuzzy matching.

---

## ⚙️ Dependencies

| Item | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.16.10 |
| Fabric API | 0.92.5+1.20.1 |
| Java | ≥ 17 |

---

## 🔧 Building from Source

```bash
# Windows
./gradlew.bat build

# macOS / Linux
./gradlew build
```

Build output is located in `build/libs/`, where `SharedInventoryMod-fabric-1.20.1-1.0.jar` is the distributable mod file.

Place it in `.minecraft/mods/` and install [Fabric API](https://modrinth.com/mod/fabric-api) to play.

---

## 🗂️ Project Structure

```
src/main/java/com/petrichor/sharedInventory/
├── SharedInventoryMod.java          # Main entry (item/block/network registration)
├── SharedInventoryModClient.java    # Client entry (keybinds, rendering)
├── block/                           # Shared Core block & block entity
├── item/                            # Shared Inventory Backpack, Inventory wrapper
├── inventory/                       # Private backpack, workstation logic, interaction handling
├── screen/                          # Integrated interface ScreenHandler
├── network/                         # Pagination / label / open backpack network packets
├── mixin/                           # Player data persistence, death retention, backpack rendering
└── client/                          # Backpack equip rendering
```

---

## 🌐 Localization

Built-in languages:

- Simplified Chinese (zh_cn)
- Traditional Chinese (zh_tw)
- English (en_us)

To add a new language, create a corresponding JSON file in `src/main/resources/assets/shared_inventory_mod/lang/`.

---

## 📜 License

This project is released under the [**CC BY-NC 4.0** (Attribution-NonCommercial 4.0 International)](https://creativecommons.org/licenses/by-nc/4.0/deed.en) license.

You are free to **share** and **adapt** (reference, modify, redistribute) this work under the following conditions:

- ✏️ **Attribution** — You must give appropriate credit to the original author (Petrichor) and provide a link to the license.
- 🚫 **NonCommercial** — You may not use the material for commercial purposes (including but not limited to selling this mod or its derivatives).

See [LICENSE](./LICENSE) for the full legal text.

---

<div align="center">

Author: **Petrichor** · For Minecraft 1.20.1 (Fabric)

</div>
