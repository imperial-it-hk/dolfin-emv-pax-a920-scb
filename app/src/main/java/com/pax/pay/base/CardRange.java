/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-22
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.base;


import androidx.annotation.NonNull;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * card range table
 */
@DatabaseTable(tableName = "card_range")
public class CardRange implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "card_id";
    public static final String NAME_FIELD_NAME = "card_name";
    public static final String RANGE_LOW_FIELD_NAME = "card_range_low";
    public static final String RANGE_HIGH_FIELD_NAME = "card_range_high";
    public static final String LENGTH_FIELD_NAME = "card_length";
    public static final String ISSUER_NAME_FIELD_NAME = "issuer_name";
    public static final String ISSUER_BRAND_FIELD_NAME = "issuer_brand";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;

    @DatabaseField(columnName = NAME_FIELD_NAME,unique = true, canBeNull = false)
    private String name;

    @DatabaseField(columnName = ISSUER_NAME_FIELD_NAME,canBeNull = false)
    private String issuerName;

    @DatabaseField(columnName = ISSUER_BRAND_FIELD_NAME,canBeNull = false)
    private String issuerBrand;

    @DatabaseField( canBeNull = false, columnName = RANGE_LOW_FIELD_NAME, width = 10)
    private String panRangeLow;

    @DatabaseField(canBeNull = false, columnName = RANGE_HIGH_FIELD_NAME, width = 10)
    private String panRangeHigh;

    @DatabaseField(columnName = LENGTH_FIELD_NAME)
    private int panLength;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Issuer.ID_FIELD_NAME)
    private Issuer issuer;

    public CardRange() {
    }

    public CardRange(String name, String panRangeLow, String panRangeHigh, int panLength, Issuer issuer,String issuerName) {
        this.setName(name);
        this.setPanRangeLow(panRangeLow);
        this.setPanRangeHigh(panRangeHigh);
        this.setPanLength(panLength);
        this.setIssuer(issuer);
        this.setIssuerName(issuerName);
    }

    public CardRange(int id, String name, String panRangeLow, String panRangeHigh, int panLength, Issuer issuer,String issuerName) {
        this.setId(id);
        this.setName(name);
        this.setPanRangeLow(panRangeLow);
        this.setPanRangeHigh(panRangeHigh);
        this.setPanLength(panLength);
        this.setIssuer(issuer);
        this.setIssuerName(issuerName);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuerBrand() {
        return issuerBrand;
    }

    public void setIssuerBrand(String issuerBrand) {
        this.issuerBrand = issuerBrand;
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

    public int getPanLength() {
        return panLength;
    }

    public void setPanLength(int panLength) {
        this.panLength = panLength;
    }

    /**
     * foreign issuer
     */
    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public void update(@NonNull CardRange cardRange) {
        name = cardRange.getName();
        panLength = cardRange.getPanLength();
        issuer = cardRange.getIssuer();
        issuerName = cardRange.getIssuerName();
    }

    public String getIssuerName() { return issuerName; }

    public void setIssuerName(String issuerName) { this.issuerName = issuerName; }
}
