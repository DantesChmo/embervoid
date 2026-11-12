---
entity: embervoid:ash_colossus
kind: boss
source: cc207d92bfb8
---

{{stats}}

## So erreichst du ihn

Der Aschenkoloss ist der Wächter der [Aschenfelder](../places/ashen-fields.md). Seine Arena ist ein alter Marktplatz,
in einen Krater eingesunken und von zehn verbrannten Kornspeicherpfeilern umringt <!-- ReachArenas.PILLAR_COUNT -->,
einige hundert Blöcke vom [Kreuzweg](../places/crossroads.md) entfernt. Folge der Straße der Felder vom Platz bis zu
ihrem Ende, oder lass dir den Weg vom {{item embervoid:ember_lodestar}} zeigen.

Ein [Glutschrein](../blocks/functional/ember-shrine.md) und eine Truhe stehen direkt vor der Nebelwand, auf der Seite zum
Kreuzweg. Der Schrein bleibt kalt, bis du den Boss besiegt hast; rast also am letzten Schrein an der Straße, bevor du
hineingehst.

Tritt in den Krater, und der Koloss erhebt sich. Eine Nebelwand schließt die Arena, bis der Kampf endet. Stirbst du oder
gehst du, verschwindet sie, und beim nächsten Betreten kehrt er mit voller Gesundheit zurück.

## Phasen und Angriffe

Jeder Angriff wird zuerst angezeigt: ein Kegel, ein Ring oder ein fallender Schatten am Boden.

Wo du ihn triffst, zählt mehr als alles andere. Von vorn (innerhalb von etwa 60° seiner Blickrichtung) erleidet er
halben Schaden <!-- AshColossusEntity.FRONT_DAMAGE -->. Von hinten erleidet er die Hälfte mehr
<!-- AshColossusEntity.BACK_DAMAGE -->, von den Seiten normalen Schaden. Kämpfe von hinten.

### Phase 1

- **Faust.** Ein Kegel vor ihm. Die Faust fährt herab und schleudert dich weg. Tritt seitlich aus dem Kegel.
- **Armschleifen.** Ein breiterer, flacher Kegel. Der Hieb zieht alle, die er erwischt, zum Koloss hin, in Reichweite
  seines nächsten Angriffs. Verlasse den Kegel auf dieselbe Weise.
- **Sprung.** Bei einem Ziel in 8 bis 22 Blöcken Entfernung geht er in die Hocke, während ein Flammenring deine Stelle
  markiert, springt dann und landet dort. Lauf aus dem Ring; er landet, wo er markiert hat, nicht dort, wohin du läufst.
- **Felsbrocken.** Er reißt Stein heraus und wirft ihn. Ein Schatten zeigt, wo er einschlägt. Er wirft auch auf jeden, der
  weit entfernt steht, Abstand ist also kein Schutz.
- **Aschesturm.** Etwa alle 20 Sekunden <!-- AshColossusEntity.STORM_INTERVAL --> hält er inne und brüllt. Dann
  erleidet jeder, den er sehen kann, sechs Sekunden lang <!-- AshColossusEntity.STORM_TICKS --> stetigen
  Magieschaden und wird geblendet. Unterbrich die Sichtlinie: Bring einen Kornspeicherpfeiler oder irgendeinen festen
  Block zwischen dich und seine Brust, bevor das Brüllen endet.
- **Pfeilerschlag.** Versteckst du dich hinter einem Pfeiler, während er nah ist, zerschmettert er den Pfeiler: erst ein
  Kegel, dann ist die ganze Steinsäule verschwunden. Die Pfeiler kehren nach dem Kampf zurück, nicht währenddessen.
  Verstecke dich nach Möglichkeit nicht zweimal hinter demselben Pfeiler, und lass einige für die späteren Stürme stehen.

### Phase 2 (unter halber Gesundheit)

Er wirft seine Steinplatten ab. Der Glutkern auf seinem Rücken liegt nun frei, Treffer von hinten richten daher doppelten
Schaden an <!-- AshColossusEntity.CORE_DAMAGE -->. Seine Rüstung sinkt, aber er geht schneller und wählt seine Angriffe
rascher.

- **Schlacke.** Alle paar Sekunden <!-- AshColossusEntity.CINDER_INTERVAL --> fällt brennende Schlacke in der Nähe jedes
  Kämpfers, jeweils durch einen Schatten markiert. Schlacke setzt dich in Brand. Bleib in Bewegung.
- Alle Angriffe der Phase 1 bleiben, einschließlich des Aschesturms.

## Beute

{{drops}}

Bei deinem ersten Sieg landen diese zusätzlich direkt in deinem Inventar:

- die {{item embervoid:ash_colossus_soul}}, die in die {{item embervoid:ashen_scythe}} kommt;
- eine {{item embervoid:flask_shard}}.

Hilfst du einem Freund, der den Boss noch braucht, nachdem du ihn besiegt hast, antwortet dir die Seele manchmal erneut
<!-- boss.repeatSoulChance -->. Ein Kampf, der mit {{item embervoid:ash_of_rematch}} begonnen wurde, bringt nur die
Beutetabelle und Erfahrung, nie die Seele.

## Strategie

**Nahkampf.** Umkreise ihn bis zu seinem Rücken und bleib dort: Er dreht sich langsam, und seine Angriffe zielen dorthin,
wohin er blickt, wenn der Ausholvorgang beginnt. Jedes Armschleifen und jede Faust, denen du ausweichst, ist ein Fenster
für zwei oder drei Treffer auf seinen Rücken. Behalte den Sturm-Timer im Auge und sei in der Nähe eines Pfeilers, wenn das
Brüllen kommt.

**Fernkampf.** Aus der Ferne springt er und wirft Felsbrocken nach dir, beides schlägt langsam ein. Schieße, und bewege
dich, sobald ein Ring oder Schatten unter dir erscheint. Pfeile, die seine Vorderseite treffen, richten halben Schaden an,
arbeite dich also zu seiner Seite vor. Kommt der Sturm, genügt ein Pfeiler zwischen dir und ihm.

**In der Gruppe.** Er hat für jeden zusätzlichen Kämpfer mehr Gesundheit und bringt für jeden Kämpfer über den ersten
hinaus eine [Verkohlte Hülle](../mobs/ashen-reach/cinder-husk.md) mit. Der Koloss wechselt alle paar Sekunden das Ziel,
oft zu dem, der ihn am meisten verletzt hat. Nutze das: Ein Spieler zieht ihn so, dass er wegschaut, während die anderen
den Kern treffen. In Phase 2 fällt die Schlacke auf alle, drängt euch also nicht zusammen.
