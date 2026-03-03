package com.datacheck.db;

import com.datacheck.Config;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 表元数据获取
 */
public class TableMetadata {
    private final Connection oracleConnection;
    private final Connection gaussConnection;
    private final Config config;

    public TableMetadata(Connection oracleConnection, Connection gaussConnection, Config config) {
        this.oracleConnection = oracleConnection;
        this.gaussConnection = gaussConnection;
        this.config = config;
    }

    /**
     * 获取Oracle表的主键列
     */
    public List<String> getOraclePrimaryKeys(String tableName) throws SQLException {
        List<String> pkColumns = new ArrayList<>();
        String sql = "SELECT cols.column_name " +
                     "FROM all_constraints cons, all_cons_columns cols " +
                     "WHERE cons.constraint_name = cols.constraint_name " +
                     "AND cons.constraint_type = 'P' " +
                     "AND cols.owner = UPPER(?) " +
                     "AND cols.table_name = UPPER(?) " +
                     "ORDER BY cols.position";

        try (PreparedStatement stmt = oracleConnection.prepareStatement(sql)) {
            stmt.setString(1, config.getOracle().getUsername());
            stmt.setString(2, tableName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pkColumns.add(rs.getString(1));
            }
        }
        return pkColumns;
    }

    /**
     * 获取Gauss表的主键列
     */
    public List<String> getGaussPrimaryKeys(String tableName) throws SQLException {
        List<String> pkColumns = new ArrayList<>();
        String sql = "SELECT kcu.column_name " +
                     "FROM information_schema.table_constraints tc " +
                     "JOIN information_schema.key_column_usage kcu " +
                     "ON tc.constraint_name = kcu.constraint_name " +
                     "AND tc.table_schema = kcu.table_schema " +
                     "WHERE tc.constraint_type = 'PRIMARY KEY' " +
                     "AND tc.table_name = ? " +
                     "ORDER BY kcu.ordinal_position";

        try (PreparedStatement stmt = gaussConnection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pkColumns.add(rs.getString(1));
            }
        }
        return pkColumns;
    }

    /**
     * 获取Oracle表的列名列表
     */
    public List<String> getOracleColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "SELECT column_name FROM all_tab_columns " +
                     "WHERE owner = UPPER(?) AND table_name = UPPER(?) ORDER BY column_id";

        try (PreparedStatement stmt = oracleConnection.prepareStatement(sql)) {
            stmt.setString(1, config.getOracle().getUsername());
            stmt.setString(2, tableName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                columns.add(rs.getString(1));
            }
        }
        return columns;
    }

    /**
     * 获取Gauss表的列名列表
     */
    public List<String> getGaussColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "SELECT column_name FROM information_schema.columns " +
                     "WHERE table_name = ? ORDER BY ordinal_position";

        try (PreparedStatement stmt = gaussConnection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                columns.add(rs.getString(1));
            }
        }
        return columns;
    }
}
