---
title: Endurance et attaques spéciales
kind: mechanic
order: 3
source: d02e492987a2
---

L'endurance d'Embervoid n'a qu'un rôle : payer l'attaque spéciale de votre arme. Les coups normaux, le sprint, les sauts
et la parade au bouclier restent gratuits, comme dans vanilla.

## La barre d'endurance

Vous commencez avec 100 d'endurance <!-- config stamina.max -->. La barre se trouve au-dessus de la barre d'accès rapide
et se masque quand elle est pleine. Après une dépense d'endurance, elle attend une seconde
<!-- config stamina.regenDelayTicks: 20 ticks --> puis se remplit rapidement, à raison de 20 par seconde au départ
<!-- config stamina.regenPerTick: 1 per tick -->.

L'**Endurance** augmente les deux : chaque point entier donne +2 d'endurance max et +2 % de régénération.

## Les règles d'une attaque spéciale

Toute arme dotée d'une attaque spéciale l'indique dans son infobulle : « Clic droit : nom », puis son coût en endurance et
sa recharge.

- **Clic droit** avec l'arme pour l'utiliser.
- Elle coûte une **quantité fixe d'endurance**, mais vous pouvez l'utiliser tant qu'il vous reste **un peu d'endurance** :
  la barre tombe à zéro et s'arrête là. Avec une barre vide, vous obtenez « Trop épuisé », la barre clignote et rien ne
  se passe.
- Après usage, l'arme a une courte **recharge**, affichée sur l'objet comme celle d'une perle d'Ender.
- Un spécial passe par les dégâts normaux : l'armure de la cible, son niveau et votre dépendance à la Force ou à la
  Dextérité s'appliquent tous.

C'est donc le coût qui vous limite : deux ou trois spéciaux d'affilée vident la barre, et vous combattez alors avec de
simples coups le temps qu'elle se remplisse.

## Les spéciaux

| Arme | Spécial | Effet | Endurance | Recharge |
|---|---|---|---|---|
| {{item embervoid:sanctum_longsword}} | Taille large | frappe tout ce qui se trouve dans un arc de 90° devant vous | 35 | 0,4 s |
| {{item embervoid:ashbearers_mace}} | Frappe au sol | un anneau autour de vous : dégâts, recul et Lenteur II | 45 | 0,5 s |
| {{item embervoid:ashen_scythe}} | Moisson | une rotation complète frappant tout autour de vous ; chaque ennemi touché rend de l'endurance | 40 | 0,5 s |
| {{item embervoid:wardens_scourge}} | Coup de fouet | attire vers vous le premier ennemi à portée | 30 | 0,4 s |
| {{item embervoid:royal_ash_rapier}} | Fente | un élan vers l'avant qui s'achève en une estocade qui fait saigner deux fois et brûle | 30 | 0,3 s |

<!-- config sanctumGear.cleave*, sanctumGear.slam*, gear.weapons.harvest*, lash*, lunge* -->

L'{{item embervoid:huntsmans_crossbow}} n'a pas de spécial ; sa force, ce sont ses carreaux. Les autres passifs des armes
figurent sur leurs pages d'encyclopédie.

## Conseils

- Ouvrez avec le spécial, puis continuez à frapper pendant que la barre se remplit.
- La Faux cendrée se rentabilise face à une foule : chaque ennemi touché par un balayage ou une Moisson rend 3 d'endurance
  <!-- config gear.weapons.sweepStamina -->.
- La Frappe au sol est votre issue quand vous êtes encerclé ; le Coup de fouet arrache un archer ou un lanceur de sorts à
  son perchoir.

## Réglages

{{config stamina.max}}
{{config stamina.regenPerTick}}
{{config stamina.regenDelayTicks}}
{{config sanctumGear.cleaveStamina}}
{{config sanctumGear.slamStamina}}
{{config gear.weapons.harvestStamina}}
{{config gear.weapons.lashStamina}}
{{config gear.weapons.lungeStamina}}

La barre d'endurance peut rester visible sur votre client avec `staminaBar.hideWhenFull` dans `embervoid-client.json`.

Suite : [Fiole de soin](healing-flask.md)
