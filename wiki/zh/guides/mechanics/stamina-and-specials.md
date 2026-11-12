---
title: 精力与特殊攻击
kind: mechanic
order: 3
source: d02e492987a2
---

Embervoid 的精力只有一个用途：支付武器特殊攻击的消耗。普通挥击、疾跑、跳跃和用盾牌格挡依然不消耗精力，和原版一样。

## 精力条

你开局有 100 点精力 <!-- config stamina.max -->。精力条位于快捷栏上方，满的时候会自动隐藏。消耗精力后，它会等一秒 <!-- config stamina.regenDelayTicks: 20 ticks -->，然后快速回满，起始速度为每秒 20 点 <!-- config stamina.regenPerTick: 1 per tick -->。

**耐力**会同时提升两者：每一整点提供 +2 最大精力和 +2 % 恢复速度。

## 特殊攻击的规则

每把带特殊攻击的武器，提示文字里都会写明：“右键：名称”，然后是精力消耗和冷却。

- 用武器**右键**来使用。
- 它消耗**固定数量的精力**，但只要你还剩**任何一点精力**就能使用：精力条降到零就停在那里。精力条为空时，你会看到“精疲力竭”，精力条闪烁，什么也不会发生。
- 使用之后，武器会有短暂的**冷却**，像末影珍珠那样显示在物品上。
- 特殊攻击走正常的伤害流程：目标的护甲、它的等级以及你的力气或灵巧加成都会生效。

所以真正的限制是消耗：连用两三次特殊攻击就会把精力条清空，之后你只能用普通挥击，等它回复。

## 各种特殊攻击

| 武器 | 特殊攻击 | 效果 | 精力 | 冷却 |
|---|---|---|---|---|
| {{item embervoid:sanctum_longsword}} | 横扫劈砍 | 命中你前方 90° 扇形范围内的一切 | 35 | 0.4 秒 |
| {{item embervoid:ashbearers_mace}} | 震地猛击 | 你周围一圈：伤害、击退和缓慢 II | 45 | 0.5 秒 |
| {{item embervoid:ashen_scythe}} | 收割 | 完整旋转一圈，命中周围的一切；每命中一个敌人就返还精力 | 40 | 0.5 秒 |
| {{item embervoid:wardens_scourge}} | 鞭笞 | 把触及范围内最近的敌人拉向你 | 30 | 0.4 秒 |
| {{item embervoid:royal_ash_rapier}} | 突刺 | 向前冲刺，以一记造成两层流血并点燃目标的刺击收尾 | 30 | 0.3 秒 |

<!-- config sanctumGear.cleave*, sanctumGear.slam*, gear.weapons.harvest*, lash*, lunge* -->

{{item embervoid:huntsmans_crossbow}}没有特殊攻击；它的优势在于弩箭。这些武器的其他被动效果见它们的百科页面。

## 小技巧

- 先用特殊攻击开局，然后一边继续挥击，一边等精力条回复。
- 灰烬镰刀在人群中能回本：每次横扫或收割命中一个敌人，就返还 3 点精力 <!-- config gear.weapons.sweepStamina -->。
- 被包围时，震地猛击是你的退路；鞭笞能把弓箭手或施法者从高处拉下来。

## 设置

{{config stamina.max}}
{{config stamina.regenPerTick}}
{{config stamina.regenDelayTicks}}
{{config sanctumGear.cleaveStamina}}
{{config sanctumGear.slamStamina}}
{{config gear.weapons.harvestStamina}}
{{config gear.weapons.lashStamina}}
{{config gear.weapons.lungeStamina}}

在你的客户端上，可以通过 `embervoid-client.json` 中的 `staminaBar.hideWhenFull` 让精力条一直显示。

下一篇：[治疗瓶](healing-flask.md)
