# Track A — reach-world (0.4.0, milestones 9 and 13.1–13.2)

Branch `claude/track-reach-world-85fd2c`. Scope and rules: [COORDINATION.md](../COORDINATION.md). Tasks: PLAN.md
"0.4.0" 9.1–9.9, 13.1–13.2. Design: DESIGN §4.5.

Owns `com.embervoid.realm` (incl. `realm.worldgen`, `realm.fog`), `com.embervoid.reach.world`,
`com.embervoid.client.realm` / `client.reach.world`, realm `dimension*` / `worldgen/**` JSON,
`structures/ashen_reach/**` except arena contents.

## Tasks

- [x] 9.1 Fog API + per-zone fog
- [ ] 9.2 Placed enemies
- [ ] 9.3 Zones renamed, eternal dusk, zone looks
- [ ] 9.4 Zone terrain density + arena sites
- [ ] 9.5 Realm 1 blocks and ores
- [ ] 9.6 Landing plaza, roads, milestones, outposts
- [ ] 9.7 Minor structures per zone
- [ ] 9.8 Necropolis crypt jigsaw + water
- [ ] 9.9 World edge
- [ ] 13.1 Depths "Burning City"
- [ ] 13.2 Citadel jigsaw

## 9.1 Fog API

- Pure `realm.fog.FogStack` (JUnit `FogStackTest`): layers by source id, the densest wins (tie: first pushed),
  shown fog eases towards it (time constant 0.8 s; from no fog the colour is taken at once and only the weight fades in).
- Density 0–1 → fog end `8 + 184·(1 − d)²` blocks, start `end·(1 − d)·0.6`; never further than vanilla (blindness,
  darkness, view distance still win). Terrain fog only, open air only (not under water / lava / powder snow).
- Colour: vanilla fog colour blended to the layer colour by the eased weight (also the clear colour, so the horizon matches).
- Clearing factors (Ring of the Fog Stalker): `RealmFog.setClearing(source, 2)` doubles the fog end of clearable layers;
  arena fog is pushed with `clearable = false` and ignores it.
- Client API `client.realm.RealmFog`: `push(source, density, color[, clearable])`, `clear(source)`, `has`,
  `setClearing`, `density()`, `reset()` (on disconnect). Mixin `RealmFogMixin` on `BackgroundRenderer`.
- Zone fog `client.reach.world.ZoneFog`: source `embervoid:zone`, from the biome under the camera, values in
  `reach.world.ReachZones` (Fields 0.35 grey-ochre, Woods 0.60 grey-green, Necropolis 0.45 blue-grey, landing 0.30,
  abyss 0.35, depths 0.40 dark red). Colours are dusk-dark because the sky at time 13500 is near night.
- Not seen: how the fog looks (no client launch). Values are first guesses for QA.
