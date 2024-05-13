/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-30
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import th.co.bkkps.utils.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.utils.Utils;
import io.reactivex.functions.Action;

public class SelfTestActivity extends AppCompatActivity {

    private static final String TAG = "SelfTest";

    public static final int REQ_INITIALIZE = 1;

    private boolean isFirstRun = true;

    public static void onSelfTest(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, SelfTestActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selftest_layout);
        boolean isInstalledNeptune = Component.neptuneInstalled(this, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                finishFailed();
            }
        });

        if (!isInstalledNeptune) {
            return;
        }

        FinancialApplication.getController().set(Controller.NEED_SET_WIZARD, Controller.Constant.YES);
        onCheckLog();
        isFirstRun = InitializeActivity.onCheckInit(SelfTestActivity.this, REQ_INITIALIZE);

        //FinancialApplication.getSysParam().init();

        if (!isFirstRun)
            onActivityResult(REQ_INITIALIZE, 0, null);
    }

    private void onCheckLog() {
        if (FinancialApplication.getController().get(Controller.CLEAR_LOG) == Controller.Constant.YES
                && FinancialApplication.getTransDataDbHelper().deleteAllTransData()
                && FinancialApplication.getTransTotalDbHelper().deleteAllTransTotal()) {
            FinancialApplication.getController().set(Controller.CLEAR_LOG, Controller.Constant.NO);
            FinancialApplication.getController().set(Controller.BATCH_UP_STATUS, Controller.Constant.WORKED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (REQ_INITIALIZE == requestCode) {
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            /*if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){

                if (ActivityCompat.checkSelfPermission(SelfTestActivity.this, permission)
                        != PackageManager.PERMISSION_GRANTED
                        ) {
                    ActivityCompat.requestPermissions(SelfTestActivity.this, new String[]
                            {permission},123);
                    onHandleResult(resultCode);

                }
            }else{*/
                Utils.callPermission(SelfTestActivity.this, permission, new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.e(TAG, "{run}");//执行顺序——2
                        onHandleResult(resultCode);
                    }
                }, getString(R.string.permission_rationale_storage));
           // }

        }
    }

    private void onHandleResult(final int resultCode) {
        if (resultCode == RESULT_OK) {
            FinancialApplication.getApp().runInBackground(new CounterThread());
        } else {
            finishOk();
        }
    }

    private class CounterThread implements Runnable {
        @Override
        public void run() {
            if (isFirstRun) {
                SystemClock.sleep(3000);
            }
            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) findViewById(R.id.selfTest);
                    textView.setText(getString(R.string.selfTest_succ));
                    finishOk();
                }
            });
        }
    }

    private void finishOk() {
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishFailed() {
        Intent intent = getIntent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
