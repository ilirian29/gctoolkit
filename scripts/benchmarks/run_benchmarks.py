#!/usr/bin/env python3
"""Run GCToolKit sample analyses against GC logs and capture throughput metrics."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List


REPO_ROOT = Path(__file__).resolve().parents[2]
os.chdir(REPO_ROOT)


@dataclass
class BenchmarkResult:
    log_path: Path
    elapsed_seconds: float
    max_rss_kb: int

    @property
    def size_bytes(self) -> int:
        return self.log_path.stat().st_size

    @property
    def throughput_mb_s(self) -> float:
        if self.elapsed_seconds == 0:
            return 0.0
        return (self.size_bytes / (1024 ** 2)) / self.elapsed_seconds

    @property
    def max_rss_mb(self) -> float:
        return self.max_rss_kb / 1024.0


def build_sample_module(skip_build: bool) -> None:
    if skip_build:
        return
    cmd = ["mvn", "-pl", "sample", "-am", "-DskipTests", "package"]
    print("Building sample module:", " ".join(cmd))
    subprocess.run(cmd, check=True)


def discover_logs(inputs: Iterable[str]) -> List[Path]:
    logs: List[Path] = []
    for item in inputs:
        path = Path(item)
        if not path.exists():
            raise FileNotFoundError(f"Log path '{item}' not found")
        if path.is_dir():
            logs.extend(sorted(p for p in path.iterdir() if p.is_file()))
        else:
            logs.append(path)
    if not logs:
        raise ValueError("No log files discovered from the supplied inputs")
    return logs


def discover_module_path() -> str:
    target_dir = Path("sample/target")
    if not target_dir.exists():
        raise RuntimeError("sample module has not been built; run mvn package first")
    jars = sorted(target_dir.glob("*.jar"))
    lib_dir = target_dir / "lib"
    if lib_dir.exists():
        jars.extend(sorted(lib_dir.glob("*.jar")))
    if not jars:
        raise RuntimeError("Unable to locate jars under sample/target; ensure the build completed successfully")
    return os_pathsep_join(str(jar) for jar in jars)


def os_pathsep_join(items: Iterable[str]) -> str:
    values = [item for item in items if item]
    return os.pathsep.join(values)


def run_benchmark(log: Path, module_path: str, heap: str, show_output: bool, time_bin: str) -> BenchmarkResult:
    java_cmd = [
        "java",
        f"-Xms{heap}",
        f"-Xmx{heap}",
        "--module-path",
        module_path,
        "--module",
        "com.microsoft.gctoolkit.sample/com.microsoft.gctoolkit.sample.Main",
        str(log.resolve()),
    ]

    cmd = [time_bin, "-f", "elapsed_s:%e\nmax_rss_kb:%M", *java_cmd]
    print(f"\nRunning benchmark for {log}...")
    completed = subprocess.run(cmd, capture_output=True, text=True, check=True)

    if show_output and completed.stdout:
        print("---- Sample Output ----")
        print(completed.stdout.strip())
        print("-----------------------")

    elapsed = None
    max_rss = None
    for line in completed.stderr.splitlines():
        if line.startswith("elapsed_s:"):
            elapsed = float(line.split(":", 1)[1])
        elif line.startswith("max_rss_kb:"):
            max_rss = int(float(line.split(":", 1)[1]))

    if elapsed is None or max_rss is None:
        raise RuntimeError("Failed to parse timing output:\n" + completed.stderr)

    return BenchmarkResult(log, elapsed, max_rss)


def print_summary(results: List[BenchmarkResult]) -> None:
    print("\nSummary:")
    header = f"{'Log':40} {'Size (MB)':>10} {'Time (s)':>10} {'Throughput (MB/s)':>18} {'Max RSS (MB)':>15}"
    print(header)
    print("-" * len(header))
    for result in results:
        print(
            f"{result.log_path.name:40} "
            f"{result.size_bytes / (1024 ** 2):10.2f} "
            f"{result.elapsed_seconds:10.2f} "
            f"{result.throughput_mb_s:18.2f} "
            f"{result.max_rss_mb:15.2f}"
        )


def resolve_time_binary() -> str:
    candidate = shutil.which("time")
    if candidate:
        return candidate
    candidate = shutil.which("/usr/bin/time")
    if candidate:
        return candidate
    raise RuntimeError("The 'time' command is required to collect resource usage metrics")


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description="Benchmark GCToolKit log parsing throughput")
    parser.add_argument("--logs", nargs="+", default=["gclogs/samples"], help="Log files or directories to benchmark")
    parser.add_argument("--heap", default="4g", help="Heap size to allocate for the sample app (e.g. 4g, 2g)")
    parser.add_argument("--skip-build", action="store_true", help="Skip building the sample module before running")
    parser.add_argument("--show-output", action="store_true", help="Print the sample application's stdout for each run")
    args = parser.parse_args(argv)

    build_sample_module(args.skip_build)
    logs = discover_logs(args.logs)
    module_path = discover_module_path()
    time_bin = resolve_time_binary()

    results: List[BenchmarkResult] = []
    for log in logs:
        try:
            result = run_benchmark(log, module_path, args.heap, args.show_output, time_bin)
            results.append(result)
        except subprocess.CalledProcessError as exc:
            print(f"Benchmark failed for {log}: {exc}", file=sys.stderr)
            if exc.stdout:
                print(exc.stdout, file=sys.stderr)
            if exc.stderr:
                print(exc.stderr, file=sys.stderr)

    if results:
        print_summary(results)
        return 0
    return 1


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except Exception as exc:  # pylint: disable=broad-except
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
