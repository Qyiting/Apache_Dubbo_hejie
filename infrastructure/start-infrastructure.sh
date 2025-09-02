#!/bin/bash

# RPC Framework Infrastructure Startup Script
# This script sets up MySQL master-slave replication and Redis cluster

echo "Starting RPC Framework Infrastructure..."

# Start all services
echo "Starting MySQL and Redis services..."
docker compose up -d mysql-master mysql-slave redis-node-1 redis-node-2 redis-node-3 redis-node-4 redis-node-5 redis-node-6

# Wait for MySQL services to be ready
echo "Waiting for MySQL services to be ready..."
sleep 20

# Check MySQL master status
echo "Checking MySQL master status..."
docker exec mysql-master mysql -uroot -proot123 -e "SHOW MASTER STATUS;"

# Setup MySQL replication
echo "Setting up MySQL replication..."

# Get master log file and position
MASTER_STATUS=$(docker exec mysql-master mysql -uroot -proot123 -e "SHOW MASTER STATUS\G")
LOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
LOG_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "Master log file: $LOG_FILE"
echo "Master log position: $LOG_POS"

# Configure slave
echo "Configuring MySQL slave..."
docker exec mysql-slave mysql -uroot -proot123 -e "
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-master',
  SOURCE_USER='repl_user',
  SOURCE_PASSWORD='repl_password123',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
"

# Check slave status
echo "Checking MySQL slave status..."
docker exec mysql-slave mysql -uroot -proot123 -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running"

# Wait for Redis nodes to be ready
echo "Waiting for Redis nodes to be ready..."
sleep 20

# Create Redis cluster
echo "Creating Redis cluster..."
docker compose up -d redis-cluster-setup

# Wait for cluster setup to complete
sleep 10

# Check Redis cluster status
echo "Checking Redis cluster status..."
docker exec redis-node-1 redis-cli -p 7001 cluster nodes
MY_IP=$(ip addr show ens33 | grep "inet " | awk '{print $2}' | cut -d/ -f1)
echo "Infrastructure setup completed!"
echo ""
echo "Services available:"
echo "  MySQL Master: $MY_IP:3306"
echo "  MySQL Slave:  $MY_IP:3307"
echo "  Redis Cluster: $MY_IP:7001-7006"
echo ""
echo "Default credentials:"
echo "  MySQL root: root123"
echo "  MySQL app user: rpc_app / rpc_app_pass123"
echo "  Default admin user: admin / admin123"
echo ""
echo "To stop all services: docker compose down"
echo "To view logs: docker compose logs [service-name]"
echo "To use stop.sh to stop all services: ./stop-infrastructure.sh"
echo "To completely clear services and data volumes: ./stop-servicesAndVolumes.sh"