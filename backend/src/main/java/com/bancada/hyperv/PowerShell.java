package com.bancada.hyperv;

import com.bancada.records.CommandResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs Windows PowerShell scripts, the way the panel drives Hyper-V. The script goes as
 * {@code -EncodedCommand}, so nothing in it is ever parsed by a shell; values enter through
 * {@link #literal(String)}.
 */
public final class PowerShell {

    private static final String PREAMBLE = "$ErrorActionPreference = 'Stop'\n$ProgressPreference = 'SilentlyContinue'\n"
        + "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n";

    private PowerShell() {
    }

    /** A PowerShell single-quoted string: nothing inside is expanded. */
    public static String literal(String value) {
        return value == null ? "$null" : "'" + value.replace("'", "''") + "'";
    }

    public static CommandResult run(String script, Duration timeout) {
        Process process = start(script);
        CompletableFuture<String> errors = CompletableFuture.supplyAsync(() -> drain(process.getErrorStream()));
        String output = drain(process.getInputStream());
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return new CommandResult(124, output, "O PowerShell não respondeu em " + timeout.toSeconds() + " s.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new CommandResult(130, output, "Interrompido.");
        }
        return new CommandResult(process.exitValue(), output, errors.join());
    }

    /** Runs and hands over each line of output (and of errors) as it comes. */
    public static int stream(String script, Consumer<String> onOutput, Consumer<Process> onProcess) {
        Process process = start(script);
        onProcess.accept(process);
        CompletableFuture<Void> errors = CompletableFuture.runAsync(() -> pump(process.getErrorStream(), onOutput));
        pump(process.getInputStream(), onOutput);
        errors.join();
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return 130;
        }
    }

    private static Process start(String script) {
        String encoded = Base64.getEncoder().encodeToString((PREAMBLE + script).getBytes(StandardCharsets.UTF_16LE));
        try {
            Process process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encoded).start();
            // nothing is typed into it: an open stdin would keep some cmdlets waiting
            process.getOutputStream().close();
            return process;
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível abrir o PowerShell: " + exception.getMessage(), exception);
        }
    }

    private static String drain(InputStream input) {
        try (input) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            input.transferTo(bytes);
            return bytes.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private static void pump(InputStream input, Consumer<String> onOutput) {
        byte[] buffer = new byte[8192];
        try (input) {
            int count;
            while ((count = input.read(buffer)) > 0) {
                onOutput.accept(new String(buffer, 0, count, StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            // the process ended or was killed
        }
    }
}
