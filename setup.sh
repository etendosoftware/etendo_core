#!/bin/bash
# setup.sh — Bootstrap script for Etendo Fast Install
#
# Delegates to setup.java which handles:
# - Checking githubToken in gradle.properties
# - GitHub Device Flow auth UI on localhost:3850 (if needed)
# - Launching ./gradlew setup.web
#
# Usage:
#   ./setup.sh               # starts setup.web (default)
#   ./setup.sh <task>        # runs any gradle task

set -e

TASK="${1:-setup.web}"
PROPS_FILE="gradle.properties"

# Check if token already set — fast path avoids Java startup
EXISTING=$(grep -E "^githubToken=.+" "$PROPS_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d '[:space:]')

if [ -n "$EXISTING" ]; then
    exec ./gradlew "$@"
fi

echo "Starting GitHub authentication UI..."
java setup.java "$TASK"
exit $?
