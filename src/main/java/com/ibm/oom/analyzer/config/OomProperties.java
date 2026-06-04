package com.ibm.oom.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oom")
public class OomProperties {

    private Mat mat = new Mat();
    private Rules rules = new Rules();
    private Jobs jobs = new Jobs();

    public Mat getMat() { return mat; }
    public Rules getRules() { return rules; }
    public Jobs getJobs() { return jobs; }

    public static class Mat {
        private String executable;
        private String indexDir;

        public String getExecutable() { return executable; }
        public void setExecutable(String executable) { this.executable = executable; }
        public String getIndexDir() { return indexDir; }
        public void setIndexDir(String indexDir) { this.indexDir = indexDir; }
    }

    public static class Rules {
        private double heapRetentionThreshold;
        private int threadBlockThreshold;

        public double getHeapRetentionThreshold() { return heapRetentionThreshold; }
        public void setHeapRetentionThreshold(double heapRetentionThreshold) { this.heapRetentionThreshold = heapRetentionThreshold; }
        public int getThreadBlockThreshold() { return threadBlockThreshold; }
        public void setThreadBlockThreshold(int threadBlockThreshold) { this.threadBlockThreshold = threadBlockThreshold; }
    }

    public static class Jobs {
        private String outputDir;

        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    }
}
