package com.datacheck.compare;

import com.datacheck.model.CompareResult;
import com.datacheck.model.Difference;
import com.datacheck.model.TableData;
import com.datacheck.util.ValueComparator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表数据对比器
 */
public class TableComparator {

    /**
     * 对比两个表的数据
     */
    public CompareResult compare(TableData oracleData, TableData gaussData) {
        CompareResult result = new CompareResult();
        result.setTableName(oracleData.getTableName());

        try {
            // 验证表结构
            if (oracleData.getColumns().isEmpty()) {
                result.setStatus("error");
                result.setMessage("Oracle表 " + oracleData.getTableName() + " 不存在或无列信息");
                return result;
            }

            if (gaussData.getColumns().isEmpty()) {
                result.setStatus("error");
                result.setMessage("Gauss表 " + oracleData.getTableName() + " 不存在或无列信息");
                return result;
            }

            // 获取主键索引
            List<String> pkColumns = oracleData.getPrimaryKeys();
            if (pkColumns.isEmpty()) {
                pkColumns = oracleData.getColumns();
            }

            List<Integer> pkIndices = getPkIndices(pkColumns, oracleData.getColumns());

            // 创建索引Map
            Map<String, Object[]> oracleMap = createIndexMap(oracleData.getRows(), pkIndices);
            Map<String, Object[]> gaussMap = createIndexMap(gaussData.getRows(), pkIndices);

            // 执行对比
            List<Difference> differences = compareData(oracleMap, gaussMap, pkIndices,
                oracleData.getColumns(), gaussData.getColumns());

            result.setDifferences(differences);

            if (differences.isEmpty()) {
                result.setStatus("success");
                result.setMessage("数据一致");
            } else {
                result.setStatus("success");
                result.setMessage(buildMessage(differences));
            }

        } catch (Exception e) {
            result.setStatus("error");
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 获取主键索引位置（忽略大小写）
     */
    private List<Integer> getPkIndices(List<String> pkColumns, List<String> allColumns) {
        // 将列名转为小写用于匹配
        List<String> lowerColumns = allColumns.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        return pkColumns.stream()
            .map(pk -> {
                int idx = lowerColumns.indexOf(pk.toLowerCase());
                return idx >= 0 ? idx : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 创建以主键为索引的数据Map
     */
    private Map<String, Object[]> createIndexMap(List<Object[]> rows, List<Integer> pkIndices) {
        Map<String, Object[]> map = new HashMap<>();
        for (Object[] row : rows) {
            String key = buildPkKey(row, pkIndices);
            map.put(key, row);
        }
        return map;
    }

    /**
     * 构建主键字符串
     */
    private String buildPkKey(Object[] row, List<Integer> pkIndices) {
        StringBuilder sb = new StringBuilder();
        for (int idx : pkIndices) {
            if (sb.length() > 0) sb.append("_");
            Object val = row[idx];
            sb.append(val == null ? "NULL" : val.toString());
        }
        return sb.toString();
    }

    /**
     * 对比数据并返回差异列表
     */
    private List<Difference> compareData(Map<String, Object[]> oracleMap,
                                         Map<String, Object[]> gaussMap,
                                         List<Integer> pkIndices,
                                         List<String> oracleColumns,
                                         List<String> gaussColumns) {
        List<Difference> differences = new ArrayList<>();

        // 检查Oracle独有和数据不同
        for (Map.Entry<String, Object[]> entry : oracleMap.entrySet()) {
            if (!gaussMap.containsKey(entry.getKey())) {
                // Oracle独有
                Difference diff = new Difference();
                diff.setType(Difference.ORACLE_ONLY);
                diff.setPkKey(entry.getKey());
                diff.setOracleData(rowToMap(entry.getValue(), oracleColumns));
                differences.add(diff);
            } else {
                // 检查数据是否相同（使用ValueComparator忽略科学计数法和小数末尾0的差异）
                Object[] gaussRow = gaussMap.get(entry.getKey());
                if (!rowEquals(entry.getValue(), gaussRow, oracleColumns)) {
                    Difference diff = new Difference();
                    diff.setType(Difference.DIFFERENT);
                    diff.setPkKey(entry.getKey());
                    diff.setOracleData(rowToMap(entry.getValue(), oracleColumns));
                    diff.setGaussData(rowToMap(gaussRow, gaussColumns));
                    differences.add(diff);
                }
            }
        }

        // 检查Gauss独有
        for (Map.Entry<String, Object[]> entry : gaussMap.entrySet()) {
            if (!oracleMap.containsKey(entry.getKey())) {
                Difference diff = new Difference();
                diff.setType(Difference.GAUSS_ONLY);
                diff.setPkKey(entry.getKey());
                diff.setGaussData(rowToMap(entry.getValue(), gaussColumns));
                differences.add(diff);
            }
        }

        return differences;
    }

    /**
     * 逐列比较两行数据
     * - 使用ValueComparator忽略科学计数法和小数末尾0的差异
     * - 忽略除data_data和datadata字段外的其他时间类型字段差异
     */
    private boolean rowEquals(Object[] row1, Object[] row2, List<String> columns) {
        if (row1.length != row2.length) return false;
        for (int i = 0; i < row1.length; i++) {
            String columnName = columns.get(i);
            // 忽略除data_data和datadata外的其他时间类型字段
            if (shouldIgnoreTimeField(columnName)) {
                continue;
            }
            if (!ValueComparator.equals(row1[i], row2[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否应该忽略时间类型字段
     * 忽略除data_data和datadata外的其他时间类型字段
     */
    private boolean shouldIgnoreTimeField(String columnName) {
        // 获取列名的小写形式进行比较
        String lowerName = columnName.toLowerCase();
        // 检查是否为时间类型字段（包含time/date等关键词）
        boolean isTimeField = lowerName.contains("time")
            || lowerName.contains("date")
            || lowerName.contains("day")
            || lowerName.contains("hour")
            || lowerName.contains("minute")
            || lowerName.contains("second")
            || lowerName.contains("created")
            || lowerName.contains("updated")
            || lowerName.contains("modified");

        // 如果是时间字段，但不是data_data或datadata，则忽略
        if (isTimeField) {
            // 精确匹配，不忽略data_data和datadata
            if (lowerName.equals("data_data") || lowerName.equals("datadata")) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 将行数据转换为Map（列名使用小写，忽略大小写）
     */
    private Map<String, Object> rowToMap(Object[] row, List<String> columns) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < columns.size() && i < row.length; i++) {
            // 使用小写列名作为key，忽略大小写差异
            map.put(columns.get(i).toLowerCase(), row[i]);
        }
        return map;
    }

    /**
     * 构建差异消息
     */
    private String buildMessage(List<Difference> differences) {
        long oracleOnly = differences.stream()
            .filter(d -> Difference.ORACLE_ONLY.equals(d.getType())).count();
        long gaussOnly = differences.stream()
            .filter(d -> Difference.GAUSS_ONLY.equals(d.getType())).count();
        long different = differences.stream()
            .filter(d -> Difference.DIFFERENT.equals(d.getType())).count();

        return String.format("发现差异: Oracle独有%d条, Gauss独有%d条, 数据不同%d条",
            oracleOnly, gaussOnly, different);
    }
}
