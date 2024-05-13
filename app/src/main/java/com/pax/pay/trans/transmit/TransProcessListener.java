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
package com.pax.pay.trans.transmit;


import androidx.annotation.NonNull;
import com.pax.pay.trans.model.TransData;

public interface TransProcessListener {
    void onShowProgress(String message, int timeout);

    int onShowProgress(String message, int timeout, boolean showTitle, boolean showConfirmCancelBtn);

    void onShowWarning(String message, int timeout);

    void onShowWarning(String message, int timeout, int msgSize);

    void onUpdateProgressTitle(String title);

    void onHideProgress();

    int onShowNormalMessage(String message, int timeout, boolean confirmable);

    int onShowErrMessage(String message, int timeout, boolean confirmable);

    int onInputOnlinePin(TransData transData);

    @NonNull
    byte[] onCalcMac(byte[] data);

    @NonNull
    byte[] onEncTrack(byte[] track);

    String getTitle();
}