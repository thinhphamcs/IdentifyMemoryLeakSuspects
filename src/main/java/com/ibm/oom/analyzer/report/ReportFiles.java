package com.ibm.oom.analyzer.report;

public class ReportFiles {

    private final String jsonPath;
    private final String htmlPath;

    public ReportFiles(String jsonPath, String htmlPath) {
        this.jsonPath = jsonPath;
        this.htmlPath = htmlPath;
    }

    public String getJsonPath() { return jsonPath; }
    public String getHtmlPath() { return htmlPath; }
}
