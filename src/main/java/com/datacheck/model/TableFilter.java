package com.datacheck.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 表过滤条件配置
 */
public class TableFilter {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 查询条件 (直接拼接到WHERE后)
     */
    private String whereClause;

    /**
     * 主键列表 (d参数)，格式: [x1,x2,x3...]
     */
    private List<String> primaryKeys;

    public TableFilter() {
    }

    public TableFilter(String tableName) {
        this.tableName = tableName;
    }

    public TableFilter(String tableName, String whereClause, List<String> primaryKeys) {
        this.tableName = tableName;
        this.whereClause = whereClause;
        this.primaryKeys = primaryKeys;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    /**
     * 解析表配置行
     * 格式: tableName [whereClause] [pk1,pk2,...]
     */
    public static TableFilter parse(String line) {
        String trimmed = line.trim();

        // 查找第一个 [ 的位置（表名结束位置）
        int firstBracketStart = trimmed.indexOf('[');
        String tableName;
        String whereClause = null;
        List<String> primaryKeys = null;

        if (firstBracketStart == -1) {
            // 只有表名，无任何条件
            tableName = trimmed;
        } else {
            // 提取表名
            tableName = trimmed.substring(0, firstBracketStart).trim();

            // 查找第一个 ] 的位置
            int firstBracketEnd = trimmed.indexOf(']');
            if (firstBracketEnd == -1) {
                throw new IllegalArgumentException("无效的表配置格式，缺少 ']': " + line);
            }

            // 提取第一个 [] 内的内容作为查询条件
            String whereContent = trimmed.substring(firstBracketStart + 1, firstBracketEnd).trim();
            if (!whereContent.isEmpty()) {
                whereClause = whereContent;
            }

            // 检查是否有第二个 [] (主键列表)
            int secondBracketStart = trimmed.indexOf('[', firstBracketEnd + 1);
            if (secondBracketStart != -1) {
                int secondBracketEnd = trimmed.indexOf(']', secondBracketStart + 1);
                if (secondBracketEnd == -1) {
                    throw new IllegalArgumentException("无效的表配置格式，缺少 ']': " + line);
                }
                String pkContent = trimmed.substring(secondBracketStart + 1, secondBracketEnd).trim();
                if (!pkContent.isEmpty()) {
                    primaryKeys = new ArrayList<>();
                    for (String pk : pkContent.split(",")) {
                        String trimmedPk = pk.trim();
                        if (!trimmedPk.isEmpty()) {
                            primaryKeys.add(trimmedPk);
                        }
                    }
                }
            }
        }

        return new TableFilter(tableName, whereClause, primaryKeys);
    }

    /**
     * 判断是否有过滤条件
     */
    public boolean hasFilter() {
        return whereClause != null && !whereClause.isEmpty();
    }

    /**
     * 判断是否指定了主键
     */
    public boolean hasPrimaryKeys() {
        return primaryKeys != null && !primaryKeys.isEmpty();
    }
}
