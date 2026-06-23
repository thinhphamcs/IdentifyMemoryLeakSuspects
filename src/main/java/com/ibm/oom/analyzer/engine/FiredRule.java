package com.ibm.oom.analyzer.engine;

public class FiredRule {

    private final String name;
    private final String severity;
    private final double confidence;
    private final String description;

    public FiredRule(String name, String severity, double confidence, String description) {
        this.name = name;
        this.severity = severity;
        this.confidence = confidence;
        this.description = description;
    }

    public String getName() { return name; }
    public String getSeverity() { return severity; }
    public double getConfidence() { return confidence; }
    public String getDescription() { return description; }
}
