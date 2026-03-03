package com.datacheck.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表过滤条件配置
 */
@Data
public class TableFilter {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 过滤列名 (b参数)
     */
    private String filterColumn;

    /**
     * 过滤值 (c参数)
     */
    private String filterValue;

    /**
     * 主键列表 (d参数)，格式: [x1,x2,x3...]
     */
    private List<String> primaryKeys;

    public TableFilter() {
    }

    public TableFilter(String tableName, String filterColumn, String filterValue, List<String> primaryKeys) {
        this.tableName = tableName;
        this.filterColumn = filterColumn;
        this.filterValue = filterValue;
        this.primaryKeys = primaryKeys;
    }

    /**
     * 解析表配置行
     * 格式: tableName 或 tableName column value [pk1,pk2,...]
     */
    public static TableFilter parse(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 1) {
            // 只有表名，无过滤条件
            return new TableFilter(parts[0].trim(), null, null, null);
        } else if (parts.length >= 3) {
            // 表名 列名 值 [pk1,pk2,...]
            String tableName = parts[0].trim();
            String filterColumn = parts[1].trim();
            String filterValue = parts[2].trim();
            List<String> primaryKeys = null;

            // 检查是否有第4个参数（主键列表）
            if (parts.length >= 4) {
                String pkStr = parts[3].trim();
                if (pkStr.startsWith("[") && pkStr.endsWith("]")) {
                    pkStr = pkStr.substring(1, pkStr.length() - 1);
                    primaryKeys = new ArrayList<>();
                    for (String pk : pkStr.split(",")) {
                        String trimmed = pk.trim();
                        if (!trimmed.isEmpty()) {
                            primaryKeys.add(trimmed);
                        }
                    }
                }
            }

            return new TableFilter(tableName, filterColumn, filterValue, primaryKeys);
        } else {
            throw new IllegalArgumentException("无效的表配置格式: " + line);
        }
    }

    /**
     * 判断是否有过滤条件
     */
    public boolean hasFilter() {
        return filterColumn != null && !filterColumn.isEmpty()
            && filterValue != null && !filterValue.isEmpty();
    }

    /**
     * 判断是否指定了主键
     */
    public boolean hasPrimaryKeys() {
        return primaryKeys != null && !primaryKeys.isEmpty();
    }
}
