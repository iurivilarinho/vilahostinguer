package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gerenciador de pacotes do sistema do dispositivo")
public enum PackageManager {

    @Schema(description = "apk — Alpine Linux e postmarketOS")
    APK("apk",
        "apk update && apk add --no-progress %s",
        "apk del --no-progress %s",
        "apk update && apk upgrade --no-progress",
        "apk info -e %s >/dev/null 2>&1"),

    @Schema(description = "apt — Debian, Ubuntu, Raspberry Pi OS, Armbian")
    APT("apt",
        "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y %s",
        "DEBIAN_FRONTEND=noninteractive apt-get remove -y %s",
        "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y",
        "dpkg-query -W -f='${Status}' %s 2>/dev/null | grep -q 'install ok installed'"),

    @Schema(description = "dnf — Fedora, Rocky, AlmaLinux")
    DNF("dnf",
        "dnf install -y %s",
        "dnf remove -y %s",
        "dnf upgrade -y",
        "rpm -q %s >/dev/null 2>&1"),

    @Schema(description = "pacman — Arch Linux e derivados")
    PACMAN("pacman",
        "pacman -Sy --noconfirm --needed %s",
        "pacman -R --noconfirm %s",
        "pacman -Syu --noconfirm",
        "pacman -Q %s >/dev/null 2>&1");

    private final String binary;
    private final String installTemplate;
    private final String removeTemplate;
    private final String upgradeCommand;
    private final String queryTemplate;

    PackageManager(String binary, String installTemplate, String removeTemplate, String upgradeCommand,
                   String queryTemplate) {
        this.binary = binary;
        this.installTemplate = installTemplate;
        this.removeTemplate = removeTemplate;
        this.upgradeCommand = upgradeCommand;
        this.queryTemplate = queryTemplate;
    }

    public String getBinary() {
        return binary;
    }

    /** Shell condition that succeeds when the package is installed. */
    public String installedCondition(String packageName) {
        return String.format(queryTemplate, packageName);
    }

    public String installCommand(String packages) {
        return String.format(installTemplate, packages);
    }

    public String removeCommand(String packages) {
        return String.format(removeTemplate, packages);
    }

    public String getUpgradeCommand() {
        return upgradeCommand;
    }

    /** Resolves the value reported by the facts script ({@code apk}, {@code apt}...). */
    public static PackageManager fromBinary(String binary) {
        for (PackageManager manager : values()) {
            if (manager.binary.equals(binary)) {
                return manager;
            }
        }
        return null;
    }
}
