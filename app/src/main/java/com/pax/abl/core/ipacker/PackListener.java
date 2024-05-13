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
package com.pax.abl.core.ipacker;

import androidx.annotation.NonNull;

/**
 * packer listener
 *
 * @author Steven.W
 */
public interface PackListener {
    /**
     * calc MAC
     *
     * @param data input data
     * @return mac value
     */
    @NonNull
    byte[] onCalcMac(byte[] data);

    /**
     * encrypt track
     *
     * @param track input track
     * @return encrypted track data
     */
    @NonNull
    byte[] onEncTrack(byte[] track);

    @NonNull
    String onGetAcqName();
}
