package com.ibm.oom.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ibm.oom.analyzer.config.OomProperties;

@SpringBootApplication
@EnableConfigurationProperties(OomProperties.class)
public class OomAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OomAnalyzerApplication.class, args);
    }
}
