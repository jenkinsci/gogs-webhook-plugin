#!/bin/sh
#
# This script will auto-configure a "blank" app stack for the demo

set -eu

# Uncomment to enable debug mode
# set -x

# Parameters
GITSERVER_URL="http://127.0.0.1:3000"
GITSERVER_API_URL="${GITSERVER_URL}/api/v1"
RUN_USER="$(grep 'RUN_USER' /etc/templates/app.ini | awk '{print $3}')"
CUSTOM_DATA_DIRECTORY="/app/data"

mkdir -p "${CUSTOM_DATA_DIRECTORY}"
chown -R "${RUN_USER}:${RUN_USER}" "${CUSTOM_DATA_DIRECTORY}"

# Launching Git Server
bash /app/gogs/docker/start.sh >/dev/stdout 2>&1 &

# Waiting for Gitserver to start
while true; do
  curl --fail -X GET "${GITSERVER_URL}" \
  && break ||
    echo "Git Server still not started, waiting 2s before retrying"

  sleep 5
done

echo "== Configuring Git Server"

# We create the first user
curl -v -X POST -s \
  -F "user_name=${FIRST_USER}" \
  -F "email=${FIRST_USER}@localhost.local" \
  -F "password=${FIRST_USER}" \
  -F "retype=${FIRST_USER}" \
  ${GITSERVER_URL}/user/sign_up

# Create Access token for first user
TOKEN=$(curl -X POST -s -F "name=${FIRST_USER}" -u "${FIRST_USER}:${FIRST_USER}" \
 ${GITSERVER_URL}/api/v1/users/${FIRST_USER}/tokens | sed 's/.*"sha1":"\(.*\)"}/\1/')

echo $TOKEN
while IFS= read -r LINE  || [[ -n "${LINE}" ]];
do
  echo "==LINE: ${LINE}"
  REMOTE_REPO_URL="$(echo ${LINE} | cut -f1 -d'|')"
  REPO_NAME="$(echo ${LINE} | cut -f2 -d'|')"

  echo "== Mirroring repo ${REPO_NAME} from ${REMOTE_REPO_URL}"

  # Create repo to migrate
  curl -v -X POST -s \
    -H "Authorization: token ${TOKEN}" \
    -F "uid=1" \
    -F "clone_addr=${REMOTE_REPO_URL}" \
    -F "repo_name=${REPO_NAME}" \
    ${GITSERVER_API_URL}/repos/migrate

done < /app/repos-to-mirror

echo "== Configuration done."