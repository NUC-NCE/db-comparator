package com.datacheck.model;

import lombok.Data;
import java.util.Map;

/**
 * 数据差异模型
 */
@Data
public class Difference {
    private String type;
    private String pkKey;
    private Map<String, Object> oracleData;
    private Map<String, Object> gaussData;

    public static final String ORACLE_ONLY = "oracle_only";
    public static final String GAUSS_ONLY = "gauss_only";
    public static final String DIFFERENT = "different";

    public String getTypeName() {
        switch (type) {
            case ORACLE_ONLY: return "Oracle独有数据";
            case GAUSS_ONLY: return "Gauss独有数据";
            case DIFFERENT: return "数据不一致";
            default: return type;
        }
    }
}
