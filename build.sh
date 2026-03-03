#!/bin/bash

# 数据库对比程序打包脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "  数据库对比程序打包脚本"
echo "========================================="
echo ""

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装，请先安装Maven"
    exit 1
fi

# 检查配置文件
if [ ! -f "config.json" ]; then
    echo "错误: 配置文件 config.json 不存在"
    exit 1
fi

# 检查表名文件
if [ ! -f "table.txt" ]; then
    echo "警告: 表名文件 table.txt 不存在，将使用空列表"
fi

echo "步骤1: 清理并编译项目..."
mvn clean compile -q

echo ""
echo "步骤2: 打包项目..."
mvn package -DskipTests -q

# 检查打包结果
if [ -f "target/db-comparator-1.0-SNAPSHOT.jar" ]; then
    echo ""
    echo "========================================="
    echo "  打包成功!"
    echo "========================================="
    echo ""
    echo "生成的JAR文件: target/db-comparator-1.0-SNAPSHOT.jar"
    echo ""
    echo "运行方式:"
    echo "  java -jar target/db-comparator-1.0-SNAPSHOT.jar"
    echo ""
    echo "或指定配置文件:"
    echo "  java -jar target/db-comparator-1.0-SNAPSHOT.jar config.json table.txt"
else
    echo "错误: 打包失败，未找到JAR文件"
    exit 1
fi
