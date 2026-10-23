#!/usr/bin/env python3
"""Writes the player attack animations (PLAN 17.1) as playerAnimator JSON (the Emotecraft format) into
src/main/resources/assets/embervoid/player_animation/. Run from the repository root:

    python3 tools/player_anim_art.py            write every clip
    python3 tools/player_anim_art.py --check    only compare: exit 1 if a file differs from what would be written

The names and lengths match com.embervoid.anim.WeaponKind (WeaponKindTest checks it). A swing is as long as the
weapon's attack cooldown: the blow lands within the first 2-4 ticks (the hit is dealt when the click arrives) and the
rest is the recovery into the resting pose (the last key, BLEND ticks before the end); playerAnimator then blends
from it back into the vanilla pose. A changed attack speed only moves the last key.

Conventions (vanilla model space, degrees; playerAnimator sets these as the part's rotation, not on top of vanilla's):
  rightArm / leftArm  pitch -90 points the arm forward, -180 straight up; yaw > 0 swings a forward arm to the player's
                      right; roll > 0 lifts the right arm sideways (the left arm: roll < 0); z < 0 pushes the shoulder
                      forward (pixels)
  rightItem           the held item turned in the hand. A vanilla-held blade stands 80 degrees off the arm line;
                      pitch -80 lays it along the arm (a spear), about -40 gives a cutting angle, and pitch = arm
                      pitch + 10 points it straight ahead (the rapier)
  body                the whole player, turned around a point 0.7 blocks up: yaw > 0 turns to the player's left,
                      pitch < 0 leans forward
  rightLeg / leftLeg  pitch < 0 steps forward
Parts a clip does not key stay vanilla (legs walk, the head looks). Mirrored on the client for the left arm.
"""
import json
import os
import sys

OUT = os.path.join("src", "main", "resources", "assets", "embervoid", "player_animation")
# Ticks from the last key (endTick) to the end (stopTick), where playerAnimator blends back into the vanilla pose. It
# always keeps at least 1 (a stopTick at or before endTick becomes endTick + 3).
BLEND = 3

# The resting pose a swing returns to: the arm holding an item (vanilla ArmPose.ITEM), the other arm down.
REST = {"rightArm": {"pitch": -18, "yaw": 0, "roll": 0, "z": 0}, "leftArm": {"pitch": 0, "yaw": 0, "roll": 0, "z": 0},
        "body": {"pitch": 0, "yaw": 0, "roll": 0}, "rightItem": {"pitch": 0, "yaw": 0, "roll": 0},
        "rightLeg": {"pitch": 0}, "leftLeg": {"pitch": 0}}


def arm(pitch, yaw=0, roll=0, z=0):
    return {"pitch": pitch, "yaw": yaw, "roll": roll, "z": z}


def body(yaw=0, pitch=0, roll=0):
    return {"pitch": pitch, "yaw": yaw, "roll": roll}


def item(pitch=0, yaw=0, roll=0):
    return {"pitch": pitch, "yaw": yaw, "roll": roll}


def leg(pitch):
    return {"pitch": pitch}


# name: (ticks, description, [(tick, easing into this key, {part: axes})]); without a key at ticks - BLEND the clip ends
# in REST there.
CLIPS = {
    # Sanctum Longsword (speed 1.6): a forehand diagonal, then a backhand rising cut.
    "sword_swing_1": (13, "Longsword forehand: high right to low left", [
        (0, "linear", {"rightItem": item(-10), "rightArm": arm(-150, 50, 20),
                       "leftArm": arm(10, 0, -10), "body": body(-20)}),
        (3, "outQuad", {"rightItem": item(-45), "rightArm": arm(-60, -50, -10),
                        "leftArm": arm(-20, 0, -15), "body": body(25)}),
        (6, "outSine", {"rightItem": item(-40), "rightArm": arm(-35, -60, -5),
                        "leftArm": arm(-10, 0, -10), "body": body(30)}),
    ]),
    "sword_swing_2": (13, "Longsword backhand: low left to high right", [
        (0, "linear", {"rightItem": item(-40), "rightArm": arm(-40, -70, -10),
                       "leftArm": arm(-10, 0, -10), "body": body(30)}),
        (3, "outQuad", {"rightItem": item(-45), "rightArm": arm(-110, 45, 25),
                        "leftArm": arm(15, 0, -15), "body": body(-20)}),
        (6, "outSine", {"rightItem": item(-35), "rightArm": arm(-120, 60, 30),
                        "leftArm": arm(10, 0, -10), "body": body(-25)}),
    ]),
    "sword_wide_cleave": (14, "Wide Cleave: one flat swing across the whole front", [
        (0, "linear", {"rightItem": item(-30), "rightArm": arm(-85, 90, 30),
                       "leftArm": arm(-20, 0, -20), "body": body(-40)}),
        (3, "outCubic", {"rightItem": item(-50), "rightArm": arm(-85, -80, -20),
                         "leftArm": arm(10, 0, -25), "body": body(40)}),
        (7, "outSine", {"rightItem": item(-45), "rightArm": arm(-70, -95, -15),
                        "leftArm": arm(5, 0, -20), "body": body(45)}),
    ]),
    # Royal Ash Rapier (speed 2.2, a 10-tick cooldown as at 2.0): the duellist's thrust, blade laid forward along the arm.
    "rapier_thrust": (10, "Rapier thrust: chambered at the hip, straight out", [
        (0, "linear", {"rightArm": arm(-70, 15, 0, 3), "rightItem": item(-60), "leftArm": arm(20, 0, -25),
                       "body": body(-15)}),
        (2, "outQuad", {"rightArm": arm(-88, -5, 0, -3), "rightItem": item(-78), "leftArm": arm(30, 0, -30),
                        "body": body(15)}),
        (4, "outSine", {"rightArm": arm(-85, -5, 0, -2), "rightItem": item(-75), "leftArm": arm(28, 0, -28),
                        "body": body(12)}),
    ]),
    "rapier_lunge": (12, "Lunge: a long step with the thrust at its end", [
        (0, "linear", {"rightArm": arm(-60, 20, 0, 4), "rightItem": item(-50), "leftArm": arm(25, 0, -30),
                       "body": body(-25, 5), "rightLeg": leg(0), "leftLeg": leg(0)}),
        (3, "outQuad", {"rightArm": arm(-88, -5, 0, -4), "rightItem": item(-78), "leftArm": arm(40, 0, -35),
                        "body": body(20, -15), "rightLeg": leg(-45), "leftLeg": leg(35)}),
        (7, "outSine", {"rightArm": arm(-86, -5, 0, -3), "rightItem": item(-76), "leftArm": arm(38, 0, -32),
                        "body": body(18, -12), "rightLeg": leg(-40), "leftLeg": leg(30)}),
    ]),
    # Ashen Scythe (speed 1.1): both hands on the snath, a low sweep through everything in front.
    "scythe_sweep": (19, "Scythe sweep: two hands, wide and low, right to left", [
        (0, "linear", {"rightItem": item(-30), "rightArm": arm(-60, 70, 25),
                       "leftArm": arm(-50, 40, 10), "body": body(-40)}),
        (4, "outCubic", {"rightItem": item(-50), "rightArm": arm(-70, -70, -15),
                         "leftArm": arm(-60, -85, -25), "body": body(45)}),
        (8, "outSine", {"rightItem": item(-45), "rightArm": arm(-55, -80, -15),
                        "leftArm": arm(-45, -90, -25), "body": body(50)}),
    ]),
    # The turn runs from -360 up to 0 (to the left): the blend at the end goes from the last key's raw value, so a
    # turn ending on 360 would unwind.
    "scythe_harvest": (16, "Harvest: a full turn with the blade held out", [
        (0, "linear", {"rightItem": item(-50), "rightArm": arm(-80, 60, 30),
                       "leftArm": arm(-70, 30, 10), "body": body(-360)}),
        (3, "inQuad", {"rightItem": item(-50), "rightArm": arm(-82, 40, 30),
                       "leftArm": arm(-72, 15, 10), "body": body(-240)}),
        (6, "linear", {"rightItem": item(-50), "rightArm": arm(-82, 20, 30),
                       "leftArm": arm(-72, 0, 10), "body": body(-120)}),
        (9, "outQuad", {"rightItem": item(-45), "rightArm": arm(-75, -30, 10),
                        "leftArm": arm(-65, -40, 0), "body": body(0)}),
    ]),
    # Ashbearer's Mace (speed 0.9): a heavy overhead blow.
    "mace_overhead": (23, "Mace overhead: raised high, brought straight down", [
        (0, "linear", {"rightItem": item(-30), "rightArm": arm(-170, 10, 10),
                       "leftArm": arm(-30, 0, -20), "body": body(0, 8)}),
        (4, "inQuad", {"rightItem": item(-60), "rightArm": arm(-45, -5, 0),
                       "leftArm": arm(10, 0, -15), "body": body(0, -12)}),
        (7, "outSine", {"rightItem": item(-55), "rightArm": arm(-35, -5, 0),
                        "leftArm": arm(10, 0, -12), "body": body(0, -10)}),
    ]),
    "mace_ground_slam": (18, "Ground Slam: both hands up, the mace driven into the ground", [
        (0, "linear", {"rightItem": item(-30), "rightArm": arm(-175, 0, 5),
                       "leftArm": arm(-160, 0, -10), "body": body(0, 10)}),
        (3, "inQuad", {"rightItem": item(-75), "rightArm": arm(-20, 0, 0),
                       "leftArm": arm(-30, 0, 0), "body": body(0, -25)}),
        (8, "linear", {"rightItem": item(-75), "rightArm": arm(-22, 0, 0),
                       "leftArm": arm(-28, 0, 0), "body": body(0, -22)}),
    ]),
    # Warden's Scourge (speed 1.0): the whip cracked from over the shoulder.
    "whip_crack": (20, "Whip crack: thrown from over the right shoulder, snapped down", [
        (0, "linear", {"rightItem": item(-20), "rightArm": arm(-150, 40, 30),
                       "leftArm": arm(10, 0, -15), "body": body(-20)}),
        (3, "outQuad", {"rightItem": item(-60), "rightArm": arm(-70, -10, 0),
                        "leftArm": arm(-10, 0, -15), "body": body(15)}),
        (5, "outSine", {"rightItem": item(-70), "rightArm": arm(-55, -20, -5),
                        "leftArm": arm(-5, 0, -12), "body": body(18)}),
    ]),
    "whip_lash": (14, "Lash: cast forward, then yanked back", [
        (0, "linear", {"rightItem": item(-70), "rightArm": arm(-95, 0, 0),
                       "leftArm": arm(0, 0, -15), "body": body(0, -5)}),
        (3, "outQuad", {"rightItem": item(-75), "rightArm": arm(-100, -10, 0, -2),
                        "leftArm": arm(-5, 0, -15), "body": body(5, -8)}),
        (6, "inQuad", {"rightItem": item(-30), "rightArm": arm(-40, 30, 20, 3),
                       "leftArm": arm(15, 0, -20), "body": body(-20, 8)}),
    ]),
    # Huntsman's Crossbow: the string drawn over the charge time; the shot kicks back.
    "crossbow_load": (32, "Crossbow load: the left hand draws the string to the body", [
        (0, "linear", {"rightArm": arm(-50, -20, 0), "leftArm": arm(-70, 30, 0), "body": body(0, 0)}),
        (16, "inOutSine", {"rightArm": arm(-55, -20, 0), "leftArm": arm(-40, 10, 0), "body": body(0, -5)}),
        (26, "inOutSine", {"rightArm": arm(-60, -15, 0), "leftArm": arm(-30, 0, 0), "body": body(0, -3)}),
        (29, "inOutSine", {"rightArm": arm(-90, -5, 0), "leftArm": arm(-90, 25, 0), "body": body(0, 0)}),
    ]),
    "crossbow_fire": (8, "Crossbow shot: the recoil throws the arms up", [
        (0, "linear", {"rightArm": arm(-90, -5, 0), "leftArm": arm(-90, 25, 0), "body": body(0, 0)}),
        (1, "outQuad", {"rightArm": arm(-110, -5, 0, 2), "leftArm": arm(-108, 25, 0, 2), "body": body(0, 6)}),
    ]),
}


def moves(ticks, keys):
    """playerAnimator moves: one entry per key and part. A clip without its own last key goes back to REST."""
    end = ticks - BLEND
    if keys[-1][0] > end:
        raise SystemExit("a key after tick %d of %d" % (end, ticks))
    if keys[-1][0] != end:
        parts = sorted({p for _, _, pose in keys for p in pose})
        keys = keys + [(end, "inOutSine", {p: REST[p] for p in parts})]
    out = []
    for tick, easing, pose in keys:
        for part in sorted(pose):
            out.append({"tick": tick, "easing": easing, part: dict(pose[part])})
    return out


def clip_json(name, ticks, description, keys):
    return {
        "name": name,
        "description": description,
        "author": "Embervoid",
        "emote": {
            "beginTick": 0, "endTick": ticks - BLEND, "stopTick": ticks, "isLoop": False, "returnTick": 0,
            "degrees": True, "easeBeforeKeyframe": True,
            "moves": moves(ticks, keys),
        },
    }


def main(argv):
    check = "--check" in argv
    stale = []
    os.makedirs(OUT, exist_ok=True)
    for name, (ticks, description, keys) in CLIPS.items():
        text = json.dumps(clip_json(name, ticks, description, keys), indent=2) + "\n"
        path = os.path.join(OUT, name + ".json")
        old = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if check:
            if old != text:
                stale.append(path)
        elif old != text:
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
            print("wrote", path)
    for path in stale:
        print("STALE", path)
    if stale:
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1:])
