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
    private int fetchSize = 1000;  // JDBC fetchSize，控制每次从网络读取的行数

    public DataFetcher(Connection oracleConnection, Connection gaussConnection, Config config) {
        this.oracleConnection = oracleConnection;
        this.gaussConnection = gaussConnection;
        this.metadata = new TableMetadata(oracleConnection, gaussConnection, config);
    }

    /**
     * 设置 JDBC fetchSize
     */
    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * 获取 JDBC fetchSize
     */
    public int getFetchSize() {
        return fetchSize;
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
     * 统计表行数
     */
    public long countRows(Connection conn, String tableName, String whereClause, boolean isOracle) throws SQLException {
        String tableNameSql = isOracle ? tableName : "\"" + tableName + "\"";
        String sql;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql = "SELECT COUNT(*) FROM " + tableNameSql + " WHERE " + whereClause;
        } else {
            sql = "SELECT COUNT(*) FROM " + tableNameSql;
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * 获取主键值列表（用于分批处理）
     * @return 主键值列表，每个元素是包含主键值的数组
     */
    public List<Object[]> fetchPrimaryKeyValues(Connection conn, String tableName,
                                                List<String> primaryKeys, String whereClause,
                                                boolean isOracle) throws SQLException {
        List<Object[]> pkValues = new ArrayList<>();
        String tableNameSql = isOracle ? tableName : "\"" + tableName + "\"";

        // 构建 SELECT 主键列
        StringBuilder selectClause = new StringBuilder();
        for (int i = 0; i < primaryKeys.size(); i++) {
            if (i > 0) selectClause.append(",");
            selectClause.append(primaryKeys.get(i));
        }

        String sql;
        if (whereClause != null && !whereClause.isEmpty()) {
            sql = "SELECT " + selectClause + " FROM " + tableNameSql + " WHERE " + whereClause;
        } else {
            sql = "SELECT " + selectClause + " FROM " + tableNameSql;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.setFetchSize(fetchSize);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Object[] pkRow = new Object[primaryKeys.size()];
                    for (int i = 0; i < primaryKeys.size(); i++) {
                        pkRow[i] = rs.getObject(i + 1);
                    }
                    pkValues.add(pkRow);
                }
            }
        }
        return pkValues;
    }

    /**
     * 按主键值列表批量获取表数据
     * @param pkValueBatch 主键值批次
     * @return 该批次对应的行数据
     */
    public List<Object[]> fetchRowsByPrimaryKeyBatch(Connection conn, String tableName,
                                                    List<String> primaryKeys,
                                                    List<Object[]> pkValueBatch,
                                                    String whereClause,
                                                    boolean isOracle) throws SQLException {
        if (pkValueBatch == null || pkValueBatch.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object[]> rows = new ArrayList<>();
        String tableNameSql = isOracle ? tableName : "\"" + tableName + "\"";

        // Oracle 使用 DECODE 处理多主键 IN 查询，GaussDB 使用标准 SQL
        String sql;
        if (isOracle) {
            // Oracle: WHERE (pk1, pk2) IN ((val1, val2), (val3, val4), ...)
            StringBuilder pkCols = new StringBuilder();
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) pkCols.append(",");
                pkCols.append(primaryKeys.get(i));
            }

            StringBuilder values = new StringBuilder();
            for (int i = 0; i < pkValueBatch.size(); i++) {
                if (i > 0) values.append(",");
                values.append("(");
                for (int j = 0; j < pkValueBatch.get(i).length; j++) {
                    if (j > 0) values.append(",");
                    Object val = pkValueBatch.get(i)[j];
                    if (val == null) {
                        values.append("NULL");
                    } else if (val instanceof Number) {
                        values.append(val);
                    } else {
                        values.append("'").append(val.toString().replace("'", "''")).append("'");
                    }
                }
                values.append(")");
            }

            sql = "SELECT * FROM " + tableNameSql + " WHERE (" + pkCols + ") IN (" + values + ")";
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                sql += " AND (" + whereClause + ")";
            }
        } else {
            // GaussDB/PostgreSQL: 使用 WHERE pk1 = ? AND pk2 = ? 配合 UNION ALL
            StringBuilder unionParts = new StringBuilder();
            for (int i = 0; i < pkValueBatch.size(); i++) {
                if (i > 0) unionParts.append(" UNION ALL ");
                unionParts.append("SELECT * FROM " + tableNameSql + " WHERE ");
                for (int j = 0; j < primaryKeys.size(); j++) {
                    if (j > 0) unionParts.append(" AND ");
                    unionParts.append(primaryKeys.get(j)).append(" = ");
                    Object val = pkValueBatch.get(i)[j];
                    if (val == null) {
                        unionParts.append("NULL");
                    } else if (val instanceof Number) {
                        unionParts.append(val);
                    } else {
                        unionParts.append("'").append(val.toString().replace("'", "''")).append("'");
                    }
                }
                if (whereClause != null && !whereClause.trim().isEmpty()) {
                    unionParts.append(" AND (").append(whereClause).append(")");
                }
            }
            sql = unionParts.toString();
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.setFetchSize(fetchSize);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    rows.add(convertRow(rs));
                }
            }
        }
        return rows;
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

        try (Statement stmt = conn.createStatement()) {
            stmt.setFetchSize(fetchSize);
            try (ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Object[] row = convertRow(rs);
                    rows.add(row);
                }
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
