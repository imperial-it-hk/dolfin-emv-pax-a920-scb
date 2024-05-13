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
package com.pax.pay.trans.receipt;

import android.content.Context;
import android.graphics.Bitmap;

import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * receipt generator
 *
 * @author Steven.W
 */
abstract class ReceiptGeneratorParam implements IReceiptGenerator {

    public ReceiptGeneratorParam() {
        //do nothing
    }

    @Override
    public Bitmap generateBitmap() {

        Context context = FinancialApplication.getApp();

        IPage page = generatePage(context);
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        Bitmap bitmap = imgProcessing.pageToBitmap(page, 384);
        return bitmap;
    }

    public List<Bitmap> generateBitmaps() {
        Context context = FinancialApplication.getApp();

        List<IPage> pages = generatePages(context);

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        List<Bitmap> bitmaps = new ArrayList<>();
        for (IPage i : pages) {
            Bitmap bitmap = imgProcessing.pageToBitmap(i, 384);
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }
        return bitmaps;
    }

    protected abstract List<IPage> generatePages(Context context);
    protected abstract IPage generatePage(Context context);

    @Override
    public String generateString() {
        return "";
    }
}
