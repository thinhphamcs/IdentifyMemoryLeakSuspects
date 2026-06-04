package com.ibm.oom.analyzer.parser;

public class MemoryConsumer {

    private final String className;
    private final long totalBytes;
    private final int instanceCount;

    public MemoryConsumer(String className, long totalBytes, int instanceCount) {
        this.className = className;
        this.totalBytes = totalBytes;
        this.instanceCount = instanceCount;
    }

    public String getClassName() { return className; }
    public long getTotalBytes() { return totalBytes; }
    public int getInstanceCount() { return instanceCount; }
}
