#!/bin/bash

# Verifica si se pasó una clave de tarea
if [ -z "$1" ]; then
  echo "Uso: $0 <ISSUE_KEY>"
  exit 1
fi

ISSUE_KEY="$1"

# Required environment variables
JIRA_BASE_URL=$2
JIRA_ACCESS_TOKEN=$3

if [ -z "$JIRA_BASE_URL" ] || [ -z "$JIRA_ACCESS_TOKEN" ]; then
  echo "Error: JIRA_BASE_URL and JIRA_ACCESS_TOKEN must be provided as arguments."
  exit 1
fi

AUTH_HEADER="Authorization: Basic $JIRA_ACCESS_TOKEN"
ACCEPT_HEADER="Accept: application/json"

response=$(mktemp)
http_code=$(curl -s -o "$response" -w "%{http_code}" -H "$AUTH_HEADER" -H "$ACCEPT_HEADER" \
  "$JIRA_BASE_URL/rest/api/3/issue/$ISSUE_KEY")

if [ "$http_code" != "200" ]; then
  echo "Error: Failed to fetch issue. HTTP status: $http_code"
  cat "$response" >&2
  rm -f "$response"
  exit 1
fi

jq -r '.fields.parent.key // ""' "$response"
rm -f "$response"
