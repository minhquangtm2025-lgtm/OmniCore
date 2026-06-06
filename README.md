# 🧠 OmniCore
> The All-in-One AI Agent for Minecraft — Fabric Client Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20Mounts%20of%20Mayhem-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.16.9-blue)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

OmniCore là mod client-side dành cho Fabric, tích hợp AI agent hoàn chỉnh với khả năng tự động hóa chiến đấu, sinh tồn, dẫn đường và xây dựng. Hỗ trợ đầy đủ các tính năng mới của **Minecraft 1.21.11 (Mounts of Mayhem)** bao gồm vũ khí Spear, thú cưỡi Nautilus và Camel Husk.

---

## 📋 Mục lục

- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt](#-cài-đặt)
- [Build từ source](#-build-từ-source)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Modules](#-modules)
  - [Combat](#%EF%B8%8F-combat)
  - [Movement](#-movement)
  - [Survival](#-survival)
  - [Building](#%EF%B8%8F-building)
- [Lệnh trong game](#-lệnh-trong-game)
- [Cấu hình](#-cấu-hình)
- [Roadmap](#-roadmap)
- [Đóng góp](#-đóng-góp)

---

## 💻 Yêu cầu hệ thống

| Thành phần | Phiên bản |
|---|---|
| Minecraft Java Edition | 1.21.11 (Mounts of Mayhem) |
| Fabric Loader | ≥ 0.16.9 |
| Fabric API | 0.110.5+1.21.11 |
| Java | 21 |
| RAM (khuyến nghị) | ≥ 4GB |

---

## 🚀 Cài đặt

1. Cài [Fabric Loader](https://fabricmc.net/use/installer/) cho Minecraft 1.21.11
2. Tải [Fabric API](https://modrinth.com/mod/fabric-api) và bỏ vào thư mục `mods/`
3. Tải file `omnicore-1.0.0.jar` từ Releases và bỏ vào thư mục `mods/`
4. Khởi động Minecraft với Fabric profile

---

## 🔨 Build từ source

```bash
# Clone repository
git clone https://github.com/omnicore/omnicore.git
cd omnicore

# Build (Windows)
gradlew.bat build

# Build (Linux/macOS)
./gradlew build
```

File jar sẽ xuất hiện tại `build/libs/omnicore-1.0.0.jar`

> **Lưu ý:** Cần Java 21 và kết nối internet để Gradle tải dependencies lần đầu.

---

## 📁 Cấu trúc dự án

```
OmniCore/
├── src/main/java/com/omnicore/
│   ├── OmniCore.java              # Entry point (ClientModInitializer)
│   ├── ai/                        # AI learning & FSM (v0.5+)
│   ├── command/
│   │   └── CommandManager.java    # Xử lý lệnh .omni
│   ├── config/
│   │   └── ConfigManager.java     # Đọc/ghi omnicore.json
│   ├── event/
│   │   ├── Event.java             # Base event class
│   │   ├── EventBus.java          # Pub/Sub event bus
│   │   ├── EventManager.java      # Đăng ký Fabric callbacks
│   │   └── GameEvents.java        # Tick, Render, Packet events
│   ├── gui/                       # In-game GUI (v0.3+)
│   ├── mixin/
│   │   ├── MinecraftClientMixin.java
│   │   ├── ClientPlayerEntityMixin.java
│   │   └── ClientPlayNetworkHandlerMixin.java
│   ├── module/
│   │   ├── Module.java            # Base module class
│   │   ├── ModuleCategory.java    # COMBAT, MOVEMENT, SURVIVAL, BUILDING, UTILITY, AI
│   │   ├── ModuleManager.java     # Quản lý và tick tất cả modules
│   │   ├── combat/
│   │   │   ├── KillAuraModule.java
│   │   │   ├── SpearCombatModule.java
│   │   │   ├── AutoDodgeModule.java
│   │   │   ├── AutoShieldModule.java
│   │   │   └── CrystalPvPModule.java
│   │   ├── movement/
│   │   │   ├── PathfindingModule.java
│   │   │   ├── ElytraFlyModule.java
│   │   │   ├── MountControlModule.java
│   │   │   └── AutoWalkModule.java
│   │   ├── survival/
│   │   │   ├── AutoEatModule.java
│   │   │   ├── AutoFarmModule.java
│   │   │   ├── AutoMineModule.java
│   │   │   ├── AutoFishModule.java
│   │   │   └── AutoCraftModule.java
│   │   └── building/
│   │       ├── SchematicBuilderModule.java
│   │       ├── AutoBridgeModule.java
│   │       └── AutoPillarModule.java
│   ├── pathfinding/
│   │   ├── PathNode.java          # Node cho A*
│   │   └── AStarPathfinder.java   # Thuật toán A* cải tiến
│   └── util/
│       ├── PlayerUtil.java        # Helper: máu, đói, vị trí
│       ├── InventoryUtil.java     # Helper: tìm/chuyển slot
│       ├── RotationUtil.java      # Helper: xoay góc nhìn
│       └── EntityUtil.java        # Helper: tìm/lọc entity
├── src/main/resources/
│   ├── fabric.mod.json
│   └── omnicore.mixins.json
├── build.gradle
├── gradle.properties
└── README.md
```

---

## 🧩 Modules

### ⚔️ Combat

#### KillAura
Tự động tấn công kẻ thù gần nhất với smart weapon switching và critical hits.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| range | 5.0 | Bán kính tấn công (blocks) |
| targetPlayers | true | Tấn công player khác |
| targetMobs | true | Tấn công hostile mobs |
| autoSwitch | true | Tự động chuyển vũ khí tốt nhất |
| criticalHits | true | Nhảy để đánh chí mạng |

**Weapon switching logic:**
- `< 3 blocks` → Sword / Axe
- `3–12 blocks` → Bow / Crossbow

---

#### SpearCombat ⭐ NEW in 1.21.11
Xử lý hai kiểu tấn công của Spear (Ngọn giáo) – vũ khí mới trong Mounts of Mayhem.

| Kiểu | Khoảng cách | Cơ chế |
|---|---|---|
| **Jab** (Đâm) | 1.5 – 4.5 blocks | Nhấn trái nhanh, đánh nhiều mục tiêu, knockback |
| **Charge** (Xốc tới) | 4.0 – 12.0 blocks | Giữ phải rồi nhả, sát thương theo tốc độ |

- Khi cưỡi Nautilus/Camel/Horse: **ưu tiên Charge** để tận dụng tốc độ
- Tự động kích hoạt enchant **Lunge** khi Jab

---

#### CrystalPvP
Tự động đặt và kích nổ End Crystal gần mục tiêu.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| maxPlaceDistance | 4.0 | Khoảng cách tối đa đặt crystal |
| selfProtection | true | Không kích nổ crystal khi quá gần bản thân |

> Yêu cầu: End Crystal trong hotbar, mặt đất là Obsidian hoặc Bedrock.

---

#### AutoDodge
Phát hiện và né tự động mũi tên, đòn cận chiến, Spear Charge.

---

#### AutoShield
Tự động giơ/hạ khiên dựa trên mối đe dọa gần nhất.

> Yêu cầu: Shield trong offhand slot.

---

### 🧭 Movement

> Toàn bộ hệ thống Movement được xây dựng tương đương **Baritone** về kiến trúc và thuật toán, tích hợp native vào OmniCore cho Minecraft 1.21.11 Fabric.

---

#### Pathfinding (tương đương Baritone core)

A* pathfinder chạy **async trên thread riêng**, không lag client. Hỗ trợ đầy đủ các loại movement:

| Movement Type | Mô tả | Cost |
|---|---|---|
| Walk / Sprint | Di chuyển phẳng cardinal + diagonal | 1.0 / 0.83 |
| Jump | Bước lên 1 block | 1.5 |
| Fall | Rơi xuống an toàn tối đa 3 blocks | 1.1–2.6 |
| Swim | Bơi trong nước mọi hướng | 8.0 |
| Swim Up | Ngoi lên mặt nước | 10.0 |
| Ladder / Vine | Leo thang/dây leo lên xuống | 5.0 |
| Parkour | Nhảy qua gap 1 block (cardinal) | 2.0 |
| Dig Horizontal | Đào 1–2 block để đi qua | 10.0–18.0 |
| Dig Down | Đào xuống thẳng đứng | 10.0 |
| Diagonal | Di chuyển chéo 45° | 1.41 |

**Kiến trúc (tương đương Baritone):**

| OmniCore class | Baritone tương đương |
|---|---|
| `AStarPathfinder` | `AbstractNodeCostSearch` |
| `PathContext` | `CalculationContext` |
| `PathNode` + `MovementType` | Individual `Movement` classes |
| `PathExecutor` | `PathExecutor` + `MovementHelper` |
| `PathfindingModule` | `IPathingBehavior` |

**Cost system** – giống Baritone: sprint = walk − 0.17, diagonal = walk × √2, mỗi block phải đào cộng thêm 10.0, có pickaxe được trừ bonus.

**Tránh nguy hiểm tự động:** lava, fire, cactus, magma block, sweet berry bush.

**Stuck detection:** nếu không di chuyển > 0.1 block trong 60 tick → tự recalculate path.

```
.omni goto 1000 2000          # goto x z (giữ nguyên Y)
.omni goto 1000 64 2000       # goto x y z (chỉ định Y)
.omni dig on                  # bật chế độ đào (như Baritone MineProcess)
.omni dig off                 # tắt chế độ đào
.omni status                  # xem trạng thái: IDLE / CALCULATING / EXECUTING (3/87)
.omni stop                    # dừng mọi movement
```

---

#### Follow (tương đương Baritone FollowProcess)

Liên tục recompute path đến một entity đang di chuyển. Tự dừng khi đến gần.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| stopRange | 3.0 blocks | Khoảng cách dừng lại |

```
.omni follow              # follow entity gần nhất
.omni follow Steve        # follow player theo tên
.omni stop                # dừng follow
```

---

#### ElytraFly (tương đương Baritone ElytraBehavior)

Bay elytra tự động với terrain avoidance và altitude hold.

| Tính năng | Mô tả |
|---|---|
| Auto-launch | Tự nhảy + kích hoạt elytra từ mặt đất |
| Altitude hold | Giữ nguyên độ cao khi bật (hoặc đặt targetAltitude) |
| Terrain avoidance | Look-ahead 4 blocks, pitch up -60° khi có chướng ngại |
| Smooth pitch | Điều chỉnh pitch tối đa 5°/tick (không snap tức thì) |
| Auto boost | Tự dùng firework khi tốc độ < 0.3 m/tick hoặc mỗi 40 tick |

| Tham số | Mặc định | Mô tả |
|---|---|---|
| targetAltitude | Y khi bật | Độ cao muốn duy trì |
| cruisePitch | -20° | Pitch bay bằng |
| autoBoost | true | Tự dùng firework |
| minSpeed | 0.3 m/tick | Boost khi dưới tốc độ này |

---

#### MountControl ⭐ NEW in 1.21.11

Điều khiển thú cưỡi tích hợp với PathfindingModule – tự lái mount theo path.

| Mount | Hành vi |
|---|---|
| **Horse / Donkey / Mule** | Auto-sprint, auto-jump qua fence/obstacle, pathfind theo goal |
| **Camel** | Auto-sprint, auto-dash (space) khi tiếp cận target |
| **Nautilus** (1.21.11) | Sprint-dash dưới nước, tự nhắm target, Breath of the Nautilus |
| **Boat** | Paddle tự động, không sprint |
| **Pig** | Tự chọn carrot-on-a-stick trong hotbar |
| **Strider** | Tự chọn warped-fungus-on-a-stick trong hotbar |

**Tích hợp PathfindingModule:** khi có goal active, MountControl tự steer mount theo waypoint tiếp theo.

---

#### AutoWalk (tương đương Baritone SimpleMoveProcess)

Di chuyển liên tục về phía trước với đầy đủ environment handling:

| Tính năng | Mô tả |
|---|---|
| Sprint | Auto-sprint trên mặt đất |
| Auto-jump | Nhảy qua obstacle khi horizontal collision |
| Swim | Tự bơi + ngoi lên mặt nước (hold jump) |
| Ladder climb | Giữ forward khi trên ladder/vine |
| Edge sneak | Tự sneak khi phát hiện drop > 3 blocks phía trước (bật tắt được) |

| Tham số | Mặc định | Mô tả |
|---|---|---|
| autoSprint | true | Sprint liên tục |
| autoSwim | true | Tự xử lý khi trong nước |
| autoClimb | true | Tự leo ladder/vine |
| sneakAtEdges | false | Sneak khi gần vực |

---

### 🌿 Survival

#### AutoEat
Tự động ăn khi đói, chọn thức ăn có nutrition cao nhất trong hotbar.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| eatThreshold | 16/20 | Mức đói bắt đầu ăn |

---

#### AutoFarm
Quét bán kính và thu hoạch crop chín, tự động gieo lại hạt giống.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| scanRadius | 5 | Bán kính quét (blocks) |

---

#### AutoMine
Đào theo vein quặng hoặc đào tunnel 1×2.

| Mode | Mô tả |
|---|---|
| `VEIN` | Quét bán kính, xếp hàng đợi và đào hết vein |
| `TUNNEL` | Đào đường hầm 1×2 theo hướng nhìn |

```
.omni mine diamond -y -59
```

---

#### AutoFish
Tự động thả cần, phát hiện cá cắn câu (bobber dip), kéo cần.

---

#### AutoCraft
Xếp hàng recipe và tự động craft khi mở bàn Crafting Table.

---

### 🏗️ Building

#### SchematicBuilder
Đọc danh sách block và tự động xây theo thứ tự.

```java
// Ví dụ dùng API
Map<BlockPos, String> blocks = new HashMap<>();
blocks.put(new BlockPos(0,0,0), "minecraft:stone");
blocks.put(new BlockPos(1,0,0), "minecraft:oak_planks");
schematicBuilder.loadSchematic(playerPos, blocks);
```

---

#### AutoBridge
Tự động đặt block dưới chân khi đi qua khoảng trống.

---

#### AutoPillar
Tự động đặt block dưới chân và nhảy để leo lên cao.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| targetHeight | 10 | Số block muốn leo lên |

---

## 💬 Lệnh trong game

Tất cả lệnh bắt đầu bằng `.omni` (gõ trong chat):

| Lệnh | Mô tả |
|---|---|
| `.omni help` | Hiện danh sách lệnh |
| `.omni list` | Liệt kê tất cả modules và trạng thái |
| `.omni toggle <module>` | Bật/tắt module theo tên |
| `.omni combat enable` | Bật tất cả combat modules |
| `.omni combat disable` | Tắt tất cả combat modules |
| `.omni goto <x> <z>` | Dẫn đường đến tọa độ |

**Ví dụ:**
```
.omni toggle KillAura
.omni toggle SpearCombat
.omni goto 500 -300
.omni combat enable
```

---

## ⚙️ Cấu hình

File config được lưu tại: `.minecraft/config/omnicore.json`

File tự tạo khi khởi động lần đầu. Ví dụ nội dung:

```json
{
  "combat": {
    "enabled": true,
    "target_priority": [
      "nearest_player",
      "attacking_player",
      "weak_player",
      "hostile_mob",
      "neutral_mob"
    ],
    "weapon_priority": {
      "melee": ["spear", "mace", "axe", "sword", "trident"],
      "ranged": ["crossbow", "bow", "trident_thrown"]
    },
    "spear": {
      "enabled": true,
      "jab_cps": 8,
      "charge_min_distance": 4,
      "charge_max_distance": 12,
      "prefer_charge_when_mounted": true,
      "lunge_enchant_use": true
    },
    "crystal_pvp": {
      "enabled": true,
      "max_distance": 4,
      "self_damage_protection": true
    },
    "dodge": {
      "enabled": true,
      "arrow_prediction": true,
      "spear_charge_dodge": true,
      "explosion_avoidance": true
    },
    "health": {
      "run_away_threshold": 25,
      "potion_heal_threshold": 50
    },
    "mount_combat": {
      "nautilus_dash": true,
      "prefer_spear_when_mounted": true
    }
  }
}
```

---

## 🗺️ Roadmap

| Phiên bản | Trạng thái | Tính năng |
|---|---|---|
| **v0.1** | ✅ Done | Pathfinding core + AutoEat + AutoTool |
| **v0.2** | ✅ Done | KillAura + SpearCombat + AutoFarm + AutoMine |
| **v0.3** | 🚧 WIP | SchematicBuilder + AutoBridge + AutoPillar + MountControl |
| **v0.4** | 📋 Planned | AI Learning Mode + AutoCrystal nâng cao |
| **v0.5** | 📋 Planned | In-game GUI + Desync support |
| **v1.0** | 📋 Planned | Release chính thức, tối ưu toàn bộ |

---

## 🤝 Đóng góp

1. Fork repository
2. Tạo branch mới: `git checkout -b feature/ten-tinh-nang`
3. Commit thay đổi: `git commit -m "Add: mô tả ngắn"`
4. Push lên branch: `git push origin feature/ten-tinh-nang`
5. Mở Pull Request

### Quy tắc code
- Java 21, tuân theo code style hiện có
- Mỗi module phải kế thừa `Module.java`
- Không hardcode giá trị – dùng config hoặc field có setter
- Comment bằng tiếng Anh hoặc tiếng Việt đều được

---

## ⚠️ Tuyên bố miễn trừ

OmniCore được tạo ra với mục đích học tập và nghiên cứu kỹ thuật lập trình game. Việc sử dụng mod này trên các server có thể vi phạm Terms of Service. **Người dùng tự chịu trách nhiệm** về cách sử dụng.

---

## 📄 License

MIT License — xem file [LICENSE](LICENSE) để biết chi tiết.
