package com.datacheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Data
public class Config {
    private DatabaseConfig oracle;
    private DatabaseConfig gauss;
    private int threadCount;
    private String outputDir;

    @Data
    public static class DatabaseConfig {
        private String host;
        private int port;
        private String serviceName;
        private String database;
        private String username;
        private String password;
    }

    public static Config load(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(configPath), Config.class);
    }

    public String getOracleJdbcUrl() {
        return String.format("jdbc:oracle:thin:@%s:%d:%s",
            oracle.getHost(), oracle.getPort(), oracle.getServiceName());
    }

    public String getGaussJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s",
            gauss.getHost(), gauss.getPort(), gauss.getDatabase());
    }
}
