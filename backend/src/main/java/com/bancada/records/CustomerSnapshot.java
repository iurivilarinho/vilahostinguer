package com.bancada.records;

import com.bancada.enums.CustomerStatus;
import com.bancada.models.Customer;

/** What the audit log keeps of a customer (never the password). */
public record CustomerSnapshot(String name, String email, String phone, String document, CustomerStatus status) {

    public CustomerSnapshot(Customer customer) {
        this(customer.getName(), customer.getEmail(), customer.getPhone(), customer.getDocument(), customer.getStatus());
    }
}
