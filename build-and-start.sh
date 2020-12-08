#!/bin/bash
#############################################################
#      Script used to rebuild project .jars and images      #
# If permission is denied, use 'chmod +x ./build-and-start' #
#############################################################
set -e
# Perform a multithreaded clean install of all project modules
mvn clean install -T 1C -DskipTests
# Rebuild all images and start docker-compose
docker-compose up --build
