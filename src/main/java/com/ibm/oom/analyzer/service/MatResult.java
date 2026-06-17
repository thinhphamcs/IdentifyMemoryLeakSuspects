package com.ibm.oom.analyzer.service;

import java.util.List;

public class MatResult {

    private final int exitCode;
    private final List<String> outputLines;
    private final List<String> errorLines;

    public MatResult(int exitCode, List<String> outputLines, List<String> errorLines) {
        this.exitCode = exitCode;
        this.outputLines = List.copyOf(outputLines);
        this.errorLines = List.copyOf(errorLines);
    }

    public int getExitCode() { return exitCode; }
    public List<String> getOutputLines() { return outputLines; }
    public List<String> getErrorLines() { return errorLines; }
    public boolean isSuccess() { return exitCode == 0; }
}
