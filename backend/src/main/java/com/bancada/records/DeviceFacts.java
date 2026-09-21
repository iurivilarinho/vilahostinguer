package com.bancada.records;

/** System information read from a device by the facts script. Any field may be null when unknown. */
public record DeviceFacts(
    String hostname,
    String osName,
    String osVersion,
    String kernelVersion,
    String architecture,
    String cpuModel,
    Integer cpuCores,
    Long memoryTotalBytes,
    Long diskTotalBytes,
    String model,
    String macAddress,
    String packageManager,
    String initSystem,
    String homeDirectory,
    boolean root
) {
}
