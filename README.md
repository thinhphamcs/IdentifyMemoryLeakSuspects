# Liberty OOM Analyzer

A passion project born from countless hours lost debugging and trying to identify suspects in memory-leaking Liberty applications.

## What it does

Given a Liberty javacore `.txt` and a heap dump (`.phd` or `.hprof`), the analyzer produces an HTML + JSON report showing the OOM root cause with a confidence level — no manual cross-referencing required.

## Build phases (one branch per phase)

| Branch | What lands |
|---|---|
| `feature/initial-setup` | Folder structure, README (you are here) |
| `feature/job-model` | Job state machine, in-memory registry, REST skeleton |
| `feature/javacore-parser` | Scratch-built JDK17/Liberty javacore parser |
| `feature/mat-integration` | MAT headless CLI integration via ProcessBuilder |
| `feature/rule-engine` | 4-rule correlation engine with externalized thresholds |
| `feature/report-generator` | HTML + JSON report writer |
| `feature/ui` | Thymeleaf form, vanilla JS status poller |

## Stack

- **Java 17**, Spring Boot 3.x, Maven (fat jar)
- Thymeleaf + vanilla JS frontend
- MAT headless CLI (`ParseHeapDump.sh`) via `ProcessBuilder`
- Scratch-built javacore parser (Liberty/JDK17 format)
- Rule-based correlation engine

## Project layout

```
src/
└── main/
    ├── java/com/ibm/oom/analyzer/
    │   ├── config/       # externalized properties (OomProperties)
    │   ├── controller/   # REST + UI controllers
    │   ├── engine/       # rule-based correlation engine
    │   ├── model/        # job state machine (PENDING→COMPLETE/FAILED)
    │   ├── parser/       # javacore parser
    │   ├── report/       # HTML + JSON report generator
    │   └── service/      # job orchestration
    └── resources/
        ├── templates/    # Thymeleaf views
        └── static/
            ├── css/
            └── js/
```

## Configuration (`application.properties`)

```properties
oom.mat.executable=/path/to/ParseHeapDump.sh
oom.mat.index.dir=/tmp/oom-indexes
oom.rules.heap-retention-threshold=0.20
oom.rules.thread-block-threshold=10
oom.jobs.output.dir=/tmp/oom-jobs
```

## How to run (once complete)

```bash
mvn package -q
java -jar target/oom-analyzer-0.1.0-SNAPSHOT.jar
# open http://localhost:8080
```
