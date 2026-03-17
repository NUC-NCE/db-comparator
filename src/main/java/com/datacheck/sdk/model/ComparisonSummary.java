package com.datacheck.sdk.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对比汇总结果
 */
public class ComparisonSummary {
    private int totalTables;
    private int successCount;
    private int errorCount;
    private int consistentCount;
    private int differentCount;
    private List<String> failedTableNames = new ArrayList<>();
    private Map<String, String> failedReasons = new HashMap<>();

    public int getTotalTables() {
        return totalTables;
    }

    public void setTotalTables(int totalTables) {
        this.totalTables = totalTables;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getConsistentCount() {
        return consistentCount;
    }

    public void setConsistentCount(int consistentCount) {
        this.consistentCount = consistentCount;
    }

    public int getDifferentCount() {
        return differentCount;
    }

    public void setDifferentCount(int differentCount) {
        this.differentCount = differentCount;
    }

    public List<String> getFailedTableNames() {
        return failedTableNames;
    }

    public void setFailedTableNames(List<String> failedTableNames) {
        this.failedTableNames = failedTableNames;
    }

    public Map<String, String> getFailedReasons() {
        return failedReasons;
    }

    public void setFailedReasons(Map<String, String> failedReasons) {
        this.failedReasons = failedReasons;
    }

    public void addFailedTable(String tableName, String reason) {
        failedTableNames.add(tableName);
        failedReasons.put(tableName, reason);
        errorCount++;
    }

    public void addSuccess(boolean isConsistent) {
        successCount++;
        if (isConsistent) {
            consistentCount++;
        } else {
            differentCount++;
        }
    }
}
