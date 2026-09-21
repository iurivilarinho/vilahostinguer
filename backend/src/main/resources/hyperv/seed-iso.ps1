# Builds the cloud-init NoCloud seed: an ISO9660 + Joliet image labelled "cidata" with the files
# of a folder. Uses IMAPI2 (part of Windows), no administrator needed. Parameters set before this
# script: $SeedFolder, $IsoPath.
$ErrorActionPreference = 'Stop'
if (-not ('Bancada.IsoWriter' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Runtime.InteropServices.ComTypes;
namespace Bancada {
    public static class IsoWriter {
        public static void Save(object stream, string path) {
            IStream source = (IStream) stream;
            byte[] buffer = new byte[65536];
            IntPtr read = System.Runtime.InteropServices.Marshal.AllocHGlobal(4);
            try {
                using (FileStream target = File.Create(path)) {
                    while (true) {
                        source.Read(buffer, buffer.Length, read);
                        int count = System.Runtime.InteropServices.Marshal.ReadInt32(read);
                        if (count <= 0) break;
                        target.Write(buffer, 0, count);
                    }
                }
            } finally {
                System.Runtime.InteropServices.Marshal.FreeHGlobal(read);
            }
        }
    }
}
'@
}
$image = New-Object -ComObject IMAPI2FS.MsftFileSystemImage
$image.FileSystemsToCreate = 3   # ISO9660 + Joliet
$image.VolumeName = 'cidata'
$image.Root.AddTree($SeedFolder, $false)
$result = $image.CreateResultImage()
[Bancada.IsoWriter]::Save($result.ImageStream, $IsoPath)
Write-Output "OK"
