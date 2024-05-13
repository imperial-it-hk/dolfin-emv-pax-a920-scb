package com.pax.pay.menu;

import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.PromptpayVoidTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionSendAdvice;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

/**
 * Created by SORAYA S on 07-Mar-18.
 */

public class PromptPayMenuActivity extends BaseMenuActivity {

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(PromptPayMenuActivity.this, 6, 2)
                .addMenuItem(getString(R.string.menu_qr_sale), R.drawable.app_sale, PromptPayQRSaleMenuActivity.class)
                .addTransItem("QR Void", R.drawable.app_sale, new PromptpayVoidTrans(PromptPayMenuActivity.this, null))
                .addMenuItem(getString(R.string.menu_qr_report), R.drawable.app_query, PromptPayQRReportMenuActivity.class)
                .addActionItem("QR Advice", R.drawable.app_sale, SendAdvicePromptpay());
        return builder.create();
    }

    private AAction SendAdvicePromptpay() {

        final Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
        ActionSendAdvice sendAdvice = new ActionSendAdvice(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionSendAdvice) action).setParam(PromptPayMenuActivity.this,
                        "QR Advice", null, acquirer);
            }
        });

        sendAdvice.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                // Fixed EDCBBLAND-362
                DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityStack.getInstance().popTo(MainActivity.class);
                        TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                    }
                };

                if (result.getRet() == 0) {
                    DialogUtils.showSuccMessage(PromptPayMenuActivity.this, getString(R.string.menu_qr_advice), onDismissListener,
                            Constants.SUCCESS_DIALOG_SHOW_TIME);
                }
                else {
                    DialogUtils.showErrMessage(PromptPayMenuActivity.this, getString(R.string.menu_qr_advice), getString(R.string.err_no_trans), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });
        return sendAdvice;
    }
}

