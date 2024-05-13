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
import android.graphics.Canvas;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;


public class ReceiptPrintTotalDetail extends AReceiptPrint {

    private boolean isOnSettlement = false;
    private boolean skipPrintFlag = false;
    public int print(String title, Acquirer acquirer, PrintListener listener, int acqNum, boolean emptyTrans, TransTotal total, boolean isReprint, boolean isOnSettlement, boolean skipPrintFlag) {
        this.isOnSettlement = isOnSettlement;
        this.skipPrintFlag = skipPrintFlag;
        return print(title,acquirer,listener,acqNum,emptyTrans,total,isReprint);
    }
    public int print(String title, Acquirer acquirer, PrintListener listener, int acqNum, boolean emptyTrans, TransTotal total, boolean isReprint, boolean isOnSettlement) {
        this.isOnSettlement = isOnSettlement;
        return print(title,acquirer,listener,acqNum,emptyTrans,total,isReprint);
    }
    public int print(String title, Acquirer acquirer, PrintListener listener, int acqNum, boolean emptyTrans, TransTotal total, boolean isReprint) {
        this.listener = listener;
        int count = 0;
        int ret = 0;

        if (listener != null)
            listener.onShowMessage(null, Utils.getString(R.string.wait_print));
        // print detail main information
        ReceiptGeneratorTotalDetail receiptGeneratorTotalDetail = new ReceiptGeneratorTotalDetail(acquirer);

        if(!(Constants.isTOPS)) {
            ret = printBitmap(receiptGeneratorTotalDetail.generateMainInfo(title, acqNum));
            if (ret != 0) {
                if (listener != null) {
                    listener.onEnd();
                }
                return ret;
            }
        }

        // for settlement with empty batch, no need to print detail
        if (!emptyTrans) {
            if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
               ret = printBitmap(receiptGeneratorTotalDetail.generateBitmapWallet(), skipPrintFlag);
            } else {
                Bitmap bitmap;
                if (total != null) {
                    if (!total.isClosed()) {//bitmap before settlement
                        List<Bitmap> bitmaps = receiptGeneratorTotalDetail.generateBitmapArray();
                        bitmaps.removeAll(Collections.singleton(null));//remove null object
                        bitmap = createBitmap(bitmaps);
                        if (bitmap != null) {
                            combineBitmap(new Canvas(bitmap), bitmaps);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
                            total.setTotalByIssuer(outputStream.toByteArray());
                            ret = printBitmap(bitmaps, skipPrintFlag);
                        }
                    } else {//bitmap for last settlement
                        if (total.getTotalByIssuer() != null) {
                            bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(total.getTotalByIssuer()));
                            ret = printBitmap(bitmap, skipPrintFlag);
                        }
                    }

                    printBitmap(receiptGeneratorTotalDetail.generateSettlementClosedSuccessfully(isOnSettlement), skipPrintFlag);

                }

                if (isReprint) {
                    ret = printBitmap(receiptGeneratorTotalDetail.generateReprint(), skipPrintFlag);
                }

            }
            if (ret != 0) {
                if (listener != null) {
                    listener.onEnd();
                }
                return ret;
            }
        } else {
            if(!skipPrintFlag) {
                printStr("\n\n\n\n\n\n", skipPrintFlag);
            }
        }

        /*if (listener != null) {
            listener.onEnd();
        }
*/
        if (ret != 0) {
            if (listener != null) {
                listener.onEnd();
            }
            return ret;
        }

        if(acqNum == 0 && !isReprint) {//Print Settlement
            if (listener != null) {
                listener.onEnd();
            }
        }

        return ret;
    }

    private Bitmap createBitmap(List<Bitmap> bitmaps) {
        if (bitmaps != null && !bitmaps.isEmpty()) {
            int height = 0;
            for (Bitmap b : bitmaps) {
                height += b.getHeight();
            }
            return Bitmap.createBitmap(bitmaps.get(0).getWidth(), height, Bitmap.Config.ARGB_8888);
        }
        return null;
    }

    private Canvas combineBitmap(Canvas cs, List<Bitmap> bpmList){
        int height = bpmList.get(0).getHeight();
        cs.drawBitmap(bpmList.get(0), 0f, 0f, null);

        for (int i = 1; i < bpmList.size(); i++) {
            cs.drawBitmap(bpmList.get(i), 0f, height, null);
            height += bpmList.get(i).getHeight();
        }
        return cs;
    }
}
