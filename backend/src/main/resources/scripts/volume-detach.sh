# Unmounts a disk of the PC and disconnects it. Variables set before this script:
#   volume_id mount_path

state_dir=/run/bancada/discos

if awk -v m="$mount_path" '$2 == m { found = 1 } END { exit !found }' /proc/mounts; then
  echo "== Desmontando $mount_path =="
  sync
  if ! umount "$mount_path" 2>&1; then
    echo "O disco está em uso. Programas com arquivos abertos nele:"
    fuser -vm "$mount_path" 2>&1 | head -20
    exit 9
  fi
fi

if [ -f "$state_dir/$volume_id" ]; then
  dev=$(cat "$state_dir/$volume_id")
  if [ -e "/sys/block/${dev#/dev/}/pid" ]; then
    echo "== Desconectando $dev =="
    nbd-client -d "$dev" 2>&1
  fi
  rm -f "$state_dir/$volume_id"
fi
# the folder was locked (chattr +i) while it belonged to the disk
chattr -i "$mount_path" 2>/dev/null
rmdir "$mount_path" 2>/dev/null
echo "== Disco desconectado =="
