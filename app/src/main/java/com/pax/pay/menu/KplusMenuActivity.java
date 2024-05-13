package com.pax.pay.menu;

import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.KplusQrSaleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.VoidQRTrans;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.Collections;

/**
 * Created by NANNAPHAT S on 12-Feb-19.
 */

public class KplusMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        KplusQrSaleTrans kplusQrSaleTrans = new KplusQrSaleTrans(KplusMenuActivity.this, null);
        int ermExceededResult = kplusQrSaleTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        int issuerSupportResult = kplusQrSaleTrans.IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_KPLUS));
        if (issuerSupportResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(issuerSupportResult));
            return;
        }

    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(KplusMenuActivity.this, 4, 2)
                .addTransItem("K+", R.drawable.app_sale, new KplusQrSaleTrans(KplusMenuActivity.this, null))
                .addActionItem("QR Void", R.drawable.app_sale, createInputPwdActionVoidQr());
        return builder.create();
    }

    private AAction createInputPwdActionVoidQr() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(KplusMenuActivity.this, 8,
                        getString(R.string.prompt_void_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                tickTimer.start(); // reset main Activity timer
                if (result.getRet() != TransResult.SUCC) {
                    finish(result);
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_VOID_PWD))) {
                    DialogUtils.showErrMessage(KplusMenuActivity.this, getString(R.string.menu_manage),
                            getString(R.string.err_password), new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    finish();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                VoidQRTrans voidQRTrans = new VoidQRTrans(KplusMenuActivity.this, null);
                voidQRTrans.execute();
            }
        });

        return inputPasswordAction;
    }
}
