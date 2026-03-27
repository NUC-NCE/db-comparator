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

    // 分批处理配置
    private int batchSize = 10000;      // 每批处理行数
    private int maxMemoryRows = 50000;  // 最大内存行数，超过则分批处理

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
     * 设置分批处理参数
     * @param batchSize 每批处理行数，默认 10000
     * @param maxMemoryRows 最大内存行数，超过则分批处理，默认 50000
     */
    public void setBatchConfig(int batchSize, int maxMemoryRows) {
        this.batchSize = batchSize;
        this.maxMemoryRows = maxMemoryRows;
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
            // 获取列信息和主键
            List<String> columns = dataFetcher.getMetadata().getOracleColumns(tableName);
            List<String> primaryKeys = tableFilter.getPrimaryKeys();
            if (primaryKeys == null) {
                primaryKeys = dataFetcher.getMetadata().getOraclePrimaryKeys(tableName);
            }

            // 检查是否需要分批处理
            long oracleCount = dataFetcher.countRows(
                connector.getOracleConnection(), tableName,
                tableFilter.getWhereClause(), true);
            long gaussCount = dataFetcher.countRows(
                connector.getGaussConnection(), tableName,
                tableFilter.getWhereClause(), false);
            long totalCount = oracleCount + gaussCount;

            if (totalCount > maxMemoryRows) {
                // 大表分批处理
                System.out.println("表 " + tableName + " 数据量较大(" + totalCount + ")，采用分批处理...");
                result = compareTableBatch(tableName, columns, primaryKeys, tableFilter.getWhereClause());
            } else {
                // 小表直接全量加载
                TableData oracleData = dataFetcher.fetchOracleTableData(
                    tableName,
                    tableFilter.getWhereClause(),
                    primaryKeys
                );
                TableData gaussData = dataFetcher.fetchGaussTableData(
                    tableName,
                    tableFilter.getWhereClause(),
                    primaryKeys
                );
                result = comparator.compare(oracleData, gaussData);
            }
        } catch (Exception e) {
            result.setStatus("error");
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 分批对比大表
     */
    private CompareResult compareTableBatch(String tableName, List<String> columns,
                                          List<String> primaryKeys, String whereClause) throws Exception {
        CompareResult result = new CompareResult();
        result.setTableName(tableName);
        result.setStatus("success");

        // 获取所有主键值
        List<Object[]> oraclePkValues = dataFetcher.fetchPrimaryKeyValues(
            connector.getOracleConnection(), tableName, primaryKeys, whereClause, true);
        List<Object[]> gaussPkValues = dataFetcher.fetchPrimaryKeyValues(
            connector.getGaussConnection(), tableName, primaryKeys, whereClause, false);

        // 构建主键集合用于查找差异
        java.util.Set<String> oraclePkSet = new java.util.HashSet<>();
        for (Object[] row : oraclePkValues) {
            oraclePkSet.add(buildPkKey(row, primaryKeys));
        }
        java.util.Set<String> gaussPkSet = new java.util.HashSet<>();
        for (Object[] row : gaussPkValues) {
            gaussPkSet.add(buildPkKey(row, primaryKeys));
        }

        // 找出各自独有的主键
        java.util.Set<String> oracleOnly = new java.util.HashSet<>(oraclePkSet);
        oracleOnly.removeAll(gaussPkSet);

        java.util.Set<String> gaussOnly = new java.util.HashSet<>(gaussPkSet);
        gaussOnly.removeAll(oraclePkSet);

        java.util.Set<String> commonPk = new java.util.HashSet<>(oraclePkSet);
        commonPk.retainAll(gaussPkSet);

        List<com.datacheck.model.Difference> differences = new ArrayList<>();

        // 处理 Oracle 独有的数据
        for (Object[] pkRow : oraclePkValues) {
            String pkKey = buildPkKey(pkRow, primaryKeys);
            if (oracleOnly.contains(pkKey)) {
                List<Object[]> rows = dataFetcher.fetchRowsByPrimaryKeyBatch(
                    connector.getOracleConnection(), tableName, primaryKeys,
                    java.util.Collections.singletonList(pkRow), whereClause, true);
                if (!rows.isEmpty()) {
                    com.datacheck.model.Difference diff = new com.datacheck.model.Difference();
                    diff.setType(com.datacheck.model.Difference.ORACLE_ONLY);
                    diff.setPkKey(pkKey);
                    diff.setOracleData(rowToMap(rows.get(0), columns));
                    differences.add(diff);
                }
            }
        }

        // 处理 Gauss 独有的数据
        for (Object[] pkRow : gaussPkValues) {
            String pkKey = buildPkKey(pkRow, primaryKeys);
            if (gaussOnly.contains(pkKey)) {
                List<Object[]> rows = dataFetcher.fetchRowsByPrimaryKeyBatch(
                    connector.getGaussConnection(), tableName, primaryKeys,
                    java.util.Collections.singletonList(pkRow), whereClause, false);
                if (!rows.isEmpty()) {
                    com.datacheck.model.Difference diff = new com.datacheck.model.Difference();
                    diff.setType(com.datacheck.model.Difference.GAUSS_ONLY);
                    diff.setPkKey(pkKey);
                    diff.setGaussData(rowToMap(rows.get(0), columns));
                    differences.add(diff);
                }
            }
        }

        // 分批比对共同主键的数据
        // 将共同主键集合转换为列表，然后分批
        java.util.List<String> commonPkList = new java.util.ArrayList<>(commonPk);
        java.util.List<java.util.List<String>> commonPkBatches = partitionStringList(commonPkList, batchSize);

        System.out.println("共同主键数量: " + commonPk.size() + "，分 " + commonPkBatches.size() + " 批处理");

        for (java.util.List<String> pkBatch : commonPkBatches) {
            // 根据主键字符串批次获取对应的主键值列表
            java.util.List<Object[]> oraclePkBatch = new java.util.ArrayList<>();
            for (Object[] pkRow : oraclePkValues) {
                if (pkBatch.contains(buildPkKey(pkRow, primaryKeys))) {
                    oraclePkBatch.add(pkRow);
                }
            }

            java.util.List<Object[]> gaussPkBatch = new java.util.ArrayList<>();
            for (Object[] pkRow : gaussPkValues) {
                if (pkBatch.contains(buildPkKey(pkRow, primaryKeys))) {
                    gaussPkBatch.add(pkRow);
                }
            }

            // 获取批次数据
            List<Object[]> oracleRows = dataFetcher.fetchRowsByPrimaryKeyBatch(
                connector.getOracleConnection(), tableName, primaryKeys, oraclePkBatch, whereClause, true);
            List<Object[]> gaussRows = dataFetcher.fetchRowsByPrimaryKeyBatch(
                connector.getGaussConnection(), tableName, primaryKeys, gaussPkBatch, whereClause, false);

            // 构建主键到行的映射
            java.util.Map<String, Object[]> oracleMap = new java.util.HashMap<>();
            for (Object[] row : oracleRows) {
                oracleMap.put(buildPkKeyFromRow(row, columns, primaryKeys), row);
            }
            java.util.Map<String, Object[]> gaussMap = new java.util.HashMap<>();
            for (Object[] row : gaussRows) {
                gaussMap.put(buildPkKeyFromRow(row, columns, primaryKeys), row);
            }

            // 找出共同主键中数据不同的
            java.util.Set<String> commonAndBothExist = new java.util.HashSet<>(oracleMap.keySet());
            commonAndBothExist.retainAll(gaussMap.keySet());

            for (String pkKey : commonAndBothExist) {
                Object[] oracleRow = oracleMap.get(pkKey);
                Object[] gaussRow = gaussMap.get(pkKey);
                if (!rowEquals(oracleRow, gaussRow, columns)) {
                    com.datacheck.model.Difference diff = new com.datacheck.model.Difference();
                    diff.setType(com.datacheck.model.Difference.DIFFERENT);
                    diff.setPkKey(pkKey);
                    diff.setOracleData(rowToMap(oracleRow, columns));
                    diff.setGaussData(rowToMap(gaussRow, columns));
                    differences.add(diff);
                }
            }
        }

        result.setDifferences(differences);
        return result;
    }

    /**
     * 将列表分批
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return batches;
    }

    /**
     * 将字符串列表分批
     */
    private List<List<String>> partitionStringList(List<String> list, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return batches;
    }

    /**
     * 根据主键值数组构建主键字符串
     */
    private String buildPkKey(Object[] pkRow, List<String> primaryKeys) {
        if (pkRow == null || pkRow.length == 0) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pkRow.length; i++) {
            if (i > 0) sb.append("_");
            sb.append(pkRow[i] != null ? pkRow[i].toString() : "NULL");
        }
        return sb.toString();
    }

    /**
     * 根据行数据和列信息构建主键字符串
     */
    private String buildPkKeyFromRow(Object[] row, List<String> columns, List<String> primaryKeys) {
        if (row == null || primaryKeys == null || primaryKeys.isEmpty()) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < primaryKeys.size(); i++) {
            if (i > 0) sb.append("_");
            int colIndex = columns.indexOf(primaryKeys.get(i));
            if (colIndex >= 0 && colIndex < row.length) {
                sb.append(row[colIndex] != null ? row[colIndex].toString() : "NULL");
            } else {
                sb.append("NULL");
            }
        }
        return sb.toString();
    }

    /**
     * 将行数据转换为 Map
     */
    private java.util.Map<String, Object> rowToMap(Object[] row, List<String> columns) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (row != null) {
            for (int i = 0; i < columns.size() && i < row.length; i++) {
                map.put(columns.get(i), row[i]);
            }
        }
        return map;
    }

    /**
     * 比较两行数据是否相等（使用 ValueComparator）
     */
    private boolean rowEquals(Object[] row1, Object[] row2, List<String> columns) {
        if (row1 == row2) return true;
        if (row1 == null || row2 == null) return false;
        if (row1.length != row2.length) return false;
        for (int i = 0; i < row1.length; i++) {
            String columnName = columns.get(i);
            if (shouldIgnoreTimeField(columnName)) {
                continue;
            }
            if (!com.datacheck.util.ValueComparator.equals(row1[i], row2[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否应该忽略时间类型字段
     */
    private boolean shouldIgnoreTimeField(String columnName) {
        if (columnName == null) return false;
        String upper = columnName.toUpperCase();
        return upper.endsWith("_TIME") || upper.endsWith("_DATE")
            || upper.contains("TIME") || upper.contains("DATE")
            || upper.contains("TIMESTAMP")
            || upper.equals("CREATEDATE") || upper.equals("UPDATEDATE")
            || upper.equals("CREATETIME") || upper.equals("UPDATETIME")
            || upper.equals("MODIFYDATE")
            || upper.equals("DATA_DATA") || upper.equals("DATADATA")
            || upper.equals("SYSDATE") || upper.equals("SYSTIMESTAMP")
            || upper.equals("CURRENT_DATE") || upper.equals("CURRENT_TIMESTAMP");
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
