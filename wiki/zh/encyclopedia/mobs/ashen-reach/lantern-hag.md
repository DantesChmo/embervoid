---
entities: [embervoid:lantern_hag, embervoid:fire_flask]
kind: mob
source: 5077934a1bc6
---

{{stats}}

## 在哪里找到

提灯老妪是[王家林地](../../places/royal-woods.md)的远程敌人。她单独刷新，更多的则被放置在猎人隐蔽所、灯笼排、狩猎小屋和道路哨站里。

## 行为

她会保持距离，大约 8 到 12 格 <!-- LanternHagEntity.MIN_RANGE, MAX_RANGE -->：你逼近时她后退，你后撤时她又走上来。从那个距离，她会以高抛弧线投掷火焰瓶。

每次投掷都有预兆。她举起灯笼，灯笼迸出余烬，四分之三秒后 <!-- LanternHagEntity.THROW_WINDUP -->火焰瓶离手，瞄准的是你在那一刻所站的位置。它在落点周围爆开造成伤害，并留下一片火焰，站在其中的人会被点燃几秒钟。<!-- FireFlaskEntity.PATCH_TICKS -->不会放置火焰方块，所以树林不会燃烧。

灯笼举起时，向侧面不停移动：火焰瓶会落在你原来所在的位置。绝不要在她的火焰区里战斗。

要杀死她，就冲上去。对于离她几格以内的目标 <!-- LanternHagEntity.MIN_THROW_RANGE -->，她无法投掷，只会后退，而且她的生命值很低。弓或弩也有效，因为她瞄准时会站着不动。当她和奴仆或树皮活尸一起出现时，先解决老妪。

## 掉落物

{{drops}}

除了战利品表之外，老妪还可能掉落{{item embervoid:royal_amber}}，被玩家杀死的老妪可能掉落一件[猎手套装](../../items/armor/huntsmans-set.md)的部件，或者极小概率掉落{{item embervoid:huntsman_greaves}}。
