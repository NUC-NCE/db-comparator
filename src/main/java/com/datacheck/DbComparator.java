package com.datacheck;

import com.datacheck.compare.TableComparator;
import com.datacheck.db.DataFetcher;
import com.datacheck.db.DatabaseConnector;
import com.datacheck.model.CompareResult;
import com.datacheck.model.Difference;
import com.datacheck.model.TableData;
import com.datacheck.model.TableFilter;
import com.datacheck.output.ResultWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 数据库对比程序主入口
 */
public class DbComparator {
    private final Config config;
    private DatabaseConnector connector;
    private DataFetcher dataFetcher;
    private TableComparator comparator;
    private ResultWriter resultWriter;
    private final Map<String, CompareResult> results = new ConcurrentHashMap<>();

    public DbComparator(Config config) {
        this.config = config;
    }

    /**
     * 初始化组件
     */
    public void init() {
        connector = new DatabaseConnector(config);
        comparator = new TableComparator();
        resultWriter = new ResultWriter(config.getOutputDir());
    }

    /**
     * 连接数据库
     */
    public void connect() throws Exception {
        connector.connectAll();
        dataFetcher = new DataFetcher(
            connector.getOracleConnection(),
            connector.getGaussConnection(),
            config
        );
    }

    /**
     * 执行单个表对比
     */
    public CompareResult compareTable(TableFilter tableFilter) {
        String tableName = tableFilter.getTableName();
        CompareResult result = new CompareResult();
        result.setTableName(tableName);

        try {
            TableData oracleData = dataFetcher.fetchOracleTableData(
                tableName,
                tableFilter.getFilterColumn(),
                tableFilter.getFilterValue(),
                tableFilter.getPrimaryKeys()
            );
            TableData gaussData = dataFetcher.fetchGaussTableData(
                tableName,
                tableFilter.getFilterColumn(),
                tableFilter.getFilterValue(),
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
    public void run(List<TableFilter> tableFilters) throws Exception {
        int threadCount = config.getThreadCount();
        System.out.println("开始对比 " + tableFilters.size() + " 个表，使用 " + threadCount + " 个线程...");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<CompareResult>> futures = new ArrayList<>();

        // 提交所有任务
        for (TableFilter tableFilter : tableFilters) {
            String desc = tableFilter.hasFilter()
                ? tableFilter.getTableName() + " (过滤: " + tableFilter.getFilterColumn() + "=" + tableFilter.getFilterValue() + ")"
                : tableFilter.getTableName();
            System.out.println("对比表: " + desc);
            futures.add(executor.submit(() -> compareTable(tableFilter)));
        }

        // 收集结果
        for (Future<CompareResult> future : futures) {
            try {
                CompareResult result = future.get();
                results.put(result.getTableName(), result);
                resultWriter.write(result);
            } catch (Exception e) {
                System.err.println("对比失败: " + e.getMessage());
            }
        }

        executor.shutdown();
        printSummary(tableFilters.size(), tableFilters);
    }

    /**
     * 打印汇总信息
     */
    private void printSummary(int total, List<TableFilter> tableFilters) {
        long success = results.values().stream()
            .filter(r -> "success".equals(r.getStatus())).count();
        long error = results.values().stream()
            .filter(r -> "error".equals(r.getStatus())).count();
        long diffCount = results.values().stream()
            .filter(CompareResult::hasDifferences).count();

        // 收集对比失败的表名称和原因
        List<String> failedTables = new ArrayList<>();
        Map<String, String> failedReasons = new HashMap<>();
        for (TableFilter filter : tableFilters) {
            String tableName = filter.getTableName();
            CompareResult result = results.get(tableName);
            if (result != null && result.hasDifferences()) {
                failedTables.add(tableName);
                // 收集差异信息 - 只打印值不同的字段
                String reason = result.getDifferences().stream()
                    .map(d -> {
                        String pkInfo = d.getPkKey() != null ? "[" + d.getPkKey() + "]" : "";
                        String typeName = d.getTypeName();

                        if (Difference.DIFFERENT.equals(d.getType())) {
                            // 值不同的字段
                            Map<String, Object> oracleData = d.getOracleData();
                            Map<String, Object> gaussData = d.getGaussData();
                            List<String> diffFields = new ArrayList<>();
                            for (String key : oracleData.keySet()) {
                                Object oracleVal = oracleData.get(key);
                                Object gaussVal = gaussData.get(key);
                                if (oracleVal == null ? gaussVal != null : !oracleVal.equals(gaussVal)) {
                                    diffFields.add(key + "=" + oracleVal + "→" + gaussVal);
                                }
                            }
                            return pkInfo + String.join(", ", diffFields);
                        } else if (Difference.ORACLE_ONLY.equals(d.getType())) {
                            // Oracle独有的数据
                            Map<String, Object> oracleData = d.getOracleData();
                            List<String> fields = new ArrayList<>(oracleData.keySet());
                            return pkInfo + "Oracle独有: " + String.join(", ", fields);
                        } else if (Difference.GAUSS_ONLY.equals(d.getType())) {
                            // Gauss独有的数据
                            Map<String, Object> gaussData = d.getGaussData();
                            List<String> fields = new ArrayList<>(gaussData.keySet());
                            return pkInfo + "Gauss独有: " + String.join(", ", fields);
                        }
                        return pkInfo + typeName;
                    })
                    .collect(Collectors.joining("; "));
                failedReasons.put(tableName, reason);
            }
        }

        System.out.println("\n========================================");
        System.out.println("对比完成!");
        System.out.println("总计: " + total + " 个表");
        System.out.println("成功: " + success + ", 失败: " + error);
        System.out.println("数据一致: " + (total - diffCount) + ", 数据不一致: " + diffCount);

        if (!failedTables.isEmpty()) {
            System.out.println("\n对比失败的表:");
            for (String tableName : failedTables) {
                String reason = failedReasons.get(tableName);
                System.out.println("  - " + tableName + ": " + reason);
            }
        }
    }

    /**
     * 关闭连接
     */
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

    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        String configPath = "config.json";
        String tableFilePath = "table.txt";

        if (args.length >= 2) {
            configPath = args[0];
            tableFilePath = args[1];
        }

        try {
            // 读取表配置列表
            List<TableFilter> tableFilters = readTableNames(tableFilePath);
            if (tableFilters.isEmpty()) {
                System.err.println("错误: " + tableFilePath + " 中没有表配置");
                System.exit(1);
            }
            System.out.println("从 " + tableFilePath + " 读取到 " + tableFilters.size() + " 个表配置");

            // 加载配置
            Config config = Config.load(configPath);

            // 运行对比
            DbComparator comparator = new DbComparator(config);
            comparator.init();
            comparator.connect();
            comparator.run(tableFilters);
            comparator.close();

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
