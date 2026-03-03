package com.datacheck.output;

import com.datacheck.model.CompareResult;
import com.datacheck.model.Difference;

import java.io.*;
import java.util.Date;
import java.util.Map;

/**
 * 对比结果写入器
 */
public class ResultWriter {
    private final String outputDir;

    public ResultWriter(String outputDir) {
        this.outputDir = outputDir.replace("~", System.getProperty("user.home"));
    }

    /**
     * 写入对比结果
     */
    public void write(CompareResult result) {
        // 如果数据一致，不生成文件
        if (!result.hasDifferences()) {
            System.out.println("表 " + result.getTableName() + ": 数据一致，不生成文件");
            return;
        }

        File dir = new File(outputDir);
        dir.mkdirs();

        File outputFile = new File(dir, result.getTableName() + ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writeHeader(writer, result);
            writeDifferences(writer, result.getDifferences());

            System.out.println("表 " + result.getTableName() + ": 结果已写入 " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("写入文件失败: " + e.getMessage());
        }
    }

    /**
     * 写入文件头
     */
    private void writeHeader(PrintWriter writer, CompareResult result) {
        writer.println("表名: " + result.getTableName());
        writer.println("对比时间: " + new Date());
        writer.println("状态: " + result.getStatus());
        writer.println("结果: " + result.getMessage());
        writer.println("================================================================================");
        writer.println();
    }

    /**
     * 写入差异详情
     */
    private void writeDifferences(PrintWriter writer, java.util.List<Difference> differences) {
        for (Difference diff : differences) {
            writer.println("主键: " + diff.getPkKey());

            if (Difference.DIFFERENT.equals(diff.getType())) {
                // 值不同的字段，逐列显示
                Map<String, Object> oracleData = diff.getOracleData();
                Map<String, Object> gaussData = diff.getGaussData();
                // 合并所有列名
                java.util.Set<String> allColumns = new java.util.HashSet<>();
                allColumns.addAll(oracleData.keySet());
                allColumns.addAll(gaussData.keySet());
                for (String key : allColumns) {
                    Object oracleVal = oracleData.get(key);
                    Object gaussVal = gaussData.get(key);
                    if (oracleVal == null ? gaussVal != null : !oracleVal.equals(gaussVal)) {
                        writer.println("差异列: " + key);
                        writer.println("oracle中值: " + (oracleVal == null ? "(无)" : oracleVal));
                        writer.println("gauss中值: " + (gaussVal == null ? "(无)" : gaussVal));
                        writer.println("----------------------------------------");
                    }
                }
            } else if (Difference.ORACLE_ONLY.equals(diff.getType())) {
                // Oracle独有的数据
                Map<String, Object> oracleData = diff.getOracleData();
                for (Map.Entry<String, Object> entry : oracleData.entrySet()) {
                    writer.println("差异列: " + entry.getKey());
                    writer.println("oracle中值: " + entry.getValue());
                    writer.println("gauss中值: (无)");
                    writer.println("----------------------------------------");
                }
            } else if (Difference.GAUSS_ONLY.equals(diff.getType())) {
                // Gauss独有的数据
                Map<String, Object> gaussData = diff.getGaussData();
                for (Map.Entry<String, Object> entry : gaussData.entrySet()) {
                    writer.println("差异列: " + entry.getKey());
                    writer.println("oracle中值: (无)");
                    writer.println("gauss中值: " + entry.getValue());
                    writer.println("----------------------------------------");
                }
            }
            writer.println();
        }
    }

    /**
     * Map转字符串
     */
    private String mapToString(Map<String, Object> map) {
        if (map == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
