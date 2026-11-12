---
title: 配置
kind: guide
order: 7
source: 11f390862e36
---

## 设置保存在哪里

| 文件 | 内容 | 由谁读取 |
|---|---|---|
| `config/embervoid.json` | 玩法规则：下面的每一个键 | 服务器（单人游戏中是你自己的游戏） |
| `config/embervoid-client.json` | 你的显示偏好 | 仅你的客户端 |

两个文件都会在游戏首次启动时以默认值创建。Embervoid 没有游戏内设置界面：请在游戏或服务器**关闭时**用文本编辑器编辑 JSON，然后再启动。文件在启动时读取。

- 缺失的键会以默认值补上。
- 无效的值（超出范围、类型错误）会被记入日志并重置为默认值。
- 不是有效 JSON 的文件会被移到 `embervoid.json.broken`，并换成默认配置。
- 在服务器上，服务器的 `embervoid.json` 会发送给每位加入的玩家，他们的客户端在本次游戏中按它执行。玩家无法用自己的文件改变规则。

新版本修改默认值时，你文件里已有的值保持不变。删除某个键即可采用新的默认值。

属性的一“整点”遵循软上限：最高 20 的值全额计算，21–40 按一半计算，41–99 按五分之一计算。

结构间距（荒废圣所出现的频率）不在这个文件里；请用数据包修改（参见[数据包与兼容性](datapacks.md)）。

## 值得修改的键

- **死亡的惩罚有多重：** `remnant.mode` 和 `remnant.secondDeath`。把 `remnant.secondDeath` 设为 `burn_all` 就是纯粹的魂系体验，把 `remnant.mode` 设为 `off` 则使用原版死亡。
- **怪物打人有多疼：** `mobs.difficulty.easy`、`normal` 和 `hard`。把它们设为 0.5、1.0 和 1.5，大致相当于原版的难度缩放。
- **怪物成长得多快：** `mobs.levels.healthPerLevel` 和 `damagePerLevel`，或者用 `mobs.levels.enabled` 关闭等级。
- **其他模组的怪物：** 把它们的实体 id 加到 `mobs.levels.baseLevels`，例如 `"othermod:ogre": 10`。
- **月夜：** 用 `moon.enabled` 关闭，用 `moon.cycleInterval` 让它们更稀少或更频繁。
- **你的治疗瓶：** `flask.baseCharges`、`flask.maxCharges` 和 `flask.healAmount`。
- **升级节奏：** 三个 `leveling.cost*` 键，以及 `leveling.levelCap`。
- **组队打首领：** `boss.healthPerExtraFighter` 和 `boss.addsPerExtraFighter`。
- **性能：** 把 `setpieces.animate` 设为 false，可以让所有会动的机械装置停下来。

## 客户端设置

在 `config/embervoid-client.json` 中：

`staminaBar.hideWhenFull` — 精力满时隐藏精力条（默认 true）。

`hud.showMobLevel` — 在你注视的怪物名字旁显示“Lv. N”（默认 true）。

在“选项 → 音乐与声音”中，**首领音乐**设置首领主题曲的音量（默认 100 %），不受“音乐”影响。在“选项 → 辅助功能设置”中，**镜头震动**设置重击使镜头震动的程度，**命中闪光**开启或关闭重击时的明亮闪光。这些设置与原版选项一起保存。

## 全部的键

按 `embervoid.json` 的各个部分分组。写作 `a.b.c` 的键，表示 `a` 里的对象 `b` 里的键 `c`。


Grouped by section of `embervoid.json`. A key written `a.b.c` is the key `c` inside the object `b` inside `a`.

### remnant：死亡与灰烬遗骸

`remnant.mode` — 死亡时哪些东西进入灰烬遗骸：`xp_and_items`、`xp`（物品像原版一样掉落）或 `off`（原版死亡）。
{{config remnant.mode}}

`remnant.secondDeath` — 新的死亡对你之前的遗骸做什么：`burn_xp`（它的经验烧掉，物品保留）或 `burn_all`（连同一切一起烧掉）。
{{config remnant.secondDeath}}

### stamina：精力

`stamina.max` — 基础耐力下的精力上限。
{{config stamina.max}}

`stamina.regenPerTick` — 精力开始恢复后每刻恢复的量，基础耐力下。
{{config stamina.regenPerTick}}

`stamina.regenDelayTicks` — 消耗精力后，开始恢复之前等待的刻数。
{{config stamina.regenDelayTicks}}

### flask：治疗瓶

`flask.baseCharges` — 全新治疗瓶的充能次数。
{{config flask.baseCharges}}

`flask.maxCharges` — 使用治疗瓶碎片后的充能上限（不会低于 `baseCharges`）。
{{config flask.maxCharges}}

`flask.healAmount` — 每口的治疗量，以半颗心为单位。
{{config flask.healAmount}}

`flask.drinkTicks` — 喝一口所需的刻数。
{{config flask.drinkTicks}}

### stats：属性

`stats.vitalityHealth` — 生命力超过 10 后，每一整点的最大生命值（半颗心）。
{{config stats.vitalityHealth}}

`stats.enduranceStamina` — 每一整点耐力的最大精力。
{{config stats.enduranceStamina}}

`stats.enduranceStaminaRegen` — 每一整点耐力的精力恢复量，以 `stamina.regenPerTick` 的比例表示。
{{config stats.enduranceStaminaRegen}}

`stats.enduranceKnockbackResistance` — 每一整点耐力的击退抗性。
{{config stats.enduranceKnockbackResistance}}

`stats.strengthDamage` — 每一整点力气，力气型武器的伤害，以比例表示。
{{config stats.strengthDamage}}

`stats.dexterityDamage` — 每一整点灵巧，灵巧型武器的伤害，以比例表示。
{{config stats.dexterityDamage}}

`stats.dexterityAttackSpeed` — 每一整点灵巧的攻击速度，以比例表示。
{{config stats.dexterityAttackSpeed}}

`stats.kindlingFlaskHeal` — 每一整点炽焰的治疗瓶治疗量，以比例表示。
{{config stats.kindlingFlaskHeal}}

`stats.resilienceToughness` — 每一整点韧性的盔甲韧性。
{{config stats.resilienceToughness}}

`stats.resilienceFireReduction` — 每一整点韧性减免的火焰伤害。
{{config stats.resilienceFireReduction}}

`stats.resilienceFireReductionCap` — 韧性最多能减免的火焰伤害。
{{config stats.resilienceFireReductionCap}}

`stats.fortuneLuck` — 每一整点幸运的幸运值。
{{config stats.fortuneLuck}}

`stats.fortuneExtraDrop` — 每一整点幸运，你击杀的怪物二次掉落战利品的几率。
{{config stats.fortuneExtraDrop}}

`stats.fortuneExtraDropCap` — 额外掉落几率的上限。
{{config stats.fortuneExtraDropCap}}

### leveling：升级

`leveling.costBase` — 升级花费的常数部分：从 L 级到 L + 1 级需要 `costBase + costLinear·L + costQuadratic·L²` 点经验值。
{{config leveling.costBase}}

`leveling.costLinear` — 升级花费中随 L 增长的部分。
{{config leveling.costLinear}}

`leveling.costQuadratic` — 升级花费中随 L² 增长的部分。
{{config leveling.costQuadratic}}

`leveling.statCap` — 属性最高能升到的值。
{{config leveling.statCap}}

`leveling.levelCap` — 角色等级上限；0 = 无上限。
{{config leveling.levelCap}}

### mobs：怪物等级与难度

`mobs.levels.enabled` — 怪物等级的开关。关闭后：不再产生新的等级，已有的等级也不起作用。
{{config mobs.levels.enabled}}

`mobs.levels.defaultBaseLevel` — 未列入 `baseLevels` 的任何敌对生物（原版或模组）的基础等级。
{{config mobs.levels.defaultBaseLevel}}

`mobs.levels.baseLevels` — 按实体 id 设定的基础等级，例如 `"minecraft:zombie": 3`。其他模组的怪物请加在这里。
{{config mobs.levels.baseLevels}}

`mobs.levels.blocksPerLevel` — 主世界：距世界出生点每这么多格，等级 +1。
{{config mobs.levels.blocksPerLevel}}

`mobs.levels.maxDistanceBonus` — 主世界：距离最多能增加的等级。
{{config mobs.levels.maxDistanceBonus}}

`mobs.levels.belowZeroBonus` — 主世界：y = 0 以下额外增加的等级。
{{config mobs.levels.belowZeroBonus}}

`mobs.levels.dimensionBonus` — 按维度 id 设定的额外等级（默认为下界和末地）。其他模组的维度请加在这里。
{{config mobs.levels.dimensionBonus}}

`mobs.levels.healthPerLevel` — 每高出 1 级增加的生命值，以基础值的比例表示。
{{config mobs.levels.healthPerLevel}}

`mobs.levels.damagePerLevel` — 每高出 1 级增加的伤害，以基础值的比例表示。
{{config mobs.levels.damagePerLevel}}

`mobs.levels.experiencePerLevel` — 每高出 1 级增加的掉落经验，以基础值的比例表示。
{{config mobs.levels.experiencePerLevel}}

`mobs.levels.healthScale` — 按实体 id，在等级之下对某种怪物基础生命值乘上的系数（未列出的为 1）。
{{config mobs.levels.healthScale}}

`mobs.levels.damageScale` — 按实体 id，在等级之下对某种怪物基础伤害乘上的系数（未列出的为 1）。
{{config mobs.levels.damageScale}}

`mobs.difficulty.easy` — 简单难度下敌对生物对玩家的伤害系数（替换原版的）。
{{config mobs.difficulty.easy}}

`mobs.difficulty.normal` — 普通难度下的同一系数。
{{config mobs.difficulty.normal}}

`mobs.difficulty.hard` — 困难难度下的同一系数。
{{config mobs.difficulty.hard}}

### sanctum：荒废圣所

`sanctum.mapChanceCartographer` — 村庄制图师的箱子里出现圣所地图的几率。
{{config sanctum.mapChanceCartographer}}

`sanctum.mapChanceTemple` — 村庄神殿的箱子里的同一几率。
{{config sanctum.mapChanceTemple}}

`sanctum.mapChanceRuinedPortal` — 废弃传送门的箱子里的同一几率。
{{config sanctum.mapChanceRuinedPortal}}

`sanctum.mapChanceShipwreck` — 沉船地图箱里的同一几率。
{{config sanctum.mapChanceShipwreck}}

`sanctum.mapSearchRadius` — 新的圣所地图寻找圣所的范围，以区块为单位。
{{config sanctum.mapSearchRadius}}

`sanctum.hallGuardChance` — 侧厅的守卫位上放置一只活尸的几率，在世界生成时决定。
{{config sanctum.hallGuardChance}}

`sanctum.hallArcherChance` — 侧厅的弓箭手位上放置一只骷髅的几率，在世界生成时决定。
{{config sanctum.hallArcherChance}}

### sanctumGear：圣所武器

`sanctumGear.cleaveRange` — 横扫劈砍（圣所长剑）：攻击距离，以格为单位。
{{config sanctumGear.cleaveRange}}

`sanctumGear.cleaveArc` — 横扫劈砍：你前方的扇形角度，以度为单位。
{{config sanctumGear.cleaveArc}}

`sanctumGear.cleaveMultiplier` — 横扫劈砍：每个目标承受的武器单次伤害占比。
{{config sanctumGear.cleaveMultiplier}}

`sanctumGear.cleaveStamina` — 横扫劈砍：精力消耗。
{{config sanctumGear.cleaveStamina}}

`sanctumGear.cleaveCooldown` — 横扫劈砍：冷却，以刻为单位。
{{config sanctumGear.cleaveCooldown}}

`sanctumGear.slamRadius` — 震地猛击（负灰者重锤）：环形半径。
{{config sanctumGear.slamRadius}}

`sanctumGear.slamMultiplier` — 震地猛击：武器单次伤害的占比。
{{config sanctumGear.slamMultiplier}}

`sanctumGear.slamKnockback` — 震地猛击：击退强度。
{{config sanctumGear.slamKnockback}}

`sanctumGear.slamSlownessTicks` — 震地猛击：缓慢的持续时间，以刻为单位。
{{config sanctumGear.slamSlownessTicks}}

`sanctumGear.slamSlownessAmplifier` — 震地猛击：缓慢的等级（1 = 缓慢 II）。
{{config sanctumGear.slamSlownessAmplifier}}

`sanctumGear.slamStamina` — 震地猛击：精力消耗。
{{config sanctumGear.slamStamina}}

`sanctumGear.slamCooldown` — 震地猛击：冷却，以刻为单位。
{{config sanctumGear.slamCooldown}}

### boss：首领

`boss.healthPerExtraFighter` — 超出第一名的每位参战者带来的首领生命加成：生命值 × (1 + 此值 × (参战者数 − 1))。
{{config boss.healthPerExtraFighter}}

`boss.addsPerExtraFighter` — 超出第一名的每位参战者带来的额外仆从数量。
{{config boss.addsPerExtraFighter}}

`boss.addLevelOffset` — 仆从比首领低多少级。
{{config boss.addLevelOffset}}

`boss.targetSwitchMinSeconds` — 首领两次切换目标之间的最短时间（有两名或更多参战者时）。
{{config boss.targetSwitchMinSeconds}}

`boss.targetSwitchMaxSeconds` — 两次切换目标之间的最长时间。
{{config boss.targetSwitchMaxSeconds}}

`boss.repeatSoulChance` — 已经击败过该首领的参战者获得首领之魂的几率（再战中永远不会）。
{{config boss.repeatSoulChance}}

### reachMobs：灰烬边域的生成

`reachMobs.spawns` — 灰烬边域怪物在所属区域的自然生成：每种怪物的 `weight`、`minGroup` 和 `maxGroup`。权重为 0 则关闭该怪物的生成。启动时读取。
{{config reachMobs.spawns}}

### gear：领域装备

`gear.bleeding.durationTicks` — 流血持续多久；每次新的命中都会让它重新计时。
{{config gear.bleeding.durationTicks}}

`gear.bleeding.maxLevel` — 流血的最高等级。
{{config gear.bleeding.maxLevel}}

`gear.bleeding.damagePerLevel` — 每级流血每秒造成的伤害，无视盔甲。
{{config gear.bleeding.damagePerLevel}}

`gear.smoldering.fireReductionPerPiece` — 每穿一件阴燃甲胄所减免的火焰伤害比例。
{{config gear.smoldering.fireReductionPerPiece}}

`gear.sets.reaperSpeed` — 收割者套装：移动速度加成（基础值的比例）。
{{config gear.sets.reaperSpeed}}

`gear.sets.huntsmanProjectile` — 猎手套装：弹射物伤害加成。
{{config gear.sets.huntsmanProjectile}}

`gear.sets.wardenKnockbackResist` — 狱卒套装：增加的击退抗性。
{{config gear.sets.wardenKnockbackResist}}

`gear.sets.wardenBlockingReduction` — 狱卒套装：用盾牌格挡时减免的伤害比例。
{{config gear.sets.wardenBlockingReduction}}

`gear.uniques.scarecrowDetection` — 稻草人面具：怪物发现你的距离占通常距离的比例。
{{config gear.uniques.scarecrowDetection}}

`gear.uniques.greavesSpeedTicks` — 猎手胫甲：击杀后加速的刻数。
{{config gear.uniques.greavesSpeedTicks}}

`gear.uniques.greavesSafeFallDamage` — 猎手胫甲：减免的摔落伤害（4 = 7 格以内不受伤害）。
{{config gear.uniques.greavesSafeFallDamage}}

`gear.uniques.novaHealthFraction` — 熔炉之心胸甲：生命低于最大生命值的这个比例时触发新星。
{{config gear.uniques.novaHealthFraction}}

`gear.uniques.novaRadius` — 熔炉之心胸甲：新星半径。
{{config gear.uniques.novaRadius}}

`gear.uniques.novaDamage` — 熔炉之心胸甲：新星对范围内每只怪物造成的余烬伤害。
{{config gear.uniques.novaDamage}}

`gear.uniques.novaCooldownTicks` — 熔炉之心胸甲：两次新星之间的刻数。
{{config gear.uniques.novaCooldownTicks}}

`gear.weapons.kindlingPerPoint` — 每一整点炽焰的余烬伤害加成。
{{config gear.weapons.kindlingPerPoint}}

`gear.weapons.sweepRadius` — 灰烬镰刀：围绕目标的横扫半径。
{{config gear.weapons.sweepRadius}}

`gear.weapons.sweepShare` — 灰烬镰刀：横扫造成的伤害占单次命中的比例。
{{config gear.weapons.sweepShare}}

`gear.weapons.sweepStamina` — 灰烬镰刀：横扫或收割每命中一个敌人返还的精力。
{{config gear.weapons.sweepStamina}}

`gear.weapons.waveLength` — 灰烬镰刀：满蓄力命中所释放灰烬波的长度。
{{config gear.weapons.waveLength}}

`gear.weapons.waveWidth` — 灰烬镰刀：灰烬波的宽度。
{{config gear.weapons.waveWidth}}

`gear.weapons.waveDamage` — 灰烬镰刀：灰烬波的余烬伤害。
{{config gear.weapons.waveDamage}}

`gear.weapons.harvestRadius` — 收割：旋转半径。
{{config gear.weapons.harvestRadius}}

`gear.weapons.harvestStamina` — 收割：精力消耗。
{{config gear.weapons.harvestStamina}}

`gear.weapons.harvestCooldown` — 收割：冷却，以刻为单位。
{{config gear.weapons.harvestCooldown}}

`gear.weapons.lashPull` — 鞭笞：目标被拉近的格数。
{{config gear.weapons.lashPull}}

`gear.weapons.lashStamina` — 鞭笞：精力消耗。
{{config gear.weapons.lashStamina}}

`gear.weapons.lashCooldown` — 鞭笞：冷却，以刻为单位。
{{config gear.weapons.lashCooldown}}

`gear.weapons.lungeDash` — 突刺：冲刺距离，以格为单位。
{{config gear.weapons.lungeDash}}

`gear.weapons.lungeStamina` — 突刺：精力消耗。
{{config gear.weapons.lungeStamina}}

`gear.weapons.lungeCooldown` — 突刺：冷却，以刻为单位。
{{config gear.weapons.lungeCooldown}}

`gear.weapons.rampPerHit` — 王家灰烬刺剑：对同一目标连续每次满蓄力命中增加的伤害。
{{config gear.weapons.rampPerHit}}

`gear.weapons.rampMax` — 王家灰烬刺剑：递增伤害的最大增加量。
{{config gear.weapons.rampMax}}

`gear.weapons.rampWindowTicks` — 王家灰烬刺剑：两次命中之间能维持递增的最长间隔。
{{config gear.weapons.rampWindowTicks}}

`gear.weapons.whipTipBand` — 狱卒之鞭：鞭梢（鞭笞的最后几格）会发出脆响的长度。
{{config gear.weapons.whipTipBand}}

`gear.weapons.whipTipMultiplier` — 狱卒之鞭：用鞭梢命中时的伤害系数。
{{config gear.weapons.whipTipMultiplier}}

`gear.weapons.whipTipStunTicks` — 狱卒之鞭：鞭梢命中会让怪物停住并打断其起手动作的刻数。
{{config gear.weapons.whipTipStunTicks}}

`gear.crossbow.chargeTicks` — 猎手弩：没有快速装填时的装填时间。
{{config gear.crossbow.chargeTicks}}

`gear.crossbow.boltDamage` — 猎手弩：弩箭全速时的伤害。
{{config gear.crossbow.boltDamage}}

`gear.crossbow.punch` — 猎手弩：弩箭的击退等级。
{{config gear.crossbow.punch}}

`gear.crossbow.pierce` — 猎手弩：一支弩箭能额外穿过的怪物数量。
{{config gear.crossbow.pierce}}

`gear.crossbow.openerMultiplier` — 猎手弩：射向没有以你为目标的怪物时的伤害系数。
{{config gear.crossbow.openerMultiplier}}

`gear.consumables.bombRadius` — 灰烬炸弹：火环的半径。
{{config gear.consumables.bombRadius}}

`gear.consumables.bombDamage` — 灰烬炸弹：余烬伤害。
{{config gear.consumables.bombDamage}}

`gear.consumables.bombFireSeconds` — 灰烬炸弹：被它波及的怪物燃烧的秒数。
{{config gear.consumables.bombFireSeconds}}

`gear.consumables.resinSeconds` — 阴燃树脂：涂过的武器能点燃所击中目标的时长。
{{config gear.consumables.resinSeconds}}

`gear.consumables.resinFireSeconds` — 阴燃树脂：目标燃烧的秒数。
{{config gear.consumables.resinFireSeconds}}

`gear.consumables.balmRegenTicks` — 琥珀药膏：其生命恢复的持续时间。
{{config gear.consumables.balmRegenTicks}}

`gear.consumables.oilMinutes` — 琥珀灯油：灯笼燃烧的时长。
{{config gear.consumables.oilMinutes}}

`gear.consumables.whetstoneShare` — 磨刀石：修复的最大耐久比例。
{{config gear.consumables.whetstoneShare}}

`gear.consumables.cinderFuelTicks` — 焦渣碎片作为熔炉燃料的燃烧刻数（煤炭为 1600）。启动时读取。
{{config gear.consumables.cinderFuelTicks}}

`gear.consumables.flaskEmberBonus` — 瓶中余烬：额外的治疗瓶治疗量。
{{config gear.consumables.flaskEmberBonus}}

`gear.loot.fodderMaterial` — 自然生成的灰烬边域怪物掉落该区域材料的几率。
{{config gear.loot.fodderMaterial}}

`gear.loot.eliteMaterial` — 放置的重装敌人或精英怪掉落 1–2 个该区域材料的几率。
{{config gear.loot.eliteMaterial}}

`gear.loot.fodderSet` — 自然生成的怪物掉落套装部件的几率（仅限玩家击杀）。
{{config gear.loot.fodderSet}}

`gear.loot.eliteSet` — 放置的重装敌人或精英怪掉落套装部件的几率。
{{config gear.loot.eliteSet}}

`gear.loot.fodderUnique` — 该区域的怪物掉落该区域独特部件的几率。
{{config gear.loot.fodderUnique}}

`gear.loot.eliteUnique` — 该区域的精英怪掉落该区域独特部件的几率。
{{config gear.loot.eliteUnique}}

`gear.loot.lootingBonus` — 每级抢夺增加的几率（独特部件为其四分之一）。
{{config gear.loot.lootingBonus}}

### reliquary：圣物匣

`reliquary.slots` — 每种的圣物匣栏位数：`artifact` 和 `ring`（0 隐藏该类，最多 8）。
{{config reliquary.slots}}

`reliquary.artifacts.points` — 单属性神器的属性点数。
{{config reliquary.artifacts.points}}

`reliquary.artifacts.sealPoints` — 灰烬之王印玺为每项属性增加的点数。
{{config reliquary.artifacts.sealPoints}}

`reliquary.rings.courierSprintBonus` — 信使戒指：额外的疾跑速度。
{{config reliquary.rings.courierSprintBonus}}

`reliquary.rings.featherSafeFall` — 灰羽戒指：不超过这么多格的坠落不造成伤害。
{{config reliquary.rings.featherSafeFall}}

`reliquary.rings.featherMaxFallSpeed` — 灰羽戒指：最快下落速度，每刻多少格。
{{config reliquary.rings.featherMaxFallSpeed}}

`reliquary.rings.fogStalkerClearing` — 雾中潜猎者戒指：领域迷雾的起止距离被推远的倍数。
{{config reliquary.rings.fogStalkerClearing}}

`reliquary.rings.delverHasteLevel` — 掘地者戒指：开采领域矿石时的急迫等级。
{{config reliquary.rings.delverHasteLevel}}

`reliquary.rings.embersFireSeconds` — 余烬戒指：近战命中使目标着火的秒数。
{{config reliquary.rings.embersFireSeconds}}

`reliquary.drops.chestArtifact` — 区域宝箱里出现神器的几率。
{{config reliquary.drops.chestArtifact}}

`reliquary.drops.chestRing` — 区域宝箱里出现戒指的几率。
{{config reliquary.drops.chestRing}}

`reliquary.drops.eliteArtifact` — 区域精英怪掉落神器的几率（玩家击杀）。
{{config reliquary.drops.eliteArtifact}}

`reliquary.drops.eliteRing` — 区域精英怪掉落戒指的几率（玩家击杀）。
{{config reliquary.drops.eliteRing}}

### moon：月夜

`moon.enabled` — 月夜的开关（调试命令仍然能开始一次）。
{{config moon.enabled}}

`moon.cycleInterval` — 每隔这么多个 8 天的月相周期出现一次月夜周期；世界的第一次满月不算。
{{config moon.cycleInterval}}

`moon.fallbackMinutes` — 即使怪物波次未被击败，月夜也会在这么多现实分钟后结束。
{{config moon.fallbackMinutes}}

`moon.fullMoon.levelBonus` — 满月时生成的主世界敌对生物额外增加的等级。
{{config moon.fullMoon.levelBonus}}

`moon.fullMoon.followRange` — 满月时主世界敌对生物的追踪距离倍数。
{{config moon.fullMoon.followRange}}

`moon.blood.vampirism` — 血色新月时，敌对生物对玩家造成的伤害中转化为自身治疗的比例。
{{config moon.blood.vampirism}}

### nightMobs：满月波次

`nightMobs.zombieLevelBonus` — 满月波次的灰烬僵尸在该地点等级之上额外获得的等级。
{{config nightMobs.zombieLevelBonus}}

`nightMobs.ghostMissChance` — 攻击幽灵时会穿过它的攻击比例。
{{config nightMobs.ghostMissChance}}

`nightMobs.maxZombies` — 单个玩家的一波中灰烬僵尸的最大数量（每层夜之诅咒一只）。
{{config nightMobs.maxZombies}}

`nightMobs.ghostCount` — 派给没有诅咒的玩家的幽灵数量。
{{config nightMobs.ghostCount}}

`nightMobs.spawnMinDistance` — 一波怪物生成处距玩家的最近格数。
{{config nightMobs.spawnMinDistance}}

`nightMobs.spawnMaxDistance` — 一波怪物生成处距玩家的最远格数。
{{config nightMobs.spawnMaxDistance}}

### vampire：吸血鬼

`vampire.provokeMinutes` — 如果没人攻击吸血鬼，离某位玩家最近的吸血鬼翻脸前的现实分钟数。
{{config vampire.provokeMinutes}}

`vampire.groupRadius` — 彼此相距这么近的玩家共享同一群吸血鬼。
{{config vampire.groupRadius}}

`vampire.groupMin` — 一群吸血鬼的最少数量。
{{config vampire.groupMin}}

`vampire.groupMax` — 一群吸血鬼的最多数量。
{{config vampire.groupMax}}

`vampire.bossMinLevel` — 吸血鬼首领的最低等级。
{{config vampire.bossMinLevel}}

`vampire.bossCapNoRealm` — 参战者中还没有人解锁领域时，首领的最高等级。
{{config vampire.bossCapNoRealm}}

`vampire.firstKillShards` — 玩家首次击杀吸血鬼获得的治疗瓶碎片数（另加一个吸血鬼之牙）。
{{config vampire.firstKillShards}}

`vampire.repeatShardChance` — 之后的击杀获得一个治疗瓶碎片的几率。
{{config vampire.repeatShardChance}}

### setpieces：机械装置

`setpieces.maxPerChunk` — 每个区块最多容纳的会动的机械装置（风车叶片、钟、香炉……）数量；超出的会变成空气。
{{config setpieces.maxPerChunk}}

`setpieces.animate` — 机械装置是否会动。关闭后：它们保持静止姿态。同步到客户端。
{{config setpieces.animate}}

下一篇：[数据包与兼容性](datapacks.md)
