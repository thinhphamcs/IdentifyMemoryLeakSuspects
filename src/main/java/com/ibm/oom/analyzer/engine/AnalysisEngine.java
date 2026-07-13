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
import com.ibm.oom.analyzer.report.ReportFiles;
import com.ibm.oom.analyzer.report.ReportGenerator;
import com.ibm.oom.analyzer.service.MatResult;
import com.ibm.oom.analyzer.service.MatRunner;

@Service
public class AnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEngine.class);

    private final JavacoreParser parser = new JavacoreParser();
    private final MatRunner matRunner;
    private final RuleEngine ruleEngine;
    private final ReportGenerator reportGenerator;

    public AnalysisEngine(MatRunner matRunner, RuleEngine ruleEngine, ReportGenerator reportGenerator) {
        this.matRunner = matRunner;
        this.ruleEngine = ruleEngine;
        this.reportGenerator = reportGenerator;
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

            RuleReport ruleReport = ruleEngine.evaluate(report, job.getMatResult());
            job.storeResults(report, ruleReport);

            try {
                ReportFiles rf = reportGenerator.generate(job);
                job.setReportFiles(rf);
            } catch (Exception e) {
                log.warn("Report generation failed for job {}: {}", job.getJobId(), e.getMessage());
            }

            job.complete();

        } catch (Exception e) {
            job.fail("analysis error: " + e);
        }
    }
}
