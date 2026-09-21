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

# Kernel without cgroup2 (3.x): OpenRC's default "unified" mode mounts nothing and dockerd refuses
# to start ("Devices cgroup isn't mounted"). Switch OpenRC to the v1 layout for the next boots and
# mount the v1 hierarchies now.
if ! grep -qw cgroup2 /proc/filesystems && ! [ -d /sys/fs/cgroup/devices ]; then
	echo 'Kernel sem cgroup2: montando os cgroups v1'
	if [ -f /etc/rc.conf ] && ! grep -q '^rc_cgroup_mode=' /etc/rc.conf; then
		echo 'rc_cgroup_mode="legacy"' >> /etc/rc.conf
	fi
	if [ -x /etc/init.d/cgroups ]; then
		rc-update add cgroups sysinit >/dev/null 2>&1
	fi
	mountpoint -q /sys/fs/cgroup || mount -t tmpfs -o nosuid,nodev,noexec,mode=755 cgroup /sys/fs/cgroup
	for controller in $(awk 'NR > 1 && $4 == 1 { print $1 }' /proc/cgroups); do
		mkdir -p "/sys/fs/cgroup/$controller"
		mountpoint -q "/sys/fs/cgroup/$controller" || mount -t cgroup -o "$controller" cgroup "/sys/fs/cgroup/$controller"
	done
fi

# Replacements for old kernels go into /usr/local/sbin: it survives package upgrades and, once in
# the service PATH, comes before the distribution binaries.
local_sbin_in_service_path() {
	mkdir -p /usr/local/sbin
	if [ -d /etc/conf.d ] && command -v rc-service >/dev/null 2>&1; then
		grep -q '/usr/local/sbin' /etc/conf.d/docker 2>/dev/null || echo 'export PATH="/usr/local/sbin:$PATH"' >> /etc/conf.d/docker
	elif [ -d /run/systemd/system ]; then
		mkdir -p /etc/systemd/system/docker.service.d
		printf '[Service]\nEnvironment="PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"\n' \
			> /etc/systemd/system/docker.service.d/local-sbin.conf
		systemctl daemon-reload
	fi
}

download() {
	if command -v curl >/dev/null 2>&1; then curl -fsSL -o "$2" "$1"; else wget -q -O "$2" "$1"; fi
}

# Kernel without nf_tables (3.x): the default iptables (nft backend) fails with "Could not fetch rule
# set generation id" and dockerd cannot create its NAT chains. The legacy backend works on these kernels.
if ! iptables -t nat -L -n >/dev/null 2>&1; then
	if ! command -v xtables-legacy-multi >/dev/null 2>&1 && command -v apk >/dev/null 2>&1; then
		apk add -q iptables-legacy
	fi
	legacy=$(command -v xtables-legacy-multi 2>/dev/null)
	if [ -n "$legacy" ] && "$legacy" iptables -t nat -L -n >/dev/null 2>&1; then
		echo 'Kernel sem nf_tables: usando o iptables legacy'
		local_sbin_in_service_path
		for tool in iptables iptables-save iptables-restore ip6tables ip6tables-save ip6tables-restore; do
			ln -sf "$legacy" "/usr/local/sbin/$tool"
		done
	fi
fi

# runc built with libpathrs (runc 1.4+ in rolling distributions) aborts on kernels older than 5.2
# ("at least one candidate /proc/thread-self path should work"): libpathrs needs fsopen/open_tree
# and has no fallback. Every container fails with "runc did not terminate successfully". The
# official static runc 1.3 has no libpathrs and runs on these kernels.
RUNC_VERSION='1.3.6'
kernel_minor=$(uname -r | cut -d. -f2)
if runc --version 2>/dev/null | grep -q libpathrs \
	&& { [ "$kernel_major" -lt 5 ] || { [ "$kernel_major" -eq 5 ] && [ "$kernel_minor" -lt 2 ]; }; }; then
	case "$(uname -m)" in
		aarch64 | arm64) runc_arch='arm64' ;;
		x86_64) runc_arch='amd64' ;;
		armv7* | armv6*) runc_arch='armhf' ;;
		*) runc_arch='' ;;
	esac
	if [ -n "$runc_arch" ] && ! /usr/local/sbin/runc --version 2>/dev/null | grep -q "runc version $RUNC_VERSION"; then
		echo "Kernel $(uname -r) sem fsopen: instalando o runc $RUNC_VERSION oficial (sem libpathrs)"
		base="https://github.com/opencontainers/runc/releases/download/v$RUNC_VERSION"
		if download "$base/runc.$runc_arch" /tmp/runc.new && download "$base/runc.sha256sum" /tmp/runc.sha256sum; then
			expected=$(awk -v file="runc.$runc_arch" '$2 == file || $2 == "*" file { print $1 }' /tmp/runc.sha256sum)
			actual=$(sha256sum /tmp/runc.new | cut -d' ' -f1)
			if [ -n "$expected" ] && [ "$expected" = "$actual" ]; then
				local_sbin_in_service_path
				install -m 755 /tmp/runc.new /usr/local/sbin/runc
				/usr/local/sbin/runc --version | head -1
			else
				echo 'AVISO: o runc baixado não confere com o sha256 publicado; nada foi instalado.'
			fi
		else
			echo 'AVISO: não foi possível baixar o runc; as máquinas não vão iniciar neste kernel.'
		fi
		rm -f /tmp/runc.new /tmp/runc.sha256sum
	fi
fi

# Internet through a proxy on the device's loopback (the USB phones): machines on the isolated
# network cannot reach 127.0.0.1 of the device, so the bridge gateway port is redirected to it.
# The redirect is redone at every boot, after docker0 exists.
case "${http_proxy:-}" in
	*127.0.0.1:* | *localhost:*)
		proxy_port=$(printf '%s' "$http_proxy" | sed -n 's#.*:\([0-9][0-9]*\)/*$#\1#p')
		if [ -n "$proxy_port" ]; then
			mkdir -p /usr/local/sbin
			cat > /usr/local/sbin/bancada-bridge-proxy <<SCRIPT
#!/bin/sh
# Machines on the isolated Docker network reach the device proxy through the bridge gateway.
export PATH=/usr/local/sbin:\$PATH
tries=0
while ! ip link show docker0 >/dev/null 2>&1 && [ \$tries -lt 90 ]; do tries=\$((tries + 1)); sleep 2; done
gateway=\$(ip -4 addr show docker0 2>/dev/null | awk '/inet /{split(\$2, a, "/"); print a[1]; exit}')
[ -n "\$gateway" ] || exit 0
sysctl -w net.ipv4.conf.docker0.route_localnet=1 >/dev/null
iptables -t nat -C PREROUTING -i docker0 -p tcp -d "\$gateway" --dport $proxy_port -j DNAT --to-destination 127.0.0.1:$proxy_port 2>/dev/null \\
	|| iptables -t nat -I PREROUTING -i docker0 -p tcp -d "\$gateway" --dport $proxy_port -j DNAT --to-destination 127.0.0.1:$proxy_port
SCRIPT
			chmod 755 /usr/local/sbin/bancada-bridge-proxy
			if command -v rc-update >/dev/null 2>&1; then
				mkdir -p /etc/local.d
				printf '#!/bin/sh\nsetsid /usr/local/sbin/bancada-bridge-proxy >/dev/null 2>&1 &\n' > /etc/local.d/bancada-bridge-proxy.start
				chmod 755 /etc/local.d/bancada-bridge-proxy.start
				rc-update add local default >/dev/null 2>&1
			elif [ -d /run/systemd/system ]; then
				printf '[Unit]\nAfter=docker.service\nWants=docker.service\n[Service]\nType=oneshot\nExecStart=/usr/local/sbin/bancada-bridge-proxy\n[Install]\nWantedBy=multi-user.target\n' \
					> /etc/systemd/system/bancada-bridge-proxy.service
				systemctl daemon-reload
				systemctl enable bancada-bridge-proxy.service >/dev/null 2>&1
			fi
			setsid /usr/local/sbin/bancada-bridge-proxy >/dev/null 2>&1 &
			echo "Rede isolada das máquinas: proxy do dispositivo na porta $proxy_port do gateway do Docker"
		fi
		;;
esac

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
