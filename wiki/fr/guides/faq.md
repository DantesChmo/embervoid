---
title: FAQ et dépannage
kind: guide
order: 10
source: 1cfe4f2fab32
---

## En jeu

**Les monstres frappent beaucoup trop fort, même en Facile.**
C'est voulu : le mode Facile frappe comme le Normal de vanilla, et les monstres gagnent des niveaux à mesure que vous
vous éloignez du point d'apparition et que vous creusez profond. Voir [Niveaux des monstres et difficulté](mechanics/mob-levels.md).
Le propriétaire d'un serveur peut l'atténuer avec `mobs.difficulty.*` dans la [config](configuration.md).

**Je suis mort. Où sont mes affaires ?**
Dans votre Vestige de cendres, sur le dernier sol ferme où vous étiez. Le chat vous a donné les coordonnées. Rendez-vous
sur place et faites un clic droit dessus. Voir [Mort et Vestige de cendres](mechanics/death-and-remnant.md).

**Je suis mort une deuxième fois en chemin et mon expérience a disparu.**
Par défaut, une deuxième mort brûle l'expérience de votre ancien vestige ; les objets y restent. Allez les récupérer.

**Ma fiole ne se boit pas.**
Vous sprintez, ou elle n'a plus de charges. Arrêtez de courir ; reposez-vous à un Autel de braise, dormez pendant une
nuit ou réapparaissez pour la remplir. Voir [Fiole de soin](mechanics/healing-flask.md).

**J'ai perdu ma fiole.**
Elle n'est jamais lâchée à la mort, mais si vous l'avez jetée, fabriquez-en une : bouteille en verre, diamant, charbon
ou charbon de bois. Vos charges sont conservées.

**Mon lit dit que je ne peux pas dormir.**
C'est une nuit de lune. Voir [Nuits de lune](mechanics/moon-nights.md).

**La nuit ne finit pas.**
Pendant une pleine lune, la nuit attend que la vague de chaque joueur soit vaincue. Trouvez vos Zombies cendreux ou vos
Spectres et achevez-les. Au bout de 30 minutes réelles, la nuit se termine de toute façon.

**Mon attaque spéciale ne fait rien.**
« Trop épuisé » signifie que votre barre d'endurance est vide : attendez qu'elle se remplisse. Une surcouche grise sur
l'objet signifie que la recharge est encore en cours.

**Où monter de niveau ?**
Uniquement auprès de la Vierge cendrée, dans le hub. On rejoint le hub par une Stèle fendue embrasée avec une Âme
éplorée, que la Sœur déchue laisse tomber au fond d'un Sanctuaire en ruine. Commencez par
[Trouver un Sanctuaire en ruine](walkthrough/ruined-sanctum.md).

**L'Âme éplorée n'embrase pas ma stèle.**
Vérifiez la forme : une pierre centrale, quatre piliers de 3 Pierres fendues à deux blocs de distance de chaque côté,
et rien dans les 3 blocs au-dessus du centre. Elle doit se trouver dans l'Overworld, le Nether ou l'End, pas dans un
domaine ni dans le hub. Voir [Embraser la Stèle fendue](walkthrough/cleft-stele.md).

**Ma stèle s'est éteinte.**
Quelqu'un en a embrasé une autre (il n'y a qu'une stèle allumée par monde), ou l'une de ses pierres a été brisée. Il
vous faut une nouvelle Âme éplorée pour en rallumer une. Le hub vous laisse toujours sortir, là où vous avez quitté
l'Overworld pour la dernière fois ou à votre lit.

**Je ne peux ni casser ni poser de blocs.**
Le hub, l'aire d'arrivée du domaine et les arènes de boss sont protégés. Construisez ailleurs.

**L'obélisque est sombre.**
Vous n'avez pas débloqué ce domaine. Les Marches cendrées se débloquent à votre première visite au hub ; l'obélisque
suivant s'allume après le Seigneur rougeoyant, mais son domaine est encore en formation dans cette version.

**La Porte de la citadelle ne s'ouvre pas.**
Elle s'ouvre pour vous une fois que vous avez vous-même vaincu les trois boss de zone ; faites un clic droit dessus pour
voir combien il en reste.

**L'autel de l'arène « ne répond pas ».**
Vous n'avez pas encore vaincu le boss de cette arène.

**Le boss est revenu avec toute sa vie.**
Tous les participants au combat sont morts ou sont partis, il s'est donc réinitialisé. Votre vestige est dans l'arène.

**La Sœur déchue a disparu et je n'ai pas eu d'Âme éplorée.**
Le boss de chaque Sanctuaire ne tombe qu'une fois par monde, et seuls les joueurs présents au combat obtiennent une
âme. Trouvez un autre Sanctuaire en ruine.

## Technique

**Tout le texte s'est transformé en carrés / le jeu a planté en entrant dans un domaine / les armes sont invisibles dans ma main.**
C'étaient des bogues de 0.5.0 et 0.5.1, corrigés dans 0.5.1 et 0.5.2. Passez à la dernière version.

**J'ai modifié la config et rien ne s'est passé.**
Le fichier est lu au démarrage du jeu ou du serveur : redémarrez-le. Sur un serveur, seul le fichier du serveur compte ;
votre propre `embervoid.json` est ignoré tant que vous êtes connecté.

**La musique de boss est trop forte, ou je n'entends rien.**
Elle a son propre curseur Musique des boss dans Options → Musique et sons, indépendant de Musique.

**Le jeu rame près des machineries du domaine.**
Mettez `setpieces.animate` à false dans la config pour arrêter les machineries en mouvement.

**J'ai mis à jour et mes affaires aux Marches cendrées ont disparu.**
Passer de 0.5.0–0.5.2 (ou de 0.4.x) génère le domaine de nouveau. La progression et les vestiges sont conservés ; les
bâtiments et le contenu des coffres du domaine ne le sont pas. Voir [Installation](installing.md).

**Puis-je jouer sans GeckoLib ou playerAnimator ?**
Non, les deux sont obligatoires. Installez-les sur le serveur et chez chaque client.
