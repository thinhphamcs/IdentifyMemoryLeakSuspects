package com.ibm.oom.analyzer.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ibm.oom.analyzer.model.AnalysisJob;

@Service
public class JobRegistry {

    private final ConcurrentHashMap<String, AnalysisJob> jobs = new ConcurrentHashMap<>();

    public AnalysisJob create(String javacorePath, String heapDumpPath) {
        String jobId = UUID.randomUUID().toString();
        AnalysisJob job = new AnalysisJob(jobId, javacorePath, heapDumpPath);
        jobs.put(jobId, job);
        return job;
    }

    public Optional<AnalysisJob> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }
}
