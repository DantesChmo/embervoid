#!/bin/sh
# HeadlessMc with Embervoid's settings: its own Minecraft dir (run/hmc, never the real launcher's), Java 17 from
# Homebrew (HeadlessMc's Java download is broken in 3.0.0-RC8), no JLine (stdin is a pipe when automated).
# Usage, from the repository root: tools/hmc.sh <headlessmc command...>, e.g. tools/hmc.sh version list
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA17="${EMBERVOID_JAVA17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
mkdir -p "$ROOT/run/hmc"
exec headlessmc -Dhmc.jline.enabled=false -Dhmc.files.mc="$ROOT/run/hmc" -Dhmc.java.download=false \
	-Dhmc.java.versions="$JAVA17" "$@"
