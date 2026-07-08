package com.ibm.oom.analyzer.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.oom.analyzer.engine.AnalysisEngine;
import com.ibm.oom.analyzer.model.AnalysisJob;
import com.ibm.oom.analyzer.model.JobStatus;
import com.ibm.oom.analyzer.service.JobRegistry;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRegistry registry;
    private final AnalysisEngine engine;

    public JobController(JobRegistry registry, AnalysisEngine engine) {
        this.registry = registry;
        this.engine = engine;
    }

    @PostMapping
    public ResponseEntity<?> createJob(
            @RequestParam String javacorePath,
            @RequestParam String heapDumpPath) {

        if (javacorePath == null || javacorePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "javacorePath must not be blank"));
        }
        if (heapDumpPath == null || heapDumpPath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "heapDumpPath must not be blank"));
        }

        AnalysisJob job = registry.create(javacorePath.trim(), heapDumpPath.trim());
        engine.process(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", job.getJobId()));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        return registry.find(jobId)
                .map(job -> {
                    var body = new java.util.LinkedHashMap<String, Object>();
                    body.put("jobId", job.getJobId());
                    body.put("status", job.getStatus());
                    if (job.getErrorMessage() != null) {
                        body.put("errorMessage", job.getErrorMessage());
                    }
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/report")
    public ResponseEntity<?> getReport(@PathVariable String jobId) {
        return registry.find(jobId)
                .map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    return ResponseEntity.ok(job.getReport());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/mat-report")
    public ResponseEntity<?> getMatReport(@PathVariable String jobId) {
        return registry.find(jobId)
                .<ResponseEntity<?>>map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    if (job.getMatResult() == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(job.getMatResult());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/rule-report")
    public ResponseEntity<?> getRuleReport(@PathVariable String jobId) {
        return registry.find(jobId)
                .<ResponseEntity<?>>map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    return ResponseEntity.ok(job.getRuleReport());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/report-files")
    public ResponseEntity<?> getReportFiles(@PathVariable String jobId) {
        return registry.find(jobId)
                .<ResponseEntity<?>>map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    if (job.getReportFiles() == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "report files not available"));
                    }
                    var body = new java.util.LinkedHashMap<String, Object>();
                    body.put("jobId", jobId);
                    body.put("jsonReport", job.getReportFiles().getJsonPath());
                    body.put("htmlReport", job.getReportFiles().getHtmlPath());
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/report-files/json")
    public ResponseEntity<?> downloadJsonReport(@PathVariable String jobId) {
        return registry.find(jobId)
                .<ResponseEntity<?>>map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    if (job.getReportFiles() == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "report files not available"));
                    }
                    try {
                        byte[] content = Files.readAllBytes(Path.of(job.getReportFiles().getJsonPath()));
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(content);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "could not read JSON report: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/report-files/html")
    public ResponseEntity<?> downloadHtmlReport(@PathVariable String jobId) {
        return registry.find(jobId)
                .<ResponseEntity<?>>map(job -> {
                    if (job.getStatus() != JobStatus.COMPLETE) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "job not complete", "status", job.getStatus()));
                    }
                    if (job.getReportFiles() == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "report files not available"));
                    }
                    try {
                        byte[] content = Files.readAllBytes(Path.of(job.getReportFiles().getHtmlPath()));
                        return ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .body(content);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "could not read HTML report: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
