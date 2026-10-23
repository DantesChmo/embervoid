#!/usr/bin/env python3
"""Runs the mod in a real headless client (HeadlessMc + hmc-specifics), plays a scenario, scans the log.

Run from the repository root: python3 tools/client_check.py [scenario.txt]

Needs `headlessmc` on PATH (tools/hmc.sh sets it up: run/hmc as its Minecraft dir, Homebrew Java 17) and
run/hmc/downloads/hmc-specifics-fabric-1.20.1-1.2.2.jar (github.com/headlesshq/hmc-specifics, release v1.20.1-1.2.2).

What it does:
1. Builds the mod jar, puts it with the same fabric-api and GeckoLib as the build into run/hmc/fabric-1.20.1/mods.
2. Launches Fabric 1.20.1 with --headless --offline (nothing is rendered, no account), waits for the title screen,
   creates a fresh survival world with cheats on (saves/ is emptied first: this game dir is only for this check)
   and waits until the player has joined.
3. Plays the scenario: one hmc-specifics command per line (`msg "/embervoid stats"`, `gui`, `click-text Respawn`,
   `wait 5`, ...; `#` starts a comment). Prints every chat line and screen dump the client logs in between.
4. Quits and scans the client log for crashes, mixin failures and ERROR lines, minus the known noise of an offline
   headless client. Exit code 0 only if there are none.

Covers client mixins, client init, screens as data and chat output. Does NOT show anything rendered: models,
textures, animations and HUD look still need the in-game QA.
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HMC_DIR = os.path.join(ROOT, "run", "hmc")
GAME_DIR = os.path.join(HMC_DIR, "fabric-1.20.1")
OUTPUT = os.path.join(HMC_DIR, "client-check.log")
HMC_SPECIFICS = os.path.join(HMC_DIR, "downloads", "hmc-specifics-fabric-1.20.1-1.2.2.jar")
GRADLE_CACHE = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
WORLD = "Client Check"
STARTUP_TIMEOUT = 300
JOIN_TIMEOUT = 180

DEFAULT_SCENARIO = """
# Player systems
msg "/embervoid stats"
msg "/embervoid data"
# Realm travel
msg "/embervoid progress Offline unlock embervoid:realm_1"
msg "/embervoid realm enter embervoid:realm_1"
wait 10
msg "/embervoid realm where"
# Death into an Ash Remnant, death screen, respawn
msg "/give @s minecraft:diamond 7"
msg "/xp add @s 150 points"
msg "/kill @s"
wait 3
gui
click-text Respawn
wait 5
msg "/embervoid data"
msg "/xp query @s points"
"""

PROBLEMS = [
    re.compile(r"/(ERROR|FATAL)\]"),
    re.compile(r"FAILED during APPLY|was not applied|Mixin apply failed|InvalidInjectionException|MixinApplyError"),
    re.compile(r"---- Minecraft Crash Report ----|Unreported exception thrown|This crash report has been saved"),
    re.compile(r"Couldn't parse|Failed to parse|Couldn't load|Failed to load", re.IGNORECASE),
]
# Noise of an offline, headless client.
IGNORED = [
    re.compile(r"Failed to verify authentication|Failed to retrieve profile key pair"),
    re.compile(r"Error starting SoundSystem"),
    re.compile(r"Couldn't save auto screenshot"),
    re.compile(r"Invalid icon for world"),  # headless screenshots are 1x1
]


class Client:
    def __init__(self):
        self.out = open(OUTPUT, "w")
        self.process = subprocess.Popen(
            [os.path.join(ROOT, "tools", "hmc.sh"), "launch", "--headless", "--offline", "fabric", "1.20.1"],
            cwd=ROOT, stdin=subprocess.PIPE, stdout=self.out, stderr=subprocess.STDOUT, text=True)
        self.read_pos = 0

    def send(self, line):
        self.process.stdin.write(line + "\n")
        self.process.stdin.flush()

    def new_output(self):
        with open(OUTPUT, errors="replace") as f:
            f.seek(self.read_pos)
            text = f.read()
            self.read_pos = f.tell()
        return text

    def wait_for(self, pattern, timeout, poll=None):
        """Waits for a line matching `pattern` in new output, sending `poll` every few seconds; the match or None."""
        deadline = time.time() + timeout
        seen = ""
        while time.time() < deadline and self.process.poll() is None:
            if poll:
                self.send(poll)
            time.sleep(3)
            seen += self.new_output()
            match = pattern.search(seen)
            if match:
                return match
        return None

    def screen(self):
        """The current screen dump of `gui` as {button text: id}."""
        self.new_output()
        self.send("gui")
        time.sleep(2)
        buttons = {}
        for line in self.new_output().splitlines():
            match = re.match(r"^(\d+)\s{2,}(.+?)\s{2,}-?\d+\s+-?\d+\s+\d+\s+\d+", line)
            if match:
                buttons[match.group(2).strip()] = match.group(1)
        return buttons

    def click_text(self, prefix):
        """Clicks the first button whose text starts with `prefix`. Widget ids change when a screen re-initialises
        (e.g. the title screen after the resource reload), so a click on a stale id is retried with a fresh dump."""
        buttons = {}
        for _ in range(5):
            buttons = self.screen()
            widget = next((widget for text, widget in buttons.items() if text.startswith(prefix)), None)
            if widget is None:
                time.sleep(2)
                continue
            self.send(f"click {widget}")
            time.sleep(2)
            if "Couldn't find GuiElement" not in self.new_output():
                return True
        print(f"  ! could not click {prefix!r}; buttons on screen: {sorted(buttons)}")
        return False

    def quit(self):
        if self.process.poll() is None:
            self.send("quit")
            try:
                self.process.wait(60)
            except subprocess.TimeoutExpired:
                self.process.kill()
        self.out.close()


def prepare():
    if not shutil.which("headlessmc"):
        sys.exit("headlessmc is not on PATH (see tools/hmc.sh)")
    if not os.path.exists(HMC_SPECIFICS):
        sys.exit(f"missing {os.path.relpath(HMC_SPECIFICS, ROOT)} (hmc-specifics release v1.20.1-1.2.2)")
    if subprocess.run([os.path.join(ROOT, "gradlew"), "remapJar", "-q"], cwd=ROOT).returncode != 0:
        sys.exit("build failed")
    props = dict(line.strip().split("=", 1) for line in open(os.path.join(ROOT, "gradle.properties"))
                 if "=" in line and not line.startswith("#"))
    jars = [
        os.path.join(ROOT, "build", "libs", f"{props['archives_base_name']}-{props['mod_version']}.jar"),
        HMC_SPECIFICS,
        *glob.glob(f"{GRADLE_CACHE}/net.fabricmc.fabric-api/fabric-api/{props['fabric_version']}/*/"
                   f"fabric-api-{props['fabric_version']}.jar"),
        *glob.glob(f"{GRADLE_CACHE}/software.bernie.geckolib/geckolib-fabric-{props['minecraft_version']}/"
                   f"{props['geckolib_version']}/*/geckolib-fabric-*-{props['geckolib_version']}.jar"),
    ]
    if len(jars) != 4:
        sys.exit("fabric-api or GeckoLib jar not in the Gradle cache; run ./gradlew build once")
    mods = os.path.join(GAME_DIR, "mods")
    shutil.rmtree(mods, ignore_errors=True)
    os.makedirs(mods)
    for jar in jars:
        shutil.copy(jar, mods)
    with open(os.path.join(GAME_DIR, "options.txt"), "w") as f:
        f.write("pauseOnLostFocus:false\nonboardAccessibility:false\nnarrator:0\nsoundCategory_master:0.0\n")
    shutil.rmtree(os.path.join(GAME_DIR, "saves"), ignore_errors=True)


def create_world(client):
    if not client.click_text("Singleplayer"):
        return False
    buttons = client.screen()
    world_field = next((widget for text, widget in buttons.items() if text == "New World"), None)
    if world_field:
        client.send(f'text {world_field} "{WORLD}"')
    if "Allow Cheats: OFF" in buttons:
        client.click_text("Allow Cheats")
    client.click_text("Create New World")
    return client.wait_for(re.compile(r"joined the game"), JOIN_TIMEOUT) is not None


def play(client, scenario):
    for raw in scenario.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        print(f"> {line}")
        if line.startswith("wait "):
            time.sleep(float(line.split()[1]))
        elif line.startswith("click-text "):
            client.click_text(line[len("click-text "):])
        else:
            client.send(line)
            time.sleep(2.5)
        for out in client.new_output().splitlines():
            if "[CHAT]" in out:
                print("    " + out.split("[CHAT] ", 1)[1])
            elif re.match(r"^(Screen:|\d+\s{2,})", out) or "/ERROR]" in out:
                print("    " + out)


def scan():
    problems = []
    with open(OUTPUT, errors="replace") as f:
        lines = f.read().splitlines()
    for i, line in enumerate(lines):
        if any(p.search(line) for p in PROBLEMS) and not any(p.search(line) for p in IGNORED):
            context = [line] + [l for l in lines[i + 1:i + 4] if l.startswith(("\t", "Caused by", "java.", "net."))]
            problems.append("\n    ".join(context))
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("scenario", nargs="?", help="file with one hmc-specifics command per line")
    args = parser.parse_args()
    scenario = open(args.scenario).read() if args.scenario else DEFAULT_SCENARIO

    prepare()
    started = time.time()
    client = Client()
    try:
        title = client.wait_for(re.compile(r"Screen: net\.minecraft\.class_442"), STARTUP_TIMEOUT, poll="gui")
        joined = title and create_world(client)
        if joined:
            print(f"In world after {time.time() - started:.0f} s")
            play(client, scenario)
    finally:
        client.quit()

    problems = scan()
    if not title:
        problems.insert(0, "client never reached the title screen")
    elif not joined:
        problems.insert(0, "could not create or join the world")
    print(f"\nClient check, {time.time() - started:.0f} s. Full log: {os.path.relpath(OUTPUT, ROOT)}")
    if problems:
        print(f"FAIL: {len(problems)} problem(s)")
        for problem in problems[:40]:
            print("  " + problem)
        sys.exit(1)
    print("PASS: no errors, crashes or mixin failures in the client log")


if __name__ == "__main__":
    main()
