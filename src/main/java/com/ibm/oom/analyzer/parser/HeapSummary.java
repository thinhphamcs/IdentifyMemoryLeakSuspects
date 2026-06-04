package com.ibm.oom.analyzer.parser;

public class HeapSummary {

    private final long usedBytes;
    private final long committedBytes;
    private final long maxBytes;

    public HeapSummary(long usedBytes, long committedBytes, long maxBytes) {
        this.usedBytes = usedBytes;
        this.committedBytes = committedBytes;
        this.maxBytes = maxBytes;
    }

    public long getUsedBytes() { return usedBytes; }
    public long getCommittedBytes() { return committedBytes; }
    public long getMaxBytes() { return maxBytes; }
}
