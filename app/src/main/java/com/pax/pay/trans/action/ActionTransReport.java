package com.pax.pay.trans.action;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.HyperComMsg;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.record.TransQueryActivity;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.DialogUtils;

import java.util.List;

public class ActionTransReport extends AAction {
    private Context context;
    private String nii;
    private ActionResult actionResult;
    private boolean isEcrProcess;

    public ActionTransReport(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String nii, boolean isEcrProcess) {
        this.context = context;
        this.nii = nii;
        this.isEcrProcess = isEcrProcess;
        EcrData.instance.nDateTime = null;
        EcrData.instance.nBatchTotalSalesCount = 0;
        EcrData.instance.nBatchTotalSalesAmount = 0;
    }

    @Override
    protected void process() {
        boolean isSkipAcquirerFoundCheck = false;
        if (isEcrProcess
                && FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass
                && HyperComMsg.instance.data_field_HN_nii.equals("999")) {
            isSkipAcquirerFoundCheck = true;
        }

        if (!isSkipAcquirerFoundCheck) {
            boolean isHostFound = false;
            List<Acquirer> acquirerList = FinancialApplication.getAcqManager().findEnableAcquirers();
            for (Acquirer acq : acquirerList) {
                if (nii.contains(acq.getNii())) {
                    isHostFound = true;
                    break;
                }
            }

            if (!isHostFound) {
                EcrData.instance.nDateTime = null;
                actionResult = new ActionResult(TransResult.ERR_HOST_NOT_FOUND, null);
                if (FinancialApplication.getEcrProcess() != null)
                    EcrData.instance.setEcrData(null, nii, actionResult);
                DialogUtils.showErrMessage(context, Utils.getString(R.string.menu_report),
                        TransResultUtils.getMessage(TransResult.ERR_HOST_NOT_FOUND),
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                setResult(actionResult);
                            }
                        }, Constants.FAILED_DIALOG_SHOW_TIME);
                return;
            }
        }


        Intent intent = new Intent(context, TransQueryActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), context.getString(R.string.menu_report));
        bundle.putBoolean(EUIParamKeys.NAV_BACK.toString(), true);
        bundle.putBoolean(EUIParamKeys.ECR_PROCESS.toString(), isEcrProcess);
        bundle.putString(EUIParamKeys.ECR_NII.toString(), nii);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
}
