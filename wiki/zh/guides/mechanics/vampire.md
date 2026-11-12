---
title: 吸血鬼
kind: mechanic
order: 12
source: ff83967294cc
---

在[血色新月](moon-nights.md)之夜，苍白的身影从黑暗中走到玩家身边：**吸血鬼**。它们看上去很友善。它们交谈，赞美夜色，请你多留一会儿。其中一只即将变成首领。

## 它何时到来

- 每逢血色新月，每一群主世界的玩家身边会出现 1 到 3 只友善的吸血鬼（彼此相距 64 格以内的玩家共享一群）。 <!-- config vampire.groupMin / groupMax / groupRadius -->
- **攻击其中一只**，它就会褪去伪装：它变成吸血鬼首领，其他的则消失。
- 如果没人动手，离某位玩家最近的吸血鬼会在 5 分钟后 <!-- config vampire.provokeMinutes --> 失去耐心，照样翻脸。

你无法靠彬彬有礼来避免这场战斗。你只能选择它何时开始。

## 吸血鬼的领域

吸血鬼翻脸时，**主世界中的每位玩家**都会被拖入它的领域，一座位于独立虚空中的竞技场。这里没有雾墙，但也无处可去。

- 首领的等级与参战者中最强的玩家相当：至少 15，在参战者中有人解锁领域之前，最高 20。 <!-- config vampire.bossMinLevel / bossCapNoRealm -->
- 留意它爪击前的起手动作；它的招式写在[吸血鬼](../../encyclopedia/bosses/vampire.md)页面上。
- **如果你在领域中死亡，你就出局了：** 你会照常重生，并且无法回来。领域关闭时，你的遗骸会被送到你被带走的地点。

## 胜利与失败

- **胜利：** 每位仍然存活的参战者都会直接在物品栏里获得个人战利品。你的首杀会给一个{{item embervoid:vampire_fang}}和 2 个治疗瓶碎片；之后的击杀有 50 % 几率给一个治疗瓶碎片 <!-- config vampire.firstKillShards / repeatShardChance -->。十秒后，你们所有人都会被送回被带走的地点，黎明降临。
- **失败：** 当所有参战者都死亡时，吸血鬼会大笑着融入夜色，夜晚结束。

## 如何准备

- 预计血色新月会在每次满月后的四天出现，并在黄昏前做好准备：治疗瓶满着，治疗瓶碎片已献上，装备已修好。
- 只有吸血鬼翻脸那一刻在主世界的玩家才会被拉进来。在余烬圣所或某个领域中的朋友是安全的，但也帮不上忙。
- 组队时，商量好由谁先动手。战斗会对主世界的所有人同时开始。

## 设置

{{config vampire.provokeMinutes}}
{{config vampire.bossMinLevel}}
{{config vampire.bossCapNoRealm}}
{{config vampire.firstKillShards}}

下一篇：[装备指南](../gear-guide.md)
