package com.datacheck.db;

import com.datacheck.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接器
 */
public class DatabaseConnector {
    private Connection oracleConnection;
    private Connection gaussConnection;
    private Connection externalOracleConnection;
    private Connection externalGaussConnection;
    private final Config config;

    public DatabaseConnector(Config config) {
        this.config = config;
    }

    /**
     * 设置外部注入的 Oracle 连接
     */
    public void setExternalOracleConnection(Connection connection) {
        this.externalOracleConnection = connection;
    }

    /**
     * 设置外部注入的 Gauss 连接
     */
    public void setExternalGaussConnection(Connection connection) {
        this.externalGaussConnection = connection;
    }

    public Connection getOracleConnection() {
        return externalOracleConnection != null ? externalOracleConnection : oracleConnection;
    }

    public Connection getGaussConnection() {
        return externalGaussConnection != null ? externalGaussConnection : gaussConnection;
    }

    /**
     * 连接Oracle数据库
     */
    public void connectOracle() throws SQLException {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC驱动未找到", e);
        }

        String jdbcUrl = config.getOracleJdbcUrl();
        System.out.println("连接Oracle数据库: " + jdbcUrl);

        oracleConnection = DriverManager.getConnection(
            jdbcUrl,
            config.getOracle().getUsername(),
            config.getOracle().getPassword()
        );
        System.out.println("Oracle数据库连接成功");
    }

    /**
     * 连接Gauss数据库
     */
    public void connectGauss() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC驱动未找到", e);
        }

        String jdbcUrl = config.getGaussJdbcUrl();
        System.out.println("连接Gauss数据库: " + jdbcUrl);

        gaussConnection = DriverManager.getConnection(
            jdbcUrl,
            config.getGauss().getUsername(),
            config.getGauss().getPassword()
        );
        System.out.println("Gauss数据库连接成功");
    }

    /**
     * 连接所有数据库
     * 如果外部连接已注入，则跳过内部连接
     */
    public void connectAll() throws SQLException {
        try {
            if (externalOracleConnection == null) {
                connectOracle();
            } else {
                System.out.println("使用外部注入的 Oracle 连接");
            }
            if (externalGaussConnection == null) {
                connectGauss();
            } else {
                System.out.println("使用外部注入的 Gauss 连接");
            }
        } catch (SQLException e) {
            closeAll();
            throw e;
        }
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        closeConnection(oracleConnection, "Oracle");
        closeConnection(gaussConnection, "Gauss");
    }

    private void closeConnection(Connection conn, String dbName) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("关闭" + dbName + "连接失败: " + e.getMessage());
            }
        }
    }
}
