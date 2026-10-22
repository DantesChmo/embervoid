# Changelog

## 0.3.0 "Moon Nights" — 2026-10-22

Every third lunar cycle the overworld gets two dangerous nights, four days apart: a full moon and a blood new
moon. Neither can be slept through. All numbers are in `config/embervoid.json` (sections `moon`, `nightMobs`,
`vampire`). Models of the new creatures are placeholders.

### Full moon
- Hostiles spawn 3 levels higher, follow you further and do not lose you while you stay in range.
- **Curse of the Night:** every death to a hostile mob adds a stack, wherever it happens. On the full moon, one
  **Ashen Zombie** per stack (up to 10) comes for you. With no curse, 5 **Ghosts** come instead: tougher, flying
  through walls, and some hits pass through them. Clearing a Ghost wave leaves a Flask Shard. The curse lifts when
  the full moon ends.
- The night does not end until your wave is beaten (or after 30 minutes).

### Blood new moon
- The sky turns red, and hostiles heal from the damage they deal to you.
- Friendly **Vampires** appear near players. Strike one and it becomes the **Vampire**, a boss that pulls everyone
  in the overworld into its own arena. First kill: a Vampire Fang and 2 Flask Shards; later kills: a chance of a
  Flask Shard. A player who dies there stays out, and their remnant waits at the spot they were taken from.

### Changed
- The Sealed Gate in Ashen Reach no longer opens into the depths: they are still forming (`realm.depthsOpen`).
