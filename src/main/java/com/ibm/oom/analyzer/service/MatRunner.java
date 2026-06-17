package com.ibm.oom.analyzer.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ibm.oom.analyzer.config.OomProperties;

@Service
public class MatRunner {

    private static final Logger log = LoggerFactory.getLogger(MatRunner.class);
    private static final String SUSPECTS_REPORT = "org.eclipse.mat.api:suspects";

    private final OomProperties props;

    public MatRunner(OomProperties props) {
        this.props = props;
    }

    public MatResult run(String heapDumpPath) throws Exception {
        String executable = props.getMat().getExecutable();
        List<String> command = List.of(executable, heapDumpPath, SUSPECTS_REPORT);
        log.info("Launching MAT: {} {} {}", executable, heapDumpPath, SUSPECTS_REPORT);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        List<String> outputLines = new ArrayList<>();
        List<String> errorLines = new ArrayList<>();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                r.lines().forEach(outputLines::add);
            } catch (Exception ignored) {}
        });
        stdoutReader.start();

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                r.lines().forEach(errorLines::add);
            } catch (Exception ignored) {}
        });
        stderrReader.start();

        int exitCode;
        try {
            if (!process.waitFor(30, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RuntimeException("MAT timed out after 30 minutes");
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            stdoutReader.join();
            stderrReader.join();
        }

        log.info("MAT exited with code {} ({} stdout lines, {} stderr lines)",
                exitCode, outputLines.size(), errorLines.size());

        return new MatResult(exitCode, outputLines, errorLines);
    }
}
