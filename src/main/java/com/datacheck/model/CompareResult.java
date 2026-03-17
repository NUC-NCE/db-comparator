package com.datacheck.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 对比结果数据模型
 */
public class CompareResult {
    private String tableName;
    private String status;
    private String message;
    private List<Difference> differences = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Difference> getDifferences() {
        return differences;
    }

    public void setDifferences(List<Difference> differences) {
        this.differences = differences;
    }

    public boolean hasDifferences() {
        return differences != null && !differences.isEmpty();
    }
}
