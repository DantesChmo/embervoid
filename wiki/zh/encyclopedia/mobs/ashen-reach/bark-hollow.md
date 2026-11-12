---
entity: embervoid:bark_hollow
kind: mob
source: 3375f36d2fdc
---

{{stats}}

## 在哪里找到

树皮活尸是[王家林地](../../places/royal-woods.md)的坦克型怪物。它从不自然刷新：树皮活尸被放置在林间空地、猎苑围墙边和狩猎小屋，杀死后不会再出现。

## 行为

它休眠时看上去和一棵枯树一模一样：不动，无声，眼睛是暗的。当你走到几格之内 <!-- BarkHollowEntity.WAKE_RANGE -->或攻击它时，它会在一秒钟内嘎吱作响地醒来，眼睛亮起，朝你走来。如果长时间无人打扰，它会走回原位，重新变成一棵树。

它唯一的攻击是对前方 120° 范围、约 3 格远的一记沉重横扫。<!-- BarkHollowEntity.SWEEP_RANGE -->它会站稳脚跟，向后拉满整整一秒 <!-- BarkHollowEntity.SWEEP_WINDUP -->，然后才挥出。这段漫长的前摇就是你的机会：后退出攻击范围，或者绕到它背后，趁它对着空气挥击时攻击它。它行动迟缓，很难被击退。

带一把斧头或者火。斧头的近战攻击和所有类型的火焰伤害，对它造成的伤害多出一半。<!-- BarkHollowEntity.WEAK_FACTOR -->对林地里可疑的枯树要小心，并按你自己的节奏唤醒它们，最好是在周围已经清理干净的时候。

## 掉落物

{{drops}}

作为放置的坦克型怪物，它比普通怪物掉落更多的领域战利品：一到两个{{item embervoid:royal_amber}}，被玩家杀死时还会掉落一件[猎手套装](../../items/armor/huntsmans-set.md)的部件，以及以更高概率掉落{{item embervoid:huntsman_greaves}}。
