package com.datacheck.db;

import com.datacheck.Config;
import com.datacheck.model.TableData;
import com.datacheck.model.TableFilter;

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
    public TableData fetchOracleTableData(String tableName, String whereClause, List<String> primaryKeys) throws SQLException {
        List<String> columns = metadata.getOracleColumns(tableName);
        if (primaryKeys == null) {
            primaryKeys = metadata.getOraclePrimaryKeys(tableName);
        }
        List<Object[]> rows = fetchRows(oracleConnection, tableName, whereClause, true);

        return new TableData(tableName, columns, rows, primaryKeys);
    }

    /**
     * 获取Gauss表数据
     */
    public TableData fetchGaussTableData(String tableName, String whereClause, List<String> primaryKeys) throws SQLException {
        List<String> columns = metadata.getGaussColumns(tableName);
        if (primaryKeys == null) {
            primaryKeys = metadata.getGaussPrimaryKeys(tableName);
        }
        List<Object[]> rows = fetchRows(gaussConnection, tableName, whereClause, false);

        return new TableData(tableName, columns, rows, primaryKeys);
    }

    /**
     * 执行查询获取数据（支持过滤条件）
     */
    private List<Object[]> fetchRows(Connection conn, String tableName,
                                     String whereClause,
                                     boolean isOracle) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql;

        if (whereClause != null && !whereClause.isEmpty()) {
            // 带过滤条件，直接拼接WHERE子句
            String tableNameSql = isOracle ? tableName : "\"" + tableName + "\"";
            sql = "SELECT * FROM " + tableNameSql + " WHERE " + whereClause;
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

    /**
     * 从Oracle数据库获取表配置列表
     * 表结构: table_check_info(table_name, condition, key)
     * 数据库中 condition 和 key 字段已包含方括号格式，如 [condition], [key]
     * 格式与文件格式一致
     */
    public List<TableFilter> fetchTableFiltersFromOracle(String configTableName) throws SQLException {
        List<TableFilter> filters = new ArrayList<>();
        String sql = "SELECT table_name, condition, key FROM " + configTableName;

        try (Statement stmt = oracleConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String condition = rs.getString("condition");
                String key = rs.getString("key");

                // 直接拼接，数据库中 condition 和 key 已包含方括号
                // 格式: tableName + condition + key
                // 例如: users [status = 'active'] [user_id]
                StringBuilder line = new StringBuilder();
                line.append(tableName);

                if (condition != null && !condition.trim().isEmpty()) {
                    line.append(" ").append(condition.trim());
                }

                if (key != null && !key.trim().isEmpty()) {
                    line.append(" ").append(key.trim());
                }

                filters.add(TableFilter.parse(line.toString()));
            }
        }
        return filters;
    }
}
