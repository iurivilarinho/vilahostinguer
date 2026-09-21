package com.bancada.enums;

import com.bancada.hyperv.CloudImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Linux distributions offered for virtual machines. Each version points at the official cloud image
 * (qcow2 with cloud-init) and at the checksum file published next to it; the image is downloaded
 * once, checked and kept on this PC.
 */
@Schema(description = "Distribuição Linux para máquinas virtuais")
public enum MachineDistribution {

    @Schema(description = "Ubuntu Server")
    UBUNTU("Ubuntu", "sudo", Map.of(
        "24.04", CloudImage.sha256("https://cloud-images.ubuntu.com/releases/noble/release/ubuntu-24.04-server-cloudimg-amd64.img",
            "https://cloud-images.ubuntu.com/releases/noble/release/SHA256SUMS"),
        "22.04", CloudImage.sha256("https://cloud-images.ubuntu.com/releases/jammy/release/ubuntu-22.04-server-cloudimg-amd64.img",
            "https://cloud-images.ubuntu.com/releases/jammy/release/SHA256SUMS"))),

    @Schema(description = "Debian")
    DEBIAN("Debian", "sudo", Map.of(
        "13", CloudImage.sha512("https://cloud.debian.org/images/cloud/trixie/latest/debian-13-genericcloud-amd64.qcow2",
            "https://cloud.debian.org/images/cloud/trixie/latest/SHA512SUMS"),
        "12", CloudImage.sha512("https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-amd64.qcow2",
            "https://cloud.debian.org/images/cloud/bookworm/latest/SHA512SUMS"))),

    @Schema(description = "Rocky Linux")
    ROCKY("Rocky Linux", "wheel", Map.of(
        "9", CloudImage.sha256("https://dl.rockylinux.org/pub/rocky/9/images/x86_64/Rocky-9-GenericCloud-Base.latest.x86_64.qcow2",
            "https://dl.rockylinux.org/pub/rocky/9/images/x86_64/Rocky-9-GenericCloud-Base.latest.x86_64.qcow2.CHECKSUM"))),

    @Schema(description = "AlmaLinux")
    ALMA("AlmaLinux", "wheel", Map.of(
        "9", CloudImage.sha256("https://repo.almalinux.org/almalinux/9/cloud/x86_64/images/AlmaLinux-9-GenericCloud-latest.x86_64.qcow2",
            "https://repo.almalinux.org/almalinux/9/cloud/x86_64/images/CHECKSUM")));

    private final String displayName;
    private final String adminGroup;
    private final Map<String, CloudImage> images;

    MachineDistribution(String displayName, String adminGroup, Map<String, CloudImage> images) {
        this.displayName = displayName;
        this.adminGroup = adminGroup;
        // newest version first
        this.images = new LinkedHashMap<>();
        images.keySet().stream().sorted((first, second) -> compareVersions(second, first)).forEach(key -> this.images.put(key, images.get(key)));
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Group that grants sudo: sudo on Debian and Ubuntu, wheel on the Red Hat family. */
    public String getAdminGroup() {
        return adminGroup;
    }

    public List<String> getVersions() {
        return List.copyOf(images.keySet());
    }

    public CloudImage imageFor(String version) {
        CloudImage image = images.get(version);
        if (image == null) {
            throw new IllegalArgumentException("Versão não oferecida para " + displayName + ": " + version);
        }
        return image;
    }

    private static int compareVersions(String first, String second) {
        String[] a = first.split("\\.");
        String[] b = second.split("\\.");
        for (int index = 0; index < Math.max(a.length, b.length); index++) {
            int left = index < a.length ? Integer.parseInt(a[index]) : 0;
            int right = index < b.length ? Integer.parseInt(b[index]) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return 0;
    }
}
