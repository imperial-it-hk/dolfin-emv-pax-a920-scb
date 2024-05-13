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
package com.pax.pay;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import th.co.bkkps.utils.Log;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;
import com.pax.abl.core.AAction;
import com.pax.abl.core.AAction.ActionEndListener;
import com.pax.abl.core.AAction.ActionStartListener;
import com.pax.abl.core.ATransaction.TransEndListener;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.*;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.Printer;
import com.pax.pay.service.ParseResp;
import com.pax.pay.trans.*;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionUpdateParam;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SettingsActivity;
import com.pax.settings.SysParam;

import java.lang.ref.WeakReference;

import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.INSERT;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.KEYIN;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.SWIPE;
import static com.pax.pay.trans.action.ActionSearchCard.SearchMode.WAVE;

public class PaymentActivity extends AppCompatActivity {

    public static final int REQ_SELF_TEST = 1;

    private static final String TAG = "EDC PAYMENT";
    public static final String TAG_EXIT = "EXIT";

    private boolean needSelfTest = true;

    private int commandType;
    private BaseRequest request = null;

    private TransEndListener endListener = new TransEndListener() {

        @Override
        public void onEnd(ActionResult result) {
            transFinish(result);
        }
    };

    private TransEndListener settleEndListener = new TransEndListener() {


        @Override
        public void onEnd(final ActionResult result) {
            if (result.getRet() != TransResult.SUCC) {
                transFinish(result);
                return;
            }

            ActionUpdateParam actionUpdateParam = new ActionUpdateParam(new ActionStartListener() {
                @Override
                public void onStart(AAction action) {
                    ((ActionUpdateParam) action).setParam(ActivityStack.getInstance().top(), false);
                }
            });
            actionUpdateParam.setEndListener(new ActionEndListener() {
                @Override
                public void onEnd(AAction action, ActionResult result1) {
                    transFinish(result);
                }
            });
            actionUpdateParam.execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_null);
        super.onCreate(savedInstanceState);
        WeakReference<PaymentActivity> weakReference = new WeakReference<>(this);
        ActivityStack.getInstance().push(weakReference.get());
        onCheckArgs();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onExit(intent);
    }

    private boolean onExit(Intent intent) {
        if (intent != null) {
            boolean isExit = intent.getBooleanExtra(TAG_EXIT, false);
            if (isExit) {
                transFinish(new ActionResult(TransResult.ERR_ABORTED, null));
                return true;
            }
        }
        return false;
    }

    private void onCheckArgs() {
        if (getIntent() != null) {
            if (onExit(getIntent())) {
                return;
            }
            request = MessageUtils.generateRequest(getIntent());
            if (request == null || !MessageUtils.checkArgs(request)) {
                transFinish(new ActionResult(TransResult.ERR_PARAM, null));
                return;
            }
            commandType = MessageUtils.getType(request);

            if (getIntent().getData() == null) {
                transFinish(new ActionResult(TransResult.ERR_PARAM, null));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityStack.getInstance().popTo(this);
        if (needSelfTest)
            SelfTestActivity.onSelfTest(PaymentActivity.this, REQ_SELF_TEST);
        //FinancialApplication.getSysParam().init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_SELF_TEST:
                if(resultCode == RESULT_CANCELED){
                    transFinish(new ActionResult(TransResult.ERR_ABORTED, null));
                    return;
                }
                onHandleSelfTest(resultCode, data);
                break;
            case Constants.REQUEST_CODE:
                transFinish(new ActionResult(TransResult.SUCC, null));
                return;
            default:
                finish();
                break;
        }
    }

    private void onHandleSelfTest(int resultCode, Intent data) {
        needSelfTest = false;
        Controller controller = FinancialApplication.getController();
        int cnt = 0;
        while (controller.get(Controller.NEED_DOWN_AID) == Controller.Constant.YES && controller.get(Controller.NEED_DOWN_CAPK) == Controller.Constant.YES) {
            SystemClock.sleep(500);
            ++cnt;
            if (cnt > 3) {
                transFinish(new ActionResult(TransResult.ERR_PARAM, null));
                return;
            }
        }
        doTrans();
    }

    private void doTrans() {
        ActivityStack.getInstance().popTo(this);
        try {
            switch (commandType) {
                case Constants.PRE_AUTH:
                    doAuth((PreAuthMsg.Request) request);
                    break;
                case Constants.SALE:
                    doSale((SaleMsg.Request) request);
                    break;
                case Constants.VOID:
                    doVoid((VoidMsg.Request) request);
                    break;
                case Constants.REFUND:
                    doRefund((RefundMsg.Request) request);
                    break;
                case Constants.SETTLE:
                    doSettle();
                    break;
                case Constants.REPRINT_TRANS:
                    doReprintTrans((ReprintTransMsg.Request) request);
                    break;
                case Constants.REPRINT_TOTAL:
                    doReprintTotal((ReprintTotalMsg.Request) request);
                    break;
                default:
                    throw new Exception("wrong processing");
            }
        } catch (Exception e) {
            Log.w(TAG, "", e);
            transFinish(new ActionResult(TransResult.ERR_PARAM, null));
        }
    }

    private void transFinish(final ActionResult result) {
        Intent intent = new Intent();
        BaseResponse response = ParseResp.generate(commandType, result);
        if (response != null) {
            intent.putExtras(MessageUtils.toBundle(response, new Bundle()));
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
        ActivityStack.getInstance().popAll(); //finish
        TransContext.getInstance().setCurrentAction(null);
        Device.enableStatusBar(true);
        Device.enableHomeRecentKey(true);
    }

    /**
     * sale
     *
     * @param requestData
     */
    private void doSale(SaleMsg.Request requestData) {
        new SaleTrans(PaymentActivity.this, Long.toString(requestData.getAmount()), Long.toString(requestData.getTipAmount()), (byte) -1, true,
                endListener).execute();
    }

    /**
     * void
     *
     * @param requestData
     */
    private void doVoid(VoidMsg.Request requestData) {
        long voucherNo = requestData.getVoucherNo();
        if (voucherNo <= 0) {
            new SaleVoidTrans(PaymentActivity.this, endListener).execute();
        } else {
            new SaleVoidTrans(PaymentActivity.this, voucherNo, endListener).execute();
        }
    }

    /**
     * refund
     *
     * @param requestData
     */
    private void doRefund(RefundMsg.Request requestData) {
        long amount = requestData.getAmount();
        if (amount <= 0) {// enter amount
            new RefundTrans(PaymentActivity.this, (byte) (SWIPE | INSERT | KEYIN | WAVE), true, endListener).execute();
        } else {
            new RefundTrans(PaymentActivity.this, Long.toString(amount), (byte) (SWIPE | INSERT | KEYIN | WAVE), endListener).execute();
        }
    }

    /**
     * pre-authorization
     *
     * @param requestData
     */
    private void doAuth(PreAuthMsg.Request requestData) {
        new PreAuthTrans(PaymentActivity.this, Long.toString(requestData.getAmount()), endListener).execute();
    }

    /**
     * settle
     */
    private void doSettle() {
        new SettleTrans(PaymentActivity.this, settleEndListener).execute();
    }

    /**
     * reprint the last transaction, or reprint the transaction by transNo
     *
     * @param requestData
     */
    private void doReprintTrans(ReprintTransMsg.Request requestData) {
        if (requestData.getVoucherNo() <= 0) {
            doPrnLast();
        } else {
            doPrnTransByTransNo(requestData.getVoucherNo());
        }
    }

    /**
     * print last transaction
     */
    private void doPrnLast() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int result = Printer.printLastTrans(PaymentActivity.this);

                transFinish(new ActionResult(result, null));
            }
        }).start();
    }

    private void doPrnTransByTransNo(long transNo) {
        final TransData transData = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(transNo, true);

        if (transData == null) {
            transFinish(new ActionResult(TransResult.ERR_NO_ORIG_TRANS, null));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Printer.printTransAgain(PaymentActivity.this, transData);
                transFinish(new ActionResult(TransResult.SUCC, null));
            }
        }).start();
    }

    private void doReprintTotal(ReprintTotalMsg.Request requestData) {
        switch (requestData.getReprintType()) {
            case ReprintTotalMsg.Request.SUMMARY:
                doPrnTotal();
                break;
            case ReprintTotalMsg.Request.DETAIL:
                doPrnDetail();
                break;
            case ReprintTotalMsg.Request.LAST_SETTLE:
                doPrnLastBatch();
                break;
            default:
                break;
        }
    }

    /**
     * print detail
     */
    private void doPrnDetail() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int result = Printer.printTransDetail(getString(R.string.print_history_detail),
                        PaymentActivity.this, FinancialApplication.getAcqManager().getCurAcq());

                transFinish(new ActionResult(result, null));
            }
        }).start();
    }

    /**
     * print total
     */
    private void doPrnTotal() {
        //FIXME may have bug the getCurAcq
        new Thread(new Runnable() {
            @Override
            public void run() {
                Printer.printTransTotal(PaymentActivity.this, FinancialApplication.getAcqManager().getCurAcq());

                transFinish(new ActionResult(TransResult.SUCC, null));
            }
        }).start();
    }

    /**
     * print last batch
     */
    private void doPrnLastBatch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int result = Printer.printLastBatch(PaymentActivity.this, null, TransContext.getInstance().getCurrentAction());

                transFinish(new ActionResult(result, null));
            }
        }).start();
    }

    /**
     * terminal setting
     */
    private void doSetting() {
        ActionInputPassword inputPasswordAction = new ActionInputPassword(new ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(PaymentActivity.this, 8,
                        getString(R.string.prompt_sys_pwd), null);
            }
        });

        inputPasswordAction.setEndListener(new ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {

                if (result.getRet() != TransResult.SUCC) {
                    transFinish(result);
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_SYS_PWD))) {
                    transFinish(new ActionResult(TransResult.ERR_PASSWORD, null));
                    return;
                }

                Intent intent = new Intent(PaymentActivity.this, SettingsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EUIParamKeys.NAV_TITLE.toString(), getString(R.string.settings_title));
                bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
                intent.putExtras(bundle);
                startActivityForResult(intent, Constants.REQUEST_CODE);
            }
        });

        inputPasswordAction.execute();
    }
}
