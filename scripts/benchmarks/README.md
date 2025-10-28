# Benchmarking GCToolKit

The scripts in this directory help exercise the sample application against large
GC logs so you can evaluate parsing throughput and memory usage.

## Prerequisites

* Java 11+
* Maven
* GNU `time` (usually available as `/usr/bin/time`)

## Running the benchmarks

```
scripts/benchmarks/run_benchmarks.py --logs gclogs/samples
```

The script builds the sample module (unless `--skip-build` is supplied) and
invokes it for every log file discovered in the provided paths.  The output
includes elapsed time, throughput, and maximum resident set size so you can
compare different parser optimisations.

### Useful flags

* `--heap 6g` &mdash; override the JVM heap size used for the sample run.
* `--skip-build` &mdash; reuse an existing build instead of invoking Maven.
* `--show-output` &mdash; forward the sample application's stdout for debugging.

You can list individual log files or directories:

```
scripts/benchmarks/run_benchmarks.py --logs path/to/log.log other/logs/
```

## Profiling

Combine the benchmark run with tools like Java Flight Recorder or
`async-profiler` to identify hotspots.  For example:

```
async-profiler/profiler.sh -d 60 -e cpu -- scripts/benchmarks/run_benchmarks.py --logs /path/to/log.log
```

Use the new progress reporting in the sample app to monitor the analysis while
the profiler is attached.
