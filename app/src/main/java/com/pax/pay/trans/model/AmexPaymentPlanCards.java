package com.pax.pay.trans.model;

public class AmexPaymentPlanCards {
    private String name;
    private String panRangeLow;
    private String panRangeHigh;

    public AmexPaymentPlanCards() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPanRangeLow() {
        return panRangeLow;
    }

    public void setPanRangeLow(String panRangeLow) {
        this.panRangeLow = panRangeLow;
    }

    public String getPanRangeHigh() {
        return panRangeHigh;
    }

    public void setPanRangeHigh(String panRangeHigh) {
        this.panRangeHigh = panRangeHigh;
    }
}
