package com.bancada.records;

/** Audit value of an action on a customer server (power, reinstall, backup, restore). */
public record ServerActionSnapshot(String action, String detail) {
}
