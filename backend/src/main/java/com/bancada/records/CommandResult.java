package com.bancada.records;

/** Exit code and text output of a command run on a device. */
public record CommandResult(int exitCode, String output, String errorOutput) {

    public boolean succeeded() {
        return exitCode == 0;
    }
}
