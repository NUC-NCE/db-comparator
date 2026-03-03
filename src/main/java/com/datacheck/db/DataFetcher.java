package com.datacheck.db;

import com.datacheck.model.TableData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 表数据获取器
 */
public class DataFetcher {
    private final Connection oracleConnection;
    private final Connection gaussConnection;
    private final TableMetadata metadata;

    public DataFetcher(Connection oracleConnection, Connection gaussConnection, Config config) {
        this.oracleConnection = oracleConnection;
        this.gaussConnection = gaussConnection;
        this.metadata = new TableMetadata(oracleConnection, gaussConnection, config);
    }

    /**
     * 获取Oracle表数据
     */
    public TableData fetchOracleTableData(String tableName, String filterColumn, String filterValue, List<String> primaryKeys) throws SQLException {
        List<String> columns = metadata.getOracleColumns(tableName);
        if (primaryKeys == null) {
            primaryKeys = metadata.getOraclePrimaryKeys(tableName);
        }
        List<Object[]> rows = fetchRows(oracleConnection, tableName, filterColumn, filterValue, true);

        return new TableData(tableName, columns, rows, primaryKeys);
    }

    /**
     * 获取Gauss表数据
     */
    public TableData fetchGaussTableData(String tableName, String filterColumn, String filterValue, List<String> primaryKeys) throws SQLException {
        List<String> columns = metadata.getGaussColumns(tableName);
        if (primaryKeys == null) {
            primaryKeys = metadata.getGaussPrimaryKeys(tableName);
        }
        List<Object[]> rows = fetchRows(gaussConnection, tableName, filterColumn, filterValue, false);

        return new TableData(tableName, columns, rows, primaryKeys);
    }

    /**
     * 执行查询获取数据（支持过滤条件）
     */
    private List<Object[]> fetchRows(Connection conn, String tableName,
                                     String filterColumn, String filterValue,
                                     boolean isOracle) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql;

        if (filterColumn != null && filterValue != null && !filterColumn.isEmpty() && !filterValue.isEmpty()) {
            // 带过滤条件
            String tableNameSql = isOracle ? tableName : "\"" + tableName + "\"";
            String columnNameSql = isOracle ? filterColumn : "\"" + filterColumn + "\"";

            sql = "SELECT * FROM " + tableNameSql + " WHERE " + columnNameSql + " = " + filterValue;
        } else {
            // 全表对比
            sql = isOracle ? "SELECT * FROM " + tableName : "SELECT * FROM \"" + tableName + "\"";
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Object[] row = convertRow(rs);
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * 将ResultSet转换为对象数组
     */
    private Object[] convertRow(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        Object[] row = new Object[columnCount];

        for (int i = 0; i < columnCount; i++) {
            row[i] = rs.getObject(i + 1);
        }
        return row;
    }

    /**
     * 获取表元数据
     */
    public TableMetadata getMetadata() {
        return metadata;
    }
}
