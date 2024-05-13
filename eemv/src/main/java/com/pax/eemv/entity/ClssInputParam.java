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

import com.pax.eemv.enums.ECvmResult;

import java.util.List;

public class ClssInputParam extends InputParam {
    private int amtZeroNoAllowedFlg;
    private boolean crypto17Flg;
    private boolean statusCheckFlg;
    private String readerTTQ;

    //for VISA
    private boolean domesticOnly;
    private List<ECvmResult> cvmReq;
    private byte enDDAVerNo;

    public ClssInputParam() {
        this.setAmtZeroNoAllowedFlg(1);
        this.setCrypto17Flg(false);
        this.setStatusCheckFlg(false);
        this.setReaderTTQ("");
    }

    /**
     * @return 0- Flag activated, online required
     * 1- Flag activated, amount zero not allowed
     * 2- Flag deactivated
     */
    public int getAmtZeroNoAllowedFlg() {
        return amtZeroNoAllowedFlg;
    }

    public void setAmtZeroNoAllowedFlg(int amtZeroNoAllowedFlg) {
        this.amtZeroNoAllowedFlg = amtZeroNoAllowedFlg;
    }

    public boolean isCrypto17Flg() {
        return crypto17Flg;
    }

    public void setCrypto17Flg(boolean crypto17Flg) {
        this.crypto17Flg = crypto17Flg;
    }

    public boolean isStatusCheckFlg() {
        return statusCheckFlg;
    }

    public void setStatusCheckFlg(boolean statusCheckFlg) {
        this.statusCheckFlg = statusCheckFlg;
    }

    public String getReaderTTQ() {
        return readerTTQ;
    }

    public void setReaderTTQ(String readerTTQ) {
        this.readerTTQ = readerTTQ;
    }

    public boolean isDomesticOnly() {
        return domesticOnly;
    }

    public void setDomesticOnly(boolean domesticOnly) {
        this.domesticOnly = domesticOnly;
    }

    public List<ECvmResult> getCvmReq() {
        return cvmReq;
    }

    public void setCvmReq(List<ECvmResult> cvmReq) {
        this.cvmReq = cvmReq;
    }

    public byte getEnDDAVerNo() {
        return enDDAVerNo;
    }

    public void setEnDDAVerNo(byte enDDAVerNo) {
        this.enDDAVerNo = enDDAVerNo;
    }
}
