package com.pax.pay.menu;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.PromptPaySettleTrans;
import com.pax.pay.trans.QRSettleTrans;
import com.pax.pay.trans.QrInquiryLastTrans;
import com.pax.pay.trans.QrInquiryTrans;
import com.pax.pay.trans.QrSaleTrans;
import com.pax.pay.trans.QrVoidTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionLastSettle;
import com.pax.view.MenuPage;

/**
 * Created by WITSUTA A on 12/4/2018.
 */

public class QRSaleMenuActivity extends BaseMenuActivity {
    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(QRSaleMenuActivity.this, 8, 2)
                .addTransItem(getString(R.string.menu_qr_all_in_one), R.drawable.app_sale, new QrSaleTrans(QRSaleMenuActivity.this, null,"00"))
                .addTransItem(getString(R.string.menu_qr_visa_master), R.drawable.app_sale, new QrSaleTrans(QRSaleMenuActivity.this, null,"02"))
                .addTransItem(getString(R.string.menu_qr_void), R.drawable.app_sale, new QrVoidTrans(QRSaleMenuActivity.this, null))
                .addTransItem(getString(R.string.menu_qr_sale_inquiry), R.drawable.app_sale, new QrInquiryTrans(QRSaleMenuActivity.this, null))
                .addTransItem(getString(R.string.menu_qr_last), R.drawable.app_sale, new QrInquiryLastTrans(QRSaleMenuActivity.this, null))
                .addTransItem(getString(R.string.menu_qr_sum_report), R.drawable.app_query, new QRSettleTrans(QRSaleMenuActivity.this, null))
                .addActionItem(getString(R.string.menu_qr_last_sum_report), R.drawable.app_query, PrintLastSettlement());
        return builder.create();

    }

    private AAction PrintLastSettlement() {
        final Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
        ActionLastSettle actionLastSettle = new ActionLastSettle(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionLastSettle) action).setParam(QRSaleMenuActivity.this, acquirer);
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
