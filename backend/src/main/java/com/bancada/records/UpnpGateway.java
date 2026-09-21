package com.bancada.records;

/** Internet gateway found through SSDP, with the SOAP endpoint that manages its port mappings. */
public record UpnpGateway(String location, String controlUrl, String serviceType, String localAddress, String name) {
}
