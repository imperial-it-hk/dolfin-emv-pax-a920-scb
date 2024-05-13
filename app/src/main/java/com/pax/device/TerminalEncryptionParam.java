package com.pax.device;

import com.pax.pay.constant.Bank;

public class TerminalEncryptionParam {
    private Bank bank;
    private String id;
    private String pin;

    public TerminalEncryptionParam(Bank bank, String id, String pin) {
        this.setBank(bank);
        this.setId(id);
        this.setPin(pin);
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
