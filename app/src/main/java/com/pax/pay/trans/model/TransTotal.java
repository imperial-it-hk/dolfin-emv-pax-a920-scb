/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.pay.base.Acquirer;

import java.io.Serializable;

/**
 * 交易总计
 *
 * @author Steven.W
 */

@DatabaseTable(tableName = "trans_total")
public class TransTotal implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "id";
    public static final String IS_CLOSED_FIELD_NAME = "closed";
    public static final String MID_FIELD_NAME = "mid";
    public static final String TID_FIELD_NAME = "tid";
    public static final String BATCHNO_FIELD_NAME = "batch_no";
    public static final String TIME_FIELD_NAME = "batch_time";
    public static final String MERCHANT_NAME_FILED_NAME = "merchant_name";

    public static final String SALE_AMOUNT = "SALE_AMOUNT";
    public static final String SALE_NUM = "SALE_NUM";
    public static final String VOID_AMOUNT = "VOID_AMOUNT";
    public static final String VOID_NUM = "VOID_NUM";
    public static final String REFUND_AMOUNT = "REFUND_AMOUNT";
    public static final String REFUND_NUM = "REFUND_NUM";
    public static final String REFUND_VOID_AMOUNT = "REFUND_VOID_AMOUNT";
    public static final String REFUND_VOID_NUM = "REFUND_VOID_NUM";
    public static final String SALE_VOID_AMOUNT = "SALE_VOID_AMOUNT";
    public static final String SALE_VOID_NUM = "SALE_VOID_NUM";
    public static final String AUTH_AMOUNT = "AUTH_AMOUNT";
    public static final String AUTH_NUM = "AUTH_NUM";
    public static final String OFFLINE_AMOUNT = "OFFLINE_AMOUNT";
    public static final String OFFLINE_NUM = "OFFLINE_NUM";
    public static final String TOPUP_AMOUNT = "TOPUP_AMOUNT";
    public static final String TOPUP_NUM = "TOPUP_NUM";
    public static final String TOPUP_VOID_AMOUNT = "TOPUP_VOID_AMOUNT";
    public static final String TOPUP_VOID_NUM = "TOPUP_VOID_NUM";


    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    protected int id;

    /**
     * 商户号
     */
    @DatabaseField(columnName = MID_FIELD_NAME)
    private String merchantID;
    /**
     * 终端号
     */
    @DatabaseField(columnName = TID_FIELD_NAME)
    private String terminalID;
    /**
     * 批次号
     */
    @DatabaseField(columnName = BATCHNO_FIELD_NAME)
    private int batchNo;

    /**
     * 日期时间
     */
    @DatabaseField(columnName = TIME_FIELD_NAME)
    private String dateTime;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Acquirer.ID_FIELD_NAME)
    private Acquirer acquirer;

    @DatabaseField(columnName = IS_CLOSED_FIELD_NAME)
    private boolean isClosed;

    /**
     * 消费总金额
     */
    @DatabaseField(columnName = SALE_AMOUNT)
    private long saleTotalAmt;
    /**
     * 消费总笔数
     */
    @DatabaseField(columnName = SALE_NUM)
    private long saleTotalNum;

    /**
     * 撤销总金额
     */
    @DatabaseField(columnName = VOID_AMOUNT)
    private long voidTotalAmt;
    /**
     * 撤销总笔数
     */
    @DatabaseField(columnName = VOID_NUM)
    private long voidTotalNum;
    /**
     * 退货总金额
     */
    @DatabaseField(columnName = REFUND_AMOUNT)
    private long refundTotalAmt;
    /**
     * 退货总笔数
     */
    @DatabaseField(columnName = REFUND_NUM)
    private long refundTotalNum;
    /**
     * refund void total amount
     */
    @DatabaseField(columnName = REFUND_VOID_AMOUNT)
    private long refundVoidTotalAmt;
    /**
     * refund void total num
     */
    @DatabaseField(columnName = REFUND_VOID_NUM)
    private long refundVoidTotalNum;
    /**
     * sale void total amount
     */
    @DatabaseField(columnName = SALE_VOID_AMOUNT)
    private long saleVoidTotalAmt;
    /**
     * sale void total num
     */
    @DatabaseField(columnName = SALE_VOID_NUM)
    private long saleVoidTotalNum;
    /**
     * 预授权总金额
     */
    @DatabaseField(columnName = AUTH_AMOUNT)
    private long authTotalAmt;
    /**
     * 预授权总笔数
     */
    @DatabaseField(columnName = AUTH_NUM)
    private long authTotalNum;
    //AET-75
    /**
     * 脱机交易总金额
     */
    @DatabaseField(columnName = OFFLINE_AMOUNT)
    private long offlineTotalAmt;
    /**
     * 脱机交易总笔数
     */
    @DatabaseField(columnName = OFFLINE_NUM)
    private long offlineTotalNum;

    @DatabaseField
    private String walletSettleSlipInfo;

    @DatabaseField(columnName = TOPUP_AMOUNT)
    private long topupTotalAmt;

    @DatabaseField(columnName = TOPUP_NUM)
    private long topupTotalNum;

    @DatabaseField(columnName = TOPUP_VOID_AMOUNT)
    private long topupVoidTotalAmt;

    @DatabaseField(columnName = TOPUP_VOID_NUM)
    private long topupVoidTotalNum;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] totalByIssuer;

    @DatabaseField(canBeNull = false, columnName = MERCHANT_NAME_FILED_NAME)
    protected String merchantName = "";


    @DatabaseField(foreign = true, foreignAutoRefresh = true,foreignAutoCreate = true, columnName = TransRedeemKbankTotal.ID_FIELD_NAME)
    private TransRedeemKbankTotal transRedeemKbankTotal;


    protected long saleTotalSmallAmt;
    protected long saleTotalSmallAmtNum;
    protected long voidTotalSmallAmt;
    protected long voidTotalSmallAmtNum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getSaleTotalAmt() {
        return saleTotalAmt;
    }

    public void setSaleTotalAmt(long saleTotalAmt) {
        this.saleTotalAmt = saleTotalAmt;
    }

    public long getSaleTotalNum() {
        return saleTotalNum;
    }

    public void setSaleTotalNum(long saleTotalNum) {
        this.saleTotalNum = saleTotalNum;
    }


    public long getVoidTotalAmt() {
        return voidTotalAmt;
    }

    public void setVoidTotalAmt(long voidTotalAmt) {
        this.voidTotalAmt = voidTotalAmt;
    }

    public long getVoidTotalNum() {
        return voidTotalNum;
    }

    public void setVoidTotalNum(long voidTotalNum) {
        this.voidTotalNum = voidTotalNum;
    }

    public long getRefundVoidTotalAmt() {
        return refundVoidTotalAmt;
    }

    public void setRefundVoidTotalAmt(long refundVoidTotalAmt) {
        this.refundVoidTotalAmt = refundVoidTotalAmt;
    }

    public long getRefundVoidTotalNum() {
        return refundVoidTotalNum;
    }

    public void setRefundVoidTotalNum(long refundVoidTotalNum) {
        this.refundVoidTotalNum = refundVoidTotalNum;
    }

    public long getSaleVoidTotalAmt() {
        return saleVoidTotalAmt;
    }

    public void setSaleVoidTotalAmt(long saleVoidTotalAmt) {
        this.saleVoidTotalAmt = saleVoidTotalAmt;
    }

    public long getSaleVoidTotalNum() {
        return saleVoidTotalNum;
    }

    public void setSaleVoidTotalNum(long saleVoidTotalNum) {
        this.saleVoidTotalNum = saleVoidTotalNum;
    }

    public long getRefundTotalAmt() {
        return refundTotalAmt;
    }

    public void setRefundTotalAmt(long refundTotalAmt) {
        this.refundTotalAmt = refundTotalAmt;
    }

    public long getRefundTotalNum() {
        return refundTotalNum;
    }

    public void setRefundTotalNum(long refundTotalNum) {
        this.refundTotalNum = refundTotalNum;
    }

    public long getAuthTotalAmt() {
        return authTotalAmt;
    }

    public void setAuthTotalAmt(long authTotalAmt) {
        this.authTotalAmt = authTotalAmt;
    }

    public long getAuthTotalNum() {
        return authTotalNum;
    }

    public void setAuthTotalNum(long authTotalNum) {
        this.authTotalNum = authTotalNum;
    }

    public String getMerchantID() {
        return merchantID;
    }

    public void setMerchantID(String merchantID) {
        this.merchantID = merchantID;
    }

    public String getTerminalID() {
        return terminalID;
    }

    public void setTerminalID(String terminalID) {
        this.terminalID = terminalID;
    }

    public int getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(int batchNo) {
        this.batchNo = batchNo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public long getOfflineTotalAmt() {
        return offlineTotalAmt;
    }

    public void setOfflineTotalAmt(long offlineTotalAmt) {
        this.offlineTotalAmt = offlineTotalAmt;
    }

    public long getOfflineTotalNum() {
        return offlineTotalNum;
    }

    public void setOfflineTotalNum(long offlineTotalNum) {
        this.offlineTotalNum = offlineTotalNum;
    }

    public Acquirer getAcquirer() {
        return acquirer;
    }

    public void setAcquirer(Acquirer acquirer) {
        this.acquirer = acquirer;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    public boolean isZero() {
        int voidRedeemCount = 0 ;
        if (getTransRedeemKbankTotal() != null) { voidRedeemCount = (int)getTransRedeemKbankTotal().getRedeemTotalVoidTransCount(); }
        return (getSaleTotalNum() + getRefundTotalNum() + getSaleVoidTotalNum() + getRefundVoidTotalNum() +
                getOfflineTotalNum() + getTopupTotalNum() + getTopupVoidTotalNum() + voidRedeemCount)  == 0 && (getTransRedeemKbankTotal() == null || getTransRedeemKbankTotal().getTotalSaleAllCards() == 0 );
    }

    public String getWalletSettleSlipInfo() {
        return walletSettleSlipInfo;
    }

    public void setWalletSettleSlipInfo(String walletSettleSlipInfo) {
        this.walletSettleSlipInfo = walletSettleSlipInfo;
    }

    public long getTopupTotalAmt() {
        return topupTotalAmt;
    }

    public void setTopupTotalAmt(long topupTotalAmt) {
        this.topupTotalAmt = topupTotalAmt;
    }

    public long getTopupTotalNum() {
        return topupTotalNum;
    }

    public void setTopupTotalNum(long topupTotalNum) {
        this.topupTotalNum = topupTotalNum;
    }

    public long getTopupVoidTotalAmt() {
        return topupVoidTotalAmt;
    }

    public void setTopupVoidTotalAmt(long topupVoidTotalAmt) {
        this.topupVoidTotalAmt = topupVoidTotalAmt;
    }

    public long getTopupVoidTotalNum() {
        return topupVoidTotalNum;
    }

    public void setTopupVoidTotalNum(long topupVoidTotalNum) {
        this.topupVoidTotalNum = topupVoidTotalNum;
    }

    public byte[] getTotalByIssuer() {
        return totalByIssuer;
    }

    public void setTotalByIssuer(byte[] totalByIssuer) {
        this.totalByIssuer = totalByIssuer;
    }

    public TransRedeemKbankTotal getTransRedeemKbankTotal() {return transRedeemKbankTotal;}

    public void setTransRedeemKbankTotal(TransRedeemKbankTotal transRedeemKbankTotal) {this.transRedeemKbankTotal = transRedeemKbankTotal;}

    public long getSaleTotalSmallAmt() { return saleTotalSmallAmt; }

    public void setSaleTotalSmallAmt(long saleTotalSmallAmt) { this.saleTotalSmallAmt = saleTotalSmallAmt; }

    public long getSaleTotalSmallAmtNum() { return saleTotalSmallAmtNum; }

    public void setSaleTotalSmallAmtNum(long saleTotalSmallAmtNum) { this.saleTotalSmallAmtNum = saleTotalSmallAmtNum; }

    public long getVoidTotalSmallAmt() { return voidTotalSmallAmt; }

    public void setVoidTotalSmallAmt(long voidTotalSmallAmt) { this.voidTotalSmallAmt = voidTotalSmallAmt; }

    public long getVoidTotalSmallAmtNum() { return voidTotalSmallAmtNum; }

    public void setVoidTotalSmallAmtNum(long voidTotalSmallAmtNum) { this.voidTotalSmallAmtNum = voidTotalSmallAmtNum; }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
}
