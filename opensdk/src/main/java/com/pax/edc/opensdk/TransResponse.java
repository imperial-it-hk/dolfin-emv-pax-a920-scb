/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-18
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base transaction response
 */
public abstract class TransResponse extends BaseResponse {
    /**
     * card type
     * @see TransResponse#NO_CARD
     * @see TransResponse#MAG
     * @see TransResponse#ICC
     * @see TransResponse#PICC
     * @see TransResponse#MANUAL
     * @see TransResponse#FALLBACK
     * @see TransResponse#QR
     */
    @IntDef({NO_CARD, MAG, ICC, PICC, MANUAL, FALLBACK, QR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CardType {
    }

    public static final int NO_CARD = 0; //default
    public static final int MAG = 1;
    public static final int ICC = 2;
    public static final int PICC = 3;
    public static final int MANUAL = 4;
    public static final int FALLBACK = 5;
    public static final int QR = 6;

    private String merchantName;
    private String merchantId;
    private String terminalId;
    private String cardNo;
    private int cardType = NO_CARD;
    @IntRange(from=1, to=999999) private long voucherNo;
    @IntRange(from=1, to=999999) private long batchNo;
    private String issuerName;
    private String acquirerName;
    private String refNo;
    private String transTime; //YYYYMMDDHHmmss
    private String amount;
    private String authCode;

    private byte[] signatureJbig;

    private byte[] signaturePath;

    TransResponse(){
        //do nothing
    }

    abstract int getType();

    @NonNull
    Bundle toBundle(@NonNull Bundle bundle) {
        super.toBundle(bundle);
        bundle.putString(Constants.Resp.RSP_MERCHANT_NAME, this.merchantName);
        bundle.putString(Constants.Resp.RSP_MERCHANT_ID, this.merchantId);
        bundle.putString(Constants.Resp.RSP_TERMINAL_ID, this.terminalId);
        bundle.putString(Constants.Resp.RSP_CARD_NO, this.cardNo);
        bundle.putInt(Constants.Resp.RSP_CARD_TYPE, this.cardType);
        bundle.putLong(Constants.Resp.RSP_VOUCHER_NO, this.voucherNo);
        bundle.putLong(Constants.Resp.RSP_BATCH_NO, this.batchNo);
        bundle.putString(Constants.Resp.RSP_ISSUER_NAME, this.issuerName);
        bundle.putString(Constants.Resp.RSP_ACQUIRER_NAME, this.acquirerName);
        bundle.putString(Constants.Resp.RSP_REF_NO, this.refNo);
        bundle.putString(Constants.Resp.RSP_TRANS_TIME, this.transTime);
        bundle.putString(Constants.Resp.RSP_AMOUNT, this.amount);
        bundle.putString(Constants.Resp.RSP_AUTH_CODE, this.authCode);
        bundle.putByteArray(Constants.Resp.RSP_CH_SIGNATURE, this.signatureJbig);
        bundle.putByteArray(Constants.Resp.RSP_CH_SIGNATURE_PATH, this.signaturePath);
        return bundle;
    }

    void fromBundle(Bundle bundle) {
        super.fromBundle(bundle);
        this.merchantName = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_MERCHANT_NAME);
        this.merchantId = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_MERCHANT_ID);
        this.terminalId = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_TERMINAL_ID);
        this.cardNo = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_CARD_NO);
        this.cardType = IntentUtil.getIntExtra(bundle, Constants.Resp.RSP_CARD_TYPE);
        this.voucherNo = IntentUtil.getLongExtra(bundle, Constants.Resp.RSP_VOUCHER_NO);
        this.batchNo = IntentUtil.getLongExtra(bundle, Constants.Resp.RSP_BATCH_NO);
        this.issuerName = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_ISSUER_NAME);
        this.acquirerName = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_ACQUIRER_NAME);
        this.refNo = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_REF_NO);
        this.transTime = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_TRANS_TIME);
        this.amount = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_AMOUNT);
        this.authCode = IntentUtil.getStringExtra(bundle, Constants.Resp.RSP_AUTH_CODE);
        this.signatureJbig = IntentUtil.getByteArrayExtra(bundle, Constants.Resp.RSP_CH_SIGNATURE);
        this.signaturePath = IntentUtil.getByteArrayExtra(bundle, Constants.Resp.RSP_CH_SIGNATURE_PATH);
    }

    abstract boolean checkArgs();

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    /**
     * @return {@link TransResponse.CardType}
     */
    public int getCardType() {
        return cardType;
    }

    /**
     * @param cardType {@link TransResponse.CardType}
     */
    public void setCardType(@TransResponse.CardType int cardType) {
        this.cardType = cardType;
    }

    public long getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(long voucherNo) {
        this.voucherNo = voucherNo;
    }

    public long getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(long batchNo) {
        this.batchNo = batchNo;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getAcquirerName() {
        return acquirerName;
    }

    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getTransTime() {
        return transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    /**
     * @return format is Jbig
     */
    public byte[] getCardholderSignature() {
        return signatureJbig;
    }

    /**
     * @param cardholderSignature format is Jbig
     */
    public void setCardholderSignature(byte[] cardholderSignature) {
        this.signatureJbig = cardholderSignature;
    }

    /**
     *
     * @return format is continuous 2bytes x coordinate and 2bytes y coordinate(LITTLE ENDIAN !!!), ended with 0xFFFFFFFF for each path.
     *                      for example, 48 00 49 00 72 00 72 00 ..... FF FF FF FF 57 01 20 02 ... FF FF FF FF
     */
    public byte[] getSignaturePath() {
        return signaturePath;
    }

    /**
     *
     * @param signaturePath format is continuous 2bytes x coordinate and 2bytes y coordinate(LITTLE ENDIAN !!!), ended with 0xFFFFFFFF for each path.
     *                      for example, 48 00 49 00 72 00 72 00 ..... FF FF FF FF 57 01 20 02 ... FF FF FF FF
     */
    public void setSignaturePath(byte[] signaturePath) {
        this.signaturePath = signaturePath;
    }

    @Override
    public String toString() {
        return super.toString() + " " + merchantName + " " + merchantId + " " + terminalId + " " +
                cardNo + " " + voucherNo + " " + batchNo  + " " + issuerName  + " " + acquirerName
                + " " + refNo + " " + transTime + " " + amount + " " + authCode;
    }
}
