package com.ibm.oom.analyzer.model;

import java.time.Instant;

import com.ibm.oom.analyzer.engine.RuleReport;
import com.ibm.oom.analyzer.parser.JavacoreReport;
import com.ibm.oom.analyzer.report.ReportFiles;
import com.ibm.oom.analyzer.service.MatResult;

public class AnalysisJob {

    private final String jobId;
    private final String javacorePath;
    private final String heapDumpPath;
    private final Instant createdAt;

    private volatile JobStatus status;
    private volatile String errorMessage;
    private volatile Instant updatedAt;
    private volatile JavacoreReport report;
    private volatile MatResult matResult;
    private volatile RuleReport ruleReport;
    private volatile ReportFiles reportFiles;

    public AnalysisJob(String jobId, String javacorePath, String heapDumpPath) {
        this.jobId = jobId;
        this.javacorePath = javacorePath;
        this.heapDumpPath = heapDumpPath;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = JobStatus.PENDING;
    }

    public String getJobId() { return jobId; }
    public String getJavacorePath() { return javacorePath; }
    public String getHeapDumpPath() { return heapDumpPath; }
    public Instant getCreatedAt() { return createdAt; }
    public JobStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getUpdatedAt() { return updatedAt; }
    public JavacoreReport getReport() { return report; }
    public MatResult getMatResult() { return matResult; }
    public void setMatResult(MatResult matResult) { this.matResult = matResult; }
    public RuleReport getRuleReport() { return ruleReport; }
    public ReportFiles getReportFiles() { return reportFiles; }
    public void setReportFiles(ReportFiles reportFiles) { this.reportFiles = reportFiles; }

    public void setStatus(JobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void storeResults(JavacoreReport report, RuleReport ruleReport) {
        this.report = report;
        this.ruleReport = ruleReport;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        setStatus(JobStatus.COMPLETE);
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        setStatus(JobStatus.FAILED);
    }
}
