package com.pax.pay.menu;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.PromptPaySettleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionLastSettle;
import com.pax.view.MenuPage;


/**
 * Created by WITSUTA A on 4/19/2018.
 */

public class PromptPayQRReportMenuActivity extends BaseMenuActivity{
    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(PromptPayQRReportMenuActivity.this, 8, 2)
                .addMenuItem(getString(R.string.menu_qr_last), R.drawable.app_query, PromtPayQRReportLastTransActivity.class)
                .addTransItem(getString(R.string.menu_qr_sum_report), R.drawable.app_query, new PromptPaySettleTrans(PromptPayQRReportMenuActivity.this, null))
                .addActionItem(getString(R.string.menu_qr_last_sum_report), R.drawable.app_query, PrintLastSettlement());

        return builder.create();
    }

    private AAction PrintLastSettlement() {
        final Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
        ActionLastSettle actionLastSettle = new ActionLastSettle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionLastSettle) action).setParam(PromptPayQRReportMenuActivity.this, acquirer);
            }
        });
        actionLastSettle.setEndListener(new AAction.ActionEndListener() {
            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null);
            }
        });

        return actionLastSettle;
    }
}

