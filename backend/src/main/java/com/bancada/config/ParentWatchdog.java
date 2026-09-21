package com.bancada.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Shuts the backend down when the Tauri shell disappears. Closing the window through the tray
 * already kills the backend; this covers the forced exits (task manager, crash) where no hook runs.
 * Without {@code --parent-pid} (backend started by hand in development) the watchdog stays off.
 */
@Component
public class ParentWatchdog {

    private static final Logger LOG = LoggerFactory.getLogger(ParentWatchdog.class);

    private final ApplicationContext context;
    private final long parentPid;

    public ParentWatchdog(ApplicationContext context, @Value("${parent-pid:0}") long parentPid) {
        this.context = context;
        this.parentPid = parentPid;
        if (parentPid > 0) {
            LOG.info("Parent watchdog enabled: exiting together with process {}.", parentPid);
        }
    }

    @Scheduled(initialDelay = 20_000, fixedDelay = 10_000)
    public void check() {
        if (parentPid <= 0) {
            return;
        }
        boolean alive = ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false);
        if (alive) {
            return;
        }
        LOG.warn("Shell process {} is gone; shutting the backend down.", parentPid);
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
