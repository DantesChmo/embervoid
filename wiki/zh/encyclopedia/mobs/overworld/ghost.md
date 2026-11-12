---
entity: embervoid:ghost
kind: mob
source: 3972120c6791
---

{{stats}}

## 在哪里找到

幽灵从不自然刷新。它们在**满月**时（见[月夜](../../../guides/mechanics/moon-nights.md)）出现，目标是每一位**没有**夜之诅咒的主世界玩家：三只 <!-- nightMobs.ghostCount -->会出现在 16 到 32 格之外 <!-- nightMobs.spawnMinDistance, nightMobs.spawnMaxDistance -->并朝你飞来。身负诅咒层数的玩家则会遇到[灰烬僵尸](ashen-zombie.md)。

## 行为

一种苍白的飞行灵体，比灰烬僵尸结实得多。它会飞，径直穿过墙壁和方块，并且能隔着它们看见你，所以你无法躲藏，也无法把自己围起来。它在碰到你时发动攻击，大约每两秒一次 <!-- GhostEntity.ATTACK_COOLDOWN_TICKS -->。

你的一些攻击会伴着一缕薄雾直接穿过它，毫无效果（大约七分之一 <!-- nightMobs.ghostMissChance -->）。做好打持久战的准备，带上食物和你的治疗瓶。

在开阔地带与它们战斗，这样你能看到三只都在哪里，并不停移动，让每次只有一只能够到你。

你的这一波会拖住夜晚：在三只幽灵都消失之前，黎明不会到来。如果你清掉了这一波，你会获得一块{{item embervoid:flask_shard}}。如果你死亡，你的幽灵波就到此结束，而这次死亡会给你增加一层诅咒。夜晚结束时还剩下的幽灵会消失。

## 掉落物

{{drops}}
