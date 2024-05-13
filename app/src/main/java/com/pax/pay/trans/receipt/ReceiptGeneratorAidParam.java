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
import com.pax.edc.R;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * receipt generator
 *
 * @author Steven.W
 */
class ReceiptGeneratorAidParam extends ReceiptGeneratorParam implements IReceiptGenerator {

    ReceiptGeneratorAidParam() {
        //do nothing
    }

    @Override
    protected List<IPage> generatePages(Context context) {
        List<IPage> pages = new ArrayList<>();
        List<EmvAid> aids = FinancialApplication.getEmvDbHelper().findAllAID();

        IPage page = Device.generatePage();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("\nAPP\n")
                        .setGravity(Gravity.CENTER));
        pages.add(page);

        for (EmvAid i : aids) {
            page = Device.generatePage();
            //appName
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("App Name")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getAppName())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            //aid
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("AID")
                            .setWeight(1.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getAid())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //selFlag
            String temp = EmvAid.PART_MATCH == i.getSelFlag() ? "Part match" : "Full match";
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("SelFlag")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(temp)
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //priority
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Priority")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getPriority()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //targetPer
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Target%")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getTargetPer()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //maxTargetPer
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Max Target%")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getMaxTargetPer()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //floorLimitCheck
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Floor Limit Check")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(Integer.toString(i.getFloorLimitCheckFlg()))
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));

            //randTransSel
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("RandTransSel")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(getYesNo(i.getRandTransSel()))
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));

            //velocityCheck
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Velocity Check")
                            .setWeight(3.0f))
                    .addUnit(page.createUnit()
                            .setText(getYesNo(i.getVelocityCheck()))
                            .setGravity(Gravity.END)
                            .setWeight(2.0f));

            //floorLimit
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Floor Limit")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Long.toString(i.getFloorLimit()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //threshold
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Threshold")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(Long.toString(i.getThreshold()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //tacDenial
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TAC Denial")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getTacDenial())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //tacOnline
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TAC Online")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getTacOnline())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //tacDefault
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TAC Default")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getTacDefault())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //acquirerId
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Acquirer Id")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getAcquirerId())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //dDOL
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("dDOL")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getDDOL())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //tDOL
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("tDOL")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getTDOL())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            //version
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Version")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getVersion())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            //riskManageData
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Risk Manage Data")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getRiskManageData())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            pages.add(page);
        }

        page = Device.generatePage();
        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        pages.add(page);
        return pages;
    }

    private String getYesNo(boolean value) {
        return value ? Utils.getString(R.string.yes) : Utils.getString(R.string.no);
    }

    @Override
    protected IPage generatePage(Context context) {return null;}
}
