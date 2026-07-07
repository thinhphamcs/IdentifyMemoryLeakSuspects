package com.ibm.oom.analyzer.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.oom.analyzer.config.OomProperties;
import com.ibm.oom.analyzer.engine.FiredRule;
import com.ibm.oom.analyzer.engine.RuleReport;
import com.ibm.oom.analyzer.model.AnalysisJob;
import com.ibm.oom.analyzer.parser.HeapSummary;
import com.ibm.oom.analyzer.parser.JavacoreReport;
import com.ibm.oom.analyzer.parser.MemoryConsumer;
import com.ibm.oom.analyzer.service.MatResult;

@Service
public class ReportGenerator {

    private final OomProperties props;
    private final ObjectMapper mapper;

    public ReportGenerator(OomProperties props) {
        this.props = props;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ReportFiles generate(AnalysisJob job) throws IOException {
        Path jobDir = Path.of(props.getJobs().getOutputDir(), job.getJobId());
        Files.createDirectories(jobDir);

        Path jsonPath = jobDir.resolve("report.json");
        Path htmlPath = jobDir.resolve("report.html");

        writeJson(job, jsonPath);
        writeHtml(job, htmlPath);

        return new ReportFiles(jsonPath.toString(), htmlPath.toString());
    }

    private void writeJson(AnalysisJob job, Path dest) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("jobId", job.getJobId());
        meta.put("status", job.getStatus().toString());
        meta.put("createdAt", job.getCreatedAt().toString());
        meta.put("updatedAt", job.getUpdatedAt().toString());
        meta.put("javacorePath", job.getJavacorePath());
        meta.put("heapDumpPath", job.getHeapDumpPath());
        root.put("job", meta);

        JavacoreReport report = job.getReport();
        if (report != null) {
            Map<String, Object> jc = new LinkedHashMap<>();
            HeapSummary heap = report.getHeapSummary();
            if (heap != null) {
                Map<String, Object> heapMap = new LinkedHashMap<>();
                heapMap.put("usedBytes", heap.getUsedBytes());
                heapMap.put("committedBytes", heap.getCommittedBytes());
                heapMap.put("maxBytes", heap.getMaxBytes());
                if (heap.getMaxBytes() > 0) {
                    heapMap.put("usedPercent",
                            Math.round((double) heap.getUsedBytes() / heap.getMaxBytes() * 1000) / 10.0);
                }
                jc.put("heapSummary", heapMap);
            }
            jc.put("threadCount", report.getThreads().size());
            jc.put("blockedCount", report.getBlockedCount());
            jc.put("waitingCount", report.getWaitingCount());
            jc.put("finalizerQueueDepth", report.getFinalizerQueueDepth());
            jc.put("topMemoryConsumers", report.getTopMemoryConsumers().stream()
                    .map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("className", c.getClassName());
                        cm.put("totalBytes", c.getTotalBytes());
                        cm.put("instanceCount", c.getInstanceCount());
                        return cm;
                    })
                    .toList());
            root.put("javacoreReport", jc);
        }

        MatResult mat = job.getMatResult();
        if (mat != null) {
            Map<String, Object> matMap = new LinkedHashMap<>();
            matMap.put("exitCode", mat.getExitCode());
            matMap.put("success", mat.isSuccess());
            matMap.put("outputLines", mat.getOutputLines());
            matMap.put("errorLines", mat.getErrorLines());
            root.put("matResult", matMap);
        }

        RuleReport rules = job.getRuleReport();
        if (rules != null) {
            Map<String, Object> rr = new LinkedHashMap<>();
            rr.put("firedRules", rules.getFiredRules().stream()
                    .map(r -> {
                        Map<String, Object> rm = new LinkedHashMap<>();
                        rm.put("name", r.getName());
                        rm.put("severity", r.getSeverity());
                        rm.put("confidence", r.getConfidence());
                        rm.put("description", r.getDescription());
                        return rm;
                    })
                    .toList());
            root.put("ruleReport", rr);
        }

        mapper.writeValue(dest.toFile(), root);
    }

    private void writeHtml(AnalysisJob job, Path dest) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>OOM Analysis Report — ").append(esc(job.getJobId())).append("</title>\n");
        sb.append("<style>");
        sb.append("body{font-family:monospace;margin:2em;background:#fafafa}");
        sb.append("h1,h2{color:#333}");
        sb.append("table{border-collapse:collapse;margin-bottom:1.5em}");
        sb.append("th,td{border:1px solid #ccc;padding:4px 10px;text-align:left}");
        sb.append("th{background:#e8e8e8}");
        sb.append(".CRITICAL{color:#c00;font-weight:bold}");
        sb.append(".HIGH{color:#d60;font-weight:bold}");
        sb.append(".MEDIUM{color:#b80}");
        sb.append(".LOW{color:#888}");
        sb.append("</style>\n</head>\n<body>\n");
        sb.append("<h1>OOM Analysis Report</h1>\n");

        sb.append("<h2>Job</h2>\n<table>\n");
        row(sb, "Job ID", job.getJobId());
        row(sb, "Status", job.getStatus().toString());
        row(sb, "Created", job.getCreatedAt().toString());
        row(sb, "Updated", job.getUpdatedAt().toString());
        row(sb, "Javacore", job.getJavacorePath());
        if (job.getHeapDumpPath() != null) {
            row(sb, "Heap Dump", job.getHeapDumpPath());
        }
        sb.append("</table>\n");

        JavacoreReport report = job.getReport();
        if (report != null) {
            HeapSummary heap = report.getHeapSummary();
            if (heap != null) {
                sb.append("<h2>Heap Summary</h2>\n<table>\n");
                row(sb, "Used", formatBytes(heap.getUsedBytes()));
                row(sb, "Committed", formatBytes(heap.getCommittedBytes()));
                row(sb, "Max", formatBytes(heap.getMaxBytes()));
                if (heap.getMaxBytes() > 0) {
                    double pct = (double) heap.getUsedBytes() / heap.getMaxBytes() * 100;
                    row(sb, "Used %", String.format("%.1f%%", pct));
                }
                sb.append("</table>\n");
            }

            sb.append("<h2>Threads</h2>\n<table>\n");
            row(sb, "Total", String.valueOf(report.getThreads().size()));
            row(sb, "Blocked", String.valueOf(report.getBlockedCount()));
            row(sb, "Waiting", String.valueOf(report.getWaitingCount()));
            row(sb, "Finalizer Queue Depth", String.valueOf(report.getFinalizerQueueDepth()));
            sb.append("</table>\n");

            List<MemoryConsumer> consumers = report.getTopMemoryConsumers();
            if (!consumers.isEmpty()) {
                sb.append("<h2>Top Memory Consumers</h2>\n");
                sb.append("<table>\n<tr><th>#</th><th>Class</th><th>Total Bytes</th><th>Instances</th></tr>\n");
                for (int i = 0; i < consumers.size(); i++) {
                    MemoryConsumer c = consumers.get(i);
                    sb.append("<tr><td>").append(i + 1).append("</td>");
                    sb.append("<td>").append(esc(c.getClassName())).append("</td>");
                    sb.append("<td>").append(formatBytes(c.getTotalBytes())).append("</td>");
                    sb.append("<td>").append(c.getInstanceCount()).append("</td></tr>\n");
                }
                sb.append("</table>\n");
            }
        }

        RuleReport rules = job.getRuleReport();
        if (rules != null) {
            sb.append("<h2>Fired Rules</h2>\n");
            if (rules.getFiredRules().isEmpty()) {
                sb.append("<p>No rules fired.</p>\n");
            } else {
                sb.append("<table>\n<tr><th>Rule</th><th>Severity</th><th>Confidence</th><th>Description</th></tr>\n");
                for (FiredRule r : rules.getFiredRules()) {
                    String sev = esc(r.getSeverity());
                    sb.append("<tr>");
                    sb.append("<td>").append(esc(r.getName())).append("</td>");
                    sb.append("<td class=\"").append(sev).append("\">").append(sev).append("</td>");
                    sb.append("<td>").append(String.format("%.0f%%", r.getConfidence() * 100)).append("</td>");
                    sb.append("<td>").append(esc(r.getDescription())).append("</td>");
                    sb.append("</tr>\n");
                }
                sb.append("</table>\n");
            }
        }

        sb.append("</body>\n</html>\n");
        Files.writeString(dest, sb, StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><th>").append(esc(label)).append("</th><td>").append(esc(value)).append("</td></tr>\n");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes >= 1_073_741_824L) return String.format("%.2f GB (%,d B)", (double) bytes / 1_073_741_824, bytes);
        if (bytes >= 1_048_576L) return String.format("%.2f MB (%,d B)", (double) bytes / 1_048_576, bytes);
        if (bytes >= 1024L) return String.format("%.2f KB (%,d B)", (double) bytes / 1024, bytes);
        return bytes + " B";
    }
}
