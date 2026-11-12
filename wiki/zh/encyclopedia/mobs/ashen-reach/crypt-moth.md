---
entity: embervoid:crypt_moth
kind: mob
source: fba5b3112c18
---

{{stats}}

## 在哪里找到

墓穴飞蛾生活在[沉陷墓城](../../places/sunken-necropolis.md)。它们只在有顶盖的地方自然刷新，比如地下墓穴和天坑的悬崖下方，最多三只成群 <!-- config reachMobs.spawns.crypt_moth -->，另外藏骨坑、地下墓穴和道路哨站里也放置有飞蛾。[墓穴看守者](../../bosses/crypt-keeper.md)会派它们去扑灭自己已点燃的火盆。

## 行为

一只被光吸引的苍白飞蛾。它会飞向附近最近的明亮光源 <!-- CryptMothEntity.LIGHT_RANGE,
MIN_LUMINANCE -->，并且在 `mobGriefing` 开启时将其熄灭：放置的火把会被破坏并掉落，点燃的蜡烛会熄灭。一群飞蛾几秒钟就能让你在地下墓穴里陷入黑暗。

你靠近时，它会转向你。近身时它会咬人，但很轻。它真正的攻击是花粉爆发：它的翅膀开始发光，四分之三秒后 <!-- CryptMothEntity.BURST_WINDUP -->它爆开，使几格之内的每位玩家失明 3 秒。<!-- CryptMothEntity.BLINDNESS_TICKS -->

看到发光的翅膀时，退出爆发范围，或者在它引爆之前杀死它。它很脆弱，所以快速的一次剑击或一支箭通常就够了。有飞蛾在附近盘旋时，绝不要和活尸哨兵或墓园敲钟人战斗：失明时，你看不清它们的前摇。先杀飞蛾。

## 掉落物

{{drops}}

除了战利品表之外，飞蛾还可能掉落{{item embervoid:raw_grave_iron}}，被玩家杀死时，还有极小概率掉落{{item embervoid:drowned_sabatons}}。
