# DB Comparator SDK 使用指南

## 概述

DB Comparator SDK 是一个用于对比 Oracle 和 GaussDB/PostgreSQL 数据库数据的 Java 库。支持通过编程方式调用，返回结构化的对比结果。

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.datacheck</groupId>
    <artifactId>db-comparator</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

或者直接引入打包好的 JAR 文件。

### 替换 JDBC 驱动

项目默认使用 PostgreSQL 驱动连接 GaussDB。如果需要使用其他驱动（如 GaussDB 专用驱动），可以在父项目中排除并替换：

```xml
<dependency>
    <groupId>com.datacheck</groupId>
    <artifactId>db-comparator</artifactId>
    <version>1.0-SNAPSHOT</version>
    <!-- 排除 PostgreSQL 驱动 -->
    <exclusions>
        <exclusion>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </exclusion>
        <!-- 如需替换 Oracle 驱动，也需要排除 -->
        <exclusion>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 添加 GaussDB 专用驱动 -->
<dependency>
    <groupId>com.huawei.gauss200</groupId>
    <artifactId>jdbc</artifactId>
    <version>3.1.0-1</version>
</dependency>
```

**说明**：JDBC 驱动不再打包进 SDK，使用时由调用方提供。

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

---

## API 参考

### DbComparatorBuilder

链式配置构建器，用于创建 `DbComparator` 实例。

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
| `options(SdkOptions options)` | SDK 选项配置 |
| `oracleConfig(DatabaseConfig config)` | Oracle 配置对象 |
| `gaussConfig(DatabaseConfig config)` | GaussDB 配置对象 |

#### 构建

| 方法 | 说明 |
|------|------|
| `build()` | 构建 DbComparator 实例 |

---

## 使用示例

### 示例1：链式调用（最简单）

直接传入 JDBC 连接串，最简单的使用方式。

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

### 示例2：使用配置对象

适合配置信息来自外部文件或数据库的场景。

```java
// 构建 Oracle 配置
DatabaseConfig oracleConfig = new DatabaseConfig(
    "jdbc:oracle:thin:@192.168.1.100:1521:orcl",
    "scott",
    "tiger"
);

// 构建 GaussDB 配置
DatabaseConfig gaussConfig = new DatabaseConfig(
    "jdbc:postgresql://192.168.1.101:5432/gaussdb",
    "scott",
    "tiger"
);

try (DbComparator comparator = DbComparator.builder()
    .oracleConfig(oracleConfig)
    .gaussConfig(gaussConfig)
    .tableListFile("table.txt")
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("数据一致: " + summary.getConsistentCount());
    System.out.println("数据不一致: " + summary.getDifferentCount());
}
```

### 示例3：代码传入表列表

适合表名是动态确定的场景。

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

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("总计对比: " + summary.getTotalTables() + " 个表");
}
```

### 示例4：带过滤条件和主键

适合需要指定过滤条件或主键的场景。

```java
List<TableFilter> filters = Arrays.asList(
    // 只对比 status='active' 的用户
    new TableFilter("users", "status = 'active'", Arrays.asList("user_id")),
    // 只对比最近一年的订单
    new TableFilter("orders", "order_date > '2025-01-01'", Arrays.asList("order_id")),
    // 对比整个产品表
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

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("一致: " + summary.getConsistentCount());
    System.out.println("不一致: " + summary.getDifferentCount());
}
```

### 示例5：启用结果文件输出

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
    .outputDir("./output")           // 设置输出目录
    .writeResultFiles(true)          // 启用结果文件写入
    .build()) {

    comparator.init();
    comparator.connect();
    comparator.compare();

    ComparisonSummary summary = comparator.getSummary();
    System.out.println("总表数: " + summary.getTotalTables());
    System.out.println("数据一致: " + summary.getConsistentCount());
}
```

---

## JDBC 连接串格式

### Oracle

| 格式 | 示例 |
|------|------|
| SID 方式 | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| Service Name 方式 | `jdbc:oracle:thin:@//192.168.1.100:1521/orclpdb1` |

### PostgreSQL / GaussDB（使用 PostgreSQL 驱动）

| 驱动 | 格式 | 示例 |
|------|------|------|
| PostgreSQL 标准驱动 | `jdbc:postgresql://host:port/database` | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |

### GaussDB（使用 GaussDB 专用驱动）

如果使用 GaussDB 专用驱动，需要在 pom.xml 中添加依赖：

```xml
<!-- GaussDB 专用驱动 -->
<dependency>
    <groupId>com.huawei.gauss200</groupId>
    <artifactId>jdbc</artifactId>
    <version>3.1.0-1</version>
</dependency>
```

GaussDB 专用驱动连接串格式：

| 格式 | 示例 |
|------|------|
| GaussDB V100 | `jdbc:gaussdb://host:port/database` |

---

## 结果获取

### ComparisonSummary - 汇总结果

```java
ComparisonSummary summary = comparator.getSummary();

summary.getTotalTables();      // 总表数
summary.getSuccessCount();     // 成功数
summary.getErrorCount();      // 错误数
summary.getConsistentCount(); // 数据一致数
summary.getDifferentCount();  // 数据不一致数
summary.getFailedTableNames(); // 失败的表名列表
summary.getFailedReasons();   // 失败原因 Map
```

### Map<String, CompareResult> - 详细结果

```java
Map<String, CompareResult> results = comparator.getResults();

for (Map.Entry<String, CompareResult> entry : results.entrySet()) {
    String tableName = entry.getKey();
    CompareResult result = entry.getValue();

    result.getStatus();      // success / error
    result.getMessage();    // 错误信息
    result.hasDifferences(); // 是否有差异

    // 差异详情
    for (Difference diff : result.getDifferences()) {
        diff.getType();        // oracle_only / gauss_only / different
        diff.getPkKey();       // 主键值
        diff.getOracleData();  // Oracle 数据
        diff.getGaussData();   // GaussDB 数据
    }
}
```

---

## 表列表文件格式

表列表文件每行一个表名，支持以下格式：

```
# 只有表名（全表对比）
users

# 带查询条件
users [status = 'active']

# 带查询条件和主键
users [status = 'active'] [user_id]

# 仅主键
users [] [user_id]

# 复杂查询条件
orders [status = 'pending' AND create_time > '2024-01-01'] [order_id,user_id]
```

格式说明：`表名 [查询条件] [主键列表]`

- `[查询条件]`：直接拼接到 WHERE 后，支持完整的 SQL 表达式
- `[主键列表]`：自定义对比主键，多个主键用逗号分隔

---

## 资源管理

推荐使用 try-with-resources 自动释放资源：

```java
try (DbComparator comparator = DbComparator.builder()
    // ... 配置
    .build()) {
    comparator.init();
    comparator.connect();
    comparator.compare();
    // 资源会自动关闭
}
```

或者手动关闭：

```java
DbComparator comparator = DbComparator.builder()
    // ... 配置
    .build();

try {
    comparator.init();
    comparator.connect();
    comparator.compare();
} finally {
    comparator.close();
}
```
