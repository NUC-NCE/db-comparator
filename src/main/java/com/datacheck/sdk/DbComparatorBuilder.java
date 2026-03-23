package com.datacheck.sdk;

import com.datacheck.Config;
import com.datacheck.model.TableFilter;
import com.datacheck.sdk.config.DatabaseConfig;
import com.datacheck.sdk.config.SdkOptions;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库对比SDK - Builder链式构建类
 */
public class DbComparatorBuilder {

    // Oracle 配置 (使用JDBC连接串)
    private String oracleJdbcUrl;
    private String oracleUsername;
    private String oraclePassword;

    // GaussDB 配置 (使用JDBC连接串)
    private String gaussJdbcUrl;
    private String gaussUsername;
    private String gaussPassword;

    // 外部注入的数据库连接
    private Connection oracleConnection;
    private Connection gaussConnection;

    // 表列表
    private String tableListFile;
    private List<String> tableNames = new ArrayList<>();
    private List<TableFilter> tableFilters = new ArrayList<>();

    // 从数据库获取表配置的表名
    private String tableConfigTable = "table_check_info";

    // 选项配置
    private int threadCount = 4;
    private String outputDir = "./output";
    private boolean writeResultFiles = false;
    private SdkOptions options;

    // 分批处理配置
    private int batchSize = 10000;      // 每批处理行数
    private int maxMemoryRows = 50000;  // 最大内存行数

    /**
     * Oracle JDBC连接串
     * 例如: jdbc:oracle:thin:@192.168.1.100:1521:orcl
     * 或:  jdbc:oracle:thin:@//192.168.1.100:1521/serviceName
     */
    public DbComparatorBuilder oracleJdbcUrl(String oracleJdbcUrl) {
        this.oracleJdbcUrl = oracleJdbcUrl;
        return this;
    }

    /**
     * Oracle 用户名
     */
    public DbComparatorBuilder oracleUsername(String oracleUsername) {
        this.oracleUsername = oracleUsername;
        return this;
    }

    /**
     * Oracle 密码
     */
    public DbComparatorBuilder oraclePassword(String oraclePassword) {
        this.oraclePassword = oraclePassword;
        return this;
    }

    /**
     * GaussDB JDBC连接串
     * 例如: jdbc:postgresql://192.168.1.101:5432/gaussdb
     */
    public DbComparatorBuilder gaussJdbcUrl(String gaussJdbcUrl) {
        this.gaussJdbcUrl = gaussJdbcUrl;
        return this;
    }

    /**
     * GaussDB 用户名
     */
    public DbComparatorBuilder gaussUsername(String gaussUsername) {
        this.gaussUsername = gaussUsername;
        return this;
    }

    /**
     * GaussDB 密码
     */
    public DbComparatorBuilder gaussPassword(String gaussPassword) {
        this.gaussPassword = gaussPassword;
        return this;
    }

    /**
     * 直接传入已建立的 Oracle 数据库连接
     * 使用此方法后将忽略 oracleJdbcUrl/oracleUsername/oraclePassword 配置
     */
    public DbComparatorBuilder oracleConnection(Connection connection) {
        this.oracleConnection = connection;
        return this;
    }

    /**
     * 直接传入已建立的 GaussDB 数据库连接
     * 使用此方法后将忽略 gaussJdbcUrl/gaussUsername/gaussPassword 配置
     */
    public DbComparatorBuilder gaussConnection(Connection connection) {
        this.gaussConnection = connection;
        return this;
    }

    /**
     * Oracle 配置对象
     */
    public DbComparatorBuilder oracleConfig(DatabaseConfig config) {
        this.oracleJdbcUrl = config.getJdbcUrl();
        this.oracleUsername = config.getUsername();
        this.oraclePassword = config.getPassword();
        return this;
    }

    /**
     * GaussDB 配置对象
     */
    public DbComparatorBuilder gaussConfig(DatabaseConfig config) {
        this.gaussJdbcUrl = config.getJdbcUrl();
        this.gaussUsername = config.getUsername();
        this.gaussPassword = config.getPassword();
        return this;
    }

    /**
     * 表列表文件路径
     */
    public DbComparatorBuilder tableListFile(String tableListFile) {
        this.tableListFile = tableListFile;
        return this;
    }

    /**
     * 表名列表
     */
    public DbComparatorBuilder tables(List<String> tableNames) {
        this.tableNames.addAll(tableNames);
        return this;
    }

    /**
     * 表过滤器列表（支持过滤条件和主键配置）
     */
    public DbComparatorBuilder tableFilters(List<TableFilter> tableFilters) {
        this.tableFilters.addAll(tableFilters);
        return this;
    }

    /**
     * 添加单个表
     */
    public DbComparatorBuilder addTable(String tableName) {
        this.tableNames.add(tableName);
        return this;
    }

    /**
     * 设置从数据库获取表配置的表名
     * 默认表名为 table_check_info
     * 表结构: table_name, condition, key
     */
    public DbComparatorBuilder tableConfigTable(String tableConfigTable) {
        this.tableConfigTable = tableConfigTable;
        return this;
    }

    /**
     * 线程数
     */
    public DbComparatorBuilder threadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    /**
     * 输出目录
     */
    public DbComparatorBuilder outputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * 是否写入结果文件
     */
    public DbComparatorBuilder writeResultFiles(boolean writeResultFiles) {
        this.writeResultFiles = writeResultFiles;
        return this;
    }

    /**
     * SDK选项配置
     */
    public DbComparatorBuilder options(SdkOptions options) {
        this.options = options;
        if (options != null) {
            if (options.getThreadCount() > 0) {
                this.threadCount = options.getThreadCount();
            }
            if (options.getOutputDir() != null) {
                this.outputDir = options.getOutputDir();
            }
            this.writeResultFiles = options.isWriteResultFiles();
            if (options.getBatchSize() > 0) {
                this.batchSize = options.getBatchSize();
            }
            if (options.getMaxMemoryRows() > 0) {
                this.maxMemoryRows = options.getMaxMemoryRows();
            }
        }
        return this;
    }

    /**
     * 设置分批处理参数
     * @param batchSize 每批处理行数，默认 10000
     * @param maxMemoryRows 最大内存行数，超过则分批处理，默认 50000
     */
    public DbComparatorBuilder batchConfig(int batchSize, int maxMemoryRows) {
        this.batchSize = batchSize;
        this.maxMemoryRows = maxMemoryRows;
        return this;
    }

    /**
     * 构建 DbComparator 实例
     */
    public DbComparator build() {
        // 构建内部 Config 对象
        Config config = new Config();
        config.setThreadCount(threadCount);
        config.setOutputDir(outputDir);

        // 构建数据库配置 - 直接使用JDBC连接串
        Config.OracleConfig oracleConfig = new Config.OracleConfig();
        oracleConfig.setJdbcUrl(oracleJdbcUrl);
        oracleConfig.setUsername(oracleUsername);
        oracleConfig.setPassword(oraclePassword);

        Config.GaussConfig gaussConfig = new Config.GaussConfig();
        gaussConfig.setJdbcUrl(gaussJdbcUrl);
        gaussConfig.setUsername(gaussUsername);
        gaussConfig.setPassword(gaussPassword);

        config.setOracle(oracleConfig);
        config.setGauss(gaussConfig);

        // 获取表过滤器列表
        List<TableFilter> filters = new ArrayList<>();

        // 优先级：tableFilters > tableListFile > tableNames
        if (tableFilters != null && !tableFilters.isEmpty()) {
            filters.addAll(tableFilters);
        } else if (tableListFile != null && !tableListFile.isEmpty()) {
            // 从文件读取表配置
            try {
                filters.addAll(DbComparator.readTableNames(tableListFile));
            } catch (Exception e) {
                throw new RuntimeException("读取表列表文件失败: " + tableListFile, e);
            }
        } else if (tableNames != null && !tableNames.isEmpty()) {
            for (String tableName : tableNames) {
                filters.add(new TableFilter(tableName));
            }
        }

        DbComparator comparator = new DbComparator(config, filters, writeResultFiles);

        // 设置外部注入的数据库连接
        if (oracleConnection != null || gaussConnection != null) {
            comparator.setExternalConnections(oracleConnection, gaussConnection);
        }

        // 设置从数据库获取表配置的表名
        if (tableConfigTable != null && !tableConfigTable.isEmpty()) {
            comparator.setTableConfigTable(tableConfigTable);
        }

        // 设置分批处理参数
        comparator.setBatchConfig(batchSize, maxMemoryRows);

        return comparator;
    }
}
