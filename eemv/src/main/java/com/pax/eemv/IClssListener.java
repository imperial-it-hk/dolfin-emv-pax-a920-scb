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
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.exception.EmvException;
import com.pax.jemv.clcommon.Clss_ProgramID_II;

import java.util.List;

public interface IClssListener {
    void onComfirmCardInfo(String track1, String track2, String track3) throws EmvException;

    int onCvmResult(ECvmResult result);

    EOnlineResult onOnlineProc(CTransResult result);

    boolean onDetect2ndTap();

    byte[] onUpdateKernelCfg(String aid);

    int onIssScrCon();

    void onPromptRemoveCard();

    int onDisplaySeePhone();

    void onSetClssTypeMode(int transactionPath);

    boolean onChkForceSettlement();

    List<Clss_ProgramID_II> onGetProgramId();

    boolean onChkIsDynamicOffline();

    int onProcessEnterRefNo();
}
