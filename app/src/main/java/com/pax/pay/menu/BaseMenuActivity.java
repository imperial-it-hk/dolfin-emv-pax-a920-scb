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
package com.pax.pay.menu;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.pay.BaseActivity;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;

public abstract class BaseMenuActivity extends BaseActivityWithTickForAction implements OnClickListener {
    private String navTitle;
    private boolean navBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onResume() {
        enableBackAction(true);
        super.onResume();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.menu_layout;
    }

    @Override
    protected void loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), true);
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        LinearLayout llContainer = (LinearLayout) findViewById(R.id.ll_container);

        android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        llContainer.addView(createMenuPage(), params);

    }

    public abstract MenuPage createMenuPage();

    @Override
    protected void setListeners() {
        enableBackAction(navBack);
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    protected void showMsgNotAllowed(final Context context){
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.NORMAL_TYPE);
                dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
                dialog.setTimeout(3);
                dialog.show();
                dialog.setNormalText(getString(R.string.err_not_allowed));
                dialog.showCancelButton(false);
                dialog.showConfirmButton(true);
            }
        });
    }

    public void showErrorDialog(Context context, String message) {
        try {
            Device.beepErr();
            EReceiptUtils.getInstance().showMsgErmError( context, CustomAlertDialog.NORMAL_TYPE, message, Constants.FAILED_DIALOG_SHOW_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
