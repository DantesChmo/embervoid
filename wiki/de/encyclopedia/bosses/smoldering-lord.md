---
entity: embervoid:smoldering_lord
kind: boss
source: 79ca67c69bba
---

{{stats}}

## So erreichst du ihn

Der Schwelende Fürst ist der Reichsboss des [Aschenmarks](../places/ashen-reach.md): der gierige König, dreieinhalb Blöcke
groß, in brennender Rüstung und mit einem Großschwert. Er wartet im Innenhof unterhalb der Wehrmauer, im Herzen der
[Schwelenden Zitadelle](../places/smoldering-citadel.md).

Um dorthin zu gelangen, musst du zuerst die drei Zonenwächter besiegen: den [Aschenkoloss](ash-colossus.md), den
[Trauernden Baum](grieving-tree.md) und den [Grufthüter](crypt-keeper.md). Dann lässt dich das
[Zitadellentor](../blocks/functional/citadel-gate.md) auf der Südseite des [Kreuzwegs](../places/crossroads.md) durch.
Folge der Torstraße nach Süden durch die brennende Stadt: die Außenbezirke, die Gasse und den Markt, das Torhaus in der
Stadtmauer, dann den inneren Ring. Der Innenhof liegt etwa 300 Blöcke vom Ankunftsplatz entfernt, vor der Burg. Der erste
Schrein der Zitadelle steht am Ende der Torstraße; ein weiterer steht vor der Nebelwand des Innenhofs und bleibt kalt, bis
du ihn besiegt hast.

Tritt in den Innenhof, und der Kampf beginnt. Eine Nebelwand schließt ihn, und seine eigene Musik spielt. Stirbst du oder
gehst du, wird der Kampf zurückgesetzt.

## Phasen und Angriffe

### Phase 0: auf der Mauer

Er beginnt hoch oben auf der Burgmauer, mit zwei [Funkenden Schützen](../mobs/ashen-reach/sparking-marksman.md) an seiner
Seite. Dort oben kann er nicht verletzt werden. Alle drei Sekunden <!-- SmolderingLordEntity.WALL_ATTACK_INTERVAL -->
greift er an:

- **Mörserringe.** Ein Ring markiert den Boden unter bis zu zwei Kämpfern; zwei Sekunden später
  <!-- SmolderingLordEntity.MORTAR_WINDUP --> fällt dort Feuer und setzt dich in Brand. Bleib in Bewegung, dann stehst du
  nie darunter.
- **Flammenlinie.** Eine Linie verläuft vom Fuß der Mauer quer über den ganzen Innenhof zu einem Kämpfer, bricht dann in
  Feuer aus und brennt einige Sekunden weiter. Tritt von ihr weg und überquere sie nicht, solange sie brennt.

Drei **Signalfeuerschalen** stehen auf Sockeln im Innenhof. Entzünde alle drei (mit Feuerzeug, Feuerkugel oder einem
brennenden Pfeil), und er kommt herab: Ein weiter Ring <!-- SmolderingLordEntity.LANDING_RADIUS --> markiert die Mitte
des Innenhofs, und er landet dort mit einer Druckwelle. Steh nicht darin.

### Phase 1

- **Großschwert-Kombo.** Zwei Hiebe in einem Kegel vor ihm, die dich in Brand setzen, dann ein Schlag auf einen Ring
  direkt vor ihm. Der Schlag geht durch einen erhobenen Schild und zerbricht ihn. Weiche aus dem Kegel zurück, dann aus
  dem Ring.
- **Ausfall.** Bei einem weiter entfernten Ziel stürmt er entlang einer markierten Linie vor und hinterlässt darauf eine
  Feuerspur. Tritt von der Linie und lauf nicht durch die Spur.

Die Schützen bleiben den ganzen Kampf über auf der Mauer. Schieße sie ab oder halte einen Pfeiler zwischen dir und ihnen.

### Phase 2 (unter halber Gesundheit)

Seine Rüstung platzt ab. Er erleidet ab jetzt ein Viertel mehr Schaden <!-- SmolderingLordEntity.SHATTERED_DAMAGE -->,
bewegt sich und greift aber schneller an. Etwa alle 25 Sekunden <!-- SmolderingLordEntity.HAZARD_INTERVAL --> ruft er
nacheinander die Gefahr eines Zonenwächters herab:

1. **Der Aschesturm des Koloss.** Sechs Sekunden Schaden und Blindheit für jeden, den er sehen kann. Unterbrich die
   Sichtlinie: Ein Sockel oder eine Mauer genügt.
2. **Der Nebel des Hains.** Ein dichter rotbrauner Nebel für zwanzig Sekunden <!-- SmolderingLordEntity.FOG_TICKS -->.
   Behalte im Auge, wo er ist, bevor er sich zuzieht.
3. **Die Dunkelheit der Nekropole.** Dunkelheit auf allen für fünfzehn Sekunden <!-- SmolderingLordEntity.DARK_TICKS -->.
   Solange sie anhält, erleidet er halben Schaden, es sei denn, er steht in Licht der Stufe 8 oder mehr
   <!-- SmolderingLordEntity.LIT_LEVEL -->. Die Signalfeuerschalen brennen weiter: Zieh ihn neben eine.

Kombo und Ausfall bleiben.

## Beute

{{drops}}

Bei deinem ersten Sieg landen diese zusätzlich direkt in deinem Inventar:

- die {{item embervoid:smoldering_lord_soul}}, die in das {{item embervoid:royal_ash_rapier}} kommt;
- eine {{item embervoid:flask_ember}}.

Sein Sieg schließt das Aschenmark für jeden Spieler im Kampf ab: Der Obelisk des Reichs im
[Glutheiligtum](../places/ember-sanctum.md) wird entfacht, und der nächste Obelisk leuchtet auf (das nächste Reich ist
noch nicht gebaut). Hilfst du später einem Freund, kann die Seele ab und zu erneut kommen
<!-- boss.repeatSoulChance -->; ein Kampf, der mit {{item embervoid:ash_of_rematch}} begonnen wurde, bringt sie nie.

## Strategie

**Phase 0.** Bleib nicht stehen. Entzünde zuerst die nächste Feuerschale, dann die anderen, und bewege dich dabei
zwischen den Markierungen. Ein Bogen mit brennenden Pfeilen entzündet sie von dort aus, wo du stehst. Wenn du kannst,
töte nebenbei die Schützen von unten; sie schießen für den Rest des Kampfes weiter.

**Nahkampf.** Bleib an seiner Seite und weiche jeder Kombo aus, dann triff ihn in der Erholungsphase nach dem Schlag.
Stürmt er vor, tritt zur Seite und bestrafe das Ende des Angriffs. Kämpfe in Phase 2 neben einer Signalfeuerschale, damit
die Dunkelheit deinen Schaden nicht halbiert, und halte einen Sockel für den Aschesturm in der Nähe.

**Fernkampf.** Halte ihn auf mittlerer Distanz, dann stürmt er vor, was leicht zu lesen ist, aber Feuer hinterlässt;
verlagere den Kampf durch den Innenhof, damit du dich nicht mit Spuren einsperrst. Schieße die Schützen früh ab.

**In der Gruppe.** Er hat pro zusätzlichem Kämpfer mehr Gesundheit und bringt für jeden Kämpfer über den ersten hinaus
einen [Zunderhund](../mobs/ashen-reach/cinder-hound.md) mit. In Phase 0 teilt euch auf: Jeder Spieler übernimmt eine
Feuerschale. Später hält ein Spieler ihn, während die anderen seine Seiten treffen; verteilt euch für den Mörser und den
Sturm.
