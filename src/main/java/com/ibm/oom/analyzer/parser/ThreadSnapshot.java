package com.ibm.oom.analyzer.parser;

import java.util.List;

public class ThreadSnapshot {

    private final String name;
    private final String state;
    private final List<String> stackFrames;

    public ThreadSnapshot(String name, String state, List<String> stackFrames) {
        this.name = name;
        this.state = state;
        this.stackFrames = List.copyOf(stackFrames);
    }

    public String getName() { return name; }
    public String getState() { return state; }
    public List<String> getStackFrames() { return stackFrames; }
}
