/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-6
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.emv.clss;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "clsstornlog")
public class ClssTornLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "id";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    protected int id;
    @DatabaseField(canBeNull = false)
    private String aucPan;
    @DatabaseField(canBeNull = false)
    private int panLen;
    @DatabaseField(canBeNull = false)
    private boolean panSeqFlg;
    @DatabaseField(dataType = DataType.BYTE, canBeNull = false)
    private byte panSeq;
    @DatabaseField(canBeNull = false)
    private String aucTornData;
    @DatabaseField(canBeNull = false)
    private int tornDataLen;

    public ClssTornLog() {
        //do nothing
    }

    public String getAucPan() {
        return aucPan;
    }

    public void setAucPan(String aucPan) {
        this.aucPan = aucPan;
    }

    public int getPanLen() {
        return panLen;
    }

    public void setPanLen(int panLen) {
        this.panLen = panLen;
    }

    public boolean getPanSeqFlg() {
        return panSeqFlg;
    }

    public void setPanSeqFlg(boolean panSeqFlg) {
        this.panSeqFlg = panSeqFlg;
    }

    public byte getPanSeq() {
        return panSeq;
    }

    public void setPanSeq(byte panSeq) {
        this.panSeq = panSeq;
    }

    public String getAucTornData() {
        return aucTornData;
    }

    public void setAucTornData(String aucTornData) {
        this.aucTornData = aucTornData;
    }

    public int getTornDataLen() {
        return tornDataLen;
    }

    public void setTornDataLen(int tornDataLen) {
        this.tornDataLen = tornDataLen;
    }
}
