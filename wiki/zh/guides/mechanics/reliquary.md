---
title: 圣物匣
kind: mechanic
order: 8
source: 19d00b168d83
---

圣物匣存放你在盔甲之外佩戴的小物件：**神器**和**戒指**。它们全都来自灰烬边域。

## 打开它

按 **R** 键（可以在“控制”中把它重新绑定为“打开圣物匣”），或点击物品栏里配方书旁边的圣物匣按钮。界面会显示你的角色，饰品栏位在旁边。

你有 **2 个神器栏**和 **2 个戒指栏** <!-- config reliquary.slots.artifact / reliquary.slots.ring -->。你不能同时佩戴两件相同的物品。

## 神器：属性点

每件神器在你佩戴期间都会增加属性点：

| 神器 | 提供 | 出处 |
|---|---|---|
| {{item embervoid:last_harvest_grain}} | +3 生命力 | 灰烬田野 |
| {{item embervoid:ploughmans_horseshoe}} | +3 耐力 | 灰烬田野 |
| {{item embervoid:royal_hound_fang}} | +3 灵巧 | 王家林地 |
| {{item embervoid:amber_tear}} | +3 炽焰 | 王家林地 |
| {{item embervoid:gravestone_medallion}} | +3 韧性 | 沉陷墓城 |
| {{item embervoid:ferrymans_coin}} | +3 幸运 | 沉陷墓城 |
| {{item embervoid:wardens_chain}} | +3 力气 | 沉陷墓城 |
| {{item embervoid:seal_of_the_ashen_king}} | 每项属性 +1 | 阴燃要塞 |

<!-- config reliquary.artifacts.points / sealPoints -->

神器的点数对属性的一切效果都算数，但不会提高你的等级、升级花费或属性上限。升级界面会把它们显示在各属性旁边。

## 戒指：效果

| 戒指 | 效果 | 出处 |
|---|---|---|
| {{item embervoid:couriers_ring}} | +15 % 疾跑速度 | 灰烬田野 |
| {{item embervoid:ring_of_ash_feathers}} | 下落变慢，8 格以内不会受到摔落伤害（潜行可正常下落） | 灰烬田野 |
| {{item embervoid:ring_of_the_fog_stalker}} | 领域中的迷雾消散距离加倍（首领竞技场除外） | 王家林地 |
| {{item embervoid:delvers_ring}} | 开采领域矿石时获得急迫 I | 王家林地 |
| {{item embervoid:drowned_mans_ring}} | 水下呼吸 | 沉陷墓城 |
| {{item embervoid:ring_of_embers}} | 近战命中会让目标着火 2 秒 | 阴燃要塞 |

<!-- config reliquary.rings.* -->

## 在哪里掉落

每个区域只掉落它自己的饰品：

- 它的**宝箱**：8 % 的几率出神器，5 % 出戒指；
- 它的**精英怪**，由玩家击杀：2 % 出神器，1 % 出戒指。精英怪分别是灰烬食腐鸦（田野）、墓园敲钟人（墓城）和阴燃骑士（要塞）。王家林地没有精英怪：它的饰品只来自狩猎小屋的箱子。

<!-- config reliquary.drops.* -->

## 死亡时

佩戴的饰品会和你的其他物品一起进入你的[灰烬遗骸](death-and-remnant.md)，你取回遗骸时会回到各自的栏位。如果服务器的遗骸模式让物品留在地上，它们会随你的物品栏一起掉落。开启 `keepInventory` 则你会保留它们。

## 设置

{{config reliquary.slots}}
{{config reliquary.artifacts.points}}
{{config reliquary.drops.chestArtifact}}

下一篇：[护甲套装](armor-sets.md)
