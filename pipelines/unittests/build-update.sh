#!/bin/bash

URI='api.github.com'
OWNER='etendosoftware'
REPO_SLUG=$1
REVISION=$5
STATUS=$2
DESCRIPTION=$3
TARGET_URL="$6"
ACCESS_TOKEN=$4
GIT_STATUS_URL="https://$ACCESS_TOKEN:x-oauth-basic@$URI/repos/$OWNER/$REPO_SLUG/statuses/${REVISION}"
TEMPLATE='{"state":"%s", "target_url":"%s", "description":"%s", "context":"build/job"}'
PAYLOAD=$(printf "$TEMPLATE" "$STATUS" "$TARGET_URL" "$DESCRIPTION")

echo $PAYLOAD
echo $GIT_STATUS_URL
curl -X POST -H "application/json" -d "$PAYLOAD" "${GIT_STATUS_URL}"