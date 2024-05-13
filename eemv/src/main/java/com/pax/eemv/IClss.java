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
package com.pax.eemv;

import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.ClssInputParam;
import com.pax.eemv.entity.ClssTornLogRecord;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.enums.EKernelType;
import com.pax.eemv.exception.EmvException;

import java.util.List;

public interface IClss extends IEmvBase {
    IClss getClss();

    EKernelType getKernelType();

    void preTransaction(ClssInputParam inputParam) throws EmvException;

    void setListener(IClssListener listener);

    void setTornLogRecords(List<ClssTornLogRecord> clssTornLogRecord);

    List<ClssTornLogRecord> getTornLogRecords();

    CTransResult clssReadCardProcess(InputParam inputParam) throws EmvException;

    CTransResult clssAfterReadCardProcess(CTransResult result) throws EmvException;
}
