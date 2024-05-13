/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-26
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.menu;

import android.content.DialogInterface;

import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.PreAuthTrans;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;

public class AuthMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CustomAlertDialog dialog = new CustomAlertDialog(AuthMenuActivity.this, CustomAlertDialog.NORMAL_TYPE);
                dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityStack.getInstance().popTo(MainActivity.class);
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
    public MenuPage createMenuPage() {
        MenuPage.Builder builder = new MenuPage.Builder(AuthMenuActivity.this, 3, 3)
                .addTransItem(getString(R.string.menu_preAuth), R.drawable.app_auth,
                        new PreAuthTrans(AuthMenuActivity.this, true, null).setBackToMain(true));
        return builder.create();
    }
}
