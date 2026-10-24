# Track V-C — setpieces (PLAN 20.1, 20.2, arenas that move)

Branch `claude/setpieces-6ff577`. Rules: [COORDINATION.md](../COORDINATION.md) "Tracks — 0.5.0 visual overhaul".
Bar: [docs/art-direction.md](../docs/art-direction.md) §2 (models), §3 "Motion". Nobody answers questions tonight:
every decision below is mine and final for this track.

Owns `com.embervoid.block.setpiece`, `com.embervoid.client.setpiece`, `tools/setpiece_art.py`, `geo/block/**`,
`animations/block/**`, `textures/block/setpiece/**`, `models/block/setpiece/**`, `textures/item/setpiece/**`,
`ArenaTemplates`, the arena-events hook in `boss/BossFight` (+ a default `id()` on `ArenaHazard`), one-line hooks in
boss classes as separate commits, placement lines in V-A's template files (last).

## Decisions

- **One block entity type** `embervoid:set_piece` for every piece; the block carries its `SetPieceKind` (shape,
  animations, reactions). One renderer, one GeoModel that picks `geo/block/setpiece/<kind>.geo.json`,
  `animations/block/setpiece/<kind>.animation.json`, `textures/block/setpiece/<kind>.png` (+ `_glowmask`) per kind:
  GeckoLib's `DefaultedBlockGeoModel` layout, so the three files always sit together.
- **Mode in NBT** (`Mode`: `always` / `triggered`), **state in NBT** (`State`, a short word: `open`, `closed`,
  `lit`, `cold`, `still`, `spinning`), synced through `toUpdatePacket` / `toInitialChunkDataNbt`. Pieces with a `lit`
  state also carry a blockstate `LIT` so they give light. ALWAYS loops `idle` client-side with no server ticking; a
  TRIGGERED piece loops `state.<state>` (or `idle` where a state has no loop) and plays `action.<name>` one-shots
  through GeckoLib's triggered animations (`triggerAnim`, a packet to the tracking clients).
- **Animation names** in the piece's file: `idle` (loop), `state.<s>` (loop, holds a pose), `action.<a>` (one-shot).
  Sound keyframes carry a word (`creak`, `toll`, `chain`, `slam`, `grind`) that the client maps to vanilla sounds:
  no `sounds.json` edits (a shared file with two other tracks tonight).
- **Facing** is `HORIZONTAL_FACING` (models authored facing north; GeckoLib's block renderer turns them), so
  templates rotate pieces like any block. **Waterlogging** on everything that can stand in a flooded crypt.
- **Hitbox** per kind as a north-facing `VoxelShape`, turned per facing; a piece only ever occupies its own block
  (a closed portcullis is a look, the fog wall is the lock — noted in CORE-style docs for the coordinator).
- **Culling:** the renderer's `getRenderDistance` per kind (64 for small pieces, 128 for sails and the mill) and
  `rendersOutsideBoundingBox` for everything that hangs or spans beyond its block.
- **Cap per chunk:** `setpieces.maxPerChunk` (8). Enforced when a chunk's set pieces come alive on the server
  (`ServerBlockEntityEvents.BLOCK_ENTITY_LOAD`, deferred one tick to the server thread): beyond the cap, the extra
  pieces (sorted by y, z, x: the lowest and northernmost stay) become air. Player placement into a full chunk is
  refused. The sort is pure (`SetPieceCap.keep`, JUnit). A structure processor was the alternative; the structure
  JSON and placement code belong to other tracks and the load hook catches worldgen and commands alike.
- **Animate toggle:** `setpieces.animate` (true). Off: the controllers stop (rest pose) and the client tickers skip
  particles. Server config, synced, so a server can switch it for everyone.
- **API:** `SetPieces.trigger(ServerWorld, BlockPos, String event)`: `always` / `triggered` (mode), a state name
  (sets it), `<state> <ticks>` (sets it, back to the default state after the ticks through a scheduled block tick),
  an action name (one-shot). `ArenaEvents.fire(BossFight, String event)` reaches every set piece inside the arena
  radius + 8 (and ±24 blocks of height) and applies the kind's reaction table; `BossFight` fires `summon`,
  `phase <n>`, `hazard <id>` (`ArenaHazard.id()`, default the class name: `ash_storm`, `flood`, `area`),
  `bossHit heavy` (one hit ≥ 4 % of the boss's max health), `kill`, `reset`. Boss moves that are not hazards or
  phases (`raise_dead`) fire from the boss class with a one-line call, in its own commit.
- **Great brazier and the campfire logic:** the Lord, the Keeper and the Sister find *campfires* and read
  `CampfireBlock.LIT`; players light them with flint and steel. The campfire stays where `ReachArenas` puts it; the
  great brazier set piece stands **one block above it** and renders its bowl down around the campfire. It follows the
  campfire (`neighborUpdate` from below → its `lit` / `cold` state), so a lit campfire shows a great flame and a
  smothered one goes dark; the campfire's own flame sits inside the bowl. A great brazier with no campfire under it
  (outpost toll gates) is lit by arena events or `SetPieces.trigger`. No boss class changes needed for braziers.
- **Phase offsets:** `getTick` adds a per-position offset so cages, scales and censers do not swing in step.

## Pieces (ids, mode, reactions)

| Id | Mode | States / actions | Reacts |
|---|---|---|---|
| `windmill_sails`, `windmill_sails_torn` | ALWAYS | `idle` (one turn in 16 s, creak at 0 and 8 s) | — |
| `granary_hoist` | ALWAYS | `idle` sway | `bossHit heavy`, `summon` → `action.swing` |
| `great_bell` | TRIGGERED, idle sway | `action.toll` (2.4 s, two tolls) | `summon`, `phase`, `raise_dead`, `kill` → toll |
| `hanging_cage` | ALWAYS | `idle` sway, `action.swing`; NBT `Bones` shows the bundle | `summon`, `bossHit heavy`, `hazard` → swing |
| `hanging_scales` | ALWAYS | `idle` sway, `action.swing` | `summon`, `hazard`, `bossHit heavy` → swing |
| `portcullis` | TRIGGERED | `open` (default) / `closed`; `action.slam`, `action.raise` | `summon` → closed+slam, `kill`/`reset` → open+raise |
| `great_gear` | TRIGGERED | `still` (default) / `turning` | `summon` → turning, `kill`/`reset` → still |
| `forge_bellows` | ALWAYS | `idle` breath; smoke and a spark puff | — |
| `chain_winch` | ALWAYS | `idle` turning drum, chain and bucket | — |
| `censer` | TRIGGERED, idle sway | `cold` (default) / `lit`; smoke when lit | `phase` → lit, `reset` → cold |
| `great_brazier` | TRIGGERED | `cold` (default) / `lit`; smoke column when lit; follows a campfire below | `phase` → lit (only without a campfire below), `reset` → cold |
| `ash_mill` | TRIGGERED | `still` (default) / `spinning`; `action.spin_up` | `hazard ash_storm` → spinning 160 ticks, `summon` → spinning 80, `kill`/`reset` → still |
| `sluice_grate` | TRIGGERED | `closed` (default) / `open` | `hazard flood` → open, `kill`/`reset` → closed |

## Arenas

- Fields: the ash mill at (0, 1, 17) (centre-back, between the pillars at r 24 and the boss's spot; the pillar ring
  and the stalls stay); hanging scales under four of the stall roofs. Woods: hanging cages on four wall posts, a
  gallows wheel (`great_gear`) over the gate turning from `phase 1` (the Tree uproots). Necropolis: a great bell in
  a chapel niche cut into the pit wall on the south axis at roof height; sluice grates in the pit wall on the east
  and west axis. Citadel: the portcullis in the keep gate's lintel, two great gears in the keep wall face, the great
  braziers over the three signal campfires, tattered standards on the wall. Sanctum arena: censers on the walls.
  Footprints, shrine and chest offsets, pillars, braziers, walkways: unchanged.

## Status

- [x] Framework (`6054929`): block, block entity, kinds, `SetPieces.trigger`, `ArenaEvents` fired from `BossFight`
  (+ `ArenaHazard.id()`), the chunk cap, config `setpieces`, GameTests `SetPieceTests` (cap, triggers, arena
  events inside / outside, the brazier following its campfire, animation files vs. kinds), JUnit `SetPieceCapTest`,
  `ReactionTest`, `SetPieceAssetsTest`
- [x] `tools/setpiece_art.py` and the fourteen pieces (`e97bd36`, the portcullis widened in `436e0fe`)
- [x] Registration, both lang files, datagen (blockstates, item models, loot, tool tags; `src/main/generated`)
- [x] Arenas that move (`2919e1c`); one-line hooks in their own commits: Crypt Keeper `raise_dead` (`3528dcb`),
  Grave Warden ring → `SetPieces.tollBellsNear` (`a446fa3`)
- [x] `git merge main` (`e126c38`: both-sides-append conflicts in the GameTest list and the lang files, both kept) and
  placement (`436e0fe`) as added lines in `MinorTemplates`, `CitadelTemplates`, `SanctumTemplates`,
  `HubTemplates`, `OutpostTemplates` (`CryptTemplates` untouched)
- [x] Previews below

Checks run here: `compileJava compileClientJava compileTestJava compileGametestJava`, `runDatagen`, JUnit
`com.embervoid.block.setpiece.*`, `SanctumStructureSetTest`, `HubLayoutTest`, `EntityModelFilesTest`,
`python3 tools/model_preview.py self-check` (68 templates). Not run (the coordinator's): GameTests, build, server check.

## Seen / not seen

Seen in `tools/model_preview.py entity setpiece/<name>` / `motion` (the tool now reads `geo/block` too):
- Every piece's rest pose from six angles, close-ups lit and at dusk, texture and glowmask atlases. Bevelled bells,
  brazier bowls, censers, cog hubs and the mill hub read as rounded forms; iron is dark with a specular edge, bronze
  and brass warm; the cage bars, grate bars and portcullis read at a distance. The ash mill is an octagonal stone
  disc with an iron tyre in a wooden frame; the windmill wheel has four laths-and-cloth blades, a tail vane and rope
  stays tip to tip. The great brazier's flame planes and the censer's pierced band glow on the glowmask.
- Motion sheets: the bell's toll (swing to ±38°, the clapper lagging and overshooting, two "toll" keyframes), the
  portcullis's slam (86 px drop in 0.45 s with a bounce, gears spinning) and raise (3 s), the sails turning, the
  cages' and scales' sways, the bellows breathing with the bag scaling.
- Structure renders of the four arenas and of the windmill, chapel, gatehouse and hanging-cages templates: the
  pieces are at the intended spots (as cubes of their particle texture: the structure renderer does not draw GeckoLib
  block models; teaching it to would be a follow-up).

Not seen offline (the coordinator's regression and the user's eyes): in-game lighting of the glowmasks, the sound
keyframes (vanilla door creak, bell, chain, anvil, grindstone), the client puffs (signal smoke and embers over a lit
brazier, soot and sparks from the bellows, soot over a lit censer), the sync of a state change to clients, the
per-position phase offsets, culling distances, how the sails look against the windmill tower in the world, whether
the seven-bar portcullis's outer bars are truly hidden in the courtyard's five-wide gate, the hoist brace's angle.

## Known limits / follow-ups

- A closed portcullis and a sluice grate are looks: only the piece's own block is solid (the fog wall is the lock).
- The windmill's torn-sail variant exists as a block but is placed nowhere yet (20.4 structure variety).
- No winch at the Necropolis sinkhole: the arena template has no rim platform to stand it on.
- The structure preview draws set pieces as cubes; rendering their geo models in place would make arena renders
  honest about the pieces' size.
- The hub island and the toll gates place censers / braziers already lit through NBT `State: lit`; the blockstate's
  `lit` is set in the template too so the light is right before the block entity loads.
