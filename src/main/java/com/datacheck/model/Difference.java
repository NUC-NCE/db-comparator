package com.datacheck.model;

import java.util.Map;

/**
 * 数据差异模型
 */
public class Difference {
    private String type;
    private String pkKey;
    private Map<String, Object> oracleData;
    private Map<String, Object> gaussData;

    public static final String ORACLE_ONLY = "oracle_only";
    public static final String GAUSS_ONLY = "gauss_only";
    public static final String DIFFERENT = "different";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPkKey() {
        return pkKey;
    }

    public void setPkKey(String pkKey) {
        this.pkKey = pkKey;
    }

    public Map<String, Object> getOracleData() {
        return oracleData;
    }

    public void setOracleData(Map<String, Object> oracleData) {
        this.oracleData = oracleData;
    }

    public Map<String, Object> getGaussData() {
        return gaussData;
    }

    public void setGaussData(Map<String, Object> gaussData) {
        this.gaussData = gaussData;
    }

    public String getTypeName() {
        switch (type) {
            case ORACLE_ONLY: return "Oracle独有数据";
            case GAUSS_ONLY: return "Gauss独有数据";
            case DIFFERENT: return "数据不一致";
            default: return type;
        }
    }
}
