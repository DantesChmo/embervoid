---
entities: [embervoid:grieving_tree, embervoid:tree_lantern]
kind: boss
source: 6bfac141eb65
---

{{stats}}

## So erreichst du ihn

Der Trauernde Baum ist der Wächter des [Königsforsts](../places/royal-woods.md): die Eiche des toten Königs, etwa zehn
Blöcke hoch, mit Laternen, die von ihren Ästen hängen. Er steht im Königlichen Hain, einem ummauerten Garten hinter dem
Jagdhaus, einige hundert Blöcke vom [Kreuzweg](../places/crossroads.md) entfernt. Folge der Straße des Forsts bis zu
ihrem Ende, oder lass dir den Weg vom {{item embervoid:ember_lodestar}} zeigen. Das Tor der Gartenmauer liegt auf der
Seite zum Kreuzweg.

Ein [Glutschrein](../blocks/functional/ember-shrine.md) und eine Truhe stehen direkt vor der Nebelwand. Der Schrein
bleibt kalt, bis du den Baum besiegt hast.

Tritt in den Hain, und der Kampf beginnt: Die Nebelwand schließt sich, und ein grau-grüner Nebel füllt die Arena. Stirbst
du oder gehst du, wird der Kampf zurückgesetzt.

## Phasen und Angriffe

**Seine Rinde lenkt jeden Schlag ab.** Schwerter, Äxte und Pfeile auf den Stamm bewirken nichts; du hörst ein dumpfes
Klopfen. Du kannst ihn nur verletzen, indem du zuerst seine Laternen zerbrichst.

### Die Laternen

Sieben Laternen <!-- GrievingTreeEntity.LANTERNS --> hängen in seinen Ästen. Jede zerbricht durch:

- jedes Projektil: einen Pfeil, einen Armbrustbolzen, einen geworfenen Dreizack;
- einen Nahkampftreffer, aber nur bei der Laterne, die nach einem Astschwung tief hängt (siehe unten).

Jede zerbrochene Laterne lässt den Baum zusammenzucken. Zerbrichst du die **letzte**, ist der Baum zehn Sekunden lang
betäubt <!-- GrievingTreeEntity.STUN_TICKS -->: Er hört auf anzugreifen, der Nebel lichtet sich, und der Stamm erleidet
zusätzlichen Schaden <!-- GrievingTreeEntity.STUNNED_DAMAGE -->. Das ist dein Fenster: Lauf hinein und triff den Stamm
mit allem, was du hast. Endet die Betäubung, hängt auf einen Schlag ein ganzer neuer Satz Laternen.

### Phase 1 (verwurzelt)

Er bewegt sich nie von seinem Platz.

- **Wurzellinien.** Der Boden bebt, und eine Linie aus Wurzeln markiert den Boden von seinem Fuß zu jedem Kämpfer, dann
  bricht sie entlang dieser Linie hervor. Ein Treffer schleudert dich hoch und verlangsamt dich zwei Sekunden lang stark.
  Tritt seitlich von deiner Linie.
- **Astschwung.** Ein breiter Kegel vor ihm, bis etwa acht Blöcke weit. Ein großer Ast fegt über den Boden und wirft dich
  zurück. Danach hängt die dem Schwung nächste Laterne drei Sekunden lang tief, in Schwertreichweite
  <!-- GrievingTreeEntity.LOWERED_TICKS -->. Weiche dem Schwung aus und schlage dann diese Laterne.
- **Verdorrte Knechte.** Etwa alle 25 Sekunden <!-- GrievingTreeEntity.THRALL_INTERVAL --> fallen zwei
  [Verdorrte Knechte](../mobs/ashen-reach/withered-thrall.md) von seinen Ästen. Töte sie schnell; sie kommen nicht mehr,
  solange der Baum betäubt ist.

### Phase 2 (unter halber Gesundheit)

Er reißt seine Wurzeln los und geht, langsam, hinter dir her. Die alten Laternen fallen; sechs neue
<!-- GrievingTreeEntity.P2_LANTERNS --> hängen tiefer an seinen eigenen Ästen und bewegen sich mit ihm. Der Nebel wird
dichter. Die Rinde lenkt außerhalb einer Betäubung weiterhin jeden Schlag ab.

- **Wurzelkäfig.** Ein Ring um dich markiert die Stelle; Wurzeln schließen sich und halten jeden darin drei Sekunden lang
  fast reglos fest. Verlasse den Ring, bevor er sich schließt.
- Wurzellinien, Astschwung und die Knechte bleiben.

## Beute

{{drops}}

Bei deinem ersten Sieg landen diese zusätzlich direkt in deinem Inventar:

- die {{item embervoid:grieving_tree_soul}}, die in die {{item embervoid:huntsmans_crossbow}} kommt;
- eine {{item embervoid:flask_shard}}.

Hilfst du später einem Freund, kann die Seele ab und zu erneut kommen <!-- boss.repeatSoulChance -->; ein Kampf, der mit
{{item embervoid:ash_of_rematch}} begonnen wurde, bringt sie nie.

## Strategie

**Fernkampf.** Dieser Kampf ist für Bogen oder Armbrust gemacht. Bring viele Pfeile mit. Bleib zurück, außerhalb der
Schwungreichweite, und schieße eine Laterne nach der anderen ab, während du den Wurzellinien ausweichst. Zerbricht die
letzte, geh für die Betäubung nah ran, oder schieße weiter auf den Stamm, wenn dein Bogen härter trifft als dein Schwert.
In Phase 2 hängen die Laternen tiefer und wandern mit dem Baum, sie sind also leichter zu erreichen, aber du hast weniger
Platz.

**Nahkampf.** Möglich, aber langsam. Bleib vor ihm, um den Astschwung herauszulocken, weiche aus und zerbrich die
herabgelassene Laterne. Eine Laterne pro Schwung bedeutet einen langen Kampf; trage mindestens ein paar Pfeile oder einen
Dreizack für die letzten Laternen bei dir.

**In der Gruppe.** Der Baum hat pro zusätzlichem Kämpfer mehr Gesundheit und bringt für jeden Kämpfer über den ersten
hinaus einen weiteren Verdorrten Knecht mit. Wurzellinien kommen zu jedem Kämpfer, verteilt euch also. Lasst die
Schützen Laternen zerbrechen, während die anderen ihnen die Knechte vom Leib halten, dann schlagen alle gemeinsam
während der Betäubung auf den Stamm. Ruft, wenn die letzte Laterne gleich zerbricht.
