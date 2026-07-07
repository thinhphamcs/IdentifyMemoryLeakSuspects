package com.ibm.oom.analyzer.engine;

import java.util.List;

public class RuleReport {

    private final List<FiredRule> firedRules;

    public RuleReport(List<FiredRule> firedRules) {
        this.firedRules = List.copyOf(firedRules);
    }

    public List<FiredRule> getFiredRules() { return firedRules; }
}
