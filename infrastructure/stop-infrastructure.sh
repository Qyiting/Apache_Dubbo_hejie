#!/bin/bash
# RPC Framework Infrastructure Stop Script
# This script Stop MySQL master-slave replication and Redis cluster

echo "Stopping RPC Framework Infrastructure..."

# Stop all services
echo "Stopping MySQL and Redis services..."
docker compose down -v
sleep 10
echo "Infrastructure stopped!"