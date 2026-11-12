---
title: 数据包与兼容性
kind: guide
order: 8
source: a2501f21866e
---

Embervoid 的很大一部分是数据：数据包可以改变地下遗迹出现的位置、箱子里的内容，以及其他模组的内容如何与之配合。下面的路径都位于数据包的 `data/` 文件夹内。

## 可以覆盖的世界生成

| 文件 | 控制的内容 |
|---|---|
| `embervoid/worldgen/structure_set/ember_sanctum.json` | 荒废圣所出现的频率：默认 `spacing` 为 22、`separation` 为 8 个区块（大约每 300–400 格一座）。两者调大，地下遗迹就更稀少。 |
| `embervoid/tags/worldgen/biome/has_structure/ember_sanctum.json` | 荒废圣所可以出现在哪些生物群系 |
| `embervoid/worldgen/structure/ember_sanctum.json`、`embervoid/worldgen/template_pool/sanctum/*` | 遗迹本身及其各个部件 |
| `embervoid/tags/worldgen/structure/on_sanctum_maps.json` | 圣所地图所指向的结构 |
| `embervoid/worldgen/biome/ashen_reach_*.json` | 领域中各区域的外观：颜色、粒子、环境音 |
| `embervoid/worldgen/configured_feature/ashen_reach/*`、`placed_feature/ashen_reach/*` | 领域的矿石、树木、碎石和道路 |

对领域的维度、噪声设置和结构集（`embervoid/dimension/ashen_reach.json`、`embervoid/worldgen/noise_settings/ashen_reach.json`、`embervoid/worldgen/structure_set/ashen_reach_*`）要格外小心。领域的区域、竞技场、大门和要塞是由代码按同样的数值布置的；只改其中一边会让领域崩坏。请不要动余烬圣所和吸血鬼的竞技场（`embervoid/dimension/hub.json`、`vampire_arena.json`）。

灰烬边域的怪物不是由生物群系文件生成的（它们的生成列表是故意留空的）。请通过[配置](configuration.md)中的 `reachMobs.spawns` 来调整它们的生成。

## 战利品表

- `embervoid/loot_tables/chests/sanctum_outer_hall.json`：荒废圣所外厅的箱子。
- `embervoid/loot_tables/gameplay/fallen_sister_first_clear.json`：玩家首次清除圣所时的额外战利品。
- `embervoid/loot_tables/chests/ashen_reach/*`：领域的箱子，每个区域三种（普通、竞技场、宝藏）。
- `embervoid/loot_tables/entities/*`：怪物和首领的掉落。

有些掉落是模组在这些表之外额外加上的：领域怪物身上的领域材料、护甲套装部件和独特部件，宝箱和精英怪身上的戒指与神器，以及原版箱子里的圣所地图。**如果你的数据包替换了其中某张表，这些额外内容就不会再加进去**：只由你的表说了算。它们的概率改由配置控制（`gear.loot.*`、`reliquary.drops.*`、`sanctum.mapChance*`）。

## 标签

| 标签 | 含义 |
|---|---|
| `embervoid:unleveled`（实体类型） | 永远没有等级的怪物：末影龙、凋灵、监守者、远古守卫者。可以把其他模组的首领加到这里。 |
| `embervoid:bleed_immune`（实体类型） | 永远不会流血的怪物 |
| `embervoid:breaks_guard`（物品） | 能像斧头一样破开活尸格挡的武器（负灰者重锤） |
| `embervoid:artifacts`、`embervoid:rings`（物品） | 能放入圣物匣神器栏和戒指栏的物品 |
| `embervoid:realm_ores`（方块） | 掘地者戒指会提供急迫效果的矿石 |
| `embervoid:dead_logs`（方块、物品） | 枯木原木 |
| `embervoid:ashen_reach*`（生物群系） | 领域的生物群系，包括全部以及各区域的 |

## 其他模组

目前没有已知的冲突。需要留意的事项：

- **其他模组的怪物**会像原版怪物一样获得等级，使用默认基础等级。请在 `mobs.levels.baseLevels` 中给它们设定合适的等级；如果它们的首领应保持原作者的设计，就把这些首领加入 `embervoid:unleveled`。
- **墓碑或尸体类模组**同样会在死亡时起作用。请只用一套系统：要么把 `remnant.mode` 设为 `off`，要么不装另一个模组。
- **伤害与难度类模组：** Embervoid 替换了原版对怪物伤害玩家所用的难度系数（`mobs.difficulty.*`）。另一个修改同一内容的模组可能会与之叠加。
- Embervoid 的属性加成在原版有对应属性的地方使用原版属性（最大生命值、攻击速度、击退抗性、盔甲韧性、幸运），因此读取属性的模组能看到这些加成。

下一篇：[命令](commands.md)
