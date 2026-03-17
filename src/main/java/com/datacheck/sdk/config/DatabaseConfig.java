package com.datacheck.sdk.config;

/**
 * 数据库连接配置 - 使用JDBC连接串
 */
public class DatabaseConfig {
    private String jdbcUrl;  // 直接使用JDBC连接串
    private String username;
    private String password;

    public DatabaseConfig() {
    }

    /**
     * 构造方法
     * @param jdbcUrl JDBC连接串，如 jdbc:oracle:thin:@host:port:service 或 jdbc:postgresql://host:port/database
     * @param username 用户名
     * @param password 密码
     */
    public DatabaseConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 判断是否为Oracle配置
     */
    public boolean isOracle() {
        return jdbcUrl != null && jdbcUrl.contains("oracle");
    }

    /**
     * 判断是否为GaussDB/PostgreSQL配置
     */
    public boolean isGaussDB() {
        return jdbcUrl != null && (jdbcUrl.contains("postgresql") || jdbcUrl.contains("gauss"));
    }
}
