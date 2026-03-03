package com.datacheck.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对比结果数据模型
 */
@Data
public class CompareResult {
    private String tableName;
    private String status;
    private String message;
    private List<Difference> differences = new ArrayList<>();

    public boolean hasDifferences() {
        return differences != null && !differences.isEmpty();
    }
}
