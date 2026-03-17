[中文](./README.md)

# Database Comparator (DbComparator)

A tool for comparing table data between Oracle and Gauss databases with multi-threaded concurrent comparison support.

## Features

- Compare data between Oracle and Huawei GaussDB databases
- Multi-threaded concurrent processing for improved efficiency
- Support table filtering conditions
- Custom primary key configuration support
- Detailed difference report output
- Support CLI and SDK usage

## Requirements

- Java 1.8 or higher
- Maven 3.x
- Oracle JDBC Driver
- PostgreSQL JDBC Driver (for GaussDB)

## Quick Start

### 1. Table List File (table.txt)

List the table names to compare in `table.txt`, one table name per line.

Format: `tableName [whereClause] [primaryKeys]`

```
# Full table comparison
users

# With WHERE clause only
orders [status = 'pending']

# With WHERE clause and primary keys
products [status = 'active'] [product_id]

# Primary keys only
users [] [user_id]

# Complex WHERE clause
orders [status = 'pending' AND create_time > '2024-01-01'] [order_id,user_id]
```

Notes:
- `[whereClause]`: Appended directly after WHERE, supports full SQL expressions (AND, OR, LIKE, etc.)
- `[primaryKeys]`: Custom primary keys for comparison, multiple keys separated by commas
- Both brackets are optional

### 2. Build the Project

```bash
mvn clean package -DskipTests
```

### 3. Usage

---

## CLI Usage

### Basic Syntax

```bash
java -jar db-comparator.jar \
    <oracle_jdbc_url> <oracle_user> <oracle_password> \
    <gauss_jdbc_url> <gauss_user> <gauss_password> \
    <table_list_file> [thread_count] [output_dir]
```

### Parameters

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| oracle_jdbc_url | Yes | Oracle JDBC URL | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| oracle_user | Yes | Oracle username | `scott` |
| oracle_password | Yes | Oracle password | `tiger` |
| gauss_jdbc_url | Yes | GaussDB/PostgreSQL JDBC URL | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |
| gauss_user | Yes | GaussDB username | `scott` |
| gauss_password | Yes | GaussDB password | `tiger` |
| table_list_file | Yes | Table list file path | `table.txt` |
| thread_count | No | Thread count, default 4 | `4` |
| output_dir | No | Output directory, default ./output | `./output` |

### Examples

**Example 1: Basic comparison**
```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt
```

**Example 2: Specify thread count and output directory**
```bash
java -jar db-comparator.jar \
    jdbc:oracle:thin:@192.168.1.100:1521:orcl scott tiger \
    jdbc:postgresql://192.168.1.101:5432/gaussdb scott tiger \
    table.txt 4 ./output
```

---

## SDK Usage

### Add Dependency

```xml
<dependency>
    <groupId>com.datacheck</groupId>
    <artifactId>db-comparator</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Or directly include the JAR file.

### Basic Usage

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
    System.out.println("Total tables: " + summary.getTotalTables());
    System.out.println("Consistent: " + summary.getConsistentCount());
    System.out.println("Different: " + summary.getDifferentCount());
}
```

### API Reference

#### Database Configuration

| Method | Description |
|--------|-------------|
| `oracleJdbcUrl(String url)` | Oracle JDBC URL |
| `oracleUsername(String user)` | Oracle username |
| `oraclePassword(String pwd)` | Oracle password |
| `gaussJdbcUrl(String url)` | GaussDB/PostgreSQL JDBC URL |
| `gaussUsername(String user)` | GaussDB username |
| `gaussPassword(String pwd)` | GaussDB password |

#### Table List Configuration

| Method | Description |
|--------|-------------|
| `tableListFile(String path)` | Table list file path |
| `tables(List<String> names)` | Table name list |
| `tableFilters(List<TableFilter> filters)` | Full table configuration |
| `addTable(String name)` | Add single table |

#### Other Configuration

| Method | Description |
|--------|-------------|
| `threadCount(int count)` | Thread count (default 4) |
| `outputDir(String path)` | Output directory |
| `writeResultFiles(boolean write)` | Write result files |
| `oracleConfig(DatabaseConfig config)` | Oracle config object |
| `gaussConfig(DatabaseConfig config)` | GaussDB config object |

### SDK Examples

**Example 1: Enable result file output**
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
    System.out.println("Total tables: " + summary.getTotalTables());
}
```

**Example 2: Pass table list in code**
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

**Example 3: With filter conditions and primary keys**
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

### Getting Results

```java
// Summary
ComparisonSummary summary = comparator.getSummary();
summary.getTotalTables();      // Total tables
summary.getSuccessCount();     // Success count
summary.getErrorCount();       // Error count
summary.getConsistentCount();  // Consistent count
summary.getDifferentCount();   // Different count

// Detailed results
Map<String, CompareResult> results = comparator.getResults();
for (Map.Entry<String, CompareResult> entry : results.entrySet()) {
    CompareResult result = entry.getValue();
    result.getStatus();        // success / error
    result.getMessage();      // Message
    result.hasDifferences();  // Has differences
}
```

---

## JDBC URL Formats

### Oracle

| Format | Example |
|--------|---------|
| SID | `jdbc:oracle:thin:@192.168.1.100:1521:orcl` |
| Service Name | `jdbc:oracle:thin:@//192.168.1.100:1521/orclpdb1` |

### PostgreSQL / GaussDB

| Format | Example |
|--------|---------|
| Standard | `jdbc:postgresql://192.168.1.101:5432/gaussdb` |

---

## Project Structure

```
db-comparator/
├── src/main/java/com/datacheck/
│   ├── Config.java              # Configuration loading
│   ├── DbComparator.java       # Main program entry
│   ├── compare/
│   │   └── TableComparator.java # Table data comparison
│   ├── db/
│   │   ├── DataFetcher.java     # Data fetching
│   │   ├── DatabaseConnector.java # Database connection
│   │   └── TableMetadata.java  # Table metadata
│   ├── model/
│   │   ├── CompareResult.java  # Comparison result
│   │   ├── Difference.java      # Difference model
│   │   ├── TableData.java       # Table data model
│   │   └── TableFilter.java     # Table filter conditions
│   ├── output/
│   │   └── ResultWriter.java    # Result output
│   └── sdk/                     # SDK module
│       ├── DbComparator.java
│       └── DbComparatorBuilder.java
├── table.txt                    # Table list
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

## Dependencies

- Oracle JDBC Driver (ojdbc8)
- PostgreSQL JDBC Driver (Gauss compatible)
- Jackson (JSON processing)

## License

MIT License
