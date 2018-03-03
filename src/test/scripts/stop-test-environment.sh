#!/usr/bin/env bash

# parameter with the location of the compose file, the project name

docker-compose -f src/test/docker/docker-compose.yml stop
docker-compose -f src/test/docker/docker-compose.yml rm -v --force