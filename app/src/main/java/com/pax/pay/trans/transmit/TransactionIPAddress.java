package com.pax.pay.trans.transmit;

public class TransactionIPAddress {
    private int index;
    private String ipAddress;
    private int port;

    public TransactionIPAddress(int index, String ipAddress, int port) {
        this.index = index;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
