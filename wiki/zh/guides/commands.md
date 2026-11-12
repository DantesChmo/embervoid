---
title: 命令
kind: guide
order: 9
source: 35a2e3962f63
---

Embervoid 的命令供服务器管理员和测试使用。所有命令都以 `/embervoid` 开头，需要管理员权限（等级 2）；单人游戏中请开启作弊。它们用于救助卡住的玩家或试验某些功能，并非日常游玩所需，输出内容仅有英文。

`<stat>` 是 `vitality`、`endurance`、`strength`、`dexterity`、`kindling`、`resilience`、`fortune` 之一，或其简称（`vit`、`end`、`str`、`dex`、`kin`、`res`、`for`）。

## 玩家

| 命令 | 作用 |
|---|---|
| `/embervoid stats` | 你的属性、等级、下一级的花费，以及各属性提供的加成 |
| `/embervoid levelup <stat>` | 像灰烬圣女那样花费经验购买一点属性 |
| `/embervoid setstat <stat> <value>` | 免费设置某项属性（1–999） |
| `/embervoid stamina` | 你的精力 |
| `/embervoid stamina spend <amount>` | 像特殊攻击那样消耗精力 |
| `/embervoid stamina set <value>` | 设置你的精力 |
| `/embervoid data` | 服务器上保存的你的 Embervoid 玩家数据 |
| `/embervoid data flaskChargeUpgrades <n>` | 设置你的治疗瓶已吸收的治疗瓶碎片数量 |
| `/embervoid remnant` | 像死亡那样，把你的物品和经验放入脚下的灰烬遗骸 |
| `/embervoid reliquary <player>` | 查看某位玩家的圣物匣 |
| `/embervoid reliquary <player> open` | 替该玩家打开其圣物匣 |
| `/embervoid reliquary <player> clear` | 清空该玩家的圣物匣 |

## 进度

| 命令 | 作用 |
|---|---|
| `/embervoid progress <player>` | 查看某位玩家已解锁、已完成的领域和已击败的首领 |
| `/embervoid progress <player> reset` | 清除以上全部 |
| `/embervoid progress <player> unlock <realm>` | 解锁一个领域，例如 `embervoid:realm_1` |
| `/embervoid progress <player> complete <realm>` | 将一个领域标记为已完成 |
| `/embervoid progress <player> boss <id>` | 将一位首领标记为已击败，例如 `embervoid:fallen_sister` 或 `embervoid:ashen_reach/zone_a` |

领域 id 为 `embervoid:realm_1` 到 `embervoid:realm_4`。首领 id：`embervoid:fallen_sister`、`embervoid:ashen_reach/zone_a`（灰烬巨像）、`zone_b`（哀恸之树）、`zone_c`（墓穴看守者）和 `final`（阴燃领主）。

## 传送

| 命令 | 作用 |
|---|---|
| `/embervoid hub` | 前往余烬圣所 |
| `/embervoid hub leave` | 走余烬圣所的备用出口，无视被点燃的石碑：回到你上次离开主世界的地方，否则回到你的床边或世界出生点 |
| `/embervoid hub check` | 检查余烬圣所是否完好 |
| `/embervoid stele` | 世界中被点燃的裂石碑所在的位置 |
| `/embervoid stele kindle <pos>` | 像哭泣之魂那样，点燃指定位置的未点燃石碑 |
| `/embervoid realm enter <realm> [toShrine]` | 像虚空方尖碑那样进入某个领域；`true` 表示前往你在该领域最后休息的祭坛 |
| `/embervoid realm leave` | 像归返方尖碑那样，离开领域前往余烬圣所 |
| `/embervoid realm where` | 你所在位置的领域、区域、等级段和地点 |
| `/embervoid realm arenas` | 领域内的每个首领竞技场，以及你是否已击败 |
| `/embervoid realm roads [posts]` | 领域的道路、里程石、哨站（以及灯柱） |
| `/embervoid realm edge` | 领域的边界墙和断桥 |
| `/embervoid realm border <from> <to>` | 最终区域的边界墙 |
| `/embervoid realm find <block> <x> <z>` | 该列中最高的指定方块，例如要塞大门 |
| `/embervoid realm data` | 你的领域旅行数据 |

## 首领、地下城与怪物

| 命令 | 作用 |
|---|---|
| `/embervoid boss fights` | 正在进行的首领战 |
| `/embervoid sanctum arena <x> <z>` | 起点位于该区块的荒废圣所的首领竞技场（即 `/locate` 所指之处），以及它是否已被清除 |
| `/embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]` | 统计世界生成会在你周围放置多少座荒废圣所，不会真正生成任何东西 |
| `/embervoid moblevel <targets>` | 显示怪物的等级，以及此处新生成的怪物会获得的等级 |
| `/embervoid moblevel <targets> <level>` | 设置怪物的等级 |
| `/embervoid gear bleed <targets> [stacks]` | 按相应次数的命中，增加流血等级 |

要寻找结构，请使用原版的 `/locate structure embervoid:ember_sanctum`（荒废圣所）；在灰烬边域中则使用 `embervoid:ashen_reach_arena_a`、`_b` 和 `_c`（各区域的竞技场）。

## 月夜与吸血鬼

| 命令 | 作用 |
|---|---|
| `/embervoid moon status` | 当前或下一次月夜 |
| `/embervoid moon start full` / `start blood` | 立即开始一次满月或血色新月 |
| `/embervoid moon stop` | 结束月夜 |
| `/embervoid moon setcycle <n>` | 把时钟设为第 n 个月相周期的清晨（第 8·n 天） |
| `/embervoid moon curse <player> <stacks>` | 设置某位玩家的夜之诅咒 |
| `/embervoid wave status` | 满月的怪物波次 |
| `/embervoid wave spawn <players>` / `clear <players>` | 向这些玩家派出或移除他们的怪物波次 |
| `/embervoid vampire status` | 今晚的吸血鬼与战斗 |
| `/embervoid vampire provoke` | 最近的吸血鬼立刻翻脸 |
| `/embervoid vampire arena open` / `close` / `check` | 在没有战斗的情况下操作吸血鬼的竞技场 |

## 性能

| 命令 | 作用 |
|---|---|
| `/embervoid perf ticks` | 服务器的刻耗时 |
| `/embervoid perf chunks <from> <to>` | 某个区域内已加载的区块 |
| `/embervoid perf entities <from> <to>` | 某个区域内的实体、怪物和放置的敌人 |

## 客户端

`/embervoid_client data`（无需管理员权限）显示你自己的客户端所看到的玩家数据。

下一篇：[常见问题与故障排除](faq.md)
