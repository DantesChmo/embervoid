---
entity: embervoid:cinder_hound
kind: mob
source: 9c3823c0cdec
---

{{stats}}

## Wo du ihn findest

Zunderhunde streifen durch die Brennende Stadt rund um die [Schwelende Zitadelle](../../places/smoldering-citadel.md), den
letzten Sektor hinter dem Zitadellentor. Sie sind ihr einziger natürlicher Spawn und kommen in Rudeln von drei bis fünf
<!-- config reachMobs.spawns.cinder_hound -->, wo immer das Blocklicht niedrig ist. Der
[Schwelende Fürst](../../bosses/smoldering-lord.md) ruft zusätzliche Hunde in seinen Innenhof, wenn du in der Gruppe gegen
ihn kämpfst.

## Verhalten

Ein schneller, brennender Hund. Wählt er ein Ziel, stößt er ein Heulen aus, und jeder untätige Hund im weiten Umkreis
<!-- CinderHoundEntity.HOWL_RANGE --> schließt sich der Jagd an. Ein Hund, der dich entdeckt, heißt: Das ganze Rudel kommt.

Er rennt schneller, als du gehst, und beißt mit einem sehr kurzen Ausfall; ein Biss, der trifft, setzt dich ein paar
Sekunden lang in Brand. <!-- CinderHoundEntity.BITE_FIRE_SECONDS --> Feuer schadet den Hunden selbst nicht, Feuerwaffen
und Lava sind an ihnen also vergeudet.

Das Rudel ist das Problem, nicht der einzelne Hund. Hörst du das Heulen, geh in eine Ecke oder eine enge Gasse zurück,
damit sie dich nur von einer Seite erreichen, und schlage nach allem, was in Reichweite kommt: Ein Hund hat wenig
Gesundheit, und weite Schwungangriffe treffen mehrere auf einmal. Feuerresistenz nimmt ihren Bissen den Stachel. Bleib
zwischen den Kämpfen in der Stadt in Bewegung, denn ein neues Heulen holt jeden noch untätigen Hund in der Nähe herbei.

## Beute

{{drops}}
