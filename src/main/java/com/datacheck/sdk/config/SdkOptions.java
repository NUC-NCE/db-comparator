package com.datacheck.sdk.config;

/**
 * SDK全局配置选项
 */
public class SdkOptions {
    private int threadCount = 4;
    private String outputDir = "./output";
    private boolean writeResultFiles = false;
    private long queryTimeout = 30000;
    private boolean ignoreTimeFields = true;

    public SdkOptions() {
    }

    public SdkOptions(int threadCount) {
        this.threadCount = threadCount;
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

    public boolean isWriteResultFiles() {
        return writeResultFiles;
    }

    public void setWriteResultFiles(boolean writeResultFiles) {
        this.writeResultFiles = writeResultFiles;
    }

    public long getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(long queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public boolean isIgnoreTimeFields() {
        return ignoreTimeFields;
    }

    public void setIgnoreTimeFields(boolean ignoreTimeFields) {
        this.ignoreTimeFields = ignoreTimeFields;
    }
}
