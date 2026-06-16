package com.ibm.oom.analyzer.engine;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ibm.oom.analyzer.model.AnalysisJob;
import com.ibm.oom.analyzer.model.JobStatus;
import com.ibm.oom.analyzer.parser.JavacoreParser;
import com.ibm.oom.analyzer.parser.JavacoreReport;

@Service
public class AnalysisEngine {

    private final JavacoreParser parser = new JavacoreParser();

    @Async
    public void process(AnalysisJob job) {
        try {
            job.setStatus(JobStatus.INDEXING);
            String path = job.getJavacorePath();
            if (!Files.exists(Path.of(path))) {
                job.fail("javacore file not found: " + path);
                return;
            }

            job.setStatus(JobStatus.ANALYZING);
            JavacoreReport report = parser.parse(path);
            job.complete(report);

        } catch (Exception e) {
            job.fail("analysis error: " + e.getMessage());
        }
    }
}
