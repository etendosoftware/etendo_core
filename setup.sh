#!/bin/bash
# setup.sh — Bootstrap script for Etendo (macOS / Linux)
#
# Flow:
#   1. Require JAVA_HOME
#   2. If githubToken missing → GitHub Device Flow auth; abort on failure
#   3. Launch ./gradlew setup.web  (always — Gradle task handles resumption)
#
# State file: .setup-progress
#   Tracks completed phases so re-runs can continue from where they stopped.
#   Phases: auth | setup.web
#   Delete .setup-progress to start over.
#
# Usage:
#   ./setup.sh               # auto-detect last step and continue
#   ./setup.sh --fresh       # reset progress and start from scratch
#   ./setup.sh <task>        # run a specific gradle task directly

set -e

STATE_FILE=".setup-progress"
PROPS_FILE="gradle.properties"

# ── --fresh: reset state ──────────────────────────────────────────────────────
if [ "${1}" = "--fresh" ]; then
    rm -f "$STATE_FILE"
    # Clear githubToken so auth runs again
    sed -i.bak 's/^githubToken=.*/githubToken=/' "$PROPS_FILE" 2>/dev/null && rm -f "$PROPS_FILE.bak" || true
    echo "Progress reset. Starting fresh."
    exec "$0"
fi

TASK="${1:-setup.web}"

# ── Helpers ───────────────────────────────────────────────────────────────────
phase_done() { grep -q "^$1=done" "$STATE_FILE" 2>/dev/null; }
mark_done()  { grep -q "^$1=done" "$STATE_FILE" 2>/dev/null || echo "$1=done" >> "$STATE_FILE"; }

# ── 1. Require JAVA_HOME ──────────────────────────────────────────────────────
if [ -z "$JAVA_HOME" ]; then
    echo ""
    echo "ERROR: JAVA_HOME is not set."
    echo "  Please set JAVA_HOME to a Java 17+ installation and re-run."
    echo "  Example: export JAVA_HOME=/path/to/java17"
    echo ""
    exit 1
fi

JAVA_CMD="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
    echo ""
    echo "ERROR: Java binary not found at: $JAVA_CMD"
    echo "  Check that JAVA_HOME points to a valid Java installation."
    echo ""
    exit 1
fi

# ── 2. Auth phase ─────────────────────────────────────────────────────────────
EXISTING=$(grep -E "^githubToken=.+" "$PROPS_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d '[:space:]')

if [ -z "$EXISTING" ]; then
    echo "Starting GitHub authentication UI..."
    if ! "$JAVA_CMD" gradle/setup.java; then
        echo ""
        echo "ERROR: GitHub authentication failed. Setup aborted."
        echo ""
        exit 1
    fi
fi
mark_done "auth"

# ── 3. Gradle phase ───────────────────────────────────────────────────────────
# Always launch Gradle — it handles incremental execution internally.
# If setup.web previously failed (e.g. at Tomcat), re-running this script
# skips auth (token already saved) and re-enters setup.web from the last
# checkpoint tracked by the Gradle task itself.
exec ./gradlew "$TASK"
