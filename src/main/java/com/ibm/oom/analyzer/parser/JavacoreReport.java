package com.ibm.oom.analyzer.parser;

import java.util.List;

public class JavacoreReport {

    private final String filePath;
    private final List<ThreadSnapshot> threads;
    private final int blockedCount;
    private final int waitingCount;
    private final HeapSummary heapSummary;
    private final List<MemoryConsumer> topMemoryConsumers;
    private final int finalizerQueueDepth;

    private JavacoreReport(Builder b) {
        this.filePath = b.filePath;
        this.threads = List.copyOf(b.threads);
        this.blockedCount = b.blockedCount;
        this.waitingCount = b.waitingCount;
        this.heapSummary = b.heapSummary;
        this.topMemoryConsumers = List.copyOf(b.topMemoryConsumers);
        this.finalizerQueueDepth = b.finalizerQueueDepth;
    }

    public String getFilePath() { return filePath; }
    public List<ThreadSnapshot> getThreads() { return threads; }
    public int getBlockedCount() { return blockedCount; }
    public int getWaitingCount() { return waitingCount; }
    public HeapSummary getHeapSummary() { return heapSummary; }
    public List<MemoryConsumer> getTopMemoryConsumers() { return topMemoryConsumers; }
    public int getFinalizerQueueDepth() { return finalizerQueueDepth; }

    public static Builder builder(String filePath) { return new Builder(filePath); }

    public static final class Builder {
        private final String filePath;
        private List<ThreadSnapshot> threads = List.of();
        private int blockedCount;
        private int waitingCount;
        private HeapSummary heapSummary;
        private List<MemoryConsumer> topMemoryConsumers = List.of();
        private int finalizerQueueDepth;

        private Builder(String filePath) { this.filePath = filePath; }

        public Builder threads(List<ThreadSnapshot> threads) { this.threads = threads; return this; }
        public Builder blockedCount(int blockedCount) { this.blockedCount = blockedCount; return this; }
        public Builder waitingCount(int waitingCount) { this.waitingCount = waitingCount; return this; }
        public Builder heapSummary(HeapSummary heapSummary) { this.heapSummary = heapSummary; return this; }
        public Builder topMemoryConsumers(List<MemoryConsumer> consumers) { this.topMemoryConsumers = consumers; return this; }
        public Builder finalizerQueueDepth(int depth) { this.finalizerQueueDepth = depth; return this; }

        public JavacoreReport build() { return new JavacoreReport(this); }
    }
}
