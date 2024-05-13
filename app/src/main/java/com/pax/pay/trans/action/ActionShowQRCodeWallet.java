package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.edc.R;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.ShowQRCodeWalletActivity;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

/**
 * Created by NANNAPHAT S on 12-Nov-18.
 */

public class ActionShowQRCodeWallet extends AAction {

    private Context context;
    private String title;
    private int typeQR;
    private TransData transData;
    private boolean isPosManualInquiry = false;
    private boolean isShowVerifyQRBtn = false;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionShowQRCodeWallet(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title, int typeQR, TransData transData) {
        this.context = context;
        this.title = title;
        this.typeQR = typeQR;
        this.transData = transData;
        this.isShowVerifyQRBtn = false;
    }

    public void setParam(Context context, String title, int typeQR, boolean isPosManualInquiry, TransData transData) {
        this.context = context;
        this.title = title;
        this.typeQR = typeQR;
        this.isPosManualInquiry = isPosManualInquiry;
        this.transData = transData;
        this.isShowVerifyQRBtn = false;
    }

    public void setParam(Context context, String title, int typeQR, boolean isPosManualInquiry, TransData transData, boolean isShowVerifyQRBtn) {
        this.context = context;
        this.title = title;
        this.typeQR = typeQR;
        this.isPosManualInquiry = isPosManualInquiry;
        this.transData = transData;
        this.isShowVerifyQRBtn = isShowVerifyQRBtn;
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, ShowQRCodeWalletActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(Utils.getString(R.string.qr_data), transData.getQrCode());
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), transData.getAmount());
        intent.putExtra(Utils.getString(R.string.linkpos_ecr_pos_manual_inquiry), isPosManualInquiry);
        intent.putExtra(EUIParamKeys.ECR_PROCESS.toString(), transData.isEcrProcess());
        intent.putExtra(Utils.getString(R.string.qr_retry_status), transData.getWalletRetryStatus() != null ? transData.getWalletRetryStatus().toString() : null);
        intent.putExtra(EUIParamKeys.QR_INQUIRY_COUNTER.toString(), Boolean.valueOf(isShowVerifyQRBtn));
//
        // intent.putExtra(Utils.getString(R.string.qr_type), transData);
        /*intent.putExtra(Utils.getString(R.string.qr_type), typeQR);
        intent.putExtra(Utils.getString(R.string.qr_trans_tid), transData.getAcquirer().getTerminalId());
        intent.putExtra(Utils.getString(R.string.qr_trans_mid), transData.getAcquirer().getMerchantId());
        intent.putExtra(Utils.getString(R.string.qr_trans_billerId), transData.getAcquirer().getBillerIdPromptPay());
        intent.putExtra(Utils.getString(R.string.qr_trans_amt), transData.getAmount());
        intent.putExtra(Utils.getString(R.string.qr_trans_datetime), transData.getDateTime());
        intent.putExtra(Utils.getString(R.string.qr_trans_biller_service_code), transData.getAcquirer().getBillerServiceCode());*/
        context.startActivity(intent);
    }
}
