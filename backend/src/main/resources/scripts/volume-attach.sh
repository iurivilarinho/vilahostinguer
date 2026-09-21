# Connects a disk kept on the PC (NBD) and mounts it. Variables set before this script:
#   nbd_host nbd_port nbd_export  where the PC serves the disk and its secret name
#   volume_id                     key of the state file under /run/bancada/discos
#   connected                     1 when the PC sees this disk connected right now
#   mount_path label              folder to mount on and ext4 label
#   allow_format                  1 only while the disk never had a filesystem
#   old_kernel                    1 on kernels older than 4.0 (ext4 without newer features)
#   install_nbd install_e2fs      package commands, empty when the manager is unknown
# Prints FORMATTED=1 when it created the filesystem, and DEVICE=/dev/nbdX.

state_dir=/run/bancada/discos
mkdir -p "$state_dir" && chmod 700 /run/bancada "$state_dir"

if [ ! -e /sys/block/nbd0 ]; then
  modprobe nbd 2>/dev/null
fi
if [ ! -e /sys/block/nbd0 ]; then
  echo "O kernel deste dispositivo não tem suporte a NBD (CONFIG_BLK_DEV_NBD). Sem ele não dá para usar discos do PC."
  exit 3
fi

if ! command -v nbd-client >/dev/null 2>&1; then
  if [ -z "$install_nbd" ]; then
    echo "Instale o nbd-client neste dispositivo (o gerenciador de pacotes não foi reconhecido)."
    exit 127
  fi
  echo "== Instalando o nbd-client =="
  sh -c "$install_nbd" || { echo "Não foi possível instalar o nbd-client."; exit 127; }
fi
if ! command -v mkfs.ext4 >/dev/null 2>&1 && [ "$allow_format" = 1 ]; then
  echo "== Instalando o e2fsprogs =="
  sh -c "$install_e2fs" || { echo "Não foi possível instalar o e2fsprogs (mkfs.ext4)."; exit 127; }
fi

is_mounted() {
  awk -v d="$1" -v m="$2" '($1 == d || d == "") && $2 == m { found = 1 } END { exit !found }' /proc/mounts
}

dev=""
if [ -f "$state_dir/$volume_id" ]; then
  old=$(cat "$state_dir/$volume_id")
  # the PC knows whether the connection is alive: /sys/block/nbdX/pid outlives a dead one
  if [ "$connected" = 1 ] && [ -e "/sys/block/${old#/dev/}/pid" ]; then
    dev=$old
  else
    # the connection with the PC died (the PC restarted): drop the dead mount and connect again
    echo "== A conexão com o PC tinha caído; conectando de novo =="
    if is_mounted "$old" "$mount_path"; then
      umount "$mount_path" 2>/dev/null || umount -l "$mount_path"
    fi
    nbd-client -d "$old" >/dev/null 2>&1
    rm -f "$state_dir/$volume_id"
  fi
fi

# already mounted from this disk: nothing to do
if [ -n "$dev" ] && is_mounted "$dev" "$mount_path"; then
  echo "== O disco já está montado em $mount_path =="
  echo "DEVICE=$dev"
  exit 0
fi

if is_mounted "" "$mount_path"; then
  echo "Já existe outra coisa montada em $mount_path. Escolha outra pasta."
  exit 7
fi
if [ -d "$mount_path" ] && [ -n "$(ls -A "$mount_path" 2>/dev/null)" ]; then
  echo "A pasta $mount_path já tem arquivos; montar o disco por cima os esconderia. Escolha uma pasta vazia."
  exit 7
fi

if [ -z "$dev" ]; then
  for block in /sys/block/nbd*; do
    [ -e "$block/pid" ] && continue
    [ "$(cat "$block/size" 2>/dev/null)" = 0 ] || continue
    dev=/dev/${block##*/}
    break
  done
  if [ -z "$dev" ]; then
    echo "Todos os /dev/nbd deste dispositivo estão em uso."
    exit 4
  fi
  echo "== Conectando ao PC ($nbd_host) em $dev =="
  # -persist: when the PC restarts the device reconnects by itself; -L: ioctl mode (old kernels have no netlink)
  if ! nbd-client "$nbd_host" "$nbd_port" "$dev" -N "$nbd_export" -b 4096 -persist -L 2>&1; then
    nbd-client "$nbd_host" "$nbd_port" "$dev" -N "$nbd_export" -b 4096 -persist 2>&1 || {
      echo "O dispositivo não conseguiu se conectar ao PC em $nbd_host:$nbd_port. Veja se o Firewall do Windows libera o Bancada."
      exit 5
    }
  fi
  echo "$dev" > "$state_dir/$volume_id"
fi

tries=0
while [ "$(cat "/sys/block/${dev#/dev/}/size" 2>/dev/null)" = 0 ] && [ $tries -lt 20 ]; do
  sleep 1
  tries=$((tries + 1))
done

fs=$(blkid "$dev" 2>/dev/null | sed -n 's/.* TYPE="\([^"]*\)".*/\1/p')
if [ -z "$fs" ]; then
  if [ "$allow_format" != 1 ]; then
    echo "O disco não mostra sistema de arquivos, mas já foi formatado antes. Nada foi apagado; confira o arquivo no PC."
    nbd-client -d "$dev" >/dev/null 2>&1
    rm -f "$state_dir/$volume_id"
    exit 6
  fi
  echo "== Criando o sistema de arquivos (ext4) =="
  if [ "$old_kernel" = 1 ]; then
    mkfs.ext4 -F -q -L "$label" -E nodiscard -O ^metadata_csum_seed,^orphan_file "$dev" 2>&1 \
      || mkfs.ext4 -F -q -L "$label" -E nodiscard -O ^metadata_csum_seed "$dev" 2>&1 \
      || mkfs.ext4 -F -q -L "$label" -E nodiscard "$dev" 2>&1 || exit 6
  else
    mkfs.ext4 -F -q -L "$label" -E nodiscard "$dev" 2>&1 || exit 6
  fi
  echo "FORMATTED=1"
elif [ "$fs" != ext4 ]; then
  echo "O disco tem $fs, não ext4; o painel não monta."
  exit 6
elif command -v e2fsck >/dev/null 2>&1; then
  # a disk cut off in the middle (PC restarted, cable pulled) is checked before it is used again
  e2fsck -p "$dev" 2>&1
  if [ $? -ge 4 ]; then
    echo "O e2fsck achou erros que não corrige sozinho. Nada foi montado; rode e2fsck -f $dev no dispositivo."
    exit 6
  fi
fi

mkdir -p "$mount_path" || exit 8
# while the disk is not mounted the folder refuses writes: nothing lands on the device by mistake
chattr +i "$mount_path" 2>/dev/null
mount -t ext4 -o noatime "$dev" "$mount_path" 2>&1 || { echo "Não foi possível montar $dev em $mount_path."; exit 8; }
echo "== Disco montado em $mount_path ($(df -h "$mount_path" | awk 'NR == 2 { print $2 }')) =="
echo "DEVICE=$dev"
