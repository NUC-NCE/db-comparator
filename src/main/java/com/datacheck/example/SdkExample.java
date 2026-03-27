package com.datacheck.example;

import com.datacheck.sdk.DbComparator;
import com.datacheck.sdk.config.DatabaseConfig;
import com.datacheck.sdk.config.SdkOptions;
import com.datacheck.sdk.model.ComparisonSummary;
import com.datacheck.model.CompareResult;
import com.datacheck.model.TableFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SDK 使用示例 - 使用JDBC连接串
 */
public class SdkExample {

    public static void main(String[] args) throws Exception {
        // 示例1: 链式调用（最简单）
        example1();

        // 示例2: 使用配置对象
        example2();

        // 示例3: 代码传入表列表
        example3();
    }

    /**
     * 示例1: 链式调用（最简单）
     * 直接传入JDBC连接串，程序不再拼接
     */
    static void example1() throws Exception {
        System.out.println("========== 示例1: 链式调用 ==========");

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

            // 获取汇总结果
            ComparisonSummary summary = comparator.getSummary();
            System.out.println("总表数: " + summary.getTotalTables());
            System.out.println("成功: " + summary.getSuccessCount());
            System.out.println("数据一致: " + summary.getConsistentCount());
            System.out.println("数据不一致: " + summary.getDifferentCount());
            System.out.println("错误: " + summary.getErrorCount());

            // 获取详细结果
            Map<String, CompareResult> results = comparator.getResults();
            for (Map.Entry<String, CompareResult> entry : results.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue().getStatus());
            }
        }
    }

    /**
     * 示例2: 使用配置对象
     * 适合配置信息来自外部文件或数据库的场景
     */
    static void example2() throws Exception {
        System.out.println("\n========== 示例2: 使用配置对象 ==========");

        // 构建 Oracle 配置 - 传入JDBC连接串
        DatabaseConfig oracleConfig = new DatabaseConfig(
            "jdbc:oracle:thin:@192.168.1.100:1521:orcl",
            "scott",
            "tiger"
        );

        // 构建 GaussDB 配置 - 传入JDBC连接串
        DatabaseConfig gaussConfig = new DatabaseConfig(
            "jdbc:postgresql://192.168.1.101:5432/gaussdb",
            "scott",
            "tiger"
        );

        // 构建 SDK 选项
        SdkOptions options = new SdkOptions();
        options.setThreadCount(4);
        options.setOutputDir("./output");
        options.setWriteResultFiles(true);

        try (DbComparator comparator = DbComparator.builder()
            .oracleConfig(oracleConfig)
            .gaussConfig(gaussConfig)
            .tableListFile("table.txt")
            .options(options)
            .build()) {

            comparator.init();
            comparator.connect();
            comparator.compare();

            ComparisonSummary summary = comparator.getSummary();
            System.out.println("对比完成!");
            System.out.println("数据一致: " + summary.getConsistentCount());
            System.out.println("数据不一致: " + summary.getDifferentCount());
        }
    }

    /**
     * 示例3: 代码传入表列表
     * 适合表名是动态确定的场景
     */
    static void example3() throws Exception {
        System.out.println("\n========== 示例3: 代码传入表列表 ==========");

        // 直接传入表名列表
        List<String> tableNames = Arrays.asList("users", "orders", "products", "employees");

        try (DbComparator comparator = DbComparator.builder()
            .oracleJdbcUrl("jdbc:oracle:thin:@192.168.1.100:1521:orcl")
            .oracleUsername("scott")
            .oraclePassword("tiger")
            .gaussJdbcUrl("jdbc:postgresql://192.168.1.101:5432/gaussdb")
            .gaussUsername("scott")
            .gaussPassword("tiger")
            .tables(tableNames)
            .threadCount(2)
            .build()) {

            comparator.init();
            comparator.connect();
            comparator.compare();

            ComparisonSummary summary = comparator.getSummary();
            System.out.println("总计对比: " + summary.getTotalTables() + " 个表");
            System.out.println("数据一致: " + summary.getConsistentCount() + " 个表");
            System.out.println("数据不一致: " + summary.getDifferentCount() + " 个表");
        }
    }
}
