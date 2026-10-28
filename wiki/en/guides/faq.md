---
title: FAQ and troubleshooting
kind: guide
order: 10
---

## Playing

**Mobs hit far too hard, even on Easy.**
That is on purpose: Easy hits like vanilla Normal, and mobs gain levels the further you go from spawn and the deeper
you dig. See [Mob levels and difficulty](mechanics/mob-levels.md). A server owner can tone it down with
`mobs.difficulty.*` in the [config](configuration.md).

**I died. Where are my things?**
In your Ash Remnant, on the last solid ground you stood on. Chat told you the coordinates. Walk there and right-click
it. See [Death and the Ash Remnant](mechanics/death-and-remnant.md).

**I died again on the way and my XP is gone.**
By default a second death burns the XP in your old remnant; its items stay in it. Go and get them.

**My flask does not drink.**
You are sprinting, or it is out of charges. Stop running; rest at an Ember Shrine, sleep through a night or respawn to
refill it. See [Healing Flask](mechanics/healing-flask.md).

**I lost my flask.**
It never drops on death, but if you threw it away, craft one: glass bottle, diamond, coal or charcoal. Your charges
are kept.

**My bed says I cannot sleep.**
It is a moon night. See [Moon nights](mechanics/moon-nights.md).

**The night will not end.**
On a full moon the night waits until every player's wave is beaten. Find your Ashen Zombies or Ghosts and finish them.
After 30 real minutes the night ends anyway.

**My special attack does nothing.**
"Too exhausted" means your stamina bar is empty: wait for it to refill. A grey overlay on the item means the cooldown
is still running.

**Where do I level up?**
Only with the Ashen Maiden in the hub. You reach the hub through a Cleft Stele kindled with a Crying Soul, which the
Fallen Sister drops at the bottom of a Ruined Sanctum. Start with
[Finding a Ruined Sanctum](walkthrough/ruined-sanctum.md).

**The Crying Soul will not kindle my stele.**
Check the shape: a centre stone, four pillars of 3 Cleft Stone standing two blocks out on each side, nothing in the
3 blocks above the centre. It must be in the Overworld, the Nether or the End, not in a realm or the hub. See
[Kindling the Cleft Stele](walkthrough/cleft-stele.md).

**My stele went out.**
Someone kindled another one (there is one lit stele per world), or a stone of it was broken. You need a new Crying
Soul to light one again. The hub still lets you out, to where you last left the overworld or to your bed.

**I cannot break or place blocks.**
The hub, the realm's landing and the boss arenas are protected. Build elsewhere.

**The obelisk is dark.**
You have not unlocked that realm. Ashen Reach unlocks on your first visit to the hub; the next obelisk lights after
the Smoldering Lord, but its realm is still forming in this version.

**The Citadel Gate will not open.**
It opens for you once you yourself have beaten all three zone bosses; right-click it to see how many are left.

**The arena's shrine "does not answer".**
You have not beaten that arena's boss yet.

**The boss came back at full health.**
Everyone in the fight died or left, so it reset. Your remnant is in the arena.

**The Fallen Sister is gone and I did not get a Crying Soul.**
Each Sanctum's boss falls once per world, and only the players in that fight get a soul. Find another Ruined Sanctum.

## Technical

**All the text turned into boxes / the game crashed when I entered a realm / weapons are invisible in my hand.**
These were bugs in 0.5.0 and 0.5.1, fixed in 0.5.1 and 0.5.2. Update to the latest version.

**I changed the config and nothing happened.**
The file is read when the game or server starts: restart it. On a server, only the server's file counts; your own
`embervoid.json` is ignored while you are connected.

**The boss music is too loud, or I hear none.**
It has its own Boss Music slider in Options → Music & Sounds, independent of Music.

**The game runs slowly near the realm's machinery.**
Set `setpieces.animate` to false in the config to stop the moving set pieces.

**I upgraded and my things in Ashen Reach are gone.**
Upgrading from 0.5.0–0.5.2 (or from 0.4.x) generates the realm again. Progress and remnants are kept; buildings and
chest contents in the realm are not. See [Installing](installing.md).

**Can I play without GeckoLib or playerAnimator?**
No, both are required. Install them on the server and on every client.
