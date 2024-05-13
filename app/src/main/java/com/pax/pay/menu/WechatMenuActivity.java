package com.pax.pay.menu;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.pax.pay.trans.WechatBScanCSaleTrans;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.MainActivity;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.AlipayQrSaleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.VoidQRWechatTrans;
import com.pax.pay.trans.WechatQrSaleTrans;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.Collections;

import th.co.bkkps.utils.DynamicOffline;

/**
 * Created by NANNAPHAT S on 12-Feb-19.
 */

public class WechatMenuActivity extends BaseMenuActivity {
    @Override
    protected void onResume() {
        super.onResume();

        WechatQrSaleTrans wechatQrSaleTrans = new WechatQrSaleTrans(WechatMenuActivity.this, null);
        int ermExceededResult = wechatQrSaleTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        int issuerSupportResult = wechatQrSaleTrans.IssuerSupportTransactionCheck(Collections.singletonList(Constants.ISSUER_WECHAT));
        if (issuerSupportResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(issuerSupportResult));
            return;
        }
    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(WechatMenuActivity.this, 4, 2);
        builder.addTransItem("Wechat", R.drawable.icon_wechatpay, new WechatQrSaleTrans(WechatMenuActivity.this,null));

        //B scan C
        if(Utils.isEnableWechatBscanC()){
            builder.addTransItem("B SCAN C", R.drawable.icon_wechatpay, new WechatBScanCSaleTrans(WechatMenuActivity.this, null));
        }

        builder.addActionItem("QR Void", R.drawable.app_sale, createInputPwdActionVoidQRWechat());
                //.addTransItem("QR Void", R.drawable.app_sale, new VoidQRWechatTrans(WechatMenuActivity.this, null));
        return builder.create();
    }

    private AAction createInputPwdActionVoidQRWechat() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(WechatMenuActivity.this, 8,
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
                    DialogUtils.showErrMessage(WechatMenuActivity.this, getString(R.string.menu_manage),
                            getString(R.string.err_password), new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    finish();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                VoidQRWechatTrans voidQRWechatTrans = new VoidQRWechatTrans(WechatMenuActivity.this, null);
                voidQRWechatTrans.execute();
            }
        });

        return inputPasswordAction;
    }
}
