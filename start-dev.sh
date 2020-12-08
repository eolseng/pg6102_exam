#!/bin/bash
############################################################
# Script used to start supporting services for development #
#    If permission is denied, use 'chmod +x ./start-dev'   #
############################################################
set -e
docker-compose -f docker-compose-dev.yml up
