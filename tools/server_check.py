#!/usr/bin/env python3
"""Boots a dedicated server with the mod, makes it generate the worldgen a player would hit first, and scans the log.

Run from the repository root: python3 tools/server_check.py [--accept-eula] [--keep-world] [--perf [--profile]]

What it does:
1. Starts `./gradlew runServerCheck` (run dir run/server-check, a fresh world each time unless --keep-world) with
   RCON on a local port, and waits for "Done".
2. Over RCON: locates the nearest Ruined Sanctum and force-loads the chunks around it (so it is really generated in
   overworld terrain; later its boss arena must be there and no Ashen Maiden, PLAN 16.1), force-loads the Ashen Reach landing and depths centre, locates the three zone arenas and the
   depths arena and force-loads them too, force-loads every road outpost and checks each got its chest, force-loads
   the chunk of every road's lantern posts and counts them lit per road (PLAN 14.2), locates and
   force-loads a minor structure of each zone and a crypt, two bridges at the world edge, locates the Smoldering
   Citadel and force-loads its whole route (then expects the keep's treasure chest and every placed Citadel mob),
   force-loads the Vampire arena, starts and stops a full moon and a blood new moon, opens, checks and closes the
   Vampire arena, checks the hub (placed on start, whole, one Ashen Maiden; PLAN 15.1), then has a bot player join
   (PLAN L6): a stat set over RCON reaches its attributes and its client,
   a point bought at a summoned Ashen Maiden through her screen's real packets costs exactly the shown XP, and a death
   leaves an Ash Remnant that gives the items and XP back when the bot right-clicks it; the bot travels to the hub
   (Realm 1 unlocks), into Realm 1, back to the hub and out to where it left (PLAN 15.2); waits for generation, saves,
   stops the server. Commands with an expected answer fail the check when
   the answer does not match.
3. Scans the whole output (build + server log) for crashes, mixin failures, ERROR lines and datapack load errors.
   Exit code 0 only if there are none. WARN lines are listed but do not fail the check.

With --perf (PLAN R40.4b) step 2 measures instead, on a fresh world with default config. Phase 1, no players: for
each area (vanilla overworld far from spawn for reference, realm landing, the three zone arenas, a minor structure of each zone and a crypt, depths landing, depths
arena, the Smoldering Citadel) the time from `forceload add` until all its chunks tick entities, then its tick time
over the last 100 ticks (`/embervoid perf ticks`) and its entities (`/embervoid perf entities`). Phase 2: three
headless bot players (`Bot`) join; one at a time goes to each area, the time until its surroundings tick ("arrival",
which includes generating its view distance) is taken, then the same readings. Phase 3: every area at once, with
three bots in three places. Results go to run/server-check/perf.json and a table. --profile also records a Java
Flight Recorder profile of the server (run/server-check/perf.jfr) and prints the mod's methods with the most samples.

The dedicated server never loads client mixins or client classes: this catches common-code problems only.
The first run needs --accept-eula (Minecraft EULA, https://aka.ms/MinecraftEULA); it is remembered in the run dir.
"""
import argparse
import json
import os
import platform
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import threading
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUN_DIR = os.path.join(ROOT, "run", "server-check")
OUTPUT = os.path.join(RUN_DIR, "check.log")
SERVER_PORT = 25599
RCON_PORT = 25598
BOOT_TIMEOUT = 600
GENERATION_WAIT = 20
STOP_TIMEOUT = 120
PERF_OUTPUT = os.path.join(RUN_DIR, "perf.json")
PERF_JFR = os.path.join(RUN_DIR, "perf.jfr")
# Seconds to let an area settle (mobs placed, lighting done) before its tick time is read; the reading covers the
# last 100 ticks (5 s), so this must be longer.
PERF_SETTLE = 10
# Bots joining at spawn: let their chunks load before the idle reading.
PERF_PLAYER_SETTLE = 20
PERF_GEN_TIMEOUT = 300
PERF_ARRIVAL_TIMEOUT = 120
# How long a boss fights the bot before the reading.
PERF_FIGHT = 15

# A problem anywhere in the output fails the check.
PROBLEMS = [
    re.compile(r"/(ERROR|FATAL)\]"),
    re.compile(r"FAILED during APPLY|was not applied|Mixin apply failed|InvalidInjectionException|MixinApplyError"),
    re.compile(r"---- Minecraft Crash Report ----|Exception in server tick loop|Encountered an unexpected exception"),
    re.compile(r"Couldn't parse|Failed to parse|Couldn't load|Failed to load (?!eula|properties)", re.IGNORECASE),
    re.compile(r"BUILD FAILED|Compilation failed"),
]
# Harmless lines that match the patterns above.
IGNORED = [
    re.compile(r"Failed to load properties from file: server\.properties"),
]
WARNING = re.compile(r"/WARN\]")
# Commands whose answer did not match what exercise() expected; each one fails the check.
FAILED_EXPECTATIONS = []
# Known harmless warnings of a dev dedicated server; not listed in the report.
IGNORED_WARNINGS = [
    re.compile(r"Failed to load eula\.txt"),
    re.compile(r"\*\*\*\* SERVER IS RUNNING IN OFFLINE/INSECURE MODE!|The server will make no attempt|While this makes the game|To change this, set"),
    re.compile(r"Mod .* uses the version .* which isn't compatible with Loader's extended semantic version"),
    re.compile(r"Can't keep up! Is the server overloaded\?"),  # expected while force-loading chunks
]


class Rcon:
    """Minimal Source RCON client (the protocol the Minecraft server speaks)."""

    def __init__(self, port, password):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=120)
        self.next_id = 1
        if self._request(3, password)[0] == -1:
            raise RuntimeError("RCON authentication failed")

    def command(self, text):
        return self._request(2, text)[1]

    def _request(self, kind, body):
        request_id = self.next_id
        self.next_id += 1
        payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        length = struct.unpack("<i", self._read(4))[0]
        data = self._read(length)
        response_id, _ = struct.unpack("<ii", data[:8])
        return response_id, data[8:-2].decode("utf-8", "replace")

    def _read(self, count):
        data = b""
        while len(data) < count:
            chunk = self.sock.recv(count - len(data))
            if not chunk:
                raise RuntimeError("RCON connection closed")
            data += chunk
        return data

    def close(self):
        self.sock.close()


class Bot:
    """A headless offline-mode player (protocol 763, Minecraft 1.20.1): logs in, answers keep-alives and teleports,
    keeps the custom payloads it is sent and the UUID -> id of entities spawned for it, and drops everything else, so
    the server ticks it like a real player (chunks, mob targets, natural spawning). It never moves on its own; RCON
    places it. It can right-click an entity and send a custom payload. The server runs without compression."""

    PROTOCOL = 763
    SPAWN_ENTITY_IN, PLUGIN_IN, KEEP_ALIVE_IN, SYNC_POSITION_IN, DISCONNECT_IN = 0x01, 0x17, 0x23, 0x3C, 0x1A
    CONFIRM_TELEPORT_OUT, CLIENT_COMMAND_OUT, PLUGIN_OUT, INTERACT_OUT, KEEP_ALIVE_OUT, POSITION_OUT = (
        0x00, 0x07, 0x0D, 0x10, 0x12, 0x14)
    # A client reports its position at least once a second; the server moves its chunk tickets only then, not on a
    # teleport alone.
    POSITION_INTERVAL = 1.0

    def __init__(self, name, port):
        self.name = name
        self.reason = None
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=60)
        self.lock = threading.Lock()
        self._send(0x00, self._varint(self.PROTOCOL) + self._string("127.0.0.1") + struct.pack(">H", port) + self._varint(2))
        self._send(0x00, self._string(name) + b"\x00")
        while True:
            packet_id, data = self._read_packet()
            if packet_id == 0x02:  # login success
                break
            if packet_id == 0x04:  # login plugin request (Fabric's early registration query): not understood
                message_id, _ = self._read_varint(data, 0)
                self._send(0x02, self._varint(message_id) + b"\x00")
            elif packet_id in (0x00, 0x01, 0x03):
                raise RuntimeError(f"bot {name}: login answered with packet {packet_id:#x} {data[:200]!r}")
        self.sock.settimeout(None)
        self.pos = None
        # (channel, data) of every custom payload received, and entity UUID (hex, no dashes) -> network id.
        self.payloads = []
        self.entities = {}
        threading.Thread(target=self._play, daemon=True).start()
        threading.Thread(target=self._report_position, daemon=True).start()

    def _play(self):
        try:
            while True:
                packet_id, data = self._read_packet()
                if packet_id == self.KEEP_ALIVE_IN:
                    self._send(self.KEEP_ALIVE_OUT, data[:8])
                elif packet_id == self.SYNC_POSITION_IN:
                    x, y, z = struct.unpack(">ddd", data[:24])
                    flags = data[32]
                    if self.pos is not None:
                        # Bits 0-2: x, y, z relative to the current position.
                        x, y, z = (v + (old if flags & bit else 0) for v, old, bit in zip((x, y, z), self.pos, (1, 2, 4)))
                    self.pos = (x, y, z)
                    teleport_id, _ = self._read_varint(data, 8 * 3 + 4 * 2 + 1)
                    self._send(self.CONFIRM_TELEPORT_OUT, self._varint(teleport_id))
                    self._send_position()
                elif packet_id == self.PLUGIN_IN:
                    length, offset = self._read_varint(data, 0)
                    self.payloads.append((data[offset:offset + length].decode("utf-8"), data[offset + length:]))
                elif packet_id == self.SPAWN_ENTITY_IN:
                    entity_id, offset = self._read_varint(data, 0)
                    self.entities[data[offset:offset + 16].hex()] = entity_id
                elif packet_id == self.DISCONNECT_IN:
                    self.reason = data.decode("utf-8", "replace")
                    return
        except (OSError, RuntimeError) as e:
            self.reason = self.reason or str(e)

    def _send_position(self):
        if self.pos is not None:
            self._send(self.POSITION_OUT, struct.pack(">ddd?", *self.pos, True))

    def _report_position(self):
        while self.reason is None:
            time.sleep(self.POSITION_INTERVAL)
            try:
                self._send_position()
            except OSError:
                return

    def respawn(self):
        self._send(self.CLIENT_COMMAND_OUT, self._varint(0))

    def interact(self, entity_id):
        """Right-click on an entity with the main hand, not sneaking."""
        self._send(self.INTERACT_OUT, self._varint(entity_id) + self._varint(0) + self._varint(0) + b"\x00")

    def send_payload(self, channel, data):
        self._send(self.PLUGIN_OUT, self._string(channel) + data)

    def channels(self, since=0):
        return [channel for channel, _ in self.payloads[since:]]

    def close(self):
        try:
            self.sock.close()
        except OSError:
            pass

    def _send(self, packet_id, data):
        body = self._varint(packet_id) + data
        with self.lock:
            self.sock.sendall(self._varint(len(body)) + body)

    def _read_packet(self):
        length = 0
        for shift in range(0, 35, 7):
            byte = self._read(1)[0]
            length |= (byte & 0x7F) << shift
            if not byte & 0x80:
                break
        data = self._read(length)
        packet_id, offset = self._read_varint(data, 0)
        return packet_id, data[offset:]

    def _read(self, count):
        data = b""
        while len(data) < count:
            chunk = self.sock.recv(count - len(data))
            if not chunk:
                raise RuntimeError("connection closed")
            data += chunk
        return data

    @staticmethod
    def _read_varint(data, offset):
        value = 0
        for shift in range(0, 35, 7):
            byte = data[offset]
            offset += 1
            value |= (byte & 0x7F) << shift
            if not byte & 0x80:
                break
        return value, offset

    @staticmethod
    def _varint(value):
        out = b""
        value &= 0xFFFFFFFF
        while True:
            byte = value & 0x7F
            value >>= 7
            out += bytes([byte | (0x80 if value else 0)])
            if not value:
                return out

    @classmethod
    def _string(cls, text):
        raw = text.encode("utf-8")
        return cls._varint(len(raw)) + raw


def prepare(accept_eula, keep_world):
    os.makedirs(RUN_DIR, exist_ok=True)
    eula = os.path.join(RUN_DIR, "eula.txt")
    if accept_eula:
        with open(eula, "w") as f:
            f.write("# Accepted via tools/server_check.py --accept-eula (https://aka.ms/MinecraftEULA)\neula=true\n")
    if not os.path.exists(eula) or "eula=true" not in open(eula).read():
        sys.exit("The Minecraft EULA is not accepted for run/server-check. Read https://aka.ms/MinecraftEULA and run "
                 "again with --accept-eula.")
    if not keep_world:
        shutil.rmtree(os.path.join(RUN_DIR, "world"), ignore_errors=True)
        # A fresh world gets the default config too (an old file keeps old defaults, e.g. depths closed).
        config = os.path.join(RUN_DIR, "config", "embervoid.json")
        if os.path.exists(config):
            os.remove(config)
    password = secrets.token_hex(12)
    with open(os.path.join(RUN_DIR, "server.properties"), "w") as f:
        f.write("\n".join([
            f"server-port={SERVER_PORT}",
            "server-ip=127.0.0.1",
            "enable-rcon=true",
            f"rcon.port={RCON_PORT}",
            f"rcon.password={password}",
            "online-mode=false",
            "spawn-protection=0",
            "level-name=world",
            # The bots speak the protocol without compression, and never fall: no flying kick for them.
            "network-compression-threshold=-1",
            "allow-flight=true",
            "",
        ]))
    return password


def wait_for(pattern, process, timeout):
    """Waits until a line of the output matches; None if the server exited or timed out first."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        with open(OUTPUT, errors="replace") as f:
            match = pattern.search(f.read())
        if match:
            return match
        if process.poll() is not None:
            return None
        time.sleep(1)
    return None


def located(response):
    """(x, z) from a /locate answer, or None."""
    match = re.search(r"at \[(-?\d+), [^,]+, (-?\d+)\]", response)
    return (int(match.group(1)), int(match.group(2))) if match else None


def exercise(rcon):
    """Runs the worldgen commands; returns (label, command, response) for the report."""
    log = []

    def run(label, command):
        response = rcon.command(command)
        log.append((label, command, response.strip()))
        return response

    def expect(label, command, pattern):
        """Runs a command whose answer must match; a mismatch fails the check."""
        response = run(label, command)
        if not re.search(pattern, response):
            FAILED_EXPECTATIONS.append(f"{label}: expected /{pattern}/, got {response.strip()!r}")

    def expect_eventually(label, command, pattern, timeout=120):
        """Like expect, but asks again every few seconds while chunks are still generating."""
        deadline = time.time() + timeout
        while True:
            response = rcon.command(command)
            if re.search(pattern, response.strip()) or time.time() > deadline:
                break
            time.sleep(5)
        log.append((label, command, response.strip()))
        if not re.search(pattern, response.strip()):
            FAILED_EXPECTATIONS.append(f"{label}: expected /{pattern}/, got {response.strip()!r}")

    def generate_around(label, dimension, pos, radius=48):
        x, z = pos
        run(label, f"execute in {dimension} run forceload add {x - radius} {z - radius} {x + radius} {z + radius}")

    sanctum = located(run("Sanctum", "locate structure embervoid:ember_sanctum"))
    if sanctum:
        # The boss arena can lie some 70 blocks from the start.
        generate_around("Sanctum", "minecraft:overworld", sanctum, 96)
    else:
        FAILED_EXPECTATIONS.append("Sanctum: not located")
    generate_around("Ashen Reach landing", "embervoid:ashen_reach", (0, 0))
    for arena in ("a", "b", "c"):
        pos = located(run(f"Arena {arena.upper()}", "execute in embervoid:ashen_reach run locate structure "
                          f"embervoid:ashen_reach_arena_{arena}"))
        if pos:
            generate_around(f"Arena {arena.upper()}", "embervoid:ashen_reach", pos, 32)
    # Roads (PLAN 9.6): generate around every outpost, then each must have its chest.
    roads = run("Roads", "execute in embervoid:ashen_reach run embervoid realm roads")
    for kind, x, z in re.findall(r"(toll_gate|watchtower) at (-?\d+) (-?\d+)", roads):
        generate_around(f"Outpost {kind}", "embervoid:ashen_reach", (int(x), int(z)), 24)
    # Lantern posts (PLAN 14.2): the chunk of every post, counted lit per road once generated (below).
    posts = run("Road posts", "execute in embervoid:ashen_reach run embervoid realm roads posts")
    for road, coords in re.findall(r"road (\d+):([^\n]*)", posts):
        for x, z in re.findall(r"(-?\d+) (-?\d+);", coords):
            rcon.command(f"execute in embervoid:ashen_reach run forceload add {x} {z}")
    # Minor structures (PLAN 9.7): one per zone, and a Necropolis crypt (9.8), located and generated.
    for structure in ("fields/barn", "woods/hanging_cages", "necropolis/chapel", "necropolis/crypt"):
        pos = located(run(f"Minor {structure}", "execute in embervoid:ashen_reach run locate structure "
                          f"embervoid:ashen_reach/{structure}"))
        if pos:
            generate_around(f"Minor {structure}", "embervoid:ashen_reach", pos, 24)
        else:
            FAILED_EXPECTATIONS.append(f"Minor {structure}: not located")
    # World edge (PLAN 9.9): two of the broken bridges at the abyss cliffs.
    edge = run("Edge", "execute in embervoid:ashen_reach run embervoid realm edge")
    for x, z in re.findall(r"bridge at (-?\d+) (-?\d+)", edge)[:2]:
        generate_around("Bridge", "embervoid:ashen_reach", (int(x), int(z)), 24)
    generate_around("Depths landing", "embervoid:ashen_reach_depths", (0, 0))
    pos = located(run("Depths arena", "execute in embervoid:ashen_reach_depths run locate structure "
                      "embervoid:ashen_reach_depths_arena"))
    if pos:
        generate_around("Depths arena", "embervoid:ashen_reach_depths", pos, 32)
    # The Smoldering Citadel (PLAN 13.2): its one start, then the whole route from the outskirts to the keep.
    expect("Citadel", "execute in embervoid:ashen_reach_depths run locate structure "
           "embervoid:ashen_reach_depths_citadel", r"at \[0, ~, -160\]")
    run("Citadel", "execute in embervoid:ashen_reach_depths run forceload add -76 -272 76 -30")
    generate_around("Vampire arena", "embervoid:vampire_arena", (0, 0), 16)
    # The Vampire arena without players (PLAN 8.12): builds whole, keeps its chunks while open, closes cleanly.
    expect("Vampire arena open", "embervoid vampire arena open", r"Vampire arena open, 0 player")
    expect("Vampire arena built", "embervoid vampire arena check", r"Vampire arena intact, chunks forced")
    expect("Vampire arena floor", "execute in embervoid:vampire_arena if block 0 64 0 minecraft:crying_obsidian",
           r"Test passed")
    expect("Vampire arena close", "embervoid vampire arena close", r"Vampire arena closed")
    expect("Vampire arena closed", "embervoid vampire arena check", r"Vampire arena intact, chunks not forced")
    # The hub (PLAN 15.1): placed once when the server started, whole, with its one Ashen Maiden.
    generate_around("Hub", "embervoid:hub", (0, 0), 24)
    expect("Hub", "embervoid hub check", r"^Hub intact")
    expect_eventually("Hub Maiden", "execute in embervoid:hub if entity @e[type=embervoid:ashen_maiden]",
                      r"Test passed, count: 1$")
    # The Ruined Sanctum (PLAN 16.1, 16.3): the boss arena where the core was, the Fallen Sister not beaten there yet,
    # and no Ashen Maiden.
    if sanctum:
        x, z = sanctum
        near = f"x={x - 112},y=-64,z={z - 112},dx=224,dy=384,dz=224"
        expect("Sanctum arena", f"execute in minecraft:overworld run embervoid sanctum arena {x} {z}",
               r"^Sanctum arena at -?\d+ -?\d+ -?\d+, not cleared")
        expect("Sanctum without Maiden",
               f"execute in minecraft:overworld if entity @e[type=embervoid:ashen_maiden,{near}]", r"^Test failed")
    for night in ("full", "blood"):
        run(f"Moon {night}", f"embervoid moon start {night}")
        run(f"Moon {night} status", "embervoid moon status")
        if night == "blood":
            # No players: no Vampires, so nothing may hold the night.
            expect("Vampires without players", "embervoid vampire status", r", NONE, ")
        run(f"Moon {night} stop", "embervoid moon stop")
    exercise_player(rcon, run, expect, expect_eventually)
    time.sleep(GENERATION_WAIT)
    # The Citadel's treasure chest in the keep and its placed mobs (7 Marksmen, 5 Golems, 4 Knights), once generated.
    citadel = "execute in embervoid:ashen_reach_depths if "
    expect_eventually("Citadel treasure", citadel + "block 0 89 -265 minecraft:chest", r"Test passed")
    for mob, count in (("sparking_marksman", 7), ("furnace_golem", 5), ("smoldering_knight", 4)):
        expect_eventually(f"Citadel {mob}", citadel + f"entity @e[type=embervoid:{mob},tag=embervoid.placed]",
                          rf"Test passed, count: {count}$")
    expect("Road outposts built", "execute in embervoid:ashen_reach run embervoid realm roads",
           r"^(?![\s\S]*(missing|not loaded))3 roads")
    # Every post of every road generated with its lantern; a road of ~600 blocks has ~25.
    road_lit = r"\nroad \d+: [^\n]*?, ([2-9]\d|1[5-9]) posts \(\{} lit\)[^\n]*"
    expect_eventually("Road posts lit", "execute in embervoid:ashen_reach run embervoid realm roads",
                      r"^3 roads" + "".join(road_lit.format(g) for g in (1, 2, 3)) + "$")
    run("Save", "save-all flush")
    return log


def uuid_hex(response):
    """The UUID in a `data get entity ... UUID` answer ([I; a, b, c, d]) as 32 hex digits, or None."""
    match = re.search(r"\[I;\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]", response)
    if not match:
        return None
    return "".join(f"{int(part) & 0xFFFFFFFF:08x}" for part in match.groups())


def total_xp(rcon, name):
    """A player's exact XP points as the mod counts them (XpUtil, from `/embervoid stats`; vanilla's `xp query`
    floors the bar)."""
    numbers = parse_numbers(r"you have (\d+)\)", rcon.command(f"execute as {name} run embervoid stats"))
    return int(numbers[0]) if numbers else None


def exercise_player(rcon, run, expect, expect_eventually):
    """PLAN L6 (2.11, 3.8): a real player on the dedicated server. A bot joins; a stat is set (attributes and the sync
    packet), a point is bought at a summoned Ashen Maiden through her screen's real packets (XP taken exactly), and the
    bot dies with items and XP, respawns and takes everything back from its Ash Remnant by right-clicking it. Then it
    travels: overworld -> hub -> Realm 1 -> hub -> back to the overworld spot it left from (PLAN 15.2)."""
    name = "Bot"
    bot = Bot(name, SERVER_PORT)
    try:
        expect_eventually("Player joins", "list", rf"\b{name}\b", timeout=30)
        deadline = time.time() + 30
        while bot.pos is None and time.time() < deadline:
            time.sleep(0.5)
        run("Player survival", f"gamemode survival {name}")
        time.sleep(4)  # the join invulnerability and the first syncs
        for channel in ("embervoid:player_data", "embervoid:progress", "embervoid:stamina", "embervoid:config"):
            if channel not in bot.channels():
                FAILED_EXPECTATIONS.append(f"Player sync: {channel} not sent on join (got {sorted(set(bot.channels()))})")

        # Stats: a set stat changes the attribute and is synced to the player's client.
        since = len(bot.payloads)
        expect("Player setstat", f"execute as {name} run embervoid setstat vit 20", r"VIT 20")
        expect("Player max health", f"attribute {name} minecraft:generic.max_health get", r"is 30(\.0+)?\b")
        time.sleep(1)
        if "embervoid:player_data" not in bot.channels(since):
            FAILED_EXPECTATIONS.append("Player sync: setstat did not send player_data")

        # Leveling through the Maiden's screen: interact -> sheet; a level-up request -> one point for its exact cost.
        run("Player XP", f"xp set {name} 0 levels")
        run("Player XP", f"xp set {name} 0 points")
        run("Player XP", f"xp add {name} 500 points")
        stats = run("Player stats", f"execute as {name} run embervoid stats")
        cost = parse_numbers(r"next: (\d+) XP points, you have (\d+)", stats)
        run("Maiden", f"execute at {name} run summon embervoid:ashen_maiden ~2 ~ ~ {{NoAI:1b}}")
        maiden = uuid_hex(run("Maiden UUID", f"execute at {name} run data get entity "
                              "@e[type=embervoid:ashen_maiden,limit=1,sort=nearest] UUID"))
        time.sleep(2)
        if not cost or maiden not in bot.entities:
            FAILED_EXPECTATIONS.append(f"Leveling: no cost ({stats.strip()!r}) or the Maiden never spawned for the bot")
        else:
            maiden_id = bot.entities[maiden]
            since = len(bot.payloads)
            bot.interact(maiden_id)
            time.sleep(1)
            if "embervoid:level_up_sheet" not in bot.channels(since):
                FAILED_EXPECTATIONS.append("Leveling: right-clicking the Maiden sent no level-up sheet")
            before = total_xp(rcon, name)
            bot.send_payload("embervoid:level_up", Bot._varint(maiden_id) + Bot._string("vit"))
            time.sleep(1)
            after = total_xp(rcon, name)
            log_line = f"XP {before} -> {after}, cost {cost[0]}"
            run("Leveling", f"execute as {name} run embervoid stats")
            if before is None or after is None or before - after != cost[0]:
                FAILED_EXPECTATIONS.append(f"Leveling: the level-up took the wrong XP ({log_line})")
            expect("Leveling max health", f"attribute {name} minecraft:generic.max_health get", r"is 30\.5|is 30\.50")
        run("Maiden gone", "kill @e[type=embervoid:ashen_maiden]")

        # Death -> Ash Remnant -> recovery by right-clicking it.
        run("Remnant XP", f"xp set {name} 0 levels")
        run("Remnant XP", f"xp set {name} 0 points")
        run("Remnant XP", f"xp add {name} 150 points")
        run("Remnant items", f"clear {name} minecraft:diamond")
        run("Remnant items", f"give {name} minecraft:diamond 7")
        run("Remnant death", f"kill {name}")
        time.sleep(1)
        expect("Remnant placed", "execute if entity @e[type=embervoid:ash_remnant]", r"Test passed, count: 1$")
        bot.respawn()
        time.sleep(2)
        expect("Remnant: items gone", f"clear {name} minecraft:diamond 0", r"No items were found|Found 0")
        run("Remnant walk", f"execute at @e[type=embervoid:ash_remnant,limit=1] run tp {name} ~1 ~ ~")
        remnant = uuid_hex(run("Remnant UUID", "data get entity @e[type=embervoid:ash_remnant,limit=1] UUID"))
        time.sleep(2)
        if remnant not in bot.entities:
            FAILED_EXPECTATIONS.append("Remnant: never spawned for the bot")
        else:
            bot.interact(bot.entities[remnant])
            time.sleep(1)
            expect("Remnant: items back", f"clear {name} minecraft:diamond 0", r"Found 7 matching item")
            xp = total_xp(rcon, name)
            if xp != 150:
                FAILED_EXPECTATIONS.append(f"Remnant: XP back {xp}, expected 150")
            expect("Remnant gone", "execute unless entity @e[type=embervoid:ash_remnant]", r"Test passed")

        # Realm travel through the hub (PLAN 15.2): the first hub visit unlocks Realm 1, the realm's way out is the
        # hub, the hub's way out (no lit stele) is the overworld spot the bot left from.
        travel = [
            ("Travel: to the hub", "embervoid hub", "embervoid:hub"),
            ("Travel: into Realm 1", "embervoid realm enter embervoid:realm_1", "embervoid:ashen_reach"),
            ("Travel: out of the realm", "embervoid realm leave", "embervoid:hub"),
        ]
        for label, command, dimension in travel:
            run(label, f"execute as {name} run {command}")
            time.sleep(1)
            expect(label, f"data get entity {name} Dimension", rf'"{dimension}"')
            if dimension == "embervoid:hub":
                expect("Travel: Realm 1 unlocked", f"embervoid progress {name}", r"unlockedRealms=\[embervoid:realm_1")
        expect("Travel: out of the hub", f"execute as {name} run embervoid hub leave", r"To minecraft:overworld ")
        time.sleep(1)
        expect("Travel: back in the overworld", f"data get entity {name} Dimension", r'"minecraft:overworld"')
        if bot.reason:
            FAILED_EXPECTATIONS.append(f"bot {name} disconnected: {bot.reason}")
    finally:
        bot.close()


def perf_areas(rcon, log):
    """The areas to measure: (label, dimension, (x1, z1, x2, z2)), located on this world's seed."""
    def run(label, command):
        response = rcon.command(command)
        log.append((label, command, response.strip()))
        return response

    def around(pos, radius):
        return (pos[0] - radius, pos[1] - radius, pos[0] + radius, pos[1] + radius)

    reach, depths = "embervoid:ashen_reach", "embervoid:ashen_reach_depths"
    # Vanilla terrain far from spawn, as a reference for the generation times.
    areas = [("Overworld (vanilla)", "minecraft:overworld", around((4000, 4000), 48)),
             ("Realm landing", reach, around((0, 0), 48))]
    for arena, zone in (("a", "Fields"), ("b", "Woods"), ("c", "Necropolis")):
        pos = located(run(f"Locate arena {arena.upper()}", f"execute in {reach} run locate structure "
                          f"embervoid:ashen_reach_arena_{arena}"))
        if pos:
            areas.append((f"{zone} arena", reach, around(pos, 48)))
        else:
            FAILED_EXPECTATIONS.append(f"Arena {arena.upper()}: not located")
    for structure in ("fields/barn", "woods/hanging_cages", "necropolis/chapel", "necropolis/crypt"):
        pos = located(run(f"Locate {structure}", f"execute in {reach} run locate structure embervoid:ashen_reach/{structure}"))
        if pos:
            areas.append((f"Minor {structure}", reach, around(pos, 32)))
        else:
            FAILED_EXPECTATIONS.append(f"Minor {structure}: not located")
    areas.append(("Depths landing", depths, around((0, 0), 48)))
    pos = located(run("Locate depths arena", f"execute in {depths} run locate structure embervoid:ashen_reach_depths_arena"))
    if pos:
        areas.append(("Depths arena", depths, around(pos, 48)))
    areas.append(("Smoldering Citadel", depths, (-76, -272, 76, -30)))
    return areas


def parse_numbers(pattern, response):
    match = re.search(pattern, response)
    return [float(g) for g in match.groups()] if match else None


def exercise_perf(rcon):
    """Measures generation and tick time per area (see the module doc); returns (log, results)."""
    log, results = [], []

    def ticks(label):
        response = rcon.command("embervoid perf ticks")
        log.append((label, "embervoid perf ticks", response.strip()))
        numbers = parse_numbers(r"mean ([\d.]+) ms, p95 ([\d.]+) ms, max ([\d.]+) ms, chunks (\d+)", response)
        if numbers is None:
            FAILED_EXPECTATIONS.append(f"{label}: no tick times in {response.strip()!r}")
            return {}
        return {"mspt_mean": numbers[0], "mspt_p95": numbers[1], "mspt_max": numbers[2], "loaded_chunks": int(numbers[3])}

    def entities(label, dimension, box):
        x1, z1, x2, z2 = box
        command = f"execute in {dimension} run embervoid perf entities {x1} {z1} {x2} {z2}"
        response = rcon.command(command).strip()
        log.append((label, command, response))
        match = re.search(r"entities (\d+), mobs (\d+), placed (\d+)(?:: (.*))?", response)
        if not match:
            FAILED_EXPECTATIONS.append(f"{label}: no entity count in {response!r}")
            return {}
        return {"entities": int(match.group(1)), "mobs": int(match.group(2)), "placed": int(match.group(3)),
                "types": match.group(4) or ""}

    def load(label, dimension, box):
        """Force-loads the box; seconds until every chunk in it ticks entities, or None on a timeout."""
        x1, z1, x2, z2 = box
        started = time.time()
        rcon.command(f"execute in {dimension} run forceload add {x1} {z1} {x2} {z2}")
        command = f"execute in {dimension} run embervoid perf chunks {x1} {z1} {x2} {z2}"
        while time.time() - started < PERF_GEN_TIMEOUT:
            response = rcon.command(command)
            numbers = parse_numbers(r"chunks (\d+)/(\d+) loaded, (\d+) ticking", response)
            if numbers and numbers[2] == numbers[1]:
                elapsed = time.time() - started
                log.append((label, command, f"{response.strip()} after {elapsed:.1f} s"))
                return elapsed, int(numbers[1])
            time.sleep(0.25)
        FAILED_EXPECTATIONS.append(f"{label}: chunks not ticking after {PERF_GEN_TIMEOUT} s")
        return None, 0

    def center(box):
        return (box[0] + box[2]) // 2, (box[1] + box[3]) // 2

    def place(bot, dimension, box):
        """Puts the bot on the ground in the middle of the box, alive and unhurtable (mobs still go for it)."""
        if bot.reason:
            FAILED_EXPECTATIONS.append(f"bot {bot.name} disconnected: {bot.reason}")
            return False
        health = rcon.command(f"data get entity {bot.name} Health")
        if "Health: 0.0f" in health or "No entity" in health:
            bot.respawn()
            time.sleep(1)
        x, z = center(box)
        response = rcon.command(f"execute in {dimension} run spreadplayers {x} {z} 0 8 false {bot.name}")
        log.append((f"Bot {bot.name}", "spreadplayers", response.strip()))
        for effect in ("resistance", "regeneration", "fire_resistance", "water_breathing", "saturation"):
            rcon.command(f"effect give {bot.name} minecraft:{effect} infinite 4 true")
        return "Spread" in response

    def surroundings_ready(placed):
        """Seconds until the chunks within 8 chunks of each placed bot tick (loaded and generated)."""
        started = time.time()
        pending = list(placed)
        while pending and time.time() - started < PERF_ARRIVAL_TIMEOUT:
            dimension, box = pending[0]
            x, z = center(box)
            response = rcon.command(f"execute in {dimension} run embervoid perf chunks {x - 128} {z - 128} {x + 128} {z + 128}")
            numbers = parse_numbers(r"chunks (\d+)/(\d+) loaded, (\d+) ticking", response)
            if numbers and numbers[2] == numbers[1]:
                pending.pop(0)
            else:
                time.sleep(0.5)
        if pending:
            FAILED_EXPECTATIONS.append(f"player surroundings not ticking after {PERF_ARRIVAL_TIMEOUT} s")
        return time.time() - started

    # Phase 1, no players: each area generated alone, then its tick time and entities.
    time.sleep(PERF_SETTLE)
    results.append({"area": "Idle (spawn chunks only)", "load_average": os.getloadavg(), **ticks("Idle")})
    areas = perf_areas(rcon, log)
    by_area = {}
    for label, dimension, box in areas:
        seconds, chunks = load(label, dimension, box)
        time.sleep(PERF_SETTLE)
        result = {"area": label, "dimension": dimension, "box": box, "chunks": chunks, "generate_s": seconds,
                  **ticks(label), **entities(label, dimension, box)}
        results.append(result)
        by_area[label] = result
        x1, z1, x2, z2 = box
        rcon.command(f"execute in {dimension} run forceload remove {x1} {z1} {x2} {z2}")
        time.sleep(3)

    # Phase 2, one player at a time: it arrives, its view distance loads (and generates), then the reading.
    bots = []
    try:
        for i in range(3):
            bots.append(Bot(f"perf_bot{i + 1}", SERVER_PORT))
        time.sleep(3)
        for bot in bots:
            rcon.command(f"gamemode survival {bot.name}")
    except (OSError, RuntimeError) as e:
        FAILED_EXPECTATIONS.append(f"bots: {e}")
    time.sleep(PERF_PLAYER_SETTLE)
    results.append({"area": f"Idle, {len(bots)} players at spawn", **ticks("Idle + players")})
    for label, dimension, box in areas:
        if not bots or not place(bots[0], dimension, box):
            continue
        arrival = surroundings_ready([(dimension, box)])
        time.sleep(PERF_SETTLE)
        by_area[label]["with_player"] = {"arrival_s": arrival, **ticks(label + " + player"),
                                         **entities(label + " + player", dimension, box)}
    # Phase 3: everything force-loaded, three players in three places.
    started = time.time()
    total = 0
    for label, dimension, box in areas:
        total += load(label, dimension, box)[1]
    loaded = time.time() - started
    time.sleep(PERF_SETTLE)
    everything = {"area": "All areas at once", "chunks": total, "load_s": loaded, **ticks("All areas")}
    counts = [entities(label, dimension, box) for label, dimension, box in areas]
    for key in ("entities", "mobs", "placed"):
        everything[key] = sum(c.get(key, 0) for c in counts)
    if len(bots) == 3:
        spots = [areas[1], next(a for a in areas if a[0] == "Minor necropolis/crypt"), areas[-1]]
        if all(place(bot, dimension, box) for bot, (_, dimension, box) in zip(bots, spots)):
            arrival = surroundings_ready([(dimension, box) for _, dimension, box in spots])
            time.sleep(PERF_SETTLE)
            everything["with_players"] = {"players": [spot[0] for spot in spots], "arrival_s": arrival,
                                          **ticks("All areas + 3 players")}
    everything["load_average"] = os.getloadavg()
    results.append(everything)
    for label, dimension, box in areas:
        x1, z1, x2, z2 = box
        rcon.command(f"execute in {dimension} run forceload remove {x1} {z1} {x2} {z2}")
    # Phase 4: a real boss fight in each arena, one player fighting (the bot cannot be hurt and never hits back).
    if bots:
        bot = bots[0]
        for dimension in ("embervoid:ashen_reach", "embervoid:ashen_reach_depths"):
            if not place(bot, dimension, (-8, -8, 8, 8)):
                continue
            listing = rcon.command(f"execute as {bot.name} run embervoid realm arenas")
            # RCON joins the command's lines without a separator.
            for boss, x, z in re.findall(r"([a-z0-9_.-]+:[a-z0-9_./-]+) at (-?\d+), (-?\d+) \(level", listing):
                results.append(boss_fight(rcon, bot, dimension, boss, int(x), int(z), place, surroundings_ready, ticks,
                                          entities, log))
            place(bot, dimension, (-8, -8, 8, 8))
    for bot in bots:
        bot.close()
    run_save = rcon.command("save-all flush")
    log.append(("Save", "save-all flush", run_save.strip()))
    return log, results


def boss_fight(rcon, bot, dimension, boss, x, z, place, surroundings_ready, ticks, entities, log):
    """The bot steps into the arena at (x, z); once the fight runs, tick time and the arena's entities mid-fight."""
    label = f"Fight {boss}"
    box = (x - 48, z - 48, x + 48, z + 48)
    result = {"area": label, "dimension": dimension, "box": box}
    if not place(bot, dimension, (x - 2, z - 2, x + 2, z + 2)):
        return result
    surroundings_ready([(dimension, box)])
    deadline = time.time() + 30
    while time.time() < deadline and "1 fight(s)" not in rcon.command("embervoid boss fights"):
        time.sleep(0.5)
    fights = rcon.command("embervoid boss fights").strip()
    log.append((label, "embervoid boss fights", fights))
    if "1 fight(s)" not in fights:
        FAILED_EXPECTATIONS.append(f"{label}: no fight started for a player in the arena ({fights!r})")
        return result
    time.sleep(PERF_FIGHT)
    result.update({**ticks(label), **entities(label, dimension, box), "fight": fights.splitlines()[0]})
    return result


def print_perf(results):
    print(f"\n  {'Area (gen s: generation; + player: arrival)':42} chunks   gen s   MSPT mean   p95    max   entities  mobs  placed")
    for r in results:
        gen = r.get("generate_s", r.get("load_s"))
        print(f"  {r['area'][:42]:42} {r.get('chunks', ''):>6} {'' if gen is None else f'{gen:7.1f}':>7}"
              f" {r.get('mspt_mean', 0):9.2f} {r.get('mspt_p95', 0):6.2f} {r.get('mspt_max', 0):6.2f}"
              f" {r.get('entities', ''):>9} {r.get('mobs', ''):>5} {r.get('placed', ''):>6}")
        for key in ("with_player", "with_players"):
            w = r.get(key)
            if w:
                arrival = w.get("arrival_s")
                print(f"    {'+ player' if key == 'with_player' else '+ 3 players':40} {'':>6} {'' if arrival is None else f'{arrival:7.1f}':>7}"
                      f" {w.get('mspt_mean', 0):9.2f} {w.get('mspt_p95', 0):6.2f} {w.get('mspt_max', 0):6.2f}"
                      f" {w.get('entities', ''):>9} {w.get('mobs', ''):>5} {w.get('placed', ''):>6}")
        if r.get("types"):
            print(f"      {r['types']}")


def server_pid():
    """Pid of the dedicated server java process running in RUN_DIR (the gradle task forks it), or None."""
    try:
        listing = subprocess.run(["jcmd", "-l"], capture_output=True, text=True, timeout=30).stdout
    except (OSError, subprocess.TimeoutExpired):
        return None
    for line in listing.splitlines():
        pid = line.split(" ", 1)[0]
        if "devlaunchinjector" not in line or not pid.isdigit():
            continue
        cwd = subprocess.run(["lsof", "-a", "-p", pid, "-d", "cwd", "-Fn"], capture_output=True, text=True).stdout
        if any(l[1:] == os.path.realpath(RUN_DIR) for l in cwd.splitlines() if l.startswith("n")):
            return pid
    return None


def profile_hot_spots(limit=25):
    """The mod's methods with the most JFR execution samples: (self, total, method) by thread kind."""
    output = subprocess.run(["jfr", "print", "--events", "jdk.ExecutionSample", "--stack-depth", "64", PERF_JFR],
                            capture_output=True, text=True).stdout
    summary = {}
    for event in output.split("jdk.ExecutionSample")[1:]:
        thread = re.search(r'sampledThread = "([^"]+)"', event)
        kind = "server thread" if thread and thread.group(1) == "Server thread" else "workers"
        frames = re.findall(r"^\s+([\w.$]+)\(", event, re.MULTILINE)
        stats = summary.setdefault(kind, {"samples": 0, "self": {}, "total": {}})
        stats["samples"] += 1
        if frames and frames[0].startswith("com.embervoid"):
            stats["self"][frames[0]] = stats["self"].get(frames[0], 0) + 1
        for frame in set(f for f in frames if f.startswith("com.embervoid")):
            stats["total"][frame] = stats["total"].get(frame, 0) + 1
    lines = []
    for kind, stats in summary.items():
        lines.append(f"{kind}: {stats['samples']} samples")
        for frame, count in sorted(stats["total"].items(), key=lambda e: -e[1])[:limit]:
            lines.append(f"  {100 * count / stats['samples']:5.1f} % total {100 * stats['self'].get(frame, 0) / stats['samples']:5.1f} % self  {frame}")
    return lines


def scan():
    problems, warnings = [], []
    with open(OUTPUT, errors="replace") as f:
        lines = f.read().splitlines()
    for i, line in enumerate(lines):
        if any(p.search(line) for p in PROBLEMS) and not any(p.search(line) for p in IGNORED):
            # Keep the first lines of a stack trace with the problem.
            context = [line] + [l for l in lines[i + 1:i + 4] if l.startswith(("\t", "Caused by", "java.", "net."))]
            problems.append("\n    ".join(context))
        elif WARNING.search(line) and not any(p.search(line) for p in IGNORED_WARNINGS):
            warnings.append(line)
    return problems, warnings


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--accept-eula", action="store_true", help="accept the Minecraft EULA for run/server-check")
    parser.add_argument("--keep-world", action="store_true", help="reuse the previous world instead of a fresh one")
    parser.add_argument("--perf", action="store_true", help="measure generation and tick times instead of the check")
    parser.add_argument("--profile", action="store_true", help="with --perf: record a JFR profile of the server")
    args = parser.parse_args()

    password = prepare(args.accept_eula, args.keep_world)
    started = time.time()
    with open(OUTPUT, "w") as out:
        process = subprocess.Popen([os.path.join(ROOT, "gradlew"), "runServerCheck", "--console=plain"], cwd=ROOT,
                                   stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    commands, results, hot_spots = [], None, []
    done = wait_for(re.compile(r"Done \(([\d.]+)s\)!"), process, BOOT_TIMEOUT)
    if done:
        pid = server_pid() if args.profile else None
        if args.profile and pid is None:
            commands.append(("Profile", "", "server process not found, no profile"))
        if pid:
            subprocess.run(["jcmd", pid, "JFR.start", "name=embervoid", "settings=profile", f"filename={PERF_JFR}"],
                           capture_output=True)
        try:
            rcon = Rcon(RCON_PORT, password)
            try:
                if args.perf:
                    commands, results = exercise_perf(rcon)
                else:
                    commands = exercise(rcon)
                if pid:
                    subprocess.run(["jcmd", pid, "JFR.stop", "name=embervoid"], capture_output=True)
                rcon.command("stop")
            finally:
                rcon.close()
        except (OSError, RuntimeError) as e:
            commands.append(("RCON", "", f"failed: {e}"))
        if pid and os.path.exists(PERF_JFR):
            hot_spots = profile_hot_spots()
    try:
        process.wait(STOP_TIMEOUT)
    except subprocess.TimeoutExpired:
        process.kill()
        commands.append(("Stop", "", "server did not stop in time, killed"))

    problems, warnings = scan()
    problems.extend(FAILED_EXPECTATIONS)
    if not done:
        problems.insert(0, "server never reached Done (crashed, failed to build or timed out)")
    print(f"Server check, {time.time() - started:.0f} s. Full output: {os.path.relpath(OUTPUT, ROOT)}")
    if done:
        print(f"Boot: Done in {done.group(1)} s")
    for label, command, response in commands:
        print(f"  {label}: {response or '(no answer)'}")
    if results is not None:
        with open(PERF_OUTPUT, "w") as f:
            json.dump({"boot_s": float(done.group(1)), "machine": machine(), "load_average_end": os.getloadavg(),
                       "areas": results}, f, indent=2)
        print(f"Load average (1, 5, 15 min) at the end: {', '.join(f'{v:.1f}' for v in os.getloadavg())}")
        print_perf(results)
        print(f"Results: {os.path.relpath(PERF_OUTPUT, ROOT)}")
    if hot_spots:
        print(f"\nHot spots (JFR execution samples, {os.path.relpath(PERF_JFR, ROOT)}):")
        for line in hot_spots:
            print("  " + line)
    if warnings:
        print(f"\nWarnings ({len(warnings)}):")
        for line in warnings[:40]:
            print("  " + line)
    if problems:
        print(f"\nFAIL: {len(problems)} problem(s)")
        for problem in problems[:40]:
            print("  " + problem)
        sys.exit(1)
    print("\nPASS: no errors, crashes or mixin failures in the log")


def machine():
    """CPU, cores and memory of this machine, for the perf write-up."""
    def sysctl(name):
        try:
            return subprocess.run(["sysctl", "-n", name], capture_output=True, text=True).stdout.strip()
        except OSError:
            return ""
    memory = sysctl("hw.memsize")
    return {"cpu": sysctl("machdep.cpu.brand_string") or platform.processor(), "cores": os.cpu_count(),
            "memory_gb": round(int(memory) / 2 ** 30) if memory.isdigit() else None, "os": platform.platform()}


if __name__ == "__main__":
    main()
