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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import th.co.bkkps.utils.Log;

import android.view.Gravity;

import com.pax.device.Device;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;

/**
 * receipt generator
 *
 * @author Steven.W
 */
public class ReceiptGeneratorTransMessage implements IReceiptGenerator {

    String[] respMsg;
    String[] reqMsg;
    int fontSize;

    /**
     * @param reqMsg  ：reqMsg
     * @param respMsg ：respMsg
     */
    public ReceiptGeneratorTransMessage(String[] reqMsg, String[] respMsg) {
        this.reqMsg = reqMsg;
        this.respMsg = respMsg;
        fontSize = FONT_NORMAL;
    }

    public ReceiptGeneratorTransMessage(String[] reqMsg, String[] respMsg, int fontSize) {
        this.reqMsg = reqMsg;
        this.respMsg = respMsg;
        this.fontSize = fontSize;
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();
        page.adjustLineSpace(1);

        if (reqMsg != null) {
            for (String s : reqMsg) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(s)
                                .setFontSize(fontSize)
                                .setGravity(Gravity.START));
            }

            page.addLine().addUnit(page.createUnit().setText("\n\n"));
        }

        if (respMsg != null) {
            for (String s : respMsg) {
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(s)
                                .setFontSize(fontSize)
                                .setGravity(Gravity.START));
            }

            page.addLine().addUnit(page.createUnit().setText("\n\n"));
        } else
            page.addLine().addUnit(page.createUnit().setText("\n\n"));

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }


}
