package com.pax.pay.trans.model;

public class TransDccKbankTotal {

    private String currencyCode;
    private String currencyNumericCode;
    private long saleTotalNum;
    private long saleTotalAmt;
    private long saleDccTotalAmt;
    private long saleVoidTotalNum;
    private long saleVoidTotalAmt;
    private long saleDccVoidTotalAmt;
    private long refundTotalNum;
    private long refundTotalAmt;
    private long refundVoidTotalNum;
    private long refundVoidTotalAmt;
    private long saleOfflineTotalNum;
    private long saleOfflineTotalAmt;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyNumericCode() {
        return currencyNumericCode;
    }

    public void setCurrencyNumericCode(String currencyNumericCode) {
        this.currencyNumericCode = currencyNumericCode;
    }

    public long getSaleTotalNum() {
        return saleTotalNum;
    }

    public void setSaleTotalNum(long saleTotalNum) {
        this.saleTotalNum = saleTotalNum;
    }

    public long getSaleTotalAmt() {
        return saleTotalAmt;
    }

    public void setSaleTotalAmt(long saleTotalAmt) {
        this.saleTotalAmt = saleTotalAmt;
    }

    public long getSaleDccTotalAmt() {
        return saleDccTotalAmt;
    }

    public void setSaleDccTotalAmt(long saleDccTotalAmt) {
        this.saleDccTotalAmt = saleDccTotalAmt;
    }

    public long getSaleVoidTotalNum() {
        return saleVoidTotalNum;
    }

    public void setSaleVoidTotalNum(long saleVoidTotalNum) {
        this.saleVoidTotalNum = saleVoidTotalNum;
    }

    public long getSaleVoidTotalAmt() {
        return saleVoidTotalAmt;
    }

    public void setSaleVoidTotalAmt(long saleVoidTotalAmt) {
        this.saleVoidTotalAmt = saleVoidTotalAmt;
    }

    public long getSaleDccVoidTotalAmt() {
        return saleDccVoidTotalAmt;
    }

    public void setSaleDccVoidTotalAmt(long saleDccVoidTotalAmt) {
        this.saleDccVoidTotalAmt = saleDccVoidTotalAmt;
    }

    public long getRefundTotalNum() {
        return refundTotalNum;
    }

    public void setRefundTotalNum(long refundTotalNum) {
        this.refundTotalNum = refundTotalNum;
    }

    public long getRefundTotalAmt() {
        return refundTotalAmt;
    }

    public void setRefundTotalAmt(long refundTotalAmt) {
        this.refundTotalAmt = refundTotalAmt;
    }

    public long getRefundVoidTotalNum() {
        return refundVoidTotalNum;
    }

    public void setRefundVoidTotalNum(long refundVoidTotalNum) {
        this.refundVoidTotalNum = refundVoidTotalNum;
    }

    public long getRefundVoidTotalAmt() {
        return refundVoidTotalAmt;
    }

    public void setRefundVoidTotalAmt(long refundVoidTotalAmt) {
        this.refundVoidTotalAmt = refundVoidTotalAmt;
    }

    public long getSaleOfflineTotalNum() {
        return saleOfflineTotalNum;
    }

    public void setSaleOfflineTotalNum(long saleOfflineTotalNum) {
        this.saleOfflineTotalNum = saleOfflineTotalNum;
    }

    public long getSaleOfflineTotalAmt() {
        return saleOfflineTotalAmt;
    }

    public void setSaleOfflineTotalAmt(long saleOfflineTotalAmt) {
        this.saleOfflineTotalAmt = saleOfflineTotalAmt;
    }
}
