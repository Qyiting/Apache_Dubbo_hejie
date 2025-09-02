#!/bin/bash
# RPC Framework Infrastructure Stop and Clear Script
# This script Stop and Clear MySQL master-slave replication and Redis cluster
echo "Stopping and clearing RPC Framework Infrastructure..."
echo "Stopping containers and networks..."
docker-compose down
echo "Listing project volumes from docker-compose.yml..."
VOLUMES=$(docker compose config --volumes)
if [ -z "$VOLUMES" ]; then
  echo "No volumes defined in docker-compose.yml"
else
  echo "The following volumes are defined in docker-compose.yml:"
  echo "$VOLUMES"
  echo ""
  read -p "Do you want to delete these volumes? (y/N): " CONFIRM
  if [[ "$CONFIRM" == "y" || "$CONFIRM" == "Y" ]]; then
    echo "Deleting volumes..."
    docker volume rm $VOLUMES
  else
    echo "Volume deletion skipped."
  fi
fi
echo ""
echo "Remaining Docker volumes:"
docker volume ls