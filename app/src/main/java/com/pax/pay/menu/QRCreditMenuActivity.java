package com.pax.pay.menu;

import android.content.Context;
import android.content.DialogInterface;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.TransDataDb;
import com.pax.pay.trans.QRCreditSaleTrans;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.VoidQRCreditTrans;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.settings.SysParam;
import com.pax.view.MenuPage;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import th.co.bkkps.utils.Log;

public class QRCreditMenuActivity extends BaseMenuActivity {
    private static final String QR_CREDIT_FORCE_SETTLEMENT_TAG = "FORCE-SETTLE-TIME";

    @Override
    protected void onResume() {
        super.onResume();

        Acquirer acquirer = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_QR_CREDIT);
        QRCreditSaleTrans qrCreditSaleTrans = new QRCreditSaleTrans(QRCreditMenuActivity.this, null);
        int ermExceededResult = qrCreditSaleTrans.ErmLimitExceedCheck();
        if (ermExceededResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(ermExceededResult));
            return;
        }

        int qrcMissingConfigResult = qrCreditSaleTrans.checkQrcConfigMissing(QRCreditMenuActivity.this, acquirer);
        if (qrcMissingConfigResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(qrcMissingConfigResult));
            return;
        }

        int qrcForcedSettleResult = qrCreditSaleTrans.checkQrcForcedSettle(QRCreditMenuActivity.this, acquirer);
        if (qrcForcedSettleResult != TransResult.SUCC) {
            showErrorDialog(this, TransResultUtils.getMessage(qrcForcedSettleResult));
            return;
        }

    }

    private boolean isTimeToForceSettlement(int force_HH, int force_Mi, int curr_HH, int curr_Mi) {

        if(curr_HH > force_HH) {return true;}
        else if(curr_HH == force_HH && curr_Mi > force_Mi) {return true;}
        else {return false;}
    }

    @Override
    public MenuPage createMenuPage() {

        MenuPage.Builder builder = new MenuPage.Builder(QRCreditMenuActivity.this, 4, 2);
        builder.addTransItem(getString(R.string.menu_qr_credit_sale), R.drawable.app_sale, new QRCreditSaleTrans(QRCreditMenuActivity.this, null));
        //builder.addActionItem(getString(R.string.menu_qr_credit_void), R.drawable.app_void, createInputPwdActionVoidQr());
        return builder.create();
    }

    private AAction createInputPwdActionVoidQr() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                tickTimer.stop();
                ((ActionInputPassword) action).setParam(QRCreditMenuActivity.this, 8,
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
                    DialogUtils.showErrMessage(QRCreditMenuActivity.this, getString(R.string.menu_manage),
                            getString(R.string.err_password), new DialogInterface.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface arg0) {
                                    finish();
                                }
                            }, Constants.FAILED_DIALOG_SHOW_TIME);
                    return;
                }
                VoidQRCreditTrans voidQRCreditTrans = new VoidQRCreditTrans(QRCreditMenuActivity.this, null);
                voidQRCreditTrans.execute();
            }
        });

        return inputPasswordAction;
    }
}
