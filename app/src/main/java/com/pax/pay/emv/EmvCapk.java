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
package com.pax.pay.emv;

import androidx.annotation.NonNull;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.eemv.entity.Capk;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.pay.app.FinancialApplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@DatabaseTable(tableName = "capk")
public class EmvCapk implements Serializable {
    public static final String ID_FIELD_NAME = "id";
    public static final String RID_FIELD_NAME = "rid";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;

    // rID
    @DatabaseField(canBeNull = false, columnName = RID_FIELD_NAME)
    private String rID;
    // key index
    @DatabaseField(canBeNull = false)
    private int keyID;
    // HASH algo index
    @DatabaseField(canBeNull = false)
    private int hashInd;
    // RSA algo index
    @DatabaseField(canBeNull = false)
    private int arithInd;
    // module
    @DatabaseField
    private String module;
    // exponent
    @DatabaseField
    private String exponent;
    // exp date(YYMMDD)
    @DatabaseField
    private String expDate;
    // check sum
    @DatabaseField
    private String checkSum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRID() {

        return rID;
    }

    public void setRID(String rID) {

        this.rID = rID;
    }

    public int getKeyID() {

        return keyID;
    }

    public void setKeyID(int keyID) {

        this.keyID = keyID;
    }

    public int getHashInd() {

        return hashInd;
    }

    public void setHashInd(int hashInd) {

        this.hashInd = hashInd;
    }

    public int getArithInd() {

        return arithInd;
    }

    public void setArithInd(int arithInd) {

        this.arithInd = arithInd;
    }

    public String getModule() {

        return module;
    }

    public void setModule(String module) {

        this.module = module;
    }

    public String getExponent() {

        return exponent;
    }

    public void setExponent(String exponent) {

        this.exponent = exponent;
    }

    public String getExpDate() {

        return expDate;
    }

    public void setExpDate(String expDate) {

        this.expDate = expDate;
    }

    public String getCheckSum() {

        return checkSum;
    }

    public void setCheckSum(String checkSum) {

        this.checkSum = checkSum;
    }

    /********************************
     * EmvCapk to Capk
     *******************************/
    @NonNull
    public static List<Capk> toCapk() {
        IConvert convert = FinancialApplication.getConvert();
        List<Capk> list = new LinkedList<>();

        List<EmvCapk> capkList = FinancialApplication.getEmvDbHelper().findAllCAPK();
        if (capkList == null) {
            return new ArrayList<>(0);
        }
        for (EmvCapk readCapk : capkList) {
            if (readCapk.getModule() == null || readCapk.getExponent() == null)
                continue;
            Capk capk = new Capk();
            capk.setRid(convert.strToBcd(readCapk.getRID(), EPaddingPosition.PADDING_LEFT));
            capk.setKeyID((byte) readCapk.getKeyID());
            capk.setHashInd((byte) readCapk.getHashInd());
            capk.setArithInd((byte) readCapk.getArithInd());
            capk.setModul(convert.strToBcd(readCapk.getModule(), EPaddingPosition.PADDING_LEFT));
            capk.setExponent(convert.strToBcd(readCapk.getExponent(), EPaddingPosition.PADDING_LEFT));
            capk.setExpDate(convert.strToBcd(readCapk.getExpDate(), EPaddingPosition.PADDING_LEFT));
            capk.setCheckSum(convert.strToBcd(readCapk.getCheckSum(), EPaddingPosition.PADDING_LEFT));
            list.add(capk);
        }
        return list;
    }

    @Override
    public String toString() {
        return rID;
    }

    public static void load(List<EmvCapk> capks) {
        // test keys
        for (EmvCapk i : capks) {
            FinancialApplication.getEmvDbHelper().insertCAPK(i);
        }
    }
}
