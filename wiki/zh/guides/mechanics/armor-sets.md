---
title: 护甲套装
kind: mechanic
order: 9
source: 09a1ad705725
---

灰烬边域有四套护甲套装和四件独特部件。只有当你穿齐某套的**全部四件**时，才会获得套装加成；每件的提示文字里都写着“全套：…”。独特部件有自己的效果，不计入任何套装。

## 套装

| 套装 | 出处 | 护甲 | 全套加成 |
|---|---|---|---|
| [阴燃甲胄](../../encyclopedia/items/armor/smoldering-set.md) | 合成（锻造） | 同下界合金 | 燃烧不会伤害你（岩浆仍会） |
| [收割者套装](../../encyclopedia/items/armor/reapers-set.md) | 灰烬田野的怪物 | 轻便，约等于铁 | +10 % 移动速度 |
| [猎手套装](../../encyclopedia/items/armor/huntsmans-set.md) | 王家林地的怪物 | 稍高一些，带一点韧性 | +15 % 弹射物伤害 |
| [狱卒套装](../../encyclopedia/items/armor/wardens-set.md) | 沉陷墓城的怪物 | 沉重，同钻石；每件会略微减速 | +0.3 击退抗性，用盾牌格挡时 −15 % 伤害 |

<!-- config gear.sets.*, gear.smoldering.fireReductionPerPiece; armor values GearArmor -->

**阴燃甲胄**也是逐件生效的：你每穿一件，就减少 8 % 的火焰伤害。在锻造台上用{{item embervoid:smoldering_template}}、一件钻石盔甲和{{item embervoid:smoldering_ingot}}制作。

三套区域套装由该区域的怪物掉落，每次随机一件，而且只有在玩家造成击杀时才会掉。较强的放置敌人掉落的几率比自然生成的更高，每个区域的宝箱里也可能有一件。

## 独特部件

| 部件 | 部位 | 效果 | 出处 |
|---|---|---|---|
| {{item embervoid:scarecrow_mask}} | 头部 | 怪物只在一半的距离外才会发现你；灰烬潜猎者不会对你伏击 | 灰烬田野 |
| {{item embervoid:huntsman_greaves}} | 腿部 | 击杀后 4 秒内 +20 % 速度；7 格以内摔落不受伤害 | 王家林地 |
| {{item embervoid:drowned_sabatons}} | 脚部 | 在水中保持全速 | 沉陷墓城 |
| {{item embervoid:furnace_heart_cuirass}} | 胸部 | 生命低于 30 % 时，一次火焰新星会灼烧并击退你周围的敌人（每 60 秒一次） | 阴燃要塞 |

<!-- config gear.uniques.* -->

它们是所属区域怪物的稀有掉落，该区域的精英怪（灰烬食腐鸦、树皮活尸、墓园敲钟人）掉得更多。在要塞中，只有熔炉傀儡和阴燃骑士会掉落胸甲。

## 混搭部件

- 套装加成需要同一套的全部四件。三件加一件独特部件，没有套装加成。
- 在某个部位换上独特部件是一种取舍：它的效果，对比另外三件带来的套装加成。
- 阴燃甲胄的各件是逐件减免火焰伤害的，所以一件阴燃胸甲配其他三件，依然能抵御火焰。
- 想怎么附魔都行；它们接受常规的盔甲附魔。

## 设置

{{config gear.sets.reaperSpeed}}
{{config gear.sets.huntsmanProjectile}}
{{config gear.sets.wardenKnockbackResist}}
{{config gear.smoldering.fireReductionPerPiece}}

下一篇：[状态效果](status-effects.md)
