package com.bancada.records;

/** What Hyper-V reports about one machine: state (Running, Off, Saved...), CPU %, memory in use, uptime. */
public record VirtualMachineState(String name, String state, int cpuPercent, long memoryMb, long uptimeSeconds) {

    public boolean running() {
        return "Running".equals(state);
    }

    public boolean off() {
        return "Off".equals(state);
    }
}
