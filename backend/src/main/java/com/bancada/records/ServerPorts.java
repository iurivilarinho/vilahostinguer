package com.bancada.records;

/** Ports reserved on the device for a customer server: SSH, and the device ports mapped to 80 and 443 of the server. */
public record ServerPorts(int ssh, int http, int https) {
}
