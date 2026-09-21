package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Linux distributions offered when creating a machine. Each entry carries the official Docker image,
 * the versions offered and the architectures the image is published for.
 */
@Schema(description = "Distribuição Linux para máquinas")
public enum MachineDistribution {

    @Schema(description = "Ubuntu")
    UBUNTU("Ubuntu", "ubuntu", List.of("24.04", "22.04", "20.04"), "apt", List.of("x86_64", "aarch64", "armv7l")),

    @Schema(description = "Debian")
    DEBIAN("Debian", "debian", List.of("12", "11"), "apt", List.of("x86_64", "aarch64", "armv7l")),

    @Schema(description = "Alpine Linux")
    ALPINE("Alpine", "alpine", List.of("3.22", "3.21", "3.20", "edge"), "apk", List.of("x86_64", "aarch64", "armv7l")),

    @Schema(description = "Fedora")
    FEDORA("Fedora", "fedora", List.of("42", "41"), "dnf", List.of("x86_64", "aarch64")),

    @Schema(description = "Rocky Linux")
    ROCKY("Rocky Linux", "rockylinux", List.of("9", "8"), "dnf", List.of("x86_64", "aarch64")),

    @Schema(description = "Arch Linux (somente x86_64)")
    ARCH("Arch Linux", "archlinux", List.of("latest"), "pacman", List.of("x86_64"));

    private final String displayName;
    private final String image;
    private final List<String> versions;
    private final String packageManager;
    private final List<String> architectures;

    MachineDistribution(String displayName, String image, List<String> versions, String packageManager, List<String> architectures) {
        this.displayName = displayName;
        this.image = image;
        this.versions = versions;
        this.packageManager = packageManager;
        this.architectures = architectures;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getVersions() {
        return versions;
    }

    public List<String> getArchitectures() {
        return architectures;
    }

    public String imageFor(String version) {
        return image + ":" + version;
    }

    public boolean supports(String architecture) {
        return architecture == null || architectures.contains(architecture);
    }

    /** Package installation run inside a new container: base tools, sudo and (optionally) the SSH server. */
    public String packageInstallCommand(boolean installSsh) {
        String packages;
        switch (packageManager) {
            case "apt" -> packages = "export DEBIAN_FRONTEND=noninteractive; apt-get update && apt-get install -y --no-install-recommends "
                + "sudo bash ca-certificates curl nano procps iproute2" + (installSsh ? " openssh-server" : "");
            case "apk" -> packages = "apk add --no-cache sudo bash shadow curl nano procps" + (installSsh ? " openssh" : "");
            case "dnf" -> packages = "dnf install -y sudo passwd procps-ng curl nano" + (installSsh ? " openssh-server" : "");
            default -> packages = "pacman -Sy --noconfirm sudo curl nano" + (installSsh ? " openssh" : "");
        }
        return packages;
    }
}
