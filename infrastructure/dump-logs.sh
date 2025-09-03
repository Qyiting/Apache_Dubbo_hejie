#!/bin/bash
set -e

# 宿主机输出目录
OUTPUT_DIR="./docker-dumps"
mkdir -p $OUTPUT_DIR

echo "================ MySQL Binlog ================="
# 获取最新的 binlog 文件名
LATEST_BINLOG=$(docker exec mysql-master \
  sh -c "mysql -uroot -proot123 -Nse 'SHOW BINARY LOGS;' | tail -n1 | awk '{print \$1}'")

echo "最新的 binlog 文件是: $LATEST_BINLOG"

# 导出 binlog 文件内容
docker exec mysql-master \
  sh -c "mysqlbinlog --base64-output=DECODE-ROWS -vv /var/lib/mysql/$LATEST_BINLOG" \
  > $OUTPUT_DIR/mysql-binlog.txt

echo "✅ MySQL binlog 已保存到 $OUTPUT_DIR/mysql-binlog.txt"


echo "================ Redis Cluster Nodes ================="
# Redis 节点数量
REDIS_NODES=6

for i in $(seq 1 $REDIS_NODES); do
  NODE_NAME="redis-node-$i"
  NODE_DIR="$OUTPUT_DIR/$NODE_NAME"
  mkdir -p $NODE_DIR

  echo ">>> 处理 $NODE_NAME ..."

  # 导出 AOF
  if docker exec $NODE_NAME test -f /data/appendonly.aof; then
    docker cp $NODE_NAME:/data/appendonly.aof $NODE_DIR/appendonly.aof
    cat $NODE_DIR/appendonly.aof > $NODE_DIR/redis-aof.txt
    echo "   ✅ AOF 已保存到 $NODE_DIR/redis-aof.txt"
  else
    echo "   ⚠️ 该节点没有 AOF 文件"
  fi

  # 导出 RDB
  if docker exec $NODE_NAME test -f /data/dump.rdb; then
    docker cp $NODE_NAME:/data/dump.rdb $NODE_DIR/dump.rdb
    docker exec $NODE_NAME redis-check-rdb /data/dump.rdb > $NODE_DIR/redis-rdb.txt
    echo "   ✅ RDB 已保存到 $NODE_DIR/redis-rdb.txt"
  else
    echo "   ⚠️ 该节点没有 RDB 文件"
  fi
done

echo ">>> 所有日志已导出到 $OUTPUT_DIR/"
