---
entities: [embervoid:fallen_sister, embervoid:crying_soul]
kind: boss
source: ea0d7d197364
---

{{stats}}

## So erreichst du sie

Die Gefallene Schwester wartet am Grund jedes [Verfallenen Heiligtums](../places/ruined-sanctum.md). Kämpfe dich das
Treppenhaus hinab und durch die bewachten Hallen bis zur Bosstür. Davor steht ein
[Glutschrein](../blocks/functional/ember-shrine.md): Rast dort, bevor du hineingehst. Geh dann durch den leuchtenden
Nebelvorhang in die Arena, einen dämmrigen Raum mit einer kalten [Gespaltenen Stele](../blocks/functional/cleft-stele.md)
auf dem Podest am hinteren Ende.

Sie erscheint, sobald du in die Mitte des Raums trittst. Hinter dir schließt sich eine Nebelwand: Niemand geht hinaus und
niemand sonst kommt hinein, bis der Kampf vorbei ist. Stirbst du oder gelangst du anders hinaus, ist sie verschwunden, und
beim nächsten Betreten beginnt sie wieder mit voller Gesundheit.

Die Schwester jedes Heiligtums fällt einmal pro Welt. Danach ist das Verlies geräumt, und sie kehrt dort nie zurück; es
gibt keinen Rückkampf. Für eine weitere [Weinende Seele](../items/souls/crying-soul.md) musst du ein anderes Heiligtum
finden.

Sie ist der Torwächter der Mod und so ausgelegt, dass ein frischer Charakter in Eisen mit einem Eisenschwert sie
besiegen kann.

## Phasen und Angriffe

Jeder Angriff wird auf dem Boden markiert, bevor er trifft. Achte auf die Flammen am Boden, nicht auf ihren Körper.

### Phase 1

- **Weihrauchfass-Bögen.** Die Kette windet sich um ihren Arm, dann schwingt sie das Weihrauchfass in einem breiten Kegel
  vor sich, von rechts nach links und sofort wieder zurück. Der erste Schwung hat die längere Ausholphase; der zweite folgt
  rasch. Tritt aus dem Kegel zurück oder hinter sie, und warte beide Schwünge ab, bevor du hineingehst.
- **Schlag.** Sie lässt das Weihrauchfass auf einen Flammenring direkt vor sich niederfahren. Er schleudert dich hoch, und
  eine Glutlache brennt dort einige Sekunden weiter <!-- FallenSisterEntity.PUDDLE_TICKS -->. Verlasse den Ring während der
  Ausholphase und kämpfe nicht in der Lache stehend.
- **Rückwärtsschritt.** Stehst du dicht hinter ihr, dreht sie sich zu dir um und hüpft davon. Das richtet keinen Schaden
  an, beendet aber deine Kombo. Triff sie von der Seite, nicht direkt von hinten.

### Phase 2 (unter halber Gesundheit)

- **Schleierbrand.** Ihr Schleier fängt Feuer. Zwei Sekunden lang <!-- FallenSisterEntity.VEIL_BURN_TICKS --> steht sie
  still und erleidet überhaupt keinen Schaden: Verschwende keine Ausdauer an ihr, weiche zurück und mach dich bereit.
- **Weinende Seelen.** Drei Seelen kreisen um ihren Kopf <!-- FallenSisterEntity.FIRST_FLOCK -->. Nacheinander bäumt sich
  eine Seele mit einem Kreischen auf, während eine Linie aus Seelenfeuer den Boden von ihr bis zu deinem Standort
  markiert, und stürzt dann entlang dieser Linie herab. Tritt seitlich von der Linie. Nach dem Sturzflug hängt die Seele
  zwei Sekunden benommen nahe am Boden <!-- CryingSoulEntity.DAZED_TICKS -->; jeder Treffer tötet sie. Sind alle Seelen
  fort, kommt etwa alle 20 Sekunden <!-- FallenSisterEntity.FLOCK_INTERVAL --> ein neuer Schwarm aus vier
  <!-- FallenSisterEntity.LATER_FLOCK -->.
- **Weihrauchfass-Wirbel.** Ein Flammenring um sie, dann schwingt sie das Weihrauchfass an der vollen Kette um sich. Es
  ist ihr härtester Treffer und wirft dich zurück. Verlasse den Ring, sobald du ihn siehst. Sie setzt ihn öfter ein, wenn
  zwei oder mehr Spieler nah bei ihr stehen.
- Bögen und Schlag bleiben; sie schlägt öfter zu als zuvor.

Die Feuerschalen im Raum entzünden sich nacheinander, während sie Gesundheit verliert. Sie brennen für immer weiter,
sobald sie besiegt ist.

## Beute

{{drops}}

Sie hat keine eigene Beutetabelle. Alles landet direkt im Inventar jedes Spielers, der beim Tod der Schwester im Kampf
ist:

- Bei **jedem** Sieg erhält jeder Spieler im Kampf eine {{item embervoid:crying_soul}}, auch wenn er schon eine andere
  Schwester besiegt hat. Benutze sie an einer Gespaltenen Stele, um den Weg ins [Glutheiligtum](../places/ember-sanctum.md)
  zu entzünden.
- Bei deinem **ersten** Sieg erhältst du außerdem eine {{item embervoid:flask_shard}} und eine Waffe des Heiligtums, das
  {{item embervoid:sanctum_longsword}} oder den {{item embervoid:ashbearers_mace}}, dazu einige Würfe aus Eisen, Gold,
  Büchern, Erfahrungsfläschchen, goldenen Äpfeln und, selten, einem Diamanten.

## Strategie

**Nahkampf.** Bleib an ihrer Seite, knapp außerhalb des Bogenkegels. Nach dem zweiten Bogen oder einem Schlag hat sie eine
kurze Erholungsphase: zwei oder drei Treffer, dann geh wieder raus. Halte in Phase 2 für jede benommene Seele einen
Schwerthieb bereit; schnelles Töten hält die Sturzflüge selten.

**Fernkampf.** Ein Bogen funktioniert in Phase 1 gut, da ihre Angriffe nur wenige Blöcke weit reichen. Bleib im Raum in
Bewegung, damit sie läuft, statt zuzuschlagen. In Phase 2 jagen dich die Seelen ohnehin, geh also nah ran und töte sie,
wenn sie aus einem Sturzflug kommen.

**In der Gruppe.** Sie bekommt für jeden zusätzlichen Kämpfer mehr Gesundheit, aber keine Diener. Verteilt euch: Der Wirbel
bestraft eine Gruppe, die dicht neben ihr steht. Ein Spieler hält ihre Aufmerksamkeit von vorn, während die anderen ihre
Seiten treffen, und ein Spieler übernimmt das Töten der Seelen. Jeder im Kampf erhält eine Seele, nehmt also mit, wer noch
eine braucht.
