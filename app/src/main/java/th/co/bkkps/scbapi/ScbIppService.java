package th.co.bkkps.scbapi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.pax.abl.core.AAction;
import com.pax.abl.utils.EncUtils;
import com.pax.edc.R;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransMultiAppData;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.util.HashMap;
import java.util.Map;

import th.co.bkkps.bpsapi.BaseResponse;
import th.co.bkkps.bpsapi.ConfigMsg;
import th.co.bkkps.bpsapi.HistoryMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.bpsapi.TransResponse;
import th.co.bkkps.bpsapi.TransResult;

public class ScbIppService {

    private static final Map<Integer, TransData.EnterMode> enterModeMap = new HashMap<>();
    private static final String PACKAGE_NAME = "th.co.bps.scb.installment";

    static {
        enterModeMap.put(1, TransData.EnterMode.SWIPE);
        enterModeMap.put(2, TransData.EnterMode.INSERT);
        enterModeMap.put(3, TransData.EnterMode.CLSS);
        enterModeMap.put(4, TransData.EnterMode.MANUAL);
        enterModeMap.put(5, TransData.EnterMode.FALLBACK);
    }

    public static void insertTransData(TransData transData, TransResponse response) {
        if (transData != null && response != null) {
            if (response.getRspCode() == TransResult.SUCC) {
                transData.setDateTime(response.getTransTime());
                transData.setStanNo(response.getStanNo());
                transData.setTraceNo(response.getVoucherNo());
                transData.setBatchNo(response.getBatchNo());
                transData.setPan(response.getCardNo());
                transData.setEnterMode(enterModeMap.get(response.getCardType()));
                transData.setAmount(response.getAmount());
                transData.setAuthCode(response.getAuthCode());
                transData.setRefNo(response.getRefNo());
                transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
                transData.setIssuer(FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_SCB_IPP));
                transData.setAcquirer(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP));
                FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                TransMultiAppData transMultiAppData = new TransMultiAppData();
                transMultiAppData.setStanNo(response.getStanNo());
                transMultiAppData.setTraceNo(response.getVoucherNo());
                transMultiAppData.setAcquirer(transData.getAcquirer());
                FinancialApplication.getTransMultiAppDataDbHelper().insertTransData(transMultiAppData);
                //TODO-change logic to get last trace/stan from multi app and update to main app
                Component.incStanNo(transData);
                Component.incTraceNo(transData);
            } else {
                updateEdcTraceStan(response);
            }
        }
    }

    public static void insertRedeemTransData(TransData transData, TransResponse response) {
        if (transData != null && response != null) {
            if (response.getRspCode() == TransResult.SUCC) {
                transData.setDateTime(response.getTransTime());
                transData.setStanNo(response.getStanNo());
                transData.setTraceNo(response.getVoucherNo());
                transData.setBatchNo(response.getBatchNo());
                transData.setPan(response.getCardNo());
                transData.setEnterMode(enterModeMap.get(response.getCardType()));
                transData.setAmount(response.getAmount());
                transData.setRedeemedAmount(response.getRedeemAmt());
                transData.setRedeemPoints(response.getRedeemPts());
                transData.setProductQty(Utils.parseIntSafe(response.getRedeemQty(), 0));
                transData.setProductCode(response.getRedeemCode());
                transData.setAuthCode(response.getAuthCode());
                transData.setRefNo(response.getRefNo());
                transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
                transData.setIssuer(FinancialApplication.getAcqManager().findIssuer(Constants.ISSUER_SCB_REDEEM));
                transData.setAcquirer(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_REDEEM));
                FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                TransMultiAppData transMultiAppData = new TransMultiAppData();
                transMultiAppData.setStanNo(response.getStanNo());
                transMultiAppData.setTraceNo(response.getVoucherNo());
                transMultiAppData.setAcquirer(transData.getAcquirer());
                FinancialApplication.getTransMultiAppDataDbHelper().insertTransData(transMultiAppData);
                //TODO-change logic to get last trace/stan from multi app and update to main app
                Component.incStanNo(transData);
                Component.incTraceNo(transData);
            } else {
                updateEdcTraceStan(response);
            }
        }
    }

    public static boolean isSCBInstalled(Context context){
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return packageInfo != null;
    }

    public static void updateEdcTraceStan(BaseResponse response) {
        if (response != null) {
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) response.getStanNo(), true);
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) response.getVoucherNo(), true);
        }
    }

    public static void updateBatchNo(BaseResponse response) {
        Acquirer acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_SCB_IPP);
        acq.setCurrBatchNo((int) response.getBatchNo());
        FinancialApplication.getAcqManager().updateAcquirer(acq);
    }

    public static AAction executeConfigMenu(final Context context, final TickTimer tickTimer) {
        ActionInputPassword actionInputPassword = new ActionInputPassword(action -> {
            tickTimer.stop();
            ((ActionInputPassword) action).setParam(context, 6,
                    context.getString(R.string.prompt_merchant_pwd), null);
        });

        actionInputPassword.setEndListener((action, result) -> {

            if (result.getRet() != com.pax.edc.opensdk.TransResult.SUCC) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
                return;
            }

            String data = EncUtils.sha1((String) result.getData());
            if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    }
                };

                DialogUtils.showErrMessage(context, context.getString(R.string.settings_title),
                        context.getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                return;
            }

            ConfigMsg.Request request = new ConfigMsg.Request();
            TransAPIFactory.createTransAPI().startTrans(context, request);
        });

        return actionInputPassword;
    }

    public static AAction executeHistoryMenu(final Context context, final TickTimer tickTimer) {
        ActionInputPassword actionInputPassword = new ActionInputPassword(action -> {
            tickTimer.stop();
            ((ActionInputPassword) action).setParam(context, 6,
                    context.getString(R.string.prompt_merchant_pwd), null);
        });

        actionInputPassword.setEndListener((action, result) -> {

            if (result.getRet() != com.pax.edc.opensdk.TransResult.SUCC) {
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
                ActivityStack.getInstance().popTo(MainActivity.class);
                return;
            }

            String data = EncUtils.sha1((String) result.getData());
            if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_TLE_PWD))) {
                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                        TransContext.getInstance().setCurrentAction(null); //fix leaks
                        ActivityStack.getInstance().popTo(MainActivity.class);
                    }
                };

                DialogUtils.showErrMessage(context, context.getString(R.string.menu_report),
                        context.getString(R.string.err_password), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                return;
            }

            HistoryMsg.Request request = new HistoryMsg.Request();
            TransAPIFactory.createTransAPI().startTrans(context, request);
        });

        return actionInputPassword;
    }
}
