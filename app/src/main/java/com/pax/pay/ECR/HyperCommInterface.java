package com.pax.pay.ECR;

public interface HyperCommInterface {

    int onTransportHeaderRcv(byte[] type, int destAddr, int srcAddr);
    int onPresentationHeaderRcv(byte formatVer, byte reqResIndicator, byte[] transactionCode, byte[] responseCode, byte moreIndicator);
    int onFieldDataRcv(byte[] fieldType, byte[] data);
    int onRcvCmpt();
}