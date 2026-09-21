# Lists disks and partitions. Output, one per line:
#   name|sizeBytes|disk(0/1)|fileSystem|label|partitionName|mountPoint|usedBytes
#
# Mounts are matched by major:minor (mountinfo), not by device path: the root is often shown as
# /dev/root, and a partition missing from /proc/mounts would look free to format.

tail -n +3 /proc/partitions | while read -r major minor blocks name; do
	[ -n "$name" ] || continue
	case "$name" in ram*|loop*|zram*|mtdblock*) continue ;; esac

	size=$((blocks * 1024))
	disk=0
	[ -d "/sys/block/$name" ] && disk=1

	part_name=$(sed -n 's/^PARTNAME=//p' "/sys/class/block/$name/uevent" 2>/dev/null)

	mount_point=$(awk -v id="$major:$minor" '$3 == id { print $5; exit }' /proc/self/mountinfo 2>/dev/null)
	if [ -z "$mount_point" ] && grep -q "^/dev/$name " /proc/swaps 2>/dev/null; then
		mount_point="[swap]"
	fi

	fs=""
	label=""
	if command -v blkid >/dev/null 2>&1; then
		info=$(blkid "/dev/$name" 2>/dev/null)
		fs=$(echo "$info" | sed -n 's/.* TYPE="\([^"]*\)".*/\1/p')
		label=$(echo "$info" | sed -n 's/.* LABEL="\([^"]*\)".*/\1/p')
	fi

	used=""
	case "$mount_point" in
		/*) used=$(df -Pk "$mount_point" 2>/dev/null | awk 'NR == 2 { printf "%.0f", $3 * 1024 }') ;;
	esac

	echo "$name|$size|$disk|$fs|$label|$part_name|$mount_point|$used"
done
