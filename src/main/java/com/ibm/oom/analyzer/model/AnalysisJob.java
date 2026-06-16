package com.ibm.oom.analyzer.model;

import java.time.Instant;

import com.ibm.oom.analyzer.parser.JavacoreReport;

public class AnalysisJob {

    private final String jobId;
    private final String javacorePath;
    private final String heapDumpPath;
    private final Instant createdAt;

    private volatile JobStatus status;
    private volatile String errorMessage;
    private volatile Instant updatedAt;
    private volatile JavacoreReport report;

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

    public void setStatus(JobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void complete(JavacoreReport report) {
        this.report = report;
        setStatus(JobStatus.COMPLETE);
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        setStatus(JobStatus.FAILED);
    }
}
