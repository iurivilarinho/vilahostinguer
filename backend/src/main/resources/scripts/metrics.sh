# Reads current resource usage. Output: one key=value per line.

sample_cpu() {
	awk '/^cpu / { total = 0; for (i = 2; i <= NF; i++) total += $i; print total, $5 + $6 }' /proc/stat
}

first=$(sample_cpu)
sleep 0.5 2>/dev/null || sleep 1
second=$(sample_cpu)
echo "$first $second" | awk '{ dt = $3 - $1; di = $4 - $2; if (dt > 0) printf "cpuPercent=%.1f\n", 100 * (dt - di) / dt }'

awk '{ print "load1=" $1; print "load5=" $2; print "load15=" $3 }' /proc/loadavg

awk '
	/^MemTotal:/ { total = $2 }
	/^MemFree:/ { free = $2 }
	/^MemAvailable:/ { available = $2 }
	/^Buffers:/ { buffers = $2 }
	/^Cached:/ { cached = $2 }
	/^SwapTotal:/ { swap_total = $2 }
	/^SwapFree:/ { swap_free = $2 }
	END {
		if (available == "") available = free + buffers + cached
		printf "memoryTotalBytes=%.0f\n", total * 1024
		printf "memoryUsedBytes=%.0f\n", (total - available) * 1024
		printf "swapTotalBytes=%.0f\n", swap_total * 1024
		printf "swapUsedBytes=%.0f\n", (swap_total - swap_free) * 1024
	}' /proc/meminfo

df -Pk / 2>/dev/null | awk 'NR == 2 { printf "diskTotalBytes=%.0f\ndiskUsedBytes=%.0f\n", $2 * 1024, $3 * 1024 }'

echo "uptimeSeconds=$(cut -d. -f1 /proc/uptime)"

for supply in /sys/class/power_supply/*; do
	[ -r "$supply/type" ] || continue
	if [ "$(cat "$supply/type")" = Battery ] && [ -r "$supply/capacity" ]; then
		echo "batteryPercent=$(cat "$supply/capacity")"
		echo "batteryStatus=$(cat "$supply/status" 2>/dev/null)"
		break
	fi
done

max_temp=""
for zone in /sys/class/thermal/thermal_zone*; do
	[ -r "$zone/temp" ] || continue
	value=$(cat "$zone/temp" 2>/dev/null)
	case "$value" in ''|*[!0-9-]*) continue ;; esac
	# Some kernels report degrees, most report millidegrees.
	[ "$value" -lt 1000 ] && value=$((value * 1000))
	[ "$value" -gt 150000 ] && continue
	if [ -z "$max_temp" ] || [ "$value" -gt "$max_temp" ]; then
		max_temp=$value
	fi
done
[ -n "$max_temp" ] && awk -v t="$max_temp" 'BEGIN { printf "temperatureCelsius=%.1f\n", t / 1000 }'

echo "processCount=$(ls -d /proc/[0-9]* 2>/dev/null | wc -l)"

rx=0
tx=0
for nic in /sys/class/net/*; do
	[ "$(basename "$nic")" = lo ] && continue
	rx=$((rx + $(cat "$nic/statistics/rx_bytes" 2>/dev/null || echo 0)))
	tx=$((tx + $(cat "$nic/statistics/tx_bytes" 2>/dev/null || echo 0)))
done
echo "networkReceivedBytes=$rx"
echo "networkSentBytes=$tx"
