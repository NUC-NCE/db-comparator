# Java 命令行调用指南

## 概述

DB Comparator 支持通过 `java -jar` 命令行直接调用，无需编写代码。直接传入 JDBC 连接串和表列表文件即可执行数据库对比。

## 使用方法

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

---

## 使用示例

### 示例1：基本对比

```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt
```

### 示例2：指定线程数

```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt 8
```

### 示例3：指定输出目录

```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt 4 ./output
```

### 示例4：使用 Oracle Service Name

```bash
java -jar db-comparator.jar \
    "jdbc:oracle:thin:@//192.168.1.100:1521/orclpdb1" scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt 4 ./output
```

---

## 表列表文件格式

创建表列表文件，每行一个表名。

### 格式：表名 [查询条件] [主键列表]

```
表名 [whereClause] [primaryKeys]
```

### 示例1：仅表名（全表对比）

```
users
orders
products
employees
```

### 示例2：带查询条件

```
users [status = 'active']
orders [order_date > '2025-01-01']
```

### 示例3：带查询条件和主键

```
users [status = 'active'] [user_id]
orders [status = 'pending' AND create_time > '2024-01-01'] [order_id,user_id]
```

### 示例4：仅主键

```
users [] [user_id]
products [] [product_id]
```

### 格式说明

| 格式 | 说明 |
|------|------|
| `表名` | 对比整个表 |
| `表名 [条件]` | 只对比满足 WHERE 条件的记录 |
| `表名 [条件] [主键1,主键2]` | 带查询条件和自定义主键 |
| `表名 [] [主键1]` | 仅指定主键 |

---

## JDBC 连接串格式

### Oracle

| 格式 | 示例 |
|------|------|
| SID 方式 | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| Service Name 方式 | `jdbc:oracle:thin:@//192.168.1.100:1521/orclpdb1` |
| 带主机名 | `jdbc:oracle:thin:@myhost.example.com:1521:orcl` |

### PostgreSQL / GaussDB（PostgreSQL 驱动）

| 格式 | 示例 |
|------|------|
| 标准格式 | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |
| 带主机名 | `jdbc:postgresql://myhost.example.com:5432/gaussdb` |

### GaussDB 专用驱动

如果需要使用 GaussDB 专用驱动，需要修改 pom.xml 添加依赖后重新打包：

```xml
<dependency>
    <groupId>com.huawei.gauss200</groupId>
    <artifactId>jdbc</artifactId>
    <version>3.1.0-1</version>
</dependency>
```

GaussDB 专用驱动连接串：`jdbc:gaussdb://host:port/database`

---

## 输出说明

### 控制台输出

程序运行时会输出对比进度和结果：

```
连接Oracle数据库: jdbc:oracle:thin:@192.168.1.100:1521:orcl
Oracle数据库连接成功
连接Gauss数据库: jdbc:postgresql://192.168.1.101:5432/gaussdb
Gauss数据库连接成功
开始对比 3 个表，使用 4 个线程...
对比表: users
对比表: orders
对比表: products
========================================
对比完成!
总计: 3 个表
成功: 3, 失败: 0
数据一致: 2, 数据不一致: 1

对比失败的表:
  - orders: [1]name=Order A→Order B
```

### 输出文件（可选）

如果需要将结果写入文件，可以使用 SDK 方式调用。

---

## 常见问题

### Q: 如何获取 JDBC 连接串？

- **Oracle**: 联系 DBA 获取，或使用 `SELECT * FROM v$instance` 查询
- **GaussDB/PostgreSQL**: 联系 DBA 获取，格式为 `jdbc:postgresql://host:port/database`

### Q: 对比速度慢怎么办？

增加线程数：

```bash
java -jar db-comparator.jar ... table.txt 8
```

建议线程数不超过 CPU 核心数的 2 倍。

### Q: 表太多内存不足怎么办？

分批对比，创建多个表列表文件：

```bash
# 对比第一批表
java -jar db-comparator.jar ... table_part1.txt

# 对比第二批表
java -jar db-comparator.jar ... table_part2.txt
```

### Q: 如何只对比部分数据？

使用过滤条件：

```
# table.txt
users status active
users status pending [user_id]
```

---

## 完整示例

### 1. 创建表列表文件

```bash
cat > table.txt << EOF
users
orders
products
customers
EOF
```

### 2. 执行对比

```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl myuser mypass \
    jdbc:postgresql://192.168.1.101:5432/gaussdb myuser mypass \
    table.txt 4
```

### 3. 查看结果

控制台会输出类似以下结果：

```
连接Oracle数据库: jdbc:oracle:thin:@192.168.1.100:1521:orcl
Oracle数据库连接成功
连接Gauss数据库: jdbc:postgresql://192.168.1.101:5432/gaussdb
Gauss数据库连接成功
开始对比 4 个表，使用 4 个线程...
对比表: users
对比表: orders
对比表: products
对比表: customers
========================================
对比完成!
总计: 4 个表
成功: 4, 失败: 0
数据一致: 3, 数据不一致: 1

对比失败的表:
  - orders: [1]status=0→1;name=Order A→Order B
```
