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
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvCapk;

import java.util.ArrayList;
import java.util.List;

/**
 * receipt generator
 *
 * @author Steven.W
 */
class ReceiptGeneratorCapkParam extends ReceiptGeneratorParam implements IReceiptGenerator {

    public ReceiptGeneratorCapkParam() {
        //do nothing
    }

    @Override
    protected List<IPage> generatePages(Context context) {
        List<IPage> pages = new ArrayList<>();
        List<EmvCapk> capks = FinancialApplication.getEmvDbHelper().findAllCAPK();

        IPage page = Device.generatePage();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("\nCAPK\n")
                        .setGravity(Gravity.CENTER));
        pages.add(page);

        for (EmvCapk i : capks) {
            page = Device.generatePage();
            //RID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("RID")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getRID())
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //KeyID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Key ID")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getKeyID()))
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //HashInd
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Hash Index")
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getHashInd()))
                            .setGravity(Gravity.END)
                            .setWeight(6.0f));

            //arithInd
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Arith Index")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getArithInd()))
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //module
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Modules")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getModule())
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //Exponent
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Exponent")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getExponent())
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //expDate
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("ExpDate")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getExpDate())
                            .setGravity(Gravity.END)
                            .setWeight(7.0f));

            //checkSum
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Checksum")
                            .setWeight(4.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getCheckSum())
                            .setGravity(Gravity.END)
                            .setWeight(6.0f));
            pages.add(page);
        }

        page = Device.generatePage();
        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        pages.add(page);
        return pages;
    }

    @Override
    protected IPage generatePage(Context context) {return null;}
}
