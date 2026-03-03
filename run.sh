#!/bin/bash

# 数据库对比程序运行脚本

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE="target/db-comparator-1.0-SNAPSHOT.jar"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR文件不存在，正在打包..."
    ./build.sh
fi

# 默认配置文件
CONFIG_FILE="${1:-config.json}"
TABLE_FILE="${2:-table.txt}"

echo "运行数据库对比程序..."
echo "配置文件: $CONFIG_FILE"
echo "表名文件: $TABLE_FILE"
echo ""

java -jar "$JAR_FILE" "$CONFIG_FILE" "$TABLE_FILE"
