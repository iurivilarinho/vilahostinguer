# Reads system information. Plain POSIX sh: runs on busybox (Alpine/postmarketOS) and on bash.
# Output: one key=value per line.

echo "hostname=$(cat /proc/sys/kernel/hostname 2>/dev/null || hostname 2>/dev/null)"

if [ -r /etc/os-release ]; then
	os_name=$(sed -n 's/^NAME=//p' /etc/os-release | tr -d '"' | head -1)
	os_version=$(sed -n 's/^VERSION_ID=//p' /etc/os-release | tr -d '"' | head -1)
	[ -n "$os_version" ] || os_version=$(sed -n 's/^VERSION=//p' /etc/os-release | tr -d '"' | head -1)
	echo "osName=$os_name"
	echo "osVersion=$os_version"
fi

echo "kernelVersion=$(uname -r)"
echo "architecture=$(uname -m)"

cpu=$(sed -n 's/^Hardware[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo 2>/dev/null | head -1)
[ -n "$cpu" ] || cpu=$(sed -n 's/^model name[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo 2>/dev/null | head -1)
[ -n "$cpu" ] || cpu=$(sed -n 's/^Processor[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo 2>/dev/null | head -1)
echo "cpuModel=$cpu"
echo "cpuCores=$(grep -c '^processor' /proc/cpuinfo 2>/dev/null)"

echo "memoryTotalBytes=$(awk '/^MemTotal:/ {printf "%.0f", $2 * 1024}' /proc/meminfo)"
echo "diskTotalBytes=$(df -Pk / 2>/dev/null | awk 'NR==2 {printf "%.0f", $2 * 1024}')"

model=""
[ -r /proc/device-tree/model ] && model=$(tr -d '\000' < /proc/device-tree/model)
if [ -z "$model" ] && [ -r /sys/class/dmi/id/product_name ]; then
	model="$(cat /sys/class/dmi/id/sys_vendor 2>/dev/null) $(cat /sys/class/dmi/id/product_name 2>/dev/null)"
fi
echo "model=$model"

mac=""
for nic in eth0 end0 enp1s0 wlan0; do
	if [ -r "/sys/class/net/$nic/address" ]; then
		mac=$(cat "/sys/class/net/$nic/address")
		break
	fi
done
if [ -z "$mac" ]; then
	for path in /sys/class/net/*; do
		[ "$(basename "$path")" = lo ] && continue
		mac=$(cat "$path/address" 2>/dev/null)
		[ -n "$mac" ] && break
	done
fi
echo "macAddress=$mac"

for pm in apk apt-get dnf pacman; do
	if command -v "$pm" >/dev/null 2>&1; then
		[ "$pm" = apt-get ] && pm=apt
		echo "packageManager=$pm"
		break
	fi
done

if [ -d /run/systemd/system ]; then
	echo "initSystem=systemd"
elif command -v rc-service >/dev/null 2>&1; then
	echo "initSystem=openrc"
fi

echo "homeDirectory=$HOME"
echo "epochSeconds=$(date +%s)"
[ "$(id -u)" = 0 ] && echo "root=1" || echo "root=0"
