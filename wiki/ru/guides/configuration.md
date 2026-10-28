---
title: Настройка
kind: guide
order: 7
source: 11f390862e36
---

## Где хранятся настройки

| Файл | Что в нём | Кто его читает |
|---|---|---|
| `config/embervoid.json` | правила игры: все ключи ниже | сервер (в одиночной игре — ваша собственная игра) |
| `config/embervoid-client.json` | ваши настройки отображения | только ваш клиент |

Оба файла создаются со значениями по умолчанию при первом запуске игры. Экрана настроек в игре у Embervoid нет:
редактируйте JSON в текстовом редакторе, **пока игра или сервер остановлены**, затем запустите их снова. Файл читается
при запуске.

- Недостающий ключ добавляется со значением по умолчанию.
- Недопустимое значение (вне диапазона, неверного типа) записывается в журнал и сбрасывается на значение по умолчанию.
- Файл с некорректным JSON переносится в `embervoid.json.broken` и заменяется значениями по умолчанию.
- На сервере серверный `embervoid.json` отправляется каждому подключившемуся игроку, и его клиент следует этому файлу
  весь сеанс. Игроки не могут изменить правила своим файлом.

Если в новой версии значение по умолчанию изменилось, значение в вашем файле остаётся прежним. Удалите ключ, чтобы
получить новое значение по умолчанию.

«Полное очко» характеристики учитывает мягкие пороги: значения до 20 идут полностью, 21–40 — наполовину, 41–99 — на
пятую часть.

Частота строений (как часто появляются Разорённые Святилища) задаётся не в этом файле; меняйте её набором данных (см.
[Наборы данных и совместимость](datapacks.md)).

## Ключи, которые стоит менять

- **Насколько сурова смерть:** `remnant.mode` и `remnant.secondDeath`. Установите `remnant.secondDeath` в `burn_all`
  для настоящего souls-like или `remnant.mode` в `off`, чтобы умирать как в ванилле.
- **Насколько сильно бьют мобы:** `mobs.difficulty.easy`, `normal` и `hard`. Значения 0,5, 1,0 и 1,5 дают примерно
  ванильное масштабирование сложности.
- **Как быстро растут мобы:** `mobs.levels.healthPerLevel` и `damagePerLevel`, или `mobs.levels.enabled`, чтобы
  отключить уровни.
- **Мобы других модов:** добавьте их идентификаторы в `mobs.levels.baseLevels`, например `"othermod:ogre": 10`.
- **Лунные ночи:** `moon.enabled`, чтобы их отключить, `moon.cycleInterval`, чтобы сделать их реже или чаще.
- **Фляга:** `flask.baseCharges`, `flask.maxCharges` и `flask.healAmount`.
- **Темп повышения уровня:** три ключа `leveling.cost*`, `leveling.levelCap`.
- **Боссы против группы:** `boss.healthPerExtraFighter` и `boss.addsPerExtraFighter`.
- **Производительность:** `setpieces.animate` со значением false останавливает все движущиеся механизмы.

## Настройки клиента

В `config/embervoid-client.json`:

`staminaBar.hideWhenFull` — скрывать шкалу выносливости, пока выносливость полна (по умолчанию true).

`hud.showMobLevel` — показывать «Ур. N» рядом с именем моба, на которого вы смотрите (по умолчанию true).

В разделе «Настройки → Музыка и звуки» ползунок **«Музыка боссов»** задаёт громкость тем боссов (по умолчанию
100 %) независимо от «Музыки». В разделе «Настройки → Специальные возможности» ползунок **«Тряска камеры»** задаёт,
насколько сильно тяжёлые удары трясут камеру, а **«Вспышки ударов»** включают или выключают яркие вспышки тяжёлых
ударов. Эти настройки сохраняются вместе с ванильными.

## Все ключи

Сгруппированы по разделам `embervoid.json`. Ключ, записанный как `a.b.c`, — это ключ `c` внутри объекта `b` внутри
`a`.

### remnant: Смерть и пепельный останок

`remnant.mode` — Что попадает в пепельный останок при смерти: `xp_and_items`, `xp` (вещи выпадают как в ванилле) или `off` (ванильная смерть).
{{config remnant.mode}}

`remnant.secondDeath` — Что новая смерть делает с прежним останком: `burn_xp` (опыт сгорает, вещи остаются) или `burn_all` (сгорает со всем содержимым).
{{config remnant.secondDeath}}

### stamina: Выносливость

`stamina.max` — Запас выносливости при базовой Выносливости.
{{config stamina.max}}

`stamina.regenPerTick` — Выносливость, восстанавливаемая за тик во время восстановления, при базовой Выносливости.
{{config stamina.regenPerTick}}

`stamina.regenDelayTicks` — Тики после траты выносливости, прежде чем она начнёт восстанавливаться.
{{config stamina.regenDelayTicks}}

### flask: Лечебная фляга

`flask.baseCharges` — Заряды новой лечебной фляги.
{{config flask.baseCharges}}

`flask.maxCharges` — Предел зарядов с осколками фляги (не ниже `baseCharges`).
{{config flask.maxCharges}}

`flask.healAmount` — Здоровье за глоток, в половинках сердца.
{{config flask.healAmount}}

`flask.drinkTicks` — Сколько тиков длится один глоток.
{{config flask.drinkTicks}}

### stats: Характеристики

`stats.vitalityHealth` — Максимальное здоровье (в половинках сердца) за каждое полное очко Живучести выше 10.
{{config stats.vitalityHealth}}

`stats.enduranceStamina` — Максимальная выносливость за каждое полное очко Выносливости.
{{config stats.enduranceStamina}}

`stats.enduranceStaminaRegen` — Восстановление выносливости за каждое полное очко Выносливости, как доля от `stamina.regenPerTick`.
{{config stats.enduranceStaminaRegen}}

`stats.enduranceKnockbackResistance` — Устойчивость к отбрасыванию за каждое полное очко Выносливости.
{{config stats.enduranceKnockbackResistance}}

`stats.strengthDamage` — Урон оружием силы за каждое полное очко Силы, как доля.
{{config stats.strengthDamage}}

`stats.dexterityDamage` — Урон оружием ловкости за каждое полное очко Ловкости, как доля.
{{config stats.dexterityDamage}}

`stats.dexterityAttackSpeed` — Скорость атаки за каждое полное очко Ловкости, как доля.
{{config stats.dexterityAttackSpeed}}

`stats.kindlingFlaskHeal` — Лечение лечебной флягой за каждое полное очко Жара, как доля.
{{config stats.kindlingFlaskHeal}}

`stats.resilienceToughness` — Прочность брони за каждое полное очко Стойкости.
{{config stats.resilienceToughness}}

`stats.resilienceFireReduction` — Снижение урона от огня за каждое полное очко Стойкости.
{{config stats.resilienceFireReduction}}

`stats.resilienceFireReductionCap` — Наибольшая доля урона от огня, которую может снять Стойкость.
{{config stats.resilienceFireReductionCap}}

`stats.fortuneLuck` — Удача (атрибут) за каждое полное очко Удачи.
{{config stats.fortuneLuck}}

`stats.fortuneExtraDrop` — Шанс за каждое полное очко Удачи, что убитый вами моб выронит добычу второй раз.
{{config stats.fortuneExtraDrop}}

`stats.fortuneExtraDropCap` — Наибольший шанс дополнительной добычи.
{{config stats.fortuneExtraDropCap}}

### leveling: Повышение уровня

`leveling.costBase` — Цена уровня, постоянная часть: переход с уровня L на L + 1 стоит `costBase + costLinear·L + costQuadratic·L²` очк. опыта.
{{config leveling.costBase}}

`leveling.costLinear` — Цена уровня, часть, растущая с L.
{{config leveling.costLinear}}

`leveling.costQuadratic` — Цена уровня, часть, растущая с L².
{{config leveling.costQuadratic}}

`leveling.statCap` — Наибольшее значение, до которого можно поднять характеристику.
{{config leveling.statCap}}

`leveling.levelCap` — Наибольший уровень персонажа; 0 — без предела.
{{config leveling.levelCap}}

### mobs: Уровни мобов и сложность

`mobs.levels.enabled` — Включить или выключить уровни мобов. Если выключено: новых уровней нет, а существующие ничего не делают.
{{config mobs.levels.enabled}}

`mobs.levels.defaultBaseLevel` — Базовый уровень любого враждебного моба, не перечисленного в `baseLevels`, ванильного или из мода.
{{config mobs.levels.defaultBaseLevel}}

`mobs.levels.baseLevels` — Базовый уровень по идентификатору сущности, например `"minecraft:zombie": 3`. Добавляйте сюда мобов других модов.
{{config mobs.levels.baseLevels}}

`mobs.levels.blocksPerLevel` — Верхний мир: +1 уровень за каждые столько блоков от точки появления мира.
{{config mobs.levels.blocksPerLevel}}

`mobs.levels.maxDistanceBonus` — Верхний мир: сколько уровней максимум может добавить расстояние.
{{config mobs.levels.maxDistanceBonus}}

`mobs.levels.belowZeroBonus` — Верхний мир: дополнительные уровни ниже y = 0.
{{config mobs.levels.belowZeroBonus}}

`mobs.levels.dimensionBonus` — Дополнительные уровни по идентификатору измерения (по умолчанию Незер и Энд). Добавляйте сюда измерения других модов.
{{config mobs.levels.dimensionBonus}}

`mobs.levels.healthPerLevel` — Здоровье за каждый уровень выше 1, как доля от базового.
{{config mobs.levels.healthPerLevel}}

`mobs.levels.damagePerLevel` — Урон за каждый уровень выше 1, как доля от базового.
{{config mobs.levels.damagePerLevel}}

`mobs.levels.experiencePerLevel` — Выпадающий опыт за каждый уровень выше 1, как доля от базового.
{{config mobs.levels.experiencePerLevel}}

`mobs.levels.healthScale` — Множитель базового здоровья типа мобов до учёта уровня, по идентификатору сущности (не указанные: 1).
{{config mobs.levels.healthScale}}

`mobs.levels.damageScale` — Множитель базового урона типа мобов до учёта уровня, по идентификатору сущности (не указанные: 1).
{{config mobs.levels.damageScale}}

`mobs.difficulty.easy` — Множитель урона враждебных мобов по игрокам на Лёгкой сложности (заменяет ванильный).
{{config mobs.difficulty.easy}}

`mobs.difficulty.normal` — То же на Нормальной.
{{config mobs.difficulty.normal}}

`mobs.difficulty.hard` — То же на Сложной.
{{config mobs.difficulty.hard}}

### sanctum: Разорённое Святилище

`sanctum.mapChanceCartographer` — Шанс карты Святилища в сундуке картографа в деревне.
{{config sanctum.mapChanceCartographer}}

`sanctum.mapChanceTemple` — То же в сундуке деревенского храма.
{{config sanctum.mapChanceTemple}}

`sanctum.mapChanceRuinedPortal` — То же в сундуке разрушенного портала.
{{config sanctum.mapChanceRuinedPortal}}

`sanctum.mapChanceShipwreck` — То же в сундуке с картами на затонувшем корабле.
{{config sanctum.mapChanceShipwreck}}

`sanctum.mapSearchRadius` — Как далеко, в чанках, новая карта Святилища ищет Святилище.
{{config sanctum.mapSearchRadius}}

`sanctum.hallGuardChance` — Шанс, что на месте стража в боковом зале стоит полый; определяется при генерации мира.
{{config sanctum.hallGuardChance}}

`sanctum.hallArcherChance` — Шанс, что на месте лучника в боковом зале стоит скелет; определяется при генерации мира.
{{config sanctum.hallArcherChance}}

### sanctumGear: Оружие Святилища

`sanctumGear.cleaveRange` — Широкий взмах (длинный меч Святилища): дальность в блоках.
{{config sanctumGear.cleaveRange}}

`sanctumGear.cleaveArc` — Широкий взмах: дуга перед вами, в градусах.
{{config sanctumGear.cleaveArc}}

`sanctumGear.cleaveMultiplier` — Широкий взмах: доля удара оружия, которую получает каждая цель.
{{config sanctumGear.cleaveMultiplier}}

`sanctumGear.cleaveStamina` — Широкий взмах: цена в выносливости.
{{config sanctumGear.cleaveStamina}}

`sanctumGear.cleaveCooldown` — Широкий взмах: перезарядка в тиках.
{{config sanctumGear.cleaveCooldown}}

`sanctumGear.slamRadius` — Удар оземь (булава пеплоносца): радиус кольца.
{{config sanctumGear.slamRadius}}

`sanctumGear.slamMultiplier` — Удар оземь: доля удара оружия.
{{config sanctumGear.slamMultiplier}}

`sanctumGear.slamKnockback` — Удар оземь: сила отбрасывания.
{{config sanctumGear.slamKnockback}}

`sanctumGear.slamSlownessTicks` — Удар оземь: длительность Замедления в тиках.
{{config sanctumGear.slamSlownessTicks}}

`sanctumGear.slamSlownessAmplifier` — Удар оземь: усиление Замедления (1 = Замедление II).
{{config sanctumGear.slamSlownessAmplifier}}

`sanctumGear.slamStamina` — Удар оземь: цена в выносливости.
{{config sanctumGear.slamStamina}}

`sanctumGear.slamCooldown` — Удар оземь: перезарядка в тиках.
{{config sanctumGear.slamCooldown}}

### boss: Боссы

`boss.healthPerExtraFighter` — Прибавка к здоровью босса за каждого участника сверх первого: здоровье × (1 + это значение × (участники − 1)).
{{config boss.healthPerExtraFighter}}

`boss.addsPerExtraFighter` — Дополнительные прислужники за каждого участника сверх первого.
{{config boss.addsPerExtraFighter}}

`boss.addLevelOffset` — На сколько уровней прислужники ниже босса.
{{config boss.addLevelOffset}}

`boss.targetSwitchMinSeconds` — Наименьшее время между двумя сменами цели у босса (при двух участниках и больше).
{{config boss.targetSwitchMinSeconds}}

`boss.targetSwitchMaxSeconds` — Наибольшее время между двумя сменами цели.
{{config boss.targetSwitchMaxSeconds}}

`boss.repeatSoulChance` — Шанс получить душу босса для участника, который уже побеждал его (в реванше — никогда).
{{config boss.repeatSoulChance}}

### reachMobs: Появление мобов Пепельного предела

`reachMobs.spawns` — Естественное появление мобов Пепельного предела в их зоне: `weight`, `minGroup` и `maxGroup` для каждого моба. Вес 0 отключает появление моба. Читается при запуске.
{{config reachMobs.spawns}}

### gear: Снаряжение обелиска

`gear.bleeding.durationTicks` — Сколько длится кровотечение; каждый новый удар запускает его заново.
{{config gear.bleeding.durationTicks}}

`gear.bleeding.maxLevel` — Наибольший уровень кровотечения.
{{config gear.bleeding.maxLevel}}

`gear.bleeding.damagePerLevel` — Урон от кровотечения в секунду за уровень, без учёта брони.
{{config gear.bleeding.damagePerLevel}}

`gear.smoldering.fireReductionPerPiece` — Доля урона от огня, которую снимает каждая надетая часть Тлеющих лат.
{{config gear.smoldering.fireReductionPerPiece}}

`gear.sets.reaperSpeed` — Комплект жнеца: прибавка к скорости передвижения (доля от базовой).
{{config gear.sets.reaperSpeed}}

`gear.sets.huntsmanProjectile` — Комплект ловчего: прибавка к урону снарядами.
{{config gear.sets.huntsmanProjectile}}

`gear.sets.wardenKnockbackResist` — Комплект стража некрополя: добавочная устойчивость к отбрасыванию.
{{config gear.sets.wardenKnockbackResist}}

`gear.sets.wardenBlockingReduction` — Комплект стража некрополя: доля урона, которую он снимает, пока вы закрываетесь щитом.
{{config gear.sets.wardenBlockingReduction}}

`gear.uniques.scarecrowDetection` — Маска пугала: доля обычного расстояния, на котором мобы вас замечают.
{{config gear.uniques.scarecrowDetection}}

`gear.uniques.greavesSpeedTicks` — Поножи королевского ловчего: тики ускорения после убийства.
{{config gear.uniques.greavesSpeedTicks}}

`gear.uniques.greavesSafeFallDamage` — Поножи королевского ловчего: снимаемый урон от падения (4 = никакого урона при падении до 7 блоков).
{{config gear.uniques.greavesSafeFallDamage}}

`gear.uniques.novaHealthFraction` — Кираса горнового сердца: вспышка срабатывает, когда здоровье падает ниже этой доли от максимального.
{{config gear.uniques.novaHealthFraction}}

`gear.uniques.novaRadius` — Кираса горнового сердца: радиус вспышки.
{{config gear.uniques.novaRadius}}

`gear.uniques.novaDamage` — Кираса горнового сердца: урон углями каждому мобу во вспышке.
{{config gear.uniques.novaDamage}}

`gear.uniques.novaCooldownTicks` — Кираса горнового сердца: тики между двумя вспышками.
{{config gear.uniques.novaCooldownTicks}}

`gear.weapons.kindlingPerPoint` — Прибавка к урону углями за каждое полное очко Жара.
{{config gear.weapons.kindlingPerPoint}}

`gear.weapons.sweepRadius` — Пепельная коса: радиус размаха вокруг цели.
{{config gear.weapons.sweepRadius}}

`gear.weapons.sweepShare` — Пепельная коса: доля удара, которую наносит размах.
{{config gear.weapons.sweepShare}}

`gear.weapons.sweepStamina` — Пепельная коса: выносливость, возвращаемая за каждого врага, задетого размахом или Жатвой.
{{config gear.weapons.sweepStamina}}

`gear.weapons.waveLength` — Пепельная коса: длина пепельной волны от полностью заряженного удара.
{{config gear.weapons.waveLength}}

`gear.weapons.waveWidth` — Пепельная коса: ширина пепельной волны.
{{config gear.weapons.waveWidth}}

`gear.weapons.waveDamage` — Пепельная коса: урон углями от пепельной волны.
{{config gear.weapons.waveDamage}}

`gear.weapons.harvestRadius` — Жатва: радиус вращения.
{{config gear.weapons.harvestRadius}}

`gear.weapons.harvestStamina` — Жатва: цена в выносливости.
{{config gear.weapons.harvestStamina}}

`gear.weapons.harvestCooldown` — Жатва: перезарядка в тиках.
{{config gear.weapons.harvestCooldown}}

`gear.weapons.lashPull` — Хлёст: на сколько блоков подтягивается цель.
{{config gear.weapons.lashPull}}

`gear.weapons.lashStamina` — Хлёст: цена в выносливости.
{{config gear.weapons.lashStamina}}

`gear.weapons.lashCooldown` — Хлёст: перезарядка в тиках.
{{config gear.weapons.lashCooldown}}

`gear.weapons.lungeDash` — Выпад: длина рывка в блоках.
{{config gear.weapons.lungeDash}}

`gear.weapons.lungeStamina` — Выпад: цена в выносливости.
{{config gear.weapons.lungeStamina}}

`gear.weapons.lungeCooldown` — Выпад: перезарядка в тиках.
{{config gear.weapons.lungeCooldown}}

`gear.weapons.rampPerHit` — Королевская пепельная рапира: урон, добавляемый за каждый полностью заряженный удар подряд по одной цели.
{{config gear.weapons.rampPerHit}}

`gear.weapons.rampMax` — Королевская пепельная рапира: наибольшая прибавка от серии.
{{config gear.weapons.rampMax}}

`gear.weapons.rampWindowTicks` — Королевская пепельная рапира: наибольший промежуток между двумя ударами, при котором серия сохраняется.
{{config gear.weapons.rampWindowTicks}}

`gear.weapons.whipTipBand` — Бич стража: длина кончика — последних блоков хлыста, — который щёлкает.
{{config gear.weapons.whipTipBand}}

`gear.weapons.whipTipMultiplier` — Бич стража: множитель урона удара кончиком.
{{config gear.weapons.whipTipMultiplier}}

`gear.weapons.whipTipStunTicks` — Бич стража: удар кончиком останавливает моба и сбивает его замах на столько тиков.
{{config gear.weapons.whipTipStunTicks}}

`gear.crossbow.chargeTicks` — Арбалет королевского ловчего: время натяжения без Быстрой перезарядки.
{{config gear.crossbow.chargeTicks}}

`gear.crossbow.boltDamage` — Арбалет королевского ловчего: урон болта на полной скорости.
{{config gear.crossbow.boltDamage}}

`gear.crossbow.punch` — Арбалет королевского ловчего: уровень отбрасывания болтами.
{{config gear.crossbow.punch}}

`gear.crossbow.pierce` — Арбалет королевского ловчего: сколько дополнительных мобов пробивает болт.
{{config gear.crossbow.pierce}}

`gear.crossbow.openerMultiplier` — Арбалет королевского ловчего: множитель урона по мобу, который не нацелен на вас.
{{config gear.crossbow.openerMultiplier}}

`gear.consumables.bombRadius` — Пепельная бомба: радиус огненного кольца.
{{config gear.consumables.bombRadius}}

`gear.consumables.bombDamage` — Пепельная бомба: урон углями.
{{config gear.consumables.bombDamage}}

`gear.consumables.bombFireSeconds` — Пепельная бомба: сколько секунд горят задетые мобы.
{{config gear.consumables.bombFireSeconds}}

`gear.consumables.resinSeconds` — Тлеющая смола: сколько покрытое оружие поджигает то, по чему бьёт.
{{config gear.consumables.resinSeconds}}

`gear.consumables.resinFireSeconds` — Тлеющая смола: сколько секунд горит цель.
{{config gear.consumables.resinFireSeconds}}

`gear.consumables.balmRegenTicks` — Янтарный бальзам: длительность его Регенерации.
{{config gear.consumables.balmRegenTicks}}

`gear.consumables.oilMinutes` — Янтарное масло для фонаря: сколько горит фонарь.
{{config gear.consumables.oilMinutes}}

`gear.consumables.whetstoneShare` — Точильный камень: доля восстанавливаемой максимальной прочности.
{{config gear.consumables.whetstoneShare}}

`gear.consumables.cinderFuelTicks` — Осколок золы как топливо для печи, в тиках (уголь — 1600). Читается при запуске.
{{config gear.consumables.cinderFuelTicks}}

`gear.consumables.flaskEmberBonus` — Уголёк фляги: дополнительное лечение флягой.
{{config gear.consumables.flaskEmberBonus}}

`gear.loot.fodderMaterial` — Шанс материала зоны с естественно появившегося моба Пепельного предела.
{{config gear.loot.fodderMaterial}}

`gear.loot.eliteMaterial` — Шанс 1–2 единиц материала зоны с размещённого танка или элитного моба.
{{config gear.loot.eliteMaterial}}

`gear.loot.fodderSet` — Шанс предмета комплекта с естественно появившегося моба (только при убийстве игроком).
{{config gear.loot.fodderSet}}

`gear.loot.eliteSet` — Шанс предмета комплекта с размещённого танка или элитного моба.
{{config gear.loot.eliteSet}}

`gear.loot.fodderUnique` — Шанс уникального предмета зоны с её мобов.
{{config gear.loot.fodderUnique}}

`gear.loot.eliteUnique` — Шанс уникального предмета зоны с её элитного моба.
{{config gear.loot.eliteUnique}}

`gear.loot.lootingBonus` — Шанс, добавляемый за каждый уровень Добычи (для уникальных предметов — четверть от него).
{{config gear.loot.lootingBonus}}

### reliquary: Реликварий

`reliquary.slots` — Ячейки реликвария каждого вида: `artifact` и `ring` (0 скрывает вид, не больше 8).
{{config reliquary.slots}}

`reliquary.artifacts.points` — Очки характеристики у артефакта с одной характеристикой.
{{config reliquary.artifacts.points}}

`reliquary.artifacts.sealPoints` — Очки, которые Печать Пепельного короля добавляет к каждой характеристике.
{{config reliquary.artifacts.sealPoints}}

`reliquary.rings.courierSprintBonus` — Кольцо гонца: дополнительная скорость бега.
{{config reliquary.rings.courierSprintBonus}}

`reliquary.rings.featherSafeFall` — Кольцо пепельных перьев: падение до стольких блоков не наносит урона.
{{config reliquary.rings.featherSafeFall}}

`reliquary.rings.featherMaxFallSpeed` — Кольцо пепельных перьев: наибольшая скорость падения, блоков за тик.
{{config reliquary.rings.featherMaxFallSpeed}}

`reliquary.rings.fogStalkerClearing` — Кольцо туманного ловчего: во сколько раз дальше начинается и заканчивается туман обелиска.
{{config reliquary.rings.fogStalkerClearing}}

`reliquary.rings.delverHasteLevel` — Кольцо копателя: уровень Спешки при добыче руд обелиска.
{{config reliquary.rings.delverHasteLevel}}

`reliquary.rings.embersFireSeconds` — Кольцо углей: на сколько секунд удар в ближнем бою поджигает цель.
{{config reliquary.rings.embersFireSeconds}}

`reliquary.drops.chestArtifact` — Шанс артефакта в сундуке с сокровищами зоны.
{{config reliquary.drops.chestArtifact}}

`reliquary.drops.chestRing` — Шанс кольца в сундуке с сокровищами зоны.
{{config reliquary.drops.chestRing}}

`reliquary.drops.eliteArtifact` — Шанс артефакта с элитного моба зоны (при убийстве игроком).
{{config reliquary.drops.eliteArtifact}}

`reliquary.drops.eliteRing` — Шанс кольца с элитного моба зоны (при убийстве игроком).
{{config reliquary.drops.eliteRing}}

### moon: Лунные ночи

`moon.enabled` — Включить или выключить лунные ночи (отладочная команда всё равно может её начать).
{{config moon.enabled}}

`moon.cycleInterval` — Лунная ночь раз в столько лунных циклов по 8 дней; первое полнолуние мира не считается.
{{config moon.cycleInterval}}

`moon.fallbackMinutes` — Через сколько минут реального времени лунная ночь кончается, даже если её волны не побеждены.
{{config moon.fallbackMinutes}}

`moon.fullMoon.levelBonus` — Дополнительные уровни враждебных мобов Верхнего мира, появившихся в полнолуние.
{{config moon.fullMoon.levelBonus}}

`moon.fullMoon.followRange` — Множитель дальности преследования враждебных мобов Верхнего мира в полнолуние.
{{config moon.fullMoon.followRange}}

`moon.blood.vampirism` — Доля урона, нанесённого враждебным мобом игроку, которая лечит этого моба в кровавое новолуние.
{{config moon.blood.vampirism}}

### nightMobs: Волны полнолуния

`nightMobs.zombieLevelBonus` — Уровни, которые пепельный зомби из волны полнолуния получает сверх уровня местности.
{{config nightMobs.zombieLevelBonus}}

`nightMobs.ghostMissChance` — Доля атак по призраку, проходящих сквозь него.
{{config nightMobs.ghostMissChance}}

`nightMobs.maxZombies` — Наибольшее число пепельных зомби в волне одного игрока (по одному за каждый уровень Проклятия ночи).
{{config nightMobs.maxZombies}}

`nightMobs.ghostCount` — Сколько призраков приходит к игроку без проклятия.
{{config nightMobs.ghostCount}}

`nightMobs.spawnMinDistance` — Наименьшее расстояние в блоках от игрока, на котором появляется волна.
{{config nightMobs.spawnMinDistance}}

`nightMobs.spawnMaxDistance` — Наибольшее расстояние в блоках от игрока, на котором появляется волна.
{{config nightMobs.spawnMaxDistance}}

### vampire: Вампир

`vampire.provokeMinutes` — Через сколько минут реального времени ближайший к игроку вампир преображается, если никто его не ударил.
{{config vampire.provokeMinutes}}

`vampire.groupRadius` — Игроки, находящиеся так близко друг к другу, делят одну группу вампиров.
{{config vampire.groupRadius}}

`vampire.groupMin` — Наименьшее число вампиров в группе.
{{config vampire.groupMin}}

`vampire.groupMax` — Наибольшее число вампиров в группе.
{{config vampire.groupMax}}

`vampire.bossMinLevel` — Наименьший уровень босса-вампира.
{{config vampire.bossMinLevel}}

`vampire.bossCapNoRealm` — Наибольший уровень босса, пока никто из участников не открыл ни одного обелиска.
{{config vampire.bossCapNoRealm}}

`vampire.firstKillShards` — Осколки фляги за первую победу игрока над вампиром (плюс клык вампира).
{{config vampire.firstKillShards}}

`vampire.repeatShardChance` — Шанс одного осколка фляги за последующие убийства.
{{config vampire.repeatShardChance}}

### setpieces: Механизмы

`setpieces.maxPerChunk` — Сколько движущихся механизмов (крылья мельниц, колокола, кадила…) может быть в одном чанке; лишние становятся воздухом.
{{config setpieces.maxPerChunk}}

`setpieces.animate` — Двигаются ли механизмы вообще. Если выключено: стоят в положении покоя. Синхронизируется с клиентами.
{{config setpieces.animate}}

Далее: [Наборы данных и совместимость](datapacks.md)
