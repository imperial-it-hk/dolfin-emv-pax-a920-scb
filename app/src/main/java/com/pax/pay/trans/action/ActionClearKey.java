package com.pax.pay.trans.action;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.EPedType;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;
import com.pax.view.dialog.CustomAlertDialog;
import com.pax.view.dialog.DialogUtils;

import java.util.List;

public class ActionClearKey extends AAction {
    private Context context;
    private String title;

    private TextView resultView;
    private EPedType type;

    public ActionClearKey(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title) {
        this.context = context;
        this.title = title;
    }

    void ClearKey()
    {
        List<Acquirer> listAcquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        for (Acquirer acq : listAcquirers) {
            acq.setTMK(null);
            acq.setTWK(null);
            FinancialApplication.getAcqManager().updateAcquirer(acq);
        }

        Device.eraseKeys();
    }

    @Override
    protected void process() {
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                DialogUtils.showConfirmDialog(context, context.getString(R.string.trans_clear_key), new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        alertDialog.dismiss();
                        setResult(new ActionResult(TransResult.ERR_ABORTED, null));
                    }
                }, new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        alertDialog.dismiss();
                        ClearKey();
                        setResult(new ActionResult(TransResult.SUCC, null));
                    }
                });
            }
        });
    }
}
