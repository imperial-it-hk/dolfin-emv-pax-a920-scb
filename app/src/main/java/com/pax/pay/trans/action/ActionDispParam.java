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
package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.DispParamActivity;
import com.pax.pay.trans.action.activity.DisplayEDCParamActivity;

import java.io.ByteArrayOutputStream;

public class ActionDispParam extends AAction {
    private Context context;
    private Bitmap loadedBitmap = null;
    private String title;

    public ActionDispParam(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 参数设置
     *
     * @param context ：应用上下文
     * @param title   ：抬头
     */
    public void setParam(Context context, String title,Bitmap loadedBitmap) {
        this.context = context;
        this.title = title;
        this.loadedBitmap = loadedBitmap;
    }

    @Override
    protected void process() {
//        FinancialApplication.getApp().runOnUiThread(new ProcessRunnable());
        Intent intent = new Intent(context, DisplayEDCParamActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
        context.startActivity(intent);
    }

    private class ProcessRunnable implements Runnable {


        @Override
        public void run() {
            Bundle bundle = new Bundle();
            bundle.putString(EUIParamKeys.NAV_TITLE.toString(), title);
            bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);

            //bitmap to byte data, then transfer data to PrintPreviewActivity
            Intent intent = new Intent(context, DispParamActivity.class);
            ByteArrayOutputStream bitmapData = new ByteArrayOutputStream();
            loadedBitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapData);
            byte[] bitmapByte = bitmapData.toByteArray();
            bundle.putByteArray(EUIParamKeys.BITMAP.toString(), bitmapByte);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

    }
}
