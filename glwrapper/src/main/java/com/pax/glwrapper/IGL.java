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
package com.pax.glwrapper;

import com.pax.glwrapper.comm.ICommHelper;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.packer.IPacker;

public interface IGL {
    ICommHelper getCommHelper();

    IConvert getConvert();

    IPacker getPacker();

    IImgProcessing getImgProcessing();
}