package com.pax.eemv.entity;


import com.pax.eemv.enums.EFlowType;
import com.pax.eemv.utils.Tools;

public class InputParam {
    protected byte[] amount;
    protected byte[] cashBackAmount;
    protected byte tag9CValue;
    protected byte[] transDate;
    protected byte[] transTime;
    protected byte[] transStanNo;
    protected byte[] transTraceNo;
    protected boolean isForceOnline;
    protected boolean isSupportCVM;
    protected boolean isEnableCardAuth;
    protected EFlowType flowType;
    protected int pciTimeout = 60 * 1000;

    public InputParam() {
        this.amount = new byte[12];
        this.cashBackAmount = new byte[12];
        this.tag9CValue = 0;
        this.transDate = new byte[8];
        this.transTime = new byte[6];
        this.transStanNo = new byte[4];
        this.transTraceNo = new byte[4];
        this.isForceOnline = false;
        this.isSupportCVM = false;
        this.isEnableCardAuth = false;
        this.flowType = EFlowType.COMPLETE;
    }

    public String getAmount() {
        return Tools.bytes2String(this.amount);
    }

    public void setAmount(String amount) {
        this.amount = Tools.fillData(12, Tools.string2Bytes(amount), 12 - amount.length(), (byte) 48);
    }

    public String getCashBackAmount() {
        return Tools.bytes2String(this.cashBackAmount);
    }

    public void setCashBackAmount(String cashBackAmount) {
        this.cashBackAmount = Tools.fillData(12, Tools.string2Bytes(cashBackAmount), 12 - cashBackAmount.length(), (byte) 48);
    }

    public byte getTag9CValue() {
        return this.tag9CValue;
    }

    public void setTag9CValue(byte tag9cValue) {
        this.tag9CValue = tag9cValue;
    }

    public String getTransDate() {
        return Tools.bytes2String(this.transDate);
    }

    public void setTransDate(String transDate) {
        this.transDate = Tools.string2Bytes(transDate);
    }

    public String getTransTime() {
        return Tools.bytes2String(this.transTime);
    }

    public void setTransTime(String transTime) {
        this.transTime = Tools.string2Bytes(transTime);
    }

    public String getTransStanNo() {
        return Tools.bytes2String(this.transStanNo);
    }

    public void setTransStanNo(String transStanNo) {
        this.transStanNo = Tools.string2Bytes(transStanNo);
    }

    public String getTransTraceNo() {
        return Tools.bytes2String(this.transTraceNo);
    }

    public void setTransTraceNo(String transTraceNo) {
        this.transTraceNo =  Tools.string2Bytes(transTraceNo);
    }

    public boolean isEnableCardAuth() {
        return this.isEnableCardAuth;
    }

    public void setEnableCardAuth(boolean isEnableCardAuth) {
        this.isEnableCardAuth = isEnableCardAuth;
    }

    public boolean isSupportCVM() {
        return this.isSupportCVM;
    }

    public void setSupportCVM(boolean isSupportCVM) {
        this.isSupportCVM = isSupportCVM;
    }

    public boolean isForceOnline() {
        return this.isForceOnline;
    }

    public void setForceOnline(boolean isForceOnline) {
        this.isForceOnline = isForceOnline;
    }

    public EFlowType getFlowType() {
        return this.flowType;
    }

    public void setFlowType(EFlowType flowType) {
        if (flowType == null)
            this.flowType = EFlowType.COMPLETE;
        else
            this.flowType = flowType;
    }

    public int getPciTimeout() {
        return pciTimeout;
    }

    public void setPciTimeout(int pciTimeout) {
        this.pciTimeout = pciTimeout;
    }

}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.entity.InputParam
 * JD-Core Version:    0.6.0
 */