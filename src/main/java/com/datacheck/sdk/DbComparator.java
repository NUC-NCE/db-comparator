package com.datacheck.sdk;

import com.datacheck.Config;
import com.datacheck.compare.TableComparator;
import com.datacheck.db.DataFetcher;
import com.datacheck.db.DatabaseConnector;
import com.datacheck.model.CompareResult;
import com.datacheck.model.TableData;
import com.datacheck.model.TableFilter;
import com.datacheck.output.ResultWriter;
import com.datacheck.sdk.model.ComparisonSummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 数据库对比SDK - 主入口类
 */
public class DbComparator implements AutoCloseable {

    private final Config config;
    private List<TableFilter> tableFilters;
    private final boolean writeResultFiles;

    private DatabaseConnector connector;
    private DataFetcher dataFetcher;
    private TableComparator comparator;
    private ResultWriter resultWriter;

    // 外部注入的数据库连接
    private Connection externalOracleConnection;
    private Connection externalGaussConnection;

    // 从数据库获取表配置的表名
    private String tableConfigTable;

    private final Map<String, CompareResult> results = new ConcurrentHashMap<>();
    private ComparisonSummary summary;

    /**
     * 私有构造函数，通过 Builder 创建
     */
    DbComparator(Config config, List<TableFilter> tableFilters, boolean writeResultFiles) {
        this.config = config;
        this.tableFilters = tableFilters;
        this.writeResultFiles = writeResultFiles;
    }

    /**
     * 设置外部注入的数据库连接
     */
    public void setExternalConnections(Connection oracleConnection, Connection gaussConnection) {
        this.externalOracleConnection = oracleConnection;
        this.externalGaussConnection = gaussConnection;
    }

    /**
     * 设置从数据库获取表配置的表名
     * 默认为 table_check_info
     */
    public void setTableConfigTable(String tableConfigTable) {
        this.tableConfigTable = tableConfigTable;
    }

    /**
     * 创建 Builder 用于链式配置
     */
    public static DbComparatorBuilder builder() {
        return new DbComparatorBuilder();
    }

    /**
     * 初始化组件
     */
    public void init() {
        // 创建输出目录
        String outputDir = config.getOutputDir();
        if (outputDir != null && !outputDir.isEmpty()) {
            File dir = new File(outputDir.replace("~", System.getProperty("user.home")));
            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("创建输出目录: " + dir.getAbsolutePath());
            }
        }

        connector = new DatabaseConnector(config);
        comparator = new TableComparator();
        if (writeResultFiles) {
            resultWriter = new ResultWriter(config.getOutputDir());
        }
    }

    /**
     * 连接数据库
     * 如果外部连接已注入，则使用外部连接
     * 如果设置了tableConfigTable，则从数据库加载表配置
     */
    public void connect() throws Exception {
        if (externalOracleConnection != null) {
            connector.setExternalOracleConnection(externalOracleConnection);
        }
        if (externalGaussConnection != null) {
            connector.setExternalGaussConnection(externalGaussConnection);
        }
        connector.connectAll();
        dataFetcher = new DataFetcher(
            connector.getOracleConnection(),
            connector.getGaussConnection(),
            config
        );

        // 如果设置了从数据库获取表配置，则加载表配置
        if (tableConfigTable != null && !tableConfigTable.isEmpty()) {
            System.out.println("从数据库表 " + tableConfigTable + " 获取表配置...");
            List<TableFilter> dbFilters = dataFetcher.fetchTableFiltersFromOracle(tableConfigTable);
            if (dbFilters != null && !dbFilters.isEmpty()) {
                this.tableFilters.clear();
                this.tableFilters.addAll(dbFilters);
                System.out.println("从数据库获取到 " + this.tableFilters.size() + " 个表配置");
            }
        }
    }

    /**
     * 执行对比（使用配置的所有表）
     */
    public List<CompareResult> compare() throws Exception {
        if (tableFilters == null || tableFilters.isEmpty()) {
            throw new IllegalStateException("没有配置要对比的表");
        }
        return runComparison(tableFilters);
    }

    /**
     * 执行指定表的对比
     */
    public List<CompareResult> compareTables(List<TableFilter> tables) throws Exception {
        if (tables == null || tables.isEmpty()) {
            throw new IllegalStateException("没有指定要对比的表");
        }
        return runComparison(tables);
    }

    /**
     * 执行单个表对比
     */
    public CompareResult compareTable(String tableName) throws Exception {
        return compareTable(new TableFilter(tableName));
    }

    /**
     * 执行单个表对比（带过滤条件）
     */
    public CompareResult compareTable(String tableName, String whereClause, List<String> primaryKeys) throws Exception {
        TableFilter filter = new TableFilter(tableName);
        filter.setWhereClause(whereClause);
        if (primaryKeys != null) {
            filter.setPrimaryKeys(primaryKeys);
        }
        return compareTable(filter);
    }

    /**
     * 内部方法：执行表对比
     */
    private CompareResult compareTable(TableFilter tableFilter) {
        String tableName = tableFilter.getTableName();
        CompareResult result = new CompareResult();
        result.setTableName(tableName);

        try {
            TableData oracleData = dataFetcher.fetchOracleTableData(
                tableName,
                tableFilter.getWhereClause(),
                tableFilter.getPrimaryKeys()
            );
            TableData gaussData = dataFetcher.fetchGaussTableData(
                tableName,
                tableFilter.getWhereClause(),
                tableFilter.getPrimaryKeys()
            );

            result = comparator.compare(oracleData, gaussData);
        } catch (Exception e) {
            result.setStatus("error");
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 执行多线程对比
     */
    private List<CompareResult> runComparison(List<TableFilter> filters) throws Exception {
        int threadCount = config.getThreadCount();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<CompareResult>> futures = new ArrayList<>();

        // 提交所有任务
        for (TableFilter tableFilter : filters) {
            futures.add(executor.submit(() -> compareTable(tableFilter)));
        }

        // 收集结果
        for (Future<CompareResult> future : futures) {
            try {
                CompareResult result = future.get();
                results.put(result.getTableName(), result);
                if (writeResultFiles && resultWriter != null) {
                    resultWriter.write(result);
                }
            } catch (Exception e) {
                // 记录错误
            }
        }

        executor.shutdown();

        // 生成汇总信息
        buildSummary(filters);

        return new ArrayList<>(results.values());
    }

    /**
     * 构建汇总信息
     */
    private void buildSummary(List<TableFilter> filters) {
        summary = new ComparisonSummary();
        summary.setTotalTables(filters.size());

        for (TableFilter filter : filters) {
            CompareResult result = results.get(filter.getTableName());
            if (result != null) {
                if ("error".equals(result.getStatus())) {
                    summary.addFailedTable(filter.getTableName(), result.getMessage());
                } else {
                    summary.addSuccess(!result.hasDifferences());
                }
            }
        }
    }

    /**
     * 获取所有对比结果
     */
    public Map<String, CompareResult> getResults() {
        return results;
    }

    /**
     * 获取汇总统计信息
     */
    public ComparisonSummary getSummary() {
        return summary;
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        if (connector != null) {
            connector.closeAll();
        }
    }

    /**
     * 读取表配置列表（支持带过滤条件）
     */
    public static List<TableFilter> readTableNames(String filePath) throws Exception {
        List<TableFilter> tableFilters = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    tableFilters.add(TableFilter.parse(line));
                }
            }
        }
        return tableFilters;
    }
}
