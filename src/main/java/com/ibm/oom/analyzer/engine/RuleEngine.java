package com.ibm.oom.analyzer.engine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ibm.oom.analyzer.config.OomProperties;
import com.ibm.oom.analyzer.parser.HeapSummary;
import com.ibm.oom.analyzer.parser.JavacoreReport;
import com.ibm.oom.analyzer.service.MatResult;

@Service
public class RuleEngine {

    private final OomProperties props;

    public RuleEngine(OomProperties props) {
        this.props = props;
    }

    public RuleReport evaluate(JavacoreReport report, MatResult mat) {
        List<FiredRule> fired = new ArrayList<>();

        checkHeapRetention(report, fired);
        checkThreadBlock(report, fired);
        checkMatSuspects(mat, fired);
        checkFinalizerBacklog(report, fired);

        return new RuleReport(fired);
    }

    private void checkHeapRetention(JavacoreReport report, List<FiredRule> fired) {
        if (report == null) return;
        HeapSummary heap = report.getHeapSummary();
        if (heap == null || heap.getMaxBytes() <= 0 || heap.getUsedBytes() < 0) return;

        double retentionRatio = (double) heap.getUsedBytes() / heap.getMaxBytes();
        double threshold = props.getRules().getHeapRetentionThreshold();
        if (retentionRatio > threshold) {
            fired.add(new FiredRule(
                    "HIGH_HEAP_RETENTION",
                    "HIGH",
                    Math.min(1.0, retentionRatio),
                    String.format("Heap retention %.1f%% exceeds threshold %.1f%%",
                            retentionRatio * 100, threshold * 100)
            ));
        }
    }

    private void checkThreadBlock(JavacoreReport report, List<FiredRule> fired) {
        if (report == null) return;
        int blocked = report.getBlockedCount();
        int threshold = props.getRules().getThreadBlockThreshold();
        if (threshold <= 0 || blocked <= threshold) return;

        double confidence = Math.min(1.0, (double) blocked / ((double) threshold * 2));
        fired.add(new FiredRule(
                "EXCESSIVE_THREAD_BLOCKING",
                "HIGH",
                confidence,
                String.format("%d blocked threads exceeds threshold of %d", blocked, threshold)
        ));
    }

    private void checkMatSuspects(MatResult mat, List<FiredRule> fired) {
        if (mat == null) return;
        if (mat.getExitCode() == 0 && !mat.getOutputLines().isEmpty()) {
            fired.add(new FiredRule(
                    "MAT_SUSPECTS_PRESENT",
                    "CRITICAL",
                    0.90,
                    "MAT analysis identified memory leak suspects in the heap dump"
            ));
        }
    }

    private void checkFinalizerBacklog(JavacoreReport report, List<FiredRule> fired) {
        if (report == null) return;
        int depth = report.getFinalizerQueueDepth();
        int threshold = props.getRules().getFinalizerQueueThreshold();
        if (threshold <= 0 || depth <= threshold) return;

        fired.add(new FiredRule(
                "FINALIZER_QUEUE_BACKLOG",
                "MEDIUM",
                Math.min(1.0, (double) depth / ((double) threshold * 2)),
                String.format("Finalizer queue has %d pending objects (threshold: %d); finalizer thread may be a bottleneck",
                        depth, threshold)
        ));
    }
}
