---
title: Installing
kind: guide
order: 2
---

## What you need

| Mod | Version |
|---|---|
| Minecraft | 1.20.1 |
| Java | 17 or newer |
| [Fabric Loader](https://fabricmc.net/use/) | 0.15 or newer |
| [Fabric API](https://modrinth.com/mod/fabric-api) | any build for 1.20.1 (developed against 0.92.12+1.20.1) |
| [GeckoLib](https://modrinth.com/mod/geckolib) | 4.8.4 or newer, for 1.20.1 (animated mobs, bosses and set pieces) |
| [playerAnimator](https://modrinth.com/mod/playeranimator) | 1.0.2-rc1+1.20 or newer, for 1.20.1 (player attack animations) |

All four mods are required: the game refuses to start without them. [Mod Menu](https://modrinth.com/mod/modmenu) is
optional; it only shows the mod's description. Embervoid has no in-game config screen: settings live in a file (see
[Configuration](configuration.md)).

## Singleplayer

1. Install Fabric Loader for 1.20.1.
2. Put Embervoid, Fabric API, GeckoLib and playerAnimator into your `mods` folder.
3. Start the game and create a new world, or open an existing one. Embervoid works in old vanilla worlds, but its
   dungeons only appear in chunks generated after you add it.

## Dedicated server

Install the same four mods on the server **and** on every client. A client without the mod cannot join.

The server's `config/embervoid.json` is the one that counts: it is sent to each player when they join, and their
client follows it for that session. Client-only settings (the stamina bar, mob level labels, Boss Music volume, camera
shake) stay with each player.

## Updating to a new version

Back up your world first, then replace the jar. Embervoid upgrades a world on its first start with the new version.

- **From 0.5.0–0.5.2 to 0.5.3, and from 0.4.x to 0.5.x:** the realm Ashen Reach is generated again with its new
  layout. Your progress stays (beaten bosses, unlocked and completed realms), Ash Remnants left in the realm are laid
  on the hub's landing, and players who logged out inside the realm join at the hub. Anything you built or left in
  chests inside Ashen Reach is gone, and the realm shrines you rested at are forgotten: you start again from the
  landing. The overworld, the Nether, the End and the hub are never touched.
- **From 0.4.x:** Sanctums that were already generated keep their old core with the Ashen Maiden and the obelisks;
  they still work. New Ruined Sanctums appear in fresh chunks.
- **Config:** missing keys are added with their defaults. Keys you already have keep their values, even when a new
  version changes the default (0.5.3 retuned the weapon specials). Delete a key to take the new default.

Next: [A harder overworld](walkthrough/harder-overworld.md)
