#!/usr/bin/env bash
set -euo pipefail

LOG_PATH=${LOG_PATH:-/logs/shell-access.log}
SLEEP_INTERVAL=${SLEEP_INTERVAL:-0.01}

mkdir -p "$(dirname "$LOG_PATH")"

echo "Starting shell log generator -> ${LOG_PATH}" >&2

trap 'echo "Shell log generator stopping..." >&2; exit 0' SIGINT SIGTERM

while true; do
    random_ip=$(dd if=/dev/urandom bs=4 count=1 2>/dev/null | od -An -tu1 | sed -e 's/^ *//' -e 's/  */./g')
    random_size=$(( (RANDOM % 65535) + 1 ))
    current_date_time=$(date '+%d/%b/%Y:%H:%M:%S %z')
    log_line="$random_ip - - [$current_date_time] \"GET /data.php HTTP/1.1\" 200 $random_size"
    echo "$log_line" | tee -a "$LOG_PATH"
    sleep "$SLEEP_INTERVAL"
done
