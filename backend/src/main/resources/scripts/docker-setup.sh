# Docker configuration applied right after installing it, before the service starts.
#
# - Small root filesystem with a big /srv (phones: root 2.4 GB, userdata 25 GB): images and
#   containers go to /srv/docker.
# - Kernel older than 4.0: overlay2 needs several lower layers, which only arrived in 4.0, so the
#   vfs driver is set explicitly (slower and uses more space, but works on any kernel).
# - Internet through an HTTP proxy (http_proxy set in the login environment): dockerd does not read
#   the login environment, so the proxy goes into its service configuration for image pulls.

echo '== Configurando o Docker =='
mkdir -p /etc/docker

root_free_kb=$(df -Pk / | awk 'NR == 2 { print $4 }')
data_root=''
if mountpoint -q /srv 2>/dev/null && [ "${root_free_kb:-0}" -lt 8000000 ]; then
	data_root='/srv/docker'
	mkdir -p "$data_root"
fi

driver=''
kernel_major=$(uname -r | cut -d. -f1)
[ "$kernel_major" -lt 4 ] && driver='vfs'

if [ ! -s /etc/docker/daemon.json ] && { [ -n "$data_root" ] || [ -n "$driver" ]; }; then
	{
		echo '{'
		if [ -n "$data_root" ] && [ -n "$driver" ]; then
			printf '  "data-root": "%s",\n  "storage-driver": "%s"\n' "$data_root" "$driver"
		elif [ -n "$data_root" ]; then
			printf '  "data-root": "%s"\n' "$data_root"
		else
			printf '  "storage-driver": "%s"\n' "$driver"
		fi
		echo '}'
	} > /etc/docker/daemon.json
	echo 'daemon.json:'
	cat /etc/docker/daemon.json
fi

if [ -n "${http_proxy:-}" ]; then
	if command -v rc-service >/dev/null 2>&1; then
		if ! grep -q HTTP_PROXY /etc/conf.d/docker 2>/dev/null; then
			printf 'export HTTP_PROXY="%s"\nexport HTTPS_PROXY="%s"\nexport NO_PROXY="localhost,127.0.0.1"\n' \
				"$http_proxy" "${https_proxy:-$http_proxy}" >> /etc/conf.d/docker
		fi
	elif [ -d /run/systemd/system ]; then
		mkdir -p /etc/systemd/system/docker.service.d
		printf '[Service]\nEnvironment="HTTP_PROXY=%s" "HTTPS_PROXY=%s" "NO_PROXY=localhost,127.0.0.1"\n' \
			"$http_proxy" "${https_proxy:-$http_proxy}" > /etc/systemd/system/docker.service.d/proxy.conf
		systemctl daemon-reload
	fi
	echo "Proxy do Docker: $http_proxy"
fi
