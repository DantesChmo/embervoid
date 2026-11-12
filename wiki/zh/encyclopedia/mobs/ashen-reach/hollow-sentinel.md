---
entity: embervoid:hollow_sentinel
kind: mob
source: 915d0a23b4f0
---

{{stats}}

## 在哪里找到

活尸哨兵是[沉陷墓城](../../places/sunken-necropolis.md)的坦克型怪物。它只会被放置，从不自然刷新，也不会再出现：哨兵守卫着陵墓的门、坍塌的墓穴和地下墓穴的大厅。

## 行为

一名躲在塔盾后面的长矛手。当它面朝你时，盾牌遮住它整个前半身：来自正面的近战攻击和箭完全无效，斧头也一样。火焰、魔法和爆炸仍然可以从任何方向伤害它。

它有两种招式，两种都要先站稳脚跟：

- **长矛突刺。** 它把长矛向后拉，然后向外刺出很远，超出身体约 3 格。<!-- HollowSentinelEntity.SPEAR_REACH -->在它拉矛时退出这个范围，或者侧步躲开。
- **盾击。** 只有在你离得很近时才会使用：它把盾牌架稳四分之三秒 <!-- HollowSentinelEntity.BASH_WINDUP -->，然后猛撞向你，并带有强力击退。

盾击之后，无论是否命中，盾牌都会敞开一秒半。<!-- HollowSentinelEntity.OPEN_TICKS -->这就是你的机会：躲开盾击，然后趁它踉跄时从正面攻击。

另一种办法是绕到它背后，盾牌够不到那里。组队时，一位玩家吸引它的注意，另一位攻击它的背部。用火烧它也有效：火焰伤害永远不会被挡住。

## 掉落物

{{drops}}

作为放置的坦克型怪物，它比普通怪物掉落更多的领域战利品：一到两个{{item embervoid:raw_grave_iron}}，被玩家杀死时，还会以更高概率掉落一件[狱卒套装](../../items/armor/wardens-set.md)的部件，或者极小概率掉落{{item embervoid:drowned_sabatons}}。
