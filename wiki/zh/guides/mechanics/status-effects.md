---
title: 状态效果
kind: mechanic
order: 10
effect: embervoid:bleeding
source: f6f67b69063a
---

Embervoid 目前只新增了一种状态效果：**流血**。

## 流血

流血每秒造成一次伤害，并且无视盔甲。它有三个等级：

| 等级 | 每秒伤害 |
|---|---|
| I | 0.5（四分之一颗心） |
| II | 1 |
| III | 1.5 |

<!-- config gear.bleeding.damagePerLevel, gear.bleeding.maxLevel -->

每次造成流血的命中都会让它升高一级，最高到 III，并重新开始它的 6 秒 <!-- config
gear.bleeding.durationTicks: 120 ticks -->。持续命中，它就保持满强度；停手后几秒它就会消退。流血而死的怪物算作你的击杀。

### 谁能造成它

流血是你的武器带来的，而不是你敌人的：

- {{item embervoid:wardens_scourge}}：每次命中增加一级。
- {{item embervoid:royal_ash_rapier}}：每次命中增加一级，其突刺一次增加两级。

有些生物没有血可流，永远不会流血：不安骸骨、树皮活尸、熔炉傀儡、灰烬巨像和哀恸之树。对它们请换别的手段。

在玩家对战中，这些武器同样会让其他玩家流血。

### 如何止住

- {{item embervoid:amber_balm}}能治愈流血和中毒，然后缓慢为你回血。
- 一桶牛奶像清除其他效果那样清除它。
- 或者等它自己结束：没有新的命中，6 秒后就会结束。

## 设置

{{config gear.bleeding.durationTicks}}
{{config gear.bleeding.maxLevel}}
{{config gear.bleeding.damagePerLevel}}

下一篇：[月夜](moon-nights.md)
