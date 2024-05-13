package com.pax.pay.constant;

public enum Bank {
    KBANK("KBANK"),
    GCS("GCS"),
    SCB("SCB"),
    AMEX("AMEX");

    final private String bankName;

    Bank(String bankName) {
        this.bankName = bankName;
    }
}
