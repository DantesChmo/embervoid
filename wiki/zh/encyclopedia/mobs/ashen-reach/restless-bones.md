---
entity: embervoid:restless_bones
kind: mob
source: e4af0464aa91
---

{{stats}}

## 在哪里找到

不安骸骨遍布[沉陷墓城](../../places/sunken-necropolis.md)。它们在地表和地下墓穴中成小群自然刷新，更多的则被放置在坟排、陵墓、礼拜堂、藏骨坑、道路哨站和地下墓穴的每个房间里。墓园敲钟人会唤起新的骸骨，[墓穴看守者](../../bosses/crypt-keeper.md)会把它们召进自己的战场。

## 行为

一具拿着生锈剑的骷髅。它的攻击和任何近战炮灰一样：举起剑，短暂前摇，一击。在前摇时后退，在它之后反击。

它的危险之处在于它不会真的死去。本该杀死它的那一击，会让它散成一堆白骨。这堆骨头会躺五秒 <!-- RestlessBonesEntity.REFORM_TICKS -->，然后以一半生命值重新站起来。你有两种办法彻底解决它：

- **打碎骨堆。** 骨堆的生命值极低；在它站起来之前再补一击就能杀死它。
- **点亮它。** 躺在方块亮度 8 或更高的地方 <!-- RestlessBonesEntity.LIGHT_TO_STAY_DEAD -->的骨堆会化为灰尘，这次击杀仍然算在你头上。在火把旁边与骸骨战斗，或者在骨堆旁放一支火把。

留意墓园敲钟人：敲钟人的钟声会让它周围所有的骨堆立刻重新站起来。

## 掉落物

{{drops}}

除了战利品表之外，骸骨还可能掉落{{item embervoid:raw_grave_iron}}，被玩家杀死时，可能掉落一件[狱卒套装](../../items/armor/wardens-set.md)的部件，或者极小概率掉落{{item embervoid:drowned_sabatons}}。
