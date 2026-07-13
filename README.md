# Liberty OOM Analyzer

A tool born from countless hours lost debugging OutOfMemoryError crashes in IBM Liberty applications. Given a Liberty javacore and a heap dump, it produces a clear HTML and JSON report identifying the likely root cause — no manual cross-referencing required.

---

## What it does

1. **Parses** the Liberty/OpenJ9 javacore to extract heap usage, thread states, memory consumers, and finalizer queue depth.
2. **Runs** Eclipse MAT headlessly against the heap dump to identify leak suspects.
3. **Correlates** both data sources through a rule engine to produce a confidence-scored diagnosis.
4. **Generates** a self-contained HTML report and a machine-readable JSON report.

---

## Rules

| Rule | Severity | Fires when |
|---|---|---|
| `HIGH_HEAP_RETENTION` | HIGH | Heap in use exceeds the configured retention threshold (default 85%) |
| `EXCESSIVE_THREAD_BLOCKING` | HIGH | Blocked thread count exceeds the configured threshold (default 10) |
| `MAT_SUSPECTS_PRESENT` | CRITICAL | MAT analysis identifies memory leak suspects in the heap dump |
| `FINALIZER_QUEUE_BACKLOG` | MEDIUM | Finalizer queue depth exceeds the configured threshold (default 50) |

Each fired rule carries a **confidence score** (0–100%) so you can triage at a glance.

---

## Stack

- Java 17, Spring Boot 3.2, Maven (fat jar)
- Scratch-built Liberty/OpenJ9 javacore parser
- Eclipse MAT headless CLI (`ParseHeapDump.sh`) via `ProcessBuilder`
- Rule-based correlation engine with externalized thresholds
- Thymeleaf + vanilla JS frontend

---

## Requirements

- Java 17+
- Maven 3.8+
- Eclipse MAT with `ParseHeapDump.sh` (only required for heap dump analysis — javacore-only runs still work)

---

## Build and run

```bash
mvn package -q
java -jar target/oom-analyzer-0.1.0-SNAPSHOT.jar
```

Then open [http://localhost:8080](http://localhost:8080).

---

## Configuration

All settings live in `src/main/resources/application.properties`:

```properties
# Path to Eclipse MAT's ParseHeapDump.sh script
oom.mat.executable=/path/to/ParseHeapDump.sh

# Rule thresholds — tune to your environment
oom.rules.heap-retention-threshold=0.85
oom.rules.thread-block-threshold=10
oom.rules.finalizer-queue-threshold=50

# Directory where per-job HTML and JSON reports are written
oom.jobs.output-dir=/tmp/oom-jobs
```

---

## How to use

1. Enter the **absolute path** to your javacore `.txt` file.
2. Enter the **absolute path** to your heap dump (`.phd` or `.hprof`).
3. Click **Analyze** and watch the status update in real time.
4. On completion, download the **HTML report** for a human-readable summary or the **JSON report** for tooling integration.

---

## Output

**HTML report** includes:
- Heap summary (used, committed, max, used %)
- Thread counts (total, blocked, waiting, finalizer queue depth)
- Top 10 memory consumers by class
- All fired rules with severity, confidence, and description

**JSON report** contains the same data in a structured format suitable for dashboards or further automation.

---

## Project layout

```
src/main/java/com/ibm/oom/analyzer/
├── config/       # Externalized properties (OomProperties)
├── controller/   # REST API + Thymeleaf UI controller
├── engine/       # Rule engine and analysis orchestration
├── model/        # Job state machine (PENDING → COMPLETE / FAILED)
├── parser/       # Liberty/OpenJ9 javacore parser
├── report/       # HTML + JSON report generator
└── service/      # MAT runner and job registry
```

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/jobs` | Submit a new analysis job |
| `GET` | `/api/jobs/{id}/status` | Poll job status |
| `GET` | `/api/jobs/{id}/report` | Full javacore report (JSON) |
| `GET` | `/api/jobs/{id}/rule-report` | Fired rules (JSON) |
| `GET` | `/api/jobs/{id}/mat-report` | MAT output (JSON) |
| `GET` | `/api/jobs/{id}/report-files/html` | Download HTML report |
| `GET` | `/api/jobs/{id}/report-files/json` | Download JSON report |
