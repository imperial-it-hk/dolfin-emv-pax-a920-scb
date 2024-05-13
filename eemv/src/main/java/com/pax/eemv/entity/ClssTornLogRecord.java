/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-6-15
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.eemv.entity;

import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.CLSS_TORN_LOG_RECORD;

public class ClssTornLogRecord {

    private CLSS_TORN_LOG_RECORD clssTornLogRecord;

    public ClssTornLogRecord() {
        clssTornLogRecord = new CLSS_TORN_LOG_RECORD();
    }

    public ClssTornLogRecord(CLSS_TORN_LOG_RECORD record) {
        clssTornLogRecord = record;
    }

    public ClssTornLogRecord(String pan, boolean panSeqFlg, byte panSeq, byte[] tornData, int tornDataLen) {
        byte[] bcdPan = Tools.str2Bcd(pan);
        clssTornLogRecord = new CLSS_TORN_LOG_RECORD(bcdPan, (byte) bcdPan.length, Tools.boolean2Byte(panSeqFlg), panSeq, tornData, tornDataLen);
    }


    public String getPan() {
        return Tools.bcd2Str(clssTornLogRecord.aucPAN, clssTornLogRecord.ucPANLen);
    }

    public void setPan(String pan) {
        clssTornLogRecord.aucPAN = Tools.str2Bcd(pan);
        clssTornLogRecord.ucPANLen = (byte) clssTornLogRecord.aucPAN.length;
    }

    public boolean getPanSeqFlg() {
        return Tools.byte2Boolean(clssTornLogRecord.ucPANSeqFlg);
    }

    public void setPanSeqFlg(boolean panSeqFlg) {
        clssTornLogRecord.ucPANSeqFlg = Tools.boolean2Byte(panSeqFlg);
    }

    public byte getPanSeq() {
        return clssTornLogRecord.ucPANSeq;
    }

    public void setPanSeq(byte panSeq) {
        clssTornLogRecord.ucPANSeq = panSeq;
    }

    public byte[] getTornData() {
        return clssTornLogRecord.aucTornData;
    }

    public void setTornData(byte[] tornData) {
        clssTornLogRecord.aucTornData = tornData;
    }

    public int getTornDataLen() {
        return clssTornLogRecord.unTornDataLen;
    }

    public void setTornDataLen(int tornDataLen) {
        clssTornLogRecord.unTornDataLen = tornDataLen;
    }
}
