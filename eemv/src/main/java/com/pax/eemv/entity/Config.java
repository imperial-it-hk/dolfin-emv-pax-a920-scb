package com.pax.eemv.entity;

public class Config {
    private long referCurrCon;
    private String merchName;
    private String merchCateCode;
    private String merchId;
    private String termId;
    private byte termType;
    private String capability;
    private String exCapability;
    private byte transCurrExp;
    private byte referCurrExp;
    private String referCurrCode;
    private String countryCode;
    private String transCurrCode;
    private byte transType;
    private boolean forceOnline;
    private boolean getDataPIN;
    private boolean supportPSESel;
    private boolean useTermAIPFlag;
    private String termAIP;
    private boolean bypassAllFlag;
    private boolean bypassPin;
    private byte batchCapture;
    private boolean adviceFlag;
    private byte scriptMethod;
    private boolean forceAccept;
    private boolean noPinConfirmAmtFlg;
    private boolean isInputAmount;
    private byte[] acquirerId;

    //For AE
    private String unpredictableNumberRange;
    private boolean supportOptTrans;
    private String transCap;
    private boolean delayAuthFlag;


    public Config() {
        this.referCurrCon = 0L;
        this.termType = 0;
        this.transCurrExp = 0;
        this.referCurrExp = 0;
        this.transType = 0;
        this.forceOnline = false;
        this.getDataPIN = false;
        this.supportPSESel = false;
        this.useTermAIPFlag = false;
        this.bypassAllFlag = false;
        this.bypassPin = false;
        this.batchCapture = 0;
        this.adviceFlag = false;
        this.scriptMethod = 0;
        this.forceAccept = false;
        this.noPinConfirmAmtFlg = false;
        this.isInputAmount = false;
        this.acquirerId = new byte[6];
    }

    public long getReferCurrCon() {
        return this.referCurrCon;
    }

    public void setReferCurrCon(long referCurrCon) {
        this.referCurrCon = referCurrCon;
    }

    public String getMerchName() {
        return this.merchName;
    }

    public void setMerchName(String merchName) {
        this.merchName = merchName;
    }

    public String getMerchCateCode() {
        return this.merchCateCode;
    }

    public void setMerchCateCode(String merchCateCode) {
        this.merchCateCode = merchCateCode;
    }

    public String getMerchId() {
        return this.merchId;
    }

    public void setMerchId(String merchId) {
        this.merchId = merchId;
    }

    public String getTermId() {
        return this.termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }

    public byte getTermType() {
        return this.termType;
    }

    public void setTermType(byte termType) {
        this.termType = termType;
    }

    public String getCapability() {
        return this.capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getExCapability() {
        return this.exCapability;
    }

    public void setExCapability(String exCapability) {
        this.exCapability = exCapability;
    }

    public byte getTransCurrExp() {
        return this.transCurrExp;
    }

    public void setTransCurrExp(byte transCurrExp) {
        this.transCurrExp = transCurrExp;
    }

    public byte getReferCurrExp() {
        return this.referCurrExp;
    }

    public void setReferCurrExp(byte referCurrExp) {
        this.referCurrExp = referCurrExp;
    }

    public String getReferCurrCode() {
        return this.referCurrCode;
    }

    public void setReferCurrCode(String referCurrCode) {
        this.referCurrCode = referCurrCode;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTransCurrCode() {
        return this.transCurrCode;
    }

    public void setTransCurrCode(String transCurrCode) {
        this.transCurrCode = transCurrCode;
    }

    public byte getTransType() {
        return this.transType;
    }

    public void setTransType(byte transType) {
        this.transType = transType;
    }

    public boolean getForceOnline() {
        return this.forceOnline;
    }

    public void setForceOnline(boolean forceOnline) {
        this.forceOnline = forceOnline;
    }

    public boolean getGetDataPIN() {
        return this.getDataPIN;
    }

    public void setGetDataPIN(boolean getDataPIN) {
        this.getDataPIN = getDataPIN;
    }

    public boolean getSurportPSESel() {
        return this.supportPSESel;
    }

    public void setSurportPSESel(boolean supportPSESel) {
        this.supportPSESel = supportPSESel;
    }

    public boolean getUseTermAIPFlag() {
        return this.useTermAIPFlag;
    }

    public void setUseTermAIPFlag(boolean useTermAIPFlag) {
        this.useTermAIPFlag = useTermAIPFlag;
    }

    public String getTermAIP() {
        return this.termAIP;
    }

    public void setTermAIP(String termAIP) {
        this.termAIP = termAIP;
    }

    public boolean getBypassAllFlag() {
        return this.bypassAllFlag;
    }

    public void setBypassAllFlag(boolean bypassAllFlag) {
        this.bypassAllFlag = bypassAllFlag;
    }

    public boolean getBypassPin() {
        return this.bypassPin;
    }

    public void setBypassPin(boolean bypassPin) {
        this.bypassPin = bypassPin;
    }

    public byte getBatchCapture() {
        return this.batchCapture;
    }

    public void setBatchCapture(byte batchCapture) {
        this.batchCapture = batchCapture;
    }

    public boolean getAdviceFlag() {
        return this.adviceFlag;
    }

    public void setAdviceFlag(boolean adviceFlag) {
        this.adviceFlag = adviceFlag;
    }

    public byte getScriptMethod() {
        return this.scriptMethod;
    }

    public void setScriptMethod(byte scriptMethod) {
        this.scriptMethod = scriptMethod;
    }

    public boolean getForceAccept() {
        return this.forceAccept;
    }

    public void setForceAccept(boolean forceAccept) {
        this.forceAccept = forceAccept;
    }

    public boolean getNoPinConfirmAmtFlg() {
        return this.noPinConfirmAmtFlg;
    }

    public void setNoPinConfirmAmtFlg(boolean noPinConfirmAmtFlg) {
        this.noPinConfirmAmtFlg = noPinConfirmAmtFlg;
    }

    public boolean getIsInputAmount() {
        return this.isInputAmount;
    }

    public void setIsInputAmount(boolean isInputAmount) {
        this.isInputAmount = isInputAmount;
    }

    public byte[] getAcquirerId() {
        return this.acquirerId;
    }

    public void setAcquirerId(byte[] acquirerId) {
        this.acquirerId = acquirerId;
    }

    public String getUnpredictableNumberRange() {
        return unpredictableNumberRange;
    }

    public void setUnpredictableNumberRange(String unpredictableNumberRange) {
        this.unpredictableNumberRange = unpredictableNumberRange;
    }

    public boolean isSupportOptTrans() {
        return supportOptTrans;
    }

    public void setSupportOptTrans(boolean supportOptTrans) {
        this.supportOptTrans = supportOptTrans;
    }

    public String getTransCap() {
        return transCap;
    }

    public void setTransCap(String transCap) {
        this.transCap = transCap;
    }

    public boolean isDelayAuthFlag() {
        return delayAuthFlag;
    }

    public void setDelayAuthFlag(boolean delayAuthFlag) {
        this.delayAuthFlag = delayAuthFlag;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.entity.Config
 * JD-Core Version:    0.6.0
 */