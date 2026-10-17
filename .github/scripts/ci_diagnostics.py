"""Read-only CI observations. Sampling intervals never synchronize Selenium tests."""
import datetime
import json
import os
from pathlib import Path
import re
import signal
import socket
import ssl
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def counters(path):
    source = Path(path)
    if not source.exists():
        return {}
    return {parts[0].rstrip(":"): int(parts[1]) for line in source.read_text().splitlines()
            if len(parts := line.split()) >= 2 and parts[1].isdigit()}


def monitor(destination):
    stop = threading.Event()
    signal.signal(signal.SIGTERM, lambda *_: stop.set())
    previous = None
    with Path(destination).open("w", buffering=1) as stream:
        while not stop.is_set():
            cpu = [int(v) for v in Path("/proc/stat").read_text().splitlines()[0].split()[1:]]
            busy = None
            if previous:
                delta = [a - b for a, b in zip(cpu, previous)]
                total = sum(delta[:8])
                busy = round(100 * (total - delta[3] - delta[4]) / total, 2) if total else 0
            previous = cpu
            memory = counters("/proc/meminfo")
            process_lines = subprocess.check_output(["ps", "-eo", "comm=,rss="], text=True).splitlines()
            chrome = [int(p[1]) for line in process_lines if len(p := line.split()) == 2 and p[0].startswith("chrome")]
            java = [int(p[1]) for line in process_lines if len(p := line.split()) == 2 and p[0] == "java"]
            sample = {"utc": utc(), "logicalCpus": os.cpu_count(), "cpuBusyPercent": busy,
                      "memTotalKiB": memory.get("MemTotal"), "memAvailableKiB": memory.get("MemAvailable"),
                      "swapFreeKiB": memory.get("SwapFree"), "chromeProcesses": len(chrome),
                      "chromeRssSumKiB": sum(chrome), "javaRssSumKiB": sum(java),
                      "cgroupMemoryEvents": counters("/sys/fs/cgroup/memory.events"),
                      "cpuPressure": Path("/proc/pressure/cpu").read_text().strip()}
            stream.write(json.dumps(sample) + "\n")
            stop.wait(5)


def probe(destination):
    # One ordinary HTTPS request, default certificate verification, no retries or identity changes.
    result = {"utc": utc(), "url": "https://automationexercise.com/", "tlsVerification": True}
    start = time.monotonic()
    try:
        result["dnsAddresses"] = sorted({a[4][0] for a in socket.getaddrinfo("automationexercise.com", 443)})
        try:
            response = urllib.request.urlopen(result["url"], timeout=30, context=ssl.create_default_context())
        except urllib.error.HTTPError as error:
            response = error
        with response:
            result["status"] = response.code
            result["headers"] = {name: response.headers[name] for name in
                                  ["Server", "CF-Ray", "Date", "Content-Type", "Retry-After"] if name in response.headers}
            text = response.read(8192).decode("utf-8", "replace")
            title = re.search(r"<title[^>]*>(.*?)</title>", text, re.I | re.S)
            result["title"] = re.sub(r"\s+", " ", title.group(1)).strip() if title else ""
    except (OSError, urllib.error.URLError) as error:
        result["transportError"] = type(error).__name__
        result["detail"] = str(error)
    result["durationSeconds"] = round(time.monotonic() - start, 3)
    Path(destination).write_text(json.dumps(result, indent=2))
    print(json.dumps(result))


if __name__ == "__main__":
    {"monitor": monitor, "probe": probe}[sys.argv[1]](sys.argv[2])
