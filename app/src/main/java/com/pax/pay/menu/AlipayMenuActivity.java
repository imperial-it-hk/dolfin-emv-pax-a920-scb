package com.pax.pay.menu;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.AlipayQrSaleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.VoidQRAlipayTrans;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;
import com.pax.pay.trans.AlipayBScanCSaleTrans;
import java.util.Arrays;
import java.util.Collections;

import th.co.bkkps.utils.DynamicOffline;

/**
 * Created by NANNAPHAT S on 12-Feb-19.
 */

public class AlipayMenuActivity extends BaseMenuActivity {

    @Override
    protected void onResume() {
        super.onResume();

        AlipayQrSaleTrans alipayQrSaleTrans = new AlipayQrSaleTrans(AlipayMenuActivity.this, null);
        int ermExceededResult = alipayQrSaleTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        int issuerSupportResult = alipayQrSaleTrans.IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_ALIPAY));
        if (issuerSupportResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(issuerSupportResult));
            return;
        }
    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(AlipayMenuActivity.this, 4, 2);
        builder.addTransItem("Alipay", R.drawable.icon_alipay, new AlipayQrSaleTrans(AlipayMenuActivity.this, null));

        //B scan C
        if(Utils.isEnableAlipayBscanC()){
            builder.addTransItem("B Scan C", R.drawable.icon_alipay, new AlipayBScanCSaleTrans(AlipayMenuActivity.this, null));
        }

        builder.addActionItem("QR Void", R.drawable.app_sale, createInputPwdActionVoidQRAlipay());
        return builder.create();
    }

    private AAction createInputPwdActionVoidQRAlipay() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(AlipayMenuActivity.this, 8,
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
                    DialogUtils.showErrMessage(AlipayMenuActivity.this, getString(R.string.menu_manage),
                            getString(R.string.err_password), new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    finish();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                VoidQRAlipayTrans voidQRAlipayTrans = new VoidQRAlipayTrans(AlipayMenuActivity.this, null);
                voidQRAlipayTrans.execute();
            }
        });

        return inputPasswordAction;
    }
}
