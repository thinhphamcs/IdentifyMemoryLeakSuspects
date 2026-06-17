package com.ibm.oom.analyzer.engine;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ibm.oom.analyzer.model.AnalysisJob;
import com.ibm.oom.analyzer.model.JobStatus;
import com.ibm.oom.analyzer.parser.JavacoreParser;
import com.ibm.oom.analyzer.parser.JavacoreReport;
import com.ibm.oom.analyzer.service.MatResult;
import com.ibm.oom.analyzer.service.MatRunner;

@Service
public class AnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEngine.class);

    private final JavacoreParser parser = new JavacoreParser();
    private final MatRunner matRunner;

    public AnalysisEngine(MatRunner matRunner) {
        this.matRunner = matRunner;
    }

    @Async
    public void process(AnalysisJob job) {
        try {
            job.setStatus(JobStatus.INDEXING);
            String javacorePath = job.getJavacorePath();
            if (!Files.exists(Path.of(javacorePath))) {
                job.fail("javacore file not found: " + javacorePath);
                return;
            }

            job.setStatus(JobStatus.ANALYZING);
            JavacoreReport report = parser.parse(javacorePath);

            String heapDumpPath = job.getHeapDumpPath();
            if (heapDumpPath != null && !heapDumpPath.isBlank() && Files.exists(Path.of(heapDumpPath))) {
                try {
                    MatResult matResult = matRunner.run(heapDumpPath);
                    job.setMatResult(matResult);
                } catch (Exception e) {
                    log.warn("MAT analysis failed for job {}: {}", job.getJobId(), e.getMessage());
                }
            }

            job.complete(report);

        } catch (Exception e) {
            job.fail("analysis error: " + e.getMessage());
        }
    }
}
