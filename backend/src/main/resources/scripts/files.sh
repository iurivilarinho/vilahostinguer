# Lists one directory. The target path comes in $1 (set by the caller).
# Output, one per line: type|sizeBytes|permissions|modifiedEpoch|name
# type: d = directory, l = symlink, f = anything else

dir="$1"
[ -d "$dir" ] || { echo "Pasta não encontrada: $dir" >&2; exit 2; }
cd "$dir" || exit 2

for entry in .* *; do
	case "$entry" in .|..) continue ;; esac
	[ -e "$entry" ] || [ -L "$entry" ] || continue
	if [ -L "$entry" ]; then
		kind=l
	elif [ -d "$entry" ]; then
		kind=d
	else
		kind=f
	fi
	info=$(stat -c '%s|%A|%Y' -- "$entry" 2>/dev/null) || info="0|----------|0"
	printf '%s|%s|%s\n' "$kind" "$info" "$entry"
done
