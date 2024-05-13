package com.pax.pay.uart;

public interface CommManageInterface {
    int onReceive(byte [] data, int len);
}
