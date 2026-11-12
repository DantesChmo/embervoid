---
entity: embervoid:grave_warden
kind: mob
source: 106eb91e3ace
---

{{stats}}

## 在哪里找到

墓园敲钟人是[沉陷墓城](../../places/sunken-necropolis.md)的精英怪物。它只会被放置，每座礼拜堂里一只，另外在地下墓穴的看守者墓室中，杀死后不会再出现。

## 行为

敲钟人：一个沉重的身影，拿着一根棍棒和一口钟。它的棍棒挥击打得很重，但出手前会缓慢地举起来；在前摇时走出攻击范围。

真正的威胁是那口钟。发现你几秒钟后，之后每隔十二秒 <!-- GraveWardenEntity.FIRST_BELL, BELL_INTERVAL -->，它会站稳脚跟，把钟举起整整一秒 <!-- GraveWardenEntity.BELL_WINDUP -->，然后敲响。钟声会：

- 让它周围所有倒下的不安骸骨骨堆立刻重新站起来；
- 在周围没有不安骸骨的情况下唤起两只新的不安骸骨，它自己的骸骨最多同时四只 <!-- GraveWardenEntity.MAX_RAISED -->；
- 使几格之内 <!-- GraveWardenEntity.STAGGER_RANGE -->的所有玩家踉跄，并施加两秒的重度缓慢。<!-- GraveWardenEntity.STAGGER_TICKS -->

钟举起时，离敲钟人远一点，让踉跄打不到你，并利用它站着不动的那一秒打出一次远程攻击。不要浪费时间去追它唤起的骸骨：只要敲钟人没死，它们就会回来。集中攻击敲钟人，只有在它倒下之后再去清理骨堆，或者在火把旁边战斗，让骨堆自己化为灰尘。

## 掉落物

{{drops}}

敲钟人是墓城的精英怪物。被玩家杀死时，可能掉落一到两个{{item embervoid:raw_grave_iron}}、一件[狱卒套装](../../items/armor/wardens-set.md)的部件、按精英概率掉落的{{item embervoid:drowned_sabatons}}，以及极小概率掉落墓城的饰品之一：{{item embervoid:gravestone_medallion}}、{{item embervoid:ferrymans_coin}}、{{item embervoid:wardens_chain}}或{{item embervoid:drowned_mans_ring}}。
