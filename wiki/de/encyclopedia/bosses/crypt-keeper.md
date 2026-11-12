---
entity: embervoid:crypt_keeper
kind: boss
source: 7cd40610551f
---

{{stats}}

## So erreichst du ihn

Der Grufthüter ist der Wächter der [Versunkenen Nekropole](../places/sunken-necropolis.md): vier Blöcke Robe und Knochen,
mit einer Hellebarde in der einen Hand und einer Laterne in der anderen. Seine Arena ist ein tiefes Senkloch; eine
Serpentinentreppe führt an der Wand hinab in ein überflutetes Beinhaus, halb überdacht, halb zum Himmel offen. Folge der
Straße der Nekropole vom [Kreuzweg](../places/crossroads.md) bis zu ihrem Ende, oder lass dir den Weg vom
{{item embervoid:ember_lodestar}} zeigen.

Ein [Glutschrein](../blocks/functional/ember-shrine.md) und eine Truhe stehen vor der Nebelwand auf der Randseite zum
Kreuzweg. Der Schrein bleibt kalt, bis du den Hüter besiegt hast.

Der Boden des Beinhauses hat in der Mitte ein erhöhtes Podest, einen Ringweg, ein Wegkreuz und vier Sockel auf den
Diagonalen, jeweils mit einer unentzündeten Feuerschale darauf. Merke dir, wo sie stehen: Im Kampf dreht sich alles um
sie.

Geh hinab ins Beinhaus, und der Hüter erscheint. Eine Nebelwand schließt die Arena, bis der Kampf endet; stirbst du oder
gehst du, wird der Kampf zurückgesetzt, und jede Feuerschale und jeder Tropfen Wasser kehrt in den Ausgangszustand
zurück.

## Phasen und Angriffe

Jeder Angriff wird zuerst angezeigt. Die Markierungen seiner Hellebarde sind seelenblau; die eine rote Linie ist der
Überkopfhieb, den du nicht blocken kannst.

### Phase 1

- **Hellebardenkombo.** Zwei schnelle Hiebe in einem Kegel vor ihm, dann ein Überkopfhieb entlang einer roten Linie. Der
  Überkopfhieb ist **unblockbar**: Er geht durch einen erhobenen Schild und zerbricht ihn. Weiche für die Hiebe aus dem
  Kegel zurück und tritt für den Überkopfhieb von der Linie.
- **Rundumhieb.** Ein Ring um ihn, dann fegt die Hellebarde ganz herum und schleudert dich zurück. Er setzt ihn öfter ein,
  wenn zwei oder mehr Spieler nah stehen. Tritt aus dem Ring.
- **Laternenwurf.** Bei einem Ziel bis zu 16 Blöcke entfernt schleudert er das Feuer seiner Laterne. Ein Schatten
  markiert die Stelle; der Treffer setzt dich in Brand, und dort brennt fünf Sekunden lang eine Flammenlache
  <!-- CryptKeeperEntity.lanternThrow, 100 ticks -->.

### Phase 2 (unter 60 % Gesundheit)

Seine Laterne zerspringt.

- **Dunkelheit.** Alle im Kampf erhalten Dunkelheit, solange die Phase dauert.
- **Wasser.** Das Beinhaus läuft einen Block hoch voll.
- **Halber Schaden im Dunkeln.** Er erleidet nun nur halben Schaden <!-- CryptKeeperEntity.DARK_DAMAGE -->, es sei denn,
  er steht im Licht: Blocklicht 8 oder mehr <!-- CryptKeeperEntity.LIT_LEVEL --> an seinem Standort, oder ein Spieler
  im Umkreis von wenigen Blöcken <!-- CryptKeeperEntity.LANTERN_REACH --> hält eine mit
  {{item embervoid:amber_lantern_oil}} entzündete {{item embervoid:ember_lantern}} in der Nebenhand.
- **Die Feuerschalen.** Jetzt kannst du die vier Feuerschalen auf den Sockeln entzünden: mit Feuerzeug, Feuerkugel oder
  einem brennenden Pfeil. Kämpfe neben einer brennenden Feuerschale, und er erleidet vollen Schaden.
- **Gruftmotten.** Solange irgendeine Feuerschale brennt, kommen alle 15 Sekunden
  <!-- CryptKeeperEntity.MOTH_INTERVAL --> [Gruftmotten](../mobs/ashen-reach/crypt-moth.md), bis zu vier gleichzeitig
  <!-- CryptKeeperEntity.MAX_MOTHS -->. Sie fliegen geradewegs zur nächsten brennenden Feuerschale und ersticken sie. Töte
  sie, bevor sie dort ankommen, oder entzünde sie erneut.
- **Stoß.** Der Laternenwurf entfällt. Stattdessen stürmt er aus der Distanz entlang einer Linie vor und trägt sich mit dem
  Stoß selbst nach vorn. Tritt von der Linie.
- Kombo und Rundumhieb bleiben, und er greift schneller an.

### Phase 3 (unter 30 % Gesundheit)

Das Wasser steigt auf zwei Blöcke; nur die Wege, das Podest und die Sockel bleiben trocken. Die Glocke läutet, und
[Ruhelose Gebeine](../mobs/ashen-reach/restless-bones.md) steigen aus dem Wasser: drei, plus eines für jeden Kämpfer über
den ersten hinaus <!-- CryptKeeperEntity.RISEN_BONES -->. Dunkelheit, Feuerschalen und Motten gehen weiter wie in Phase 2.

## Beute

{{drops}}

Bei deinem ersten Sieg landen diese zusätzlich direkt in deinem Inventar:

- die {{item embervoid:crypt_keeper_soul}}, die in die {{item embervoid:wardens_scourge}} kommt;
- eine {{item embervoid:flask_shard}}.

Hilfst du später einem Freund, kann die Seele ab und zu erneut kommen <!-- boss.repeatSoulChance -->; ein Kampf, der mit
{{item embervoid:ash_of_rematch}} begonnen wurde, bringt sie nie.

## Strategie

**Vor dem Kampf.** Bring Feuerzeug oder Feuerkugeln mit, und wenn du sie hast, eine Glutlaterne mit Bernstein-Laternenöl
für deine Nebenhand. Licht ist ab Phase 2 der ganze Kampf.

**Nahkampf.** Locke in Phase 1 die Kombo heraus, weiche den zwei Hieben aus, geh dem roten Strich seitlich aus dem Weg und
triff ihn in der Erholungsphase. Wenn die Laterne zerspringt, lauf zum nächsten Sockel, entzünde seine Feuerschale und
zieh den Hüter zu dir; kämpfe dort. Töte jede Motte, die auf deine Feuerschale zufliegt. Bleib in Phase 3 auf den Wegen:
Das Wasser bremst dich und verbirgt die Gebeine.

**Fernkampf.** Brennende Pfeile entzünden die Feuerschalen aus der Ferne. Halte in Phase 1 Abstand und achte auf den
Schatten der Laterne. Ab Phase 2 schließt sein Stoß die Lücke schnell, Fernkampf bedeutet also viel Ausweichen. Pfeile
sind gut gegen die Motten.

**In der Gruppe.** Er hat pro zusätzlichem Kämpfer mehr Gesundheit, bringt beim Erscheinen für jeden Kämpfer über den
ersten hinaus ein [Ruheloses Gebein](../mobs/ashen-reach/restless-bones.md) mit und erweckt in Phase 3 weitere. Verteilt die Aufgaben: Ein oder zwei Spieler halten
den Hüter neben einer brennenden Feuerschale, ein weiterer hält die Feuerschalen am Brennen und schießt die Motten ab.
Drängt euch nicht um ihn, sonst setzt er den Rundumhieb ein.
