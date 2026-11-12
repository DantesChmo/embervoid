---
title: 怪物等级与难度
kind: mechanic
order: 2
source: 27bec0020add
---

每只敌对生物，无论原版还是其他模组的，在首次进入世界时都会获得一个等级，只此一次。看向一只怪物，它的名字上会显示“Lv. N”。

## 怪物的等级如何确定

在主世界、下界和末地，等级是该怪物类型的**基础等级**加上各种加成：

| 基础等级 | 怪物 |
|---|---|
| 3 | 僵尸、僵尸村民、尸壳、溺尸、蜘蛛 |
| 4 | 骷髅、流浪者、史莱姆，以及任何未列出的敌对生物 |
| 5 | 灰烬食尸鬼 |
| 6 | 苦力怕、女巫 |
| 8 | 余烬鬼火 |
| 12 | 末影人 |
| 15 | 活尸 |

<!-- config mobs.levels.baseLevels, mobs.levels.defaultBaseLevel -->

| 加成 | 等级 |
|---|---|
| 主世界：距世界出生点每 1000 格 | +1，最多 +5 |
| 主世界：y = 0 以下 | +3 |
| 下界 | +5 |
| 末地 | +8 |
| 主世界满月之夜 | +3 |

<!-- config mobs.levels.blocksPerLevel, maxDistanceBonus, belowZeroBonus, dimensionBonus, moon.fullMoon.levelBonus -->

在领域中，怪物的等级改由它生成所在的区域决定：在灰烬边域，灰烬田野为 15–18，王家林地为 18–21，沉陷墓城为 21–25，阴燃要塞为 25–30。首领的等级是固定的。

末影龙、凋灵、监守者和远古守卫者没有等级，保持原版的样子。

## 等级带来的变化

每高出 1 级，怪物会获得：

- +6 % 生命值，
- +5 % 伤害（近战、箭矢、爆炸和药水一视同仁），
- +6 % 死亡时的经验。

<!-- config mobs.levels.healthPerLevel / damagePerLevel / experiencePerLevel -->

所以一只 11 级的怪物，生命比 1 级时多 60 %，打人也重 50 %。有些原版怪物会先被削弱，以免成长得超过其他怪物：僵尸和尸壳的基础生命较低，女巫和末影人的基础伤害较低。

## 难度

Embervoid 替换了原版对敌对生物打到玩家身上的伤害所用的难度系数：

| 难度 | Embervoid | 原版 |
|---|---|---|
| 简单 | ×1.0 | 约 ×0.5 |
| 普通 | ×1.25 | ×1.0 |
| 困难 | ×1.75 | ×1.5 |

<!-- config mobs.difficulty.* -->

简单难度现在相当于原版普通难度。该系数先于等级加成生效。

## 设置

{{config mobs.levels.enabled}}
{{config mobs.levels.baseLevels}}
{{config mobs.difficulty.normal}}

其他模组的怪物使用默认基础等级；请在 `mobs.levels.baseLevels` 中给它们设定各自的等级。怪物等级标签可以在你的客户端上通过 `embervoid-client.json` 中的 `hud.showMobLevel` 隐藏。所有键都在[配置](../configuration.md)中。

下一篇：[精力与特殊攻击](stamina-and-specials.md)
