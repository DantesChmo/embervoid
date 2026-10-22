#!/usr/bin/env python3
"""Boots a dedicated server with the mod, makes it generate the worldgen a player would hit first, and scans the log.

Run from the repository root: python3 tools/server_check.py [--accept-eula] [--keep-world]

What it does:
1. Starts `./gradlew runServerCheck` (run dir run/server-check, a fresh world each time unless --keep-world) with
   RCON on a local port, and waits for "Done".
2. Over RCON: locates the nearest Ember Sanctum and force-loads the chunks around it (so it is really generated in
   overworld terrain), force-loads the Ashen Reach landing and depths centre, locates the three zone arenas and the
   depths arena and force-loads them too, force-loads the Vampire arena, starts and stops a full moon and a blood
   new moon, waits for generation, saves, stops the server.
3. Scans the whole output (build + server log) for crashes, mixin failures, ERROR lines and datapack load errors.
   Exit code 0 only if there are none. WARN lines are listed but do not fail the check.

The dedicated server never loads client mixins or client classes: this catches common-code problems only.
The first run needs --accept-eula (Minecraft EULA, https://aka.ms/MinecraftEULA); it is remembered in the run dir.
"""
import argparse
import os
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUN_DIR = os.path.join(ROOT, "run", "server-check")
OUTPUT = os.path.join(RUN_DIR, "check.log")
SERVER_PORT = 25599
RCON_PORT = 25598
BOOT_TIMEOUT = 600
GENERATION_WAIT = 20
STOP_TIMEOUT = 120

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

    def generate_around(label, dimension, pos, radius=48):
        x, z = pos
        run(label, f"execute in {dimension} run forceload add {x - radius} {z - radius} {x + radius} {z + radius}")

    sanctum = located(run("Sanctum", "locate structure embervoid:ember_sanctum"))
    if sanctum:
        generate_around("Sanctum", "minecraft:overworld", sanctum, 64)
    generate_around("Ashen Reach landing", "embervoid:ashen_reach", (0, 0))
    for arena in ("a", "b", "c"):
        pos = located(run(f"Arena {arena.upper()}", "execute in embervoid:ashen_reach run locate structure "
                          f"embervoid:ashen_reach_arena_{arena}"))
        if pos:
            generate_around(f"Arena {arena.upper()}", "embervoid:ashen_reach", pos, 32)
    generate_around("Depths landing", "embervoid:ashen_reach_depths", (0, 0))
    pos = located(run("Depths arena", "execute in embervoid:ashen_reach_depths run locate structure "
                      "embervoid:ashen_reach_depths_arena"))
    if pos:
        generate_around("Depths arena", "embervoid:ashen_reach_depths", pos, 32)
    generate_around("Vampire arena", "embervoid:vampire_arena", (0, 0), 16)
    for night in ("full", "blood"):
        run(f"Moon {night}", f"embervoid moon start {night}")
        run(f"Moon {night} status", "embervoid moon status")
        run(f"Moon {night} stop", "embervoid moon stop")
    time.sleep(GENERATION_WAIT)
    run("Save", "save-all flush")
    return log


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
    args = parser.parse_args()

    password = prepare(args.accept_eula, args.keep_world)
    started = time.time()
    with open(OUTPUT, "w") as out:
        process = subprocess.Popen([os.path.join(ROOT, "gradlew"), "runServerCheck", "--console=plain"], cwd=ROOT,
                                   stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    commands = []
    done = wait_for(re.compile(r"Done \(([\d.]+)s\)!"), process, BOOT_TIMEOUT)
    if done:
        try:
            rcon = Rcon(RCON_PORT, password)
            try:
                commands = exercise(rcon)
                rcon.command("stop")
            finally:
                rcon.close()
        except (OSError, RuntimeError) as e:
            commands.append(("RCON", "", f"failed: {e}"))
    try:
        process.wait(STOP_TIMEOUT)
    except subprocess.TimeoutExpired:
        process.kill()
        commands.append(("Stop", "", "server did not stop in time, killed"))

    problems, warnings = scan()
    if not done:
        problems.insert(0, "server never reached Done (crashed, failed to build or timed out)")
    print(f"Server check, {time.time() - started:.0f} s. Full output: {os.path.relpath(OUTPUT, ROOT)}")
    if done:
        print(f"Boot: Done in {done.group(1)} s")
    for label, command, response in commands:
        print(f"  {label}: {response or '(no answer)'}")
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


if __name__ == "__main__":
    main()
