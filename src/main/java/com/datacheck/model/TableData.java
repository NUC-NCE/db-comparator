package com.datacheck.model;

import java.util.List;

/**
 * 表数据模型
 */
public class TableData {
    private final String tableName;
    private final List<String> columns;
    private final List<Object[]> rows;
    private final List<String> primaryKeys;

    public TableData(String tableName, List<String> columns, List<Object[]> rows, List<String> primaryKeys) {
        this.tableName = tableName;
        this.columns = columns;
        this.rows = rows;
        this.primaryKeys = primaryKeys;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Object[]> getRows() {
        return rows;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }
}
