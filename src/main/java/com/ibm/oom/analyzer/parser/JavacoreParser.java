package com.ibm.oom.analyzer.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scratch-built parser for IBM Liberty / OpenJ9 javacore .txt files (JDK 17).
 *
 * Section headers follow the pattern:
 *   NULL           -----------------------------------------------------------------------
 *   1TISIGINFO     Dump Event "systhrow" (00040000) Detail "java/lang/OutOfMemoryError" ...
 *   0SECTION       TITLE subcomponent dump routine
 *   ...
 *
 * Thread blocks begin with:
 *   3XMTHREADINFO  "threadName" J9VMThread:0x... ...
 *   3XMTHREADINFO1 ... State: ...
 *   4XESTACKTRACE  at com.example.Foo.bar(Foo.java:42)
 *
 * Heap summary lines begin with:
 *   1STHEAPTOTAL / 1STHEAPFREE / 1STHEAPALLOC / 1STHEAPINUSE
 *   or the GC memory section:
 *   1STGCHTYPE     ...
 *   1STSEGHTYPE    ...
 *   ...
 *   1STHEAPTOTAL   Total memory:         536870912 (0x20000000)
 *   1STHEAPFREE    Total memory free:    402653184 (0x18000000)
 *   1STHEAPINUSE   Total memory in use:  134217728 (0x08000000)
 *
 * Memory-consumer lines (top objects by class) appear in the CLASSES section:
 *   1CLTEXTCLLOS        Class                                          Bytes    Count
 *   2CLTEXTCLLOS        [B                                          1234567      456
 */
public class JavacoreParser {

    // ---- thread section patterns ----
    private static final Pattern THREAD_INFO =
            Pattern.compile("^3XMTHREADINFO\\s+\"([^\"]+)\".*$");
    // 3XMTHREADINFO1 carries "Java Lang State: CW, VM State: ..." — use [A-Z]+ to avoid trailing comma
    private static final Pattern THREAD_STATE =
            Pattern.compile("^3XMTHREADINFO1\\s+Java Lang State:\\s*([A-Z]+).*$");
    private static final Pattern STACK_FRAME =
            Pattern.compile("^4XESTACKTRACE\\s+at\\s+(\\S+)$");

    // ---- heap summary patterns (OpenJ9 javacore format) ----
    private static final Pattern HEAP_TOTAL =
            Pattern.compile("^1STHEAPTOTAL\\s+Total memory:\\s+(\\d+)");
    private static final Pattern HEAP_FREE =
            Pattern.compile("^1STHEAPFREE\\s+Total memory free:\\s+(\\d+)");
    private static final Pattern HEAP_INUSE =
            Pattern.compile("^1STHEAPINUSE\\s+Total memory in use:\\s+(\\d+)");

    // ---- memory consumer (class statistics) patterns ----
    // Line format varies; capture class name, bytes, instance count
    private static final Pattern MEM_CONSUMER =
            Pattern.compile("^2CLTEXTCLLOS\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)");

    // ---- finalizer queue pattern (OpenJ9 javacore: 1STFINQ line) ----
    private static final Pattern FINALIZER_QUEUE =
            Pattern.compile("^1STFINQ\\s+.*?(\\d+)\\s*$");

    private static final int TOP_CONSUMERS = 10;

    public JavacoreReport parse(String filePath) throws IOException {
        List<ThreadSnapshot> threads = new ArrayList<>();
        List<MemoryConsumer> consumers = new ArrayList<>();

        long heapTotal = -1;
        long heapFree = -1;
        long heapInUse = -1;
        int finalizerQueueDepth = 0;

        // mutable thread-accumulation state
        String currentThreadName = null;
        String currentThreadState = null;
        List<String> currentFrames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {

                // --- thread name ---
                Matcher tm = THREAD_INFO.matcher(line);
                if (tm.matches()) {
                    flushThread(threads, currentThreadName, currentThreadState, currentFrames);
                    currentThreadName = tm.group(1);
                    currentThreadState = "UNKNOWN";
                    currentFrames = new ArrayList<>();
                    continue;
                }

                // --- thread state ---
                Matcher sm = THREAD_STATE.matcher(line);
                if (sm.matches() && currentThreadName != null) {
                    currentThreadState = sm.group(1);
                    continue;
                }

                // --- stack frame ---
                Matcher fm = STACK_FRAME.matcher(line);
                if (fm.matches() && currentThreadName != null) {
                    currentFrames.add(fm.group(1));
                    continue;
                }

                // --- heap summary ---
                Matcher htm = HEAP_TOTAL.matcher(line);
                if (htm.find()) { heapTotal = Long.parseLong(htm.group(1)); continue; }

                Matcher hfm = HEAP_FREE.matcher(line);
                if (hfm.find()) { heapFree = Long.parseLong(hfm.group(1)); continue; }

                Matcher him = HEAP_INUSE.matcher(line);
                if (him.find()) { heapInUse = Long.parseLong(him.group(1)); continue; }

                // --- finalizer queue ---
                Matcher fqm = FINALIZER_QUEUE.matcher(line);
                if (fqm.matches()) { finalizerQueueDepth = Integer.parseInt(fqm.group(1)); continue; }

                // --- memory consumers ---
                Matcher cm = MEM_CONSUMER.matcher(line);
                if (cm.find()) {
                    consumers.add(new MemoryConsumer(
                            cm.group(1),
                            Long.parseLong(cm.group(2)),
                            Integer.parseInt(cm.group(3))));
                }
            }
        }

        // flush last thread
        flushThread(threads, currentThreadName, currentThreadState, currentFrames);

        // derive counts
        int blockedCount = (int) threads.stream()
                .filter(t -> t.getState().startsWith("B"))
                .count();
        int waitingCount = (int) threads.stream()
                .filter(t -> t.getState().startsWith("W") || t.getState().startsWith("CW"))
                .count();

        // build heap summary (fall back to inferred values when partial data present)
        HeapSummary heapSummary = buildHeapSummary(heapTotal, heapFree, heapInUse);

        // top consumers by bytes descending
        List<MemoryConsumer> topConsumers = consumers.stream()
                .sorted(Comparator.comparingLong(MemoryConsumer::getTotalBytes).reversed())
                .limit(TOP_CONSUMERS)
                .toList();

        return JavacoreReport.builder(filePath)
                .threads(threads)
                .blockedCount(blockedCount)
                .waitingCount(waitingCount)
                .heapSummary(heapSummary)
                .topMemoryConsumers(topConsumers)
                .finalizerQueueDepth(finalizerQueueDepth)
                .build();
    }

    private void flushThread(List<ThreadSnapshot> out,
                             String name, String state, List<String> frames) {
        if (name != null) {
            out.add(new ThreadSnapshot(name, state != null ? state : "UNKNOWN", frames));
        }
    }

    private HeapSummary buildHeapSummary(long total, long free, long inUse) {
        // Prefer directly parsed values; derive missing ones when possible
        long used = inUse >= 0 ? inUse : (total >= 0 && free >= 0 ? total - free : -1);
        long committed = total >= 0 ? total : -1;
        long max = committed; // javacore does not always expose Xmx separately
        return new HeapSummary(used, committed, max);
    }
}
