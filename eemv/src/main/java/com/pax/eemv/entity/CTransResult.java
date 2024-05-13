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
import com.pax.eemv.enums.ETransResult;

public class CTransResult {

    private ETransResult transResult;
    private ECvmResult cvmResult;

    public CTransResult(ETransResult transResult) {
        this.transResult = transResult;
        this.cvmResult = ECvmResult.CONSUMER_DEVICE;
    }

    public CTransResult(ETransResult transResult, ECvmResult cvmType) {
        this.transResult = transResult;
        this.cvmResult = cvmType;
    }

    public ETransResult getTransResult() {
        return this.transResult;
    }

    public void setTransResult(ETransResult transResult) {
        this.transResult = transResult;
    }

    public ECvmResult getCvmResult() {
        return this.cvmResult;
    }

    public void setCvmResult(ECvmResult cvmResult) {
        this.cvmResult = cvmResult;
    }

}
