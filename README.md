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
The mod reads `config/embervoid.json` (created with defaults on first launch). Missing keys are filled in, and invalid values are logged and reset to defaults. If the file is not valid JSON, it is moved to `embervoid.json.broken` and replaced with defaults. On a server, the server's config is sent to each joining client and used for that session, so the client follows the server's values.

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `remnant.mode` | `xp_and_items` | `off`, `xp`, `xp_and_items` | What goes into the Ash Remnant on death |
| `remnant.secondDeath` | `burn_xp` | `burn_xp`, `burn_all` | What dying again does to the previous remnant: its XP burns and the items stay, or it burns with everything |
| `stamina.max` | 100 | 1–10000 | Stamina pool size |
| `stamina.regenPerTick` | 1.0 | 0.01–1000 | Stamina restored per tick while regenerating |
| `stamina.regenDelayTicks` | 20 | 0–1200 | Ticks after spending stamina before it regenerates |
| `flask.baseCharges` | 3 | 1–20 | Charges of a fresh Healing Flask |
| `flask.maxCharges` | 8 | 1–20 | Charge cap after upgrades (at least `baseCharges`) |
| `flask.healAmount` | 8.0 | 0.5–1000 | Health per sip, in half-hearts |
| `flask.drinkTicks` | 32 | 1–200 | Ticks one sip takes |
| `stats.vitalityHealth` | 1.0 | 0–100 | Max health (half-hearts) per full Vitality point above 10 |
| `stats.enduranceStamina` | 2.0 | 0–1000 | Max stamina per full Endurance point |
| `stats.enduranceStaminaRegen` | 0.02 | 0–10 | Stamina regen per full Endurance point, as a fraction of `stamina.regenPerTick` |
| `stats.enduranceKnockbackResistance` | 0.005 | 0–1 | Knockback resistance per full Endurance point |
| `stats.strengthDamage` | 0.02 | 0–10 | Damage with STR weapons (axes, tridents half) per full Strength point, as a fraction |
| `stats.dexterityDamage` | 0.02 | 0–10 | Damage with DEX weapons (swords, bows, crossbows, tridents half) per full Dexterity point |
| `stats.dexterityAttackSpeed` | 0.005 | 0–10 | Attack speed per full Dexterity point, as a fraction |
| `stats.kindlingFlaskHeal` | 0.02 | 0–10 | Healing Flask heal per full Kindling point, as a fraction |
| `stats.resilienceToughness` | 0.1 | 0–100 | Armor toughness per full Resilience point |
| `stats.resilienceFireReduction` | 0.01 | 0–1 | Fire damage removed per full Resilience point |
| `stats.resilienceFireReductionCap` | 0.5 | 0–1 | Most fire damage Resilience can remove |
| `stats.fortuneLuck` | 0.1 | 0–100 | Luck per full Fortune point |
| `leveling.costBase` / `costLinear` / `costQuadratic` | 20 / 6 / 0.15 | ≥ 0 | Cost of level L → L+1 in XP points: `costBase + costLinear·L + costQuadratic·L²` |
| `leveling.statCap` | 99 | 10–999 | Highest value a stat can be levelled to |
| `leveling.levelCap` | 0 | ≥ 0 | Highest character level reachable by leveling; 0 = no cap |

A "full point" follows the soft caps: stat values up to 20 count fully, 21–40 at half, 41–99 at a fifth.

Structure spacing is not in the config: it lives in the `structure_set` JSON files, which datapacks can override.

Client preferences live in `config/embervoid-client.json` (same rules; never read by a server, not synced):

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `staminaBar.hideWhenFull` | true | true, false | Hide the stamina bar while stamina is full |

## Debug commands
- `/embervoid data` (operator) — show your Embervoid player data as stored on the server.
- `/embervoid data flaskChargeUpgrades <n>` (operator) — set a test value and sync it to the client.
- `/embervoid stamina` (operator) — show your stamina.
- `/embervoid stamina spend <amount>` (operator) — spend stamina as an action would (survival/adventure only).
- `/embervoid stamina set <value>` (operator) — set your stamina.
- `/embervoid stats` (operator) — show your stats, level, next level cost and what the stats give.
- `/embervoid levelup <stat>` (operator) — level a stat up as the Maiden will, paying XP points.
- `/embervoid setstat <stat> <value>` (operator) — set a stat for free (1–999).
- `/embervoid remnant` (operator) — move your items and XP into an Ash Remnant at your feet, as a death would.
- `/embervoid_client data` — show the player data as your client sees it.
- `embervoid:debug_sweep` (dev environment only, `/give` or the creative tab) — test weapon for the special-attack framework: right-click does a knockback sweep for 30 stamina with a 1 s cooldown.
