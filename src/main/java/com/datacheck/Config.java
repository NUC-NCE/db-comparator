package com.datacheck;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Config {
    private OracleConfig oracle;
    private GaussConfig gauss;
    private int threadCount;
    private String outputDir;

    public OracleConfig getOracle() {
        return oracle;
    }

    public void setOracle(OracleConfig oracle) {
        this.oracle = oracle;
    }

    public GaussConfig getGauss() {
        return gauss;
    }

    public void setGauss(GaussConfig gauss) {
        this.gauss = gauss;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public static class OracleConfig {
        private String jdbcUrl;  // 直接使用JDBC连接串，不再拼接
        private String username;
        private String password;

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
    }

    public static class GaussConfig {
        private String jdbcUrl;  // 直接使用JDBC连接串，不再拼接
        private String username;
        private String password;

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
    }

    public static Config load(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(configPath), Config.class);
    }

    /**
     * 获取Oracle JDBC URL（兼容旧代码）
     * 优先使用直接传入的jdbcUrl，否则为空
     */
    public String getOracleJdbcUrl() {
        if (oracle != null && oracle.getJdbcUrl() != null) {
            return oracle.getJdbcUrl();
        }
        return null;
    }

    /**
     * 获取Gauss JDBC URL（兼容旧代码）
     * 优先使用直接传入的jdbcUrl，否则为空
     */
    public String getGaussJdbcUrl() {
        if (gauss != null && gauss.getJdbcUrl() != null) {
            return gauss.getJdbcUrl();
        }
        return null;
    }
}
