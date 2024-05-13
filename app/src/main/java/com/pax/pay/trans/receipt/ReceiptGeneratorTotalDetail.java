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
import android.view.Gravity;

import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.QrTag31Utils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.List;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;

/**
 * transaction detail generator
 *
 * @author Steven.W
 */
class ReceiptGeneratorTotalDetail implements IReceiptGenerator {

    private Acquirer acquirer;
    private static final List<TransData.ETransStatus> filter = new ArrayList<>();

    /**
     *
     *
     * @param acquirer ：acquirer
     */
    public ReceiptGeneratorTotalDetail(Acquirer acquirer) {
        this.acquirer = acquirer;
    }

    /**
     * generate detail main information, use this construction method
     */
    public ReceiptGeneratorTotalDetail() {
        //do nothing
    }

    @Override
    public Bitmap generateBitmap() {
        IPage page = Device.generatePage();
        filter.add(NORMAL);
        filter.add(ADJUSTED);

        List<Issuer> listIssuers = FinancialApplication.getAcqManager().findAllIssuers();
        List<Issuer> issuers = filterIssuers(listIssuers, filter, acquirer);

        Component.generateTotalByIssuer(page, acquirer, filter, issuers, FONT_NORMAL);
        if (Constants.ACQ_DCC.equals(acquirer.getName())) {
            Component.generateDccTotal(page, acquirer);
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public List<Bitmap> generateBitmapArray() {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        List<Bitmap> bitmaps = new ArrayList<>();

        filter.add(NORMAL);
        filter.add(ADJUSTED);

        List<Issuer> listIssuers = FinancialApplication.getAcqManager().findAllIssuers();
        List<Issuer> issuers = filterIssuers(listIssuers, filter, acquirer);

        if(Constants.isTOPS){
            if (acquirer.getName().equals(Constants.ACQ_KPLUS)) {
                Component.generateTotalByThaiQRSourceOfFundBitmappArrayTOPS(acquirer, QrTag31Utils.Companion.getThaiQrTransDataForReporting(), FONT_NORMAL, bitmaps, imgProcessing);
            } else {
                Component.generateTotalByIssuerBitmapArrayTOPS(acquirer, filter, issuers, FONT_NORMAL, bitmaps, imgProcessing);
            }
        }else{
            Component.generateTotalByIssuerBitmapArray(acquirer, filter, issuers, FONT_NORMAL, bitmaps, imgProcessing);
        }

        return bitmaps;
    }

    public Bitmap generateBitmapWallet() {
        IPage page = Device.generatePage();
        filter.add(NORMAL);
        filter.add(ADJUSTED);

        List<Issuer> listIssuers = FinancialApplication.getAcqManager().findAllIssuers();

        Component.generateTotalWalletByIssuer(page, acquirer, filter, listIssuers, FONT_NORMAL);

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    @Override
    public String generateString() {
        return null;
    }

    /**
     * generate detail information
     *
     * @param title
     * @param acqNum
     * @return
     */
    public Bitmap generateMainInfo(String title,int acqNum) {
        IPage page = Device.generatePage();
//
//       Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();

        Component.generateTotalDetailMainInfo(page, acquirer, title, FONT_NORMAL,acqNum);

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    public Bitmap generateReprint() {
        IPage page = Device.generatePage();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("* " + Utils.getString(R.string.receipt_print_again) + " *" )
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.CENTER));
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }
    public Bitmap generateSettlementClosedSuccessfully(boolean isOnSettlement) {
        IPage page = Device.generatePage();
        if (isOnSettlement) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("*** SETTLEMENT ***")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("CLOSED SUCCESSFULLY")
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.CENTER));

            page.addLine().addUnit(page.createUnit()
                    .setText(Utils.getString(R.string.receipt_double_line))
                    .setGravity(Gravity.CENTER));
        } else {
            page.addLine().addUnit(page.createUnit()
                    .setText("\n"));
        }

        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        return imgProcessing.pageToBitmap(page, 384);
    }

    private List<Issuer> filterIssuers(List<Issuer> issuers, List<TransData.ETransStatus> filter, Acquirer acquirer) {
        List<Issuer> result = new ArrayList<>();
        for (Issuer issuer : issuers) {
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                result.add(issuer);
            }
        }
        return result;
    }
}
