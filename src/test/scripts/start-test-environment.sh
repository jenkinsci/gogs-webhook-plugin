#!/usr/bin/env bash

# parameter with the location of the compose file, the project name

pwd

docker-compose -f src/test/docker/docker-compose.yml build --no-cache
docker-compose -f src/test/docker/docker-compose.yml up --force-recreate -d
