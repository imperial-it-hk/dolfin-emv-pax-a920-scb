/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-5-23
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.glwrapper.packer;

import com.pax.gl.pack.IApdu;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.ITlv;

public interface IPacker {
    IApdu getApdu();

    IIso8583 getIso8583();

    ITlv getTlv();
}