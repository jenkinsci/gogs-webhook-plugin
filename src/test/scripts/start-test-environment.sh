#!/usr/bin/env bash

# parameter with the location of the compose file, the project name

pwd

docker-compose -f src/test/docker/docker-compose.yml up --build --force-recreate -d

#docker-compose -f src/test/docker/docker-compose.yml rm -v