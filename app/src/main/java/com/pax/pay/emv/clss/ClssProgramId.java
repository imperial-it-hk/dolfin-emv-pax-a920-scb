/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190618  	         Roy                     Create
 * ===========================================================================================
 */

package com.pax.pay.emv.clss;
import java.io.Serializable;

public class ClssProgramId implements Serializable {
    public ClssProgramId() {
    }

    private String aucRdClssTxnLmt;
    private String aucRdCVMLmt;
    private String aucRdClssFLmt;
    private String aucTermFLmt;
    private String aucProgramId;
    private int ucPrgramIdLen;
    private int ucRdClssFLmtFlg;
    private int ucRdClssTxnLmtFlg;
    private int ucRdCVMLmtFlg;
    private int ucTermFLmtFlg;
    private int ucStatusCheckFlg;
    private int ucAmtZeroNoAllowed;
    private int ucDynamicLimitSet;
    private int ucRFU;

    public String getAucRdClssTxnLmt() {
        return aucRdClssTxnLmt;
    }

    public void setAucRdClssTxnLmt(String aucRdClssTxnLmt) {
        this.aucRdClssTxnLmt = aucRdClssTxnLmt;
    }

    public String getAucRdCVMLmt() {
        return aucRdCVMLmt;
    }

    public void setAucRdCVMLmt(String aucRdCVMLmt) {
        this.aucRdCVMLmt = aucRdCVMLmt;
    }

    public String getAucRdClssFLmt() {
        return aucRdClssFLmt;
    }

    public void setAucRdClssFLmt(String aucRdClssFLmt) {
        this.aucRdClssFLmt = aucRdClssFLmt;
    }

    public String getAucTermFLmt() {
        return aucTermFLmt;
    }

    public void setAucTermFLmt(String aucTermFLmt) {
        this.aucTermFLmt = aucTermFLmt;
    }

    public String getAucProgramId() {
        return aucProgramId;
    }

    public void setAucProgramId(String aucProgramId) {
        this.aucProgramId = aucProgramId;
    }

    public int getUcPrgramIdLen() {
        return ucPrgramIdLen;
    }

    public void setUcPrgramIdLen(int ucPrgramIdLen) {
        this.ucPrgramIdLen = ucPrgramIdLen;
    }

    public int getUcRdClssFLmtFlg() {
        return ucRdClssFLmtFlg;
    }

    public void setUcRdClssFLmtFlg(int ucRdClssFLmtFlg) {
        this.ucRdClssFLmtFlg = ucRdClssFLmtFlg;
    }

    public int getUcRdClssTxnLmtFlg() {
        return ucRdClssTxnLmtFlg;
    }

    public void setUcRdClssTxnLmtFlg(int ucRdClssTxnLmtFlg) {
        this.ucRdClssTxnLmtFlg = ucRdClssTxnLmtFlg;
    }

    public int getUcRdCVMLmtFlg() {
        return ucRdCVMLmtFlg;
    }

    public void setUcRdCVMLmtFlg(int ucRdCVMLmtFlg) {
        this.ucRdCVMLmtFlg = ucRdCVMLmtFlg;
    }

    public int getUcTermFLmtFlg() {
        return ucTermFLmtFlg;
    }

    public void setUcTermFLmtFlg(int ucTermFLmtFlg) {
        this.ucTermFLmtFlg = ucTermFLmtFlg;
    }

    public int getUcStatusCheckFlg() {
        return ucStatusCheckFlg;
    }

    public void setUcStatusCheckFlg(int ucStatusCheckFlg) {
        this.ucStatusCheckFlg = ucStatusCheckFlg;
    }

    public int getUcAmtZeroNoAllowed() {
        return ucAmtZeroNoAllowed;
    }

    public void setUcAmtZeroNoAllowed(int ucAmtZeroNoAllowed) {
        this.ucAmtZeroNoAllowed = ucAmtZeroNoAllowed;
    }

    public int getUcDynamicLimitSet() {
        return ucDynamicLimitSet;
    }

    public void setUcDynamicLimitSet(int ucDynamicLimitSet) {
        this.ucDynamicLimitSet = ucDynamicLimitSet;
    }

    public int getUcRFU() {
        return ucRFU;
    }

    public void setUcRFU(int ucRFU) {
        this.ucRFU = ucRFU;
    }
}
