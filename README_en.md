[中文](./README.md)

# Database Comparator (DbComparator)

A tool for comparing table data between Oracle and Gauss databases with multi-threaded concurrent comparison support.

## Features

- Compare data between Oracle and Huawei GaussDB databases
- Multi-threaded concurrent processing for improved efficiency
- Support table filtering conditions
- Custom primary key configuration support
- Detailed difference report output

## Requirements

- Java 1.8 or higher
- Maven 3.x
- Oracle JDBC Driver
- PostgreSQL JDBC Driver (for GaussDB)

## Quick Start

### 1. Configuration File (config.json)

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

Configuration:
- `oracle`: Oracle database connection settings
- `gauss`: Gauss database connection settings
- `threadCount`: Number of concurrent threads (default: 4)
- `outputDir`: Output directory for difference reports

### 2. Table List File (table.txt)

List the table names to compare in `table.txt`, one table name per line.

Supports configuration with filter conditions:


Examples:
```
users 1 1 []
orders status active [pk1,pk2...]
```

### 3. Build the Project

```bash
./build.sh
```

Or manually execute:
```bash
mvn clean package -DskipTests
```

### 4. Run the Program

```bash
./run.sh
```

Or specify configuration files:
```bash
java -jar target/db-comparator-1.0-SNAPSHOT.jar config.json table.txt
```

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
│   └── output/
│       └── ResultWriter.java    # Result output
├── config.json                  # Configuration file
├── table.txt                    # Table list
├── build.sh                     # Build script
├── run.sh                       # Run script
└── pom.xml                      # Maven configuration
```

## Dependencies

- Oracle JDBC Driver (ojdbc8)
- PostgreSQL JDBC Driver (Gauss compatible)
- Jackson (JSON processing)
- Lombok

## License

MIT License
