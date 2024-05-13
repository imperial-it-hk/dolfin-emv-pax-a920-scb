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
package com.pax.glwrapper.impl;

import android.content.Context;
import android.graphics.Bitmap;

import com.pax.gl.impl.IRgbToMonoAlgorithm;
import com.pax.gl.impl.ImgProcessing;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;

class ImgProcessingImp implements IImgProcessing {

    private ImgProcessing imgProcessing;
    private PaxGLPage paxGLPage;

    ImgProcessingImp(Context context) {
        imgProcessing = ImgProcessing.getInstance(context);
        paxGLPage = new PaxGLPage(context);
    }

    @Override
    public byte[] bitmapToJbig(Bitmap bitmap, IRgbToMonoAlgorithm algo) {
        return imgProcessing.bitmapToJbig(bitmap, algo);
    }

    @Override
    public Bitmap jbigToBitmap(byte[] jbig) {
        return imgProcessing.jbigToBitmap(jbig);
    }

    @Override
    public byte[] bitmapToMonoDots(Bitmap bitmap, IRgbToMonoAlgorithm algo) {
        return imgProcessing.bitmapToMonoDots(bitmap, algo);
    }

    @Override
    public byte[] bitmapToMonoBmp(Bitmap bitmap, IRgbToMonoAlgorithm algo) {
        return imgProcessing.bitmapToMonoBmp(bitmap, algo);
    }

    @Override
    public Bitmap scale(Bitmap bitmap, int w, int h) {
        return imgProcessing.scale(bitmap, w, h);
    }

    @Override
    public Bitmap generateBarCode(java.lang.String contents, int width, int height, com.google.zxing.BarcodeFormat format) {
        return imgProcessing.generateBarCode(contents, width, height, format);
    }

    @Override
    public IPage createPage() {
        return paxGLPage.createPage();
    }

    @Override
    public Bitmap pageToBitmap(IPage page, int pageWidth) {
        return paxGLPage.pageToBitmap(page, pageWidth);
    }
}
