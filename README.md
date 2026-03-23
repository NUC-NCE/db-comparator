[English](./README_en.md)

# 数据库对比工具 (DbComparator)

一个用于比对 Oracle 和 Gauss 数据库表数据的工具，支持多线程并发对比。

## 功能特性

- 支持 Oracle 和 Gauss (华为GaussDB) 数据库之间的数据比对
- 多线程并发处理，提高对比效率
- 支持表过滤条件筛选数据
- 支持自定义主键配置
- 输出详细的差异报告
- 支持 CLI 命令行和 SDK 编程调用
- 支持直接传入数据库 Connection 对象（SDK）
- 支持从数据库表获取表配置列表（SDK）

## 环境要求

- Java 1.8 或更高版本
- Maven 3.x
- Oracle JDBC 驱动
- PostgreSQL JDBC 驱动 (用于 GaussDB)

## 快速开始

### 1. 表名配置文件 (table.txt)

在 `table.txt` 文件中列出要比对的表名，每行一个表名。

格式：`表名 [查询条件] [主键列表]`

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

### 2. 构建项目

```bash
mvn clean package -DskipTests
```

### 3. 使用方式

---

## CLI 命令行调用

### 基本语法

```bash
java -jar db-comparator.jar \
    <oracle_jdbc_url> <oracle_user> <oracle_password> \
    <gauss_jdbc_url> <gauss_user> <gauss_password> \
    <table_list_file> [thread_count] [output_dir]
```

### 参数说明

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| oracle_jdbc_url | 是 | Oracle JDBC 连接串 | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| oracle_user | 是 | Oracle 用户名 | `scott` |
| oracle_password | 是 | Oracle 密码 | `tiger` |
| gauss_jdbc_url | 是 | GaussDB/PostgreSQL JDBC 连接串 | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |
| gauss_user | 是 | GaussDB 用户名 | `scott` |
| gauss_password | 是 | GaussDB 密码 | `tiger` |
| table_list_file | 是 | 表列表文件路径 | `table.txt` |
| thread_count | 否 | 线程数，默认 4 | `4` |
| output_dir | 否 | 输出目录，默认 ./output | `./output` |

### 使用示例

**示例1：基本对比**
```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt
```

**示例2：指定线程数和输出目录**
```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt 4 ./output
```

---

## SDK 编程调用

### 添加依赖

```xml
<dependency>
    <groupId>com.datacheck</groupId>
    <artifactId>db-comparator</artifactId>
    <version>1.1</version>
</dependency>
```

或者直接引入打包好的 JAR 文件。

### 基本使用

```java
import com.datacheck.sdk.DbComparator;
import com.datacheck.sdk.model.ComparisonSummary;

try (DbComparator comparator = DbComparator.builder()
    .oracleJdbcUrl("jdbc:oracle:thin:@192.168.1.100:1521:orcl")
    .oracleUsername("scott")
    .oraclePassword("tiger")
    .gaussJdbcUrl("jdbc:postgresql://192.168.1.101:5432/gaussdb")
    .gaussUsername("scott")
    .gaussPassword("tiger")
    .tableListFile("table.txt")
    .threadCount(4)
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("总表数: " + summary.getTotalTables());
    System.out.println("数据一致: " + summary.getConsistentCount());
    System.out.println("数据不一致: " + summary.getDifferentCount());
}
```

### API 参考

#### 数据库配置

| 方法 | 说明 |
|------|------|
| `oracleJdbcUrl(String url)` | Oracle JDBC 连接串 |
| `oracleUsername(String user)` | Oracle 用户名 |
| `oraclePassword(String pwd)` | Oracle 密码 |
| `gaussJdbcUrl(String url)` | GaussDB/PostgreSQL JDBC 连接串 |
| `gaussUsername(String user)` | GaussDB 用户名 |
| `gaussPassword(String pwd)` | GaussDB 密码 |

#### 表列表配置

| 方法 | 说明 |
|------|------|
| `tableListFile(String path)` | 表列表文件路径 |
| `tables(List<String> names)` | 表名列表 |
| `tableFilters(List<TableFilter> filters)` | 完整表配置（支持过滤条件和主键） |
| `addTable(String name)` | 添加单个表 |

#### 其他配置

| 方法 | 说明 |
|------|------|
| `threadCount(int count)` | 线程数（默认4） |
| `outputDir(String path)` | 输出目录 |
| `writeResultFiles(boolean write)` | 是否写入结果文件 |
| `oracleConfig(DatabaseConfig config)` | Oracle 配置对象 |
| `gaussConfig(DatabaseConfig config)` | GaussDB 配置对象 |
| `oracleConnection(Connection conn)` | 直接传入已建立的 Oracle Connection |
| `gaussConnection(Connection conn)` | 直接传入已建立的 GaussDB Connection |
| `tableConfigTable(String tableName)` | 从数据库表获取表配置的表名（默认 table_check_info） |
| `batchConfig(int batchSize, int maxMemoryRows)` | 分批处理配置（默认 10000/50000） |

### SDK 使用示例

**示例1：启用结果文件输出**
```java
try (DbComparator comparator = DbComparator.builder()
    .oracleJdbcUrl("jdbc:oracle:thin:@192.168.1.100:1521:orcl")
    .oracleUsername("scott")
    .oraclePassword("tiger")
    .gaussJdbcUrl("jdbc:postgresql://192.168.1.101:5432/gaussdb")
    .gaussUsername("scott")
    .gaussPassword("tiger")
    .tableListFile("table.txt")
    .threadCount(4)
    .outputDir("./output")
    .writeResultFiles(true)
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("总表数: " + summary.getTotalTables());
}
```

**示例2：代码传入表列表**
```java
List<String> tables = Arrays.asList("users", "orders", "products");

try (DbComparator comparator = DbComparator.builder()
    .oracleJdbcUrl("jdbc:oracle:thin:@192.168.1.100:1521:orcl")
    .oracleUsername("scott")
    .oraclePassword("tiger")
    .gaussJdbcUrl("jdbc:postgresql://192.168.1.101:5432/gaussdb")
    .gaussUsername("scott")
    .gaussPassword("tiger")
    .tables(tables)
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();
}
```

**示例3：带过滤条件和主键**
```java
List<TableFilter> filters = Arrays.asList(
    new TableFilter("users", "status = 'active'", Arrays.asList("user_id")),
    new TableFilter("orders", "order_date > '2025-01-01'", Arrays.asList("order_id")),
    new TableFilter("products")
);

try (DbComparator comparator = DbComparator.builder()
    .oracleJdbcUrl("jdbc:oracle:thin:@192.168.1.100:1521:orcl")
    .oracleUsername("scott")
    .oraclePassword("tiger")
    .gaussJdbcUrl("jdbc:postgresql://192.168.1.101:5432/gaussdb")
    .gaussUsername("scott")
    .gaussPassword("tiger")
    .tableFilters(filters)
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();
}
```

**示例4：直接传入数据库连接**
```java
try (DbComparator comparator = DbComparator.builder()
    .oracleConnection(existingOracleConn)
    .gaussConnection(existingGaussConn)
    .tableListFile("table.txt")
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();
}
```

**示例5：从数据库获取表配置**
```java
try (DbComparator comparator = DbComparator.builder()
    .oracleConnection(existingOracleConn)
    .gaussConnection(existingGaussConn)
    .tableConfigTable("table_check_info")  // 可选，默认 table_check_info
    .build()) {

    comparator.init();
    comparator.connect();  // connect() 时自动从数据库加载表配置
    comparator.compare();
}
```

**table_check_info 表格式：**

| table_name | condition | key |
|------------|-----------|-----|
| users | [status = 'active'] | [user_id] |
| orders | [data_date = date'2026-03-22' and falg = '1'] | [fund_code,asset_code] |
| products | [] | [product_id] |

**说明**：condition 和 key 字段值已包含方括号格式，与 table.txt 文件格式一致。

### 大表分批处理

当数据量较大时（如上百万行），SDK 会自动采用分批处理策略，防止内存溢出。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `batchSize` | 10000 | 每批处理的主键行数 |
| `maxMemoryRows` | 50000 | 超过此阈值则启用分批处理 |

```java
.batchConfig(10000, 50000)  // 可选，已为默认值
```

### 结果获取

```java
// 汇总结果
ComparisonSummary summary = comparator.getSummary();
summary.getTotalTables();      // 总表数
summary.getSuccessCount();    // 成功数
summary.getErrorCount();      // 错误数
summary.getConsistentCount(); // 数据一致数
summary.getDifferentCount();  // 数据不一致数

// 详细结果
Map<String, CompareResult> results = comparator.getResults();
for (Map.Entry<String, CompareResult> entry : results.entrySet()) {
    CompareResult result = entry.getValue();
    result.getStatus();        // success / error
    result.getMessage();      // 信息
    result.hasDifferences();  // 是否有差异
}
```

---

## JDBC 连接串格式

### Oracle

| 格式 | 示例 |
|------|------|
| SID 方式 | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| Service Name 方式 | `jdbc:oracle:thin:@//192.168.1.100:1521/orclpdb1` |

### PostgreSQL / GaussDB

| 格式 | 示例 |
|------|------|
| 标准格式 | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |

---

## 数值比较说明

对比数值类型数据时，以下差异会被自动忽略，视为数据一致：

| 差异类型 | 示例 | 比较结果 |
|----------|------|----------|
| 末尾零差异 | `0.8` vs `0.8000000` | 一致 |
| 整数与小数 | `8` vs `8.0` | 一致 |
| 科学计数法 | `1.2E-5` vs `0.000012` | 一致 |
| 零的不同表示 | `0` vs `0E-8` | 一致 |

---

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
│   ├── output/
│   │   └── ResultWriter.java    # 结果输出
│   └── sdk/                     # SDK 模块
│       ├── DbComparator.java
│       └── DbComparatorBuilder.java
├── table.txt                    # 表名列表
├── pom.xml                      # Maven 配置
└── README.md                    # 本文档
```

## 依赖项

- Oracle JDBC Driver (ojdbc8)
- PostgreSQL JDBC Driver (Gauss兼容)
- Jackson (JSON处理)

## 许可证

MIT License
