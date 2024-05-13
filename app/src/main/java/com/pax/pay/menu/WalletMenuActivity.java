package com.pax.pay.menu;

import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.WalletSaleCBTrans;
import com.pax.pay.trans.WalletQrSaleTrans;
import com.pax.pay.trans.WalletQrVoidTrans;
import com.pax.pay.trans.WalletRefundTrans;
import com.pax.pay.trans.action.ActionQrSaleInquiry;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.DialogUtils;

/**
 * Created by WITSUTA A on 4/4/2018.
 */

public class WalletMenuActivity extends BaseMenuActivity {
    @Override
    public MenuPage createMenuPage() {
        boolean isWalletCscanB = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B);
        MenuPage.Builder builder = new MenuPage.Builder(WalletMenuActivity.this, 9, 3);
        if (isWalletCscanB) {
            builder.addTransItem(getString(R.string.menu_qr_sale), R.drawable.app_sale, new WalletSaleCBTrans(WalletMenuActivity.this, null));
        }else {
            builder.addTransItem(getString(R.string.menu_qr_sale), R.drawable.app_sale, new WalletQrSaleTrans(WalletMenuActivity.this, null));
        }
        builder.addTransItem(getString(R.string.menu_qr_void), R.drawable.app_sale, new WalletQrVoidTrans(WalletMenuActivity.this, null));
        builder.addTransItem(getString(R.string.menu_qr_refund), R.drawable.app_sale, new WalletRefundTrans(WalletMenuActivity.this, null));
        builder.addActionItem(getString(R.string.menu_qr_advice), R.drawable.app_sale, SendAdviceWallet());

        return builder.create();
    }

    private AAction SendAdviceWallet() {
        ActionQrSaleInquiry sendAdvice = new ActionQrSaleInquiry(new AAction.ActionStartListener() {
            @Override
            public void onStart(AAction action) {
                ((ActionQrSaleInquiry) action).setParam(WalletMenuActivity.this,
                        getString(R.string.menu_wallet_advice), null);
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
                    DialogUtils.showSuccMessage(WalletMenuActivity.this, getString(R.string.menu_qr_advice), onDismissListener,
                            Constants.SUCCESS_DIALOG_SHOW_TIME);
                }
                else {
                    DialogUtils.showErrMessage(WalletMenuActivity.this, getString(R.string.menu_qr_advice), getString(R.string.err_no_trans), onDismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
        });
        return sendAdvice;
    }
}
