/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-6
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import io.reactivex.functions.Action;

public class PrintPreviewActivity extends BaseActivityWithTickForAction {
    private Bitmap bitmap;
    private ImageView imageView;
    private Button btnCancel;
    private Button btnPrint;

    private boolean enablePaperless = true;

    public static final String CANCEL_BUTTON = "CANCEL";
    public static final String PRINT_BUTTON = "PRINT";
    public static final String SMS_BUTTON = "SMS";
    public static final String EMAIL_BUTTON = "EMAIL";

    private Animation receiptOutAnim;

    @Override
    protected void loadParam() {
        //get data
        Intent intent = getIntent();
        if (intent != null) {
            //byte data to
            byte[] bis = intent.getByteArrayExtra(EUIParamKeys.BITMAP.toString());
            bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.paperless_action, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_print_preview_layout;
    }


    @Override
    protected String getTitleString() {
        return getString(R.string.receipt_preview);
    }

    @Override
    protected void initViews() {

        enableBackAction(false);

        imageView = (ImageView) findViewById(R.id.print_preview);
        imageView.setImageBitmap(bitmap);
        btnCancel = (Button) findViewById(R.id.cancel_button);
        btnPrint = (Button) findViewById(R.id.print_button);

        btnCancel.setVisibility(View.INVISIBLE);
        btnPrint.setVisibility(View.INVISIBLE);

        enablePaperless = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PAPERLESS);
        receiptOutAnim = AnimationUtils.loadAnimation(this, R.anim.receipt_out);

//        if (enablePaperless) {
//            Utils.callPermission(PrintPreviewActivity.this, Manifest.permission.SEND_SMS, new Action() {
//                @Override
//                public void run() throws Exception {
//                    Log.e(TAG, "{run}");
//                }
//            }, getString(R.string.permission_rationale_sms));
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.startAnimation(receiptOutAnim);
            }
        });
        finish(new ActionResult(TransResult.SUCC, PRINT_BUTTON));
    }

    @Override
    protected void setListeners() {
        btnCancel.setOnClickListener(this);
        btnPrint.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        Log.i("On Click", v.toString());
        switch (v.getId()) {
            case R.id.cancel_button:
                btnPrint.setClickable(false); //AET-200
                btnCancel.setClickable(false); //AET-240
                //end trans
                finish(new ActionResult(TransResult.SUCC, CANCEL_BUTTON));
                break;
            case R.id.print_button:
                btnPrint.setClickable(false); //AET-240
                btnCancel.setClickable(false); //AET-200
                //print
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.startAnimation(receiptOutAnim);
                    }
                });
                finish(new ActionResult(TransResult.SUCC, PRINT_BUTTON));
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //cannot be here
                return true;
            case R.id.dialog_sms:
                if (enablePaperless && Utils.isSMSAvailable(PrintPreviewActivity.this))
                    finish(new ActionResult(TransResult.SUCC, SMS_BUTTON));
                else
                    ToastUtils.showMessage(R.string.err_unsupported_func);
                return true;
            case R.id.dialog_email:
                if (enablePaperless && Utils.isNetworkAvailable(PrintPreviewActivity.this))
                    finish(new ActionResult(TransResult.SUCC, EMAIL_BUTTON));
                else
                    ToastUtils.showMessage(R.string.err_unsupported_func);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected boolean onKeyBackDown() {
        // AET-91
        ToastUtils.showMessage(R.string.err_not_allowed);
        return true;
    }

    // AET-102
    @Override
    protected void onTimerFinish() {
        quickClickProtection.stop();
        onClick(btnPrint);
    }
}
