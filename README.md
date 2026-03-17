[English](./README_en.md)

# 数据库对比工具 (DbComparator)

一个用于比对 Oracle 和 Gauss 数据库表数据的工具，支持多线程并发对比。

## 功能特性

- 支持 Oracle 和 Gauss (华为GaussDB) 数据库之间的数据比对
- 多线程并发处理，提高对比效率
- 支持表过滤条件筛选数据
- 支持自定义主键配置
- 输出详细的差异报告

## 环境要求

- Java 1.8 或更高版本
- Maven 3.x
- Oracle JDBC 驱动
- PostgreSQL JDBC 驱动 (用于 GaussDB)

## 快速开始

### 1. 配置文件 (config.json)

```json
{
    "oracle": {
        "host": "localhost",
        "port": 1521,
        "serviceName": "orcl",
        "username": "your_oracle_user",
        "password": "your_oracle_password"
    },
    "gauss": {
        "host": "localhost",
        "port": 5432,
        "database": "gaussdb",
        "username": "your_gauss_user",
        "password": "your_gauss_password"
    },
    "threadCount": 4,
    "outputDir": "~/answer"
}
```

配置说明:
- `oracle`: Oracle 数据库连接配置
- `gauss`: Gauss 数据库连接配置
- `threadCount`: 并发线程数，默认为 4
- `outputDir`: 差异结果输出目录

### 2. 表名配置文件 (table.txt)

在 `table.txt` 文件中列出要比对的表名，每行一个表名。

支持带过滤条件和主键的配置格式：

```
表名 [查询条件] [主键列表]
```

示例：
```
# 全表对比
users

# 仅查询条件
orders [status = 'pending']

# 查询条件 + 主键
products [status = 'active'] [product_id]

# 仅主键
users [] [user_id]

# 复杂查询条件
orders [status = 'pending' AND create_time > '2024-01-01'] [order_id,user_id]
```

说明：
- `[查询条件]`：直接拼接到 WHERE 后，支持完整的 SQL 表达式（AND、OR、LIKE 等）
- `[主键列表]`：自定义对比主键，多个主键用逗号分隔
- 两个方括号都是可选的，可以只填一个

### 3. 构建项目

```bash
./build.sh
```

或者手动执行：
```bash
mvn clean package -DskipTests
```

### 4. 运行程序

```bash
./run.sh
```

或者指定配置文件：
```bash
java -jar target/db-comparator-1.0-SNAPSHOT.jar config.json table.txt
```

## 项目结构

```
db-comparator/
├── src/main/java/com/datacheck/
│   ├── Config.java              # 配置加载
│   ├── DbComparator.java       # 主程序入口
│   ├── compare/
│   │   └── TableComparator.java # 表数据比对
│   ├── db/
│   │   ├── DataFetcher.java     # 数据获取
│   │   ├── DatabaseConnector.java # 数据库连接
│   │   └── TableMetadata.java  # 表元数据
│   ├── model/
│   │   ├── CompareResult.java  # 对比结果
│   │   ├── Difference.java      # 差异模型
│   │   ├── TableData.java       # 表数据模型
│   │   └── TableFilter.java     # 表过滤条件
│   └── output/
│       └── ResultWriter.java    # 结果输出
├── config.json                  # 配置文件
├── table.txt                    # 表名列表
├── build.sh                     # 构建脚本
├── run.sh                       # 运行脚本
└── pom.xml                      # Maven 配置
```

## 依赖项

- Oracle JDBC Driver (ojdbc8)
- PostgreSQL JDBC Driver (Gauss兼容)
- Jackson (JSON处理)
- Lombok

## 许可证

MIT License
