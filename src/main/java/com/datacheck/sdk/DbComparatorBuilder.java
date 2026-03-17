package com.datacheck.sdk;

import com.datacheck.Config;
import com.datacheck.model.TableFilter;
import com.datacheck.sdk.config.DatabaseConfig;
import com.datacheck.sdk.config.SdkOptions;

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

    // 表列表
    private String tableListFile;
    private List<String> tableNames = new ArrayList<>();
    private List<TableFilter> tableFilters = new ArrayList<>();

    // 选项配置
    private int threadCount = 4;
    private String outputDir = "./output";
    private boolean writeResultFiles = false;
    private SdkOptions options;

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
        }
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

        return new DbComparator(config, filters, writeResultFiles);
    }
}
