package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.ShowQRCodeActivity;
import com.pax.pay.trans.action.activity.ShowQROnlyActivity;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

/**
 * Created by SORAYA S on 30-Jan-18.
 */

public class ActionShowQRCode extends AAction {

    private Context context;
    private String title;
    private int typeQR;
    private TransData transData;
    private boolean IsOnlyDisplayQr;
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionShowQRCode(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, int typeQR, TransData transData, boolean IsOnlyDisplayQr) {
        this.context = context;
        this.title = title;
        this.typeQR = typeQR;
        this.transData = transData;
        this.IsOnlyDisplayQr = IsOnlyDisplayQr;
    }

    @Override
    protected void process() {
        if (IsOnlyDisplayQr) {
            Intent intent = new Intent(context, ShowQROnlyActivity.class);
            intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
            intent.putExtra(Utils.getString(R.string.qr_code_info), transData.getQrCode());
            intent.putExtra(Utils.getString(R.string.qr_id), transData.getQrID());
            intent.putExtra(Utils.getString(R.string.qr_trans_tid), transData.getAcquirer().getTerminalId());
            intent.putExtra(Utils.getString(R.string.qr_trans_mid), transData.getAcquirer().getMerchantId());
            intent.putExtra(Utils.getString(R.string.qr_trans_amt), transData.getAmount());
            intent.putExtra(Utils.getString(R.string.qr_trans_datetime), transData.getDateTime());
            intent.putExtra(Utils.getString(R.string.qr_trans_biller_service_code), transData.getAcquirer().getBillerServiceCode());
            intent.putExtra(Utils.getString(R.string.qr_trans_biller_service_code), transData.getAcquirer().getBillerServiceCode());
            intent.putExtra(Utils.getString(R.string.acquirer), transData.getAcquirer().getName());
            context.startActivity(intent);
        }else {
            Intent intent = new Intent(context, ShowQRCodeActivity.class);
            intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
            intent.putExtra(Utils.getString(R.string.qr_type), typeQR);
            intent.putExtra(Utils.getString(R.string.qr_trans_tid), transData.getAcquirer().getTerminalId());
            intent.putExtra(Utils.getString(R.string.qr_trans_mid), transData.getAcquirer().getMerchantId());
            intent.putExtra(Utils.getString(R.string.qr_trans_billerId), transData.getAcquirer().getBillerIdPromptPay());
            intent.putExtra(Utils.getString(R.string.qr_trans_amt), transData.getAmount());
            intent.putExtra(Utils.getString(R.string.qr_trans_datetime), transData.getDateTime());
            intent.putExtra(Utils.getString(R.string.qr_trans_biller_service_code), transData.getAcquirer().getBillerServiceCode());
            intent.putExtra(Utils.getString(R.string.acquirer), transData.getAcquirer().getName());
            context.startActivity(intent);
        }
    }
}
