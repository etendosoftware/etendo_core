#!/bin/bash

# Parameters
REPO_SLUG=$1
STATUS=$2
DESCRIPTION=$3
ACCESS_TOKEN=$4
REVISION=$5
TARGET_URL="$6"
CONTEXT="$7"

# GitHub API URL
URI='api.github.com'
OWNER='etendosoftware'

# GitHub status URL
GIT_STATUS_URL="https://$ACCESS_TOKEN:x-oauth-basic@$URI/repos/$OWNER/$REPO_SLUG/statuses/${REVISION}"

# Template for JSON data
TEMPLATE='{"state":"%s", "target_url":"%s", "description":"%s", "context":"%s"}'
PAYLOAD=$(printf "$TEMPLATE" "$STATUS" "$TARGET_URL" "$DESCRIPTION" "$CONTEXT")

# Function to display error messages and exit
display_error() {
  echo "***********************"
  echo "BUILD-UPDATE Error"
  echo "Check: $1"
  echo "***********************"
  echo "Parameters received:"
  echo "REPO_SLUG: ${REPO_SLUG:-EMPTY}"
  echo "STATUS: ${STATUS:-EMPTY}"
  echo "DESCRIPTION: ${DESCRIPTION:-EMPTY}"
  echo "ACCESS_TOKEN: ${ACCESS_TOKEN:-EMPTY}"
  echo "REVISION: ${REVISION:-EMPTY}"
  echo "TARGET_URL: ${TARGET_URL:-EMPTY}"
  echo "CONTEXT: ${CONTEXT:-EMPTY}"
  echo "***********************"
  exit 1
}

# Check for empty parameters and exit with an error message if any are found
if [ -z "$REPO_SLUG" ]; then display_error "REPO_SLUG is empty"; fi
if [ -z "$STATUS" ]; then display_error "STATUS is empty"; fi
if [ -z "$DESCRIPTION" ]; then display_error "DESCRIPTION is empty"; fi
if [ -z "$ACCESS_TOKEN" ]; then display_error "ACCESS_TOKEN is empty"; fi
if [ -z "$REVISION" ]; then display_error "REVISION is empty"; fi
if [ -z "$TARGET_URL" ]; then display_error "TARGET_URL is empty"; fi
if [ -z "$CONTEXT" ]; then display_error "CONTEXT is empty"; fi

# Print the URL and data for debugging purposes
echo "$PAYLOAD"
echo "$GIT_STATUS_URL"

# Perform the cURL request to update the build status on GitHub
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "application/json" -d "$PAYLOAD" "$GIT_STATUS_URL")

# Check if the response code is not 20x
if [[ ! "$RESPONSE" =~ ^20[0-9]$ ]]; then
  echo "***********************"
  echo "BUILD-UPDATE Error"
  echo "Failed to update build status on GitHub. HTTP status code: $RESPONSE"
  echo "***********************"
  echo "Parameters received:"
  echo "REPO_SLUG: $REPO_SLUG"
  echo "STATUS: $STATUS"
  echo "DESCRIPTION: $DESCRIPTION"
  echo "ACCESS_TOKEN: $ACCESS_TOKEN"
  echo "REVISION: $REVISION"
  echo "TARGET_URL: $TARGET_URL"
  echo "CONTEXT: $CONTEXT"
  echo "***********************"
  exit 1
fi
