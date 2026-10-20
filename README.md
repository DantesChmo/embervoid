# Embervoid

A dark fantasy adventure mod for **Minecraft 1.20.1 (Fabric)**: generated dungeons, bosses and cursed gear in a sandbox world. See [CONCEPT.md](CONCEPT.md).

Languages: English, Russian.

## Requirements
- Java 17
- Fabric Loader ≥ 0.15, Fabric API

## Development
```bash
./gradlew runClient    # launch the game with the mod
./gradlew runDatagen   # generate JSON resources
./gradlew build        # build the jar into build/libs/
```

## Configuration
The mod reads `config/embervoid.json` (created with defaults on first launch). Missing keys are filled in, and invalid values are logged and reset to defaults. If the file is not valid JSON, it is moved to `embervoid.json.broken` and replaced with defaults.

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `remnant.mode` | `xp_and_items` | `off`, `xp`, `xp_and_items` | What goes into the Ash Remnant on death |
| `stamina.max` | 100 | 1–10000 | Stamina pool size |
| `stamina.regenPerTick` | 1.0 | 0.01–1000 | Stamina restored per tick while regenerating |
| `stamina.regenDelayTicks` | 20 | 0–1200 | Ticks after spending stamina before it regenerates |
| `flask.baseCharges` | 3 | 1–20 | Charges of a fresh Healing Flask |
| `flask.maxCharges` | 8 | 1–20 | Charge cap after upgrades (at least `baseCharges`) |
| `flask.healAmount` | 8.0 | 0.5–1000 | Health per sip, in half-hearts |
| `flask.drinkTicks` | 32 | 1–200 | Ticks one sip takes |
| `structures.<ruins\|crypt\|fortress>.spacing` | 24 / 40 / 64 | 2–4096 | Average distance between structures, in chunks |
| `structures.<ruins\|crypt\|fortress>.separation` | 8 / 16 / 24 | 0–4095 | Minimum distance, in chunks (less than `spacing`) |

## Debug commands
- `/embervoid data` (operator) — show your Embervoid player data as stored on the server.
- `/embervoid data flaskChargeUpgrades <n>` (operator) — set a test value and sync it to the client.
- `/embervoid stamina` (operator) — show your stamina.
- `/embervoid stamina spend <amount>` (operator) — spend stamina as an action would (survival/adventure only).
- `/embervoid stamina set <value>` (operator) — set your stamina.
- `/embervoid_client data` — show the player data as your client sees it.
- `embervoid:debug_sweep` (dev environment only, `/give` or the creative tab) — test weapon for the special-attack framework: right-click does a knockback sweep for 30 stamina with a 1 s cooldown.
