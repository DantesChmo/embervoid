---
title: Команды
kind: guide
order: 9
source: 35a2e3962f63
---

Команды Embervoid нужны операторам сервера и для тестирования. Все они начинаются с `/embervoid` и требуют прав
оператора (уровень 2); в одиночной игре включите читы. Они предназначены для того, чтобы вызволить застрявшего игрока
или что-то опробовать, а не для обычной игры, и выводят текст только на английском.

`<stat>` — это одно из `vitality`, `endurance`, `strength`, `dexterity`, `kindling`, `resilience`, `fortune` или
короткое имя (`vit`, `end`, `str`, `dex`, `kin`, `res`, `for`).

## Игрок

| Команда | Что делает |
|---|---|
| `/embervoid stats` | ваши характеристики, уровень, цена следующего уровня и что дают характеристики |
| `/embervoid levelup <stat>` | купить одно очко, как у Пепельной Девы, за опыт |
| `/embervoid setstat <stat> <value>` | бесплатно задать характеристику (1–999) |
| `/embervoid stamina` | ваша выносливость |
| `/embervoid stamina spend <amount>` | потратить выносливость, как это делает особая атака |
| `/embervoid stamina set <value>` | задать выносливость |
| `/embervoid data` | ваши данные игрока Embervoid в том виде, в каком они хранятся на сервере |
| `/embervoid data flaskChargeUpgrades <n>` | задать, сколько осколков фляги приняла ваша фляга |
| `/embervoid remnant` | сложить ваши вещи и опыт в пепельный останок у ваших ног, как при смерти |
| `/embervoid reliquary <player>` | показать реликварий игрока |
| `/embervoid reliquary <player> open` | открыть игроку его реликварий |
| `/embervoid reliquary <player> clear` | очистить реликварий игрока |

## Прогресс

| Команда | Что делает |
|---|---|
| `/embervoid progress <player>` | показать открытые и пройденные обелиски игрока и побеждённых боссов |
| `/embervoid progress <player> reset` | забыть всё это |
| `/embervoid progress <player> unlock <realm>` | открыть обелиск, например `embervoid:realm_1` |
| `/embervoid progress <player> complete <realm>` | отметить обелиск пройденным |
| `/embervoid progress <player> boss <id>` | отметить босса побеждённым, например `embervoid:fallen_sister` или `embervoid:ashen_reach/zone_a` |

Идентификаторы обелисков — от `embervoid:realm_1` до `embervoid:realm_4`. Идентификаторы боссов:
`embervoid:fallen_sister`, `embervoid:ashen_reach/zone_a` (Пепельный колосс), `zone_b` (Скорбящее древо), `zone_c`
(Хранитель склепа) и `final` (Тлеющий владыка).

## Перемещение

| Команда | Что делает |
|---|---|
| `/embervoid hub` | отправиться в Святилище Углей |
| `/embervoid hub leave` | выйти из Святилища Углей запасным путём, не глядя на горящую стелу: туда, где вы в последний раз покинули Верхний мир, иначе к кровати или к точке появления мира |
| `/embervoid hub check` | проверить, что Святилище Углей цело |
| `/embervoid stele` | где горит Расколотая стела мира |
| `/embervoid stele kindle <pos>` | разжечь холодную стелу в указанной точке, как это делает Плачущая душа |
| `/embervoid realm enter <realm> [toShrine]` | войти в мир обелиска, как через его Обелиск Пустоты; `true` переносит к последнему алтарю, у которого вы там отдыхали |
| `/embervoid realm leave` | уйти из мира обелиска в Святилище Углей, как через Обелиск возвращения |
| `/embervoid realm where` | мир обелиска, зона, диапазон уровней и место, где вы стоите |
| `/embervoid realm arenas` | все арены боссов мира обелиска и побеждены ли они вами |
| `/embervoid realm roads [posts]` | дороги мира обелиска, верстовые камни, заставы (и фонарные столбы) |
| `/embervoid realm edge` | пограничная стена мира обелиска и разрушенные мосты |
| `/embervoid realm border <from> <to>` | пограничная стена последней зоны |
| `/embervoid realm find <block> <x> <z>` | самый высокий такой блок в этом столбце, например Врата цитадели |
| `/embervoid realm data` | ваши данные о перемещениях между мирами |

## Боссы, подземелья и мобы

| Команда | Что делает |
|---|---|
| `/embervoid boss fights` | идущие бои с боссами |
| `/embervoid sanctum arena <x> <z>` | арена босса Разорённого Святилища, которое начинается в этом чанке (куда указывает `/locate`), и зачищена ли она |
| `/embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]` | подсчитать, сколько Разорённых Святилищ генерация мира разместит вокруг вас, ничего не создавая |
| `/embervoid moblevel <targets>` | показать уровни мобов и уровень, который получит новый моб, появившийся здесь |
| `/embervoid moblevel <targets> <level>` | задать уровни мобов |
| `/embervoid gear bleed <targets> [stacks]` | добавить уровни кровотечения, как от такого же числа ударов |

Чтобы найти строение, используйте ванильную команду `/locate structure embervoid:ember_sanctum` (Разорённое
Святилище) или, в Пепельном пределе, `embervoid:ashen_reach_arena_a`, `_b` и `_c` (арены зон).

## Лунные ночи и вампир

| Команда | Что делает |
|---|---|
| `/embervoid moon status` | текущая или следующая лунная ночь |
| `/embervoid moon start full` / `start blood` | начать полнолуние или кровавое новолуние прямо сейчас |
| `/embervoid moon stop` | закончить лунную ночь |
| `/embervoid moon setcycle <n>` | перевести часы на утро лунного цикла n (день 8·n) |
| `/embervoid moon curse <player> <stacks>` | задать игроку Проклятие ночи |
| `/embervoid wave status` | волны полнолуния |
| `/embervoid wave spawn <players>` / `clear <players>` | отправить или убрать волны этих игроков |
| `/embervoid vampire status` | вампиры этой ночи и бой |
| `/embervoid vampire provoke` | ближайший вампир преображается сейчас |
| `/embervoid vampire arena open` / `close` / `check` | арена вампира без боя |

## Производительность

| Команда | Что делает |
|---|---|
| `/embervoid perf ticks` | время тиков сервера |
| `/embervoid perf chunks <from> <to>` | загруженные чанки в области |
| `/embervoid perf entities <from> <to>` | сущности, мобы и размещённые враги в области |

## Клиент

`/embervoid_client data` (права оператора не нужны) показывает ваши данные игрока так, как их видит ваш клиент.

Далее: [Вопросы и решение проблем](faq.md)
