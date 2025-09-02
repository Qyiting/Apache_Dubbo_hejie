-- 主从复制设置脚本
-- 此脚本需要在主库和从库启动后手动执行

-- 在主库上创建复制用户
CREATE USER IF NOT EXISTS 'repl_user'@'%' IDENTIFIED WITH mysql_native_password BY 'repl_password123';
GRANT REPLICATION SLAVE ON *.* TO 'repl_user'@'%';
FLUSH PRIVILEGES;

-- 显示主库状态（记录File和Position用于从库配置）
SHOW MASTER STATUS;

-- 在从库上执行以下命令（需要替换实际的MASTER_LOG_FILE和MASTER_LOG_POS）
/*
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='repl_user',
  MASTER_PASSWORD='repl_password123',
  MASTER_AUTO_POSITION=1;

START SLAVE;

-- 检查从库状态
SHOW SLAVE STATUS\G
*/

-- 创建应用程序数据库用户
CREATE USER IF NOT EXISTS 'rpc_app'@'%' IDENTIFIED WITH mysql_native_password BY 'rpc_app_pass123';
GRANT SELECT, INSERT, UPDATE, DELETE ON rpc_auth.* TO 'rpc_app'@'%';
FLUSH PRIVILEGES;