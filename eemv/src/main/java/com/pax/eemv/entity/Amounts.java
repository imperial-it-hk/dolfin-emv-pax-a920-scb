package com.pax.eemv.entity;

public class Amounts {
    private int retCode;
    private String transAmount;
    private String cashBackAmount;

    public Amounts() {
        this.retCode = 0;
        this.transAmount = "";
        this.cashBackAmount = "00000000";
    }

    public int getRetCode() {
        return this.retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public String getTransAmount() {
        return this.transAmount;
    }

    public void setTransAmount(String transAmount) {
        this.transAmount = transAmount;
    }

    public String getCashBackAmount() {
        return this.cashBackAmount;
    }

    public void setCashBackAmount(String cashBackAmount) {
        this.cashBackAmount = cashBackAmount;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.entity.Amounts
 * JD-Core Version:    0.6.0
 */