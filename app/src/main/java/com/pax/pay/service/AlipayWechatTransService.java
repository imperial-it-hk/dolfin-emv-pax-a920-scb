package com.pax.pay.service;

import androidx.annotation.IntDef;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.utils.ResponseCode;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AlipayWechatTransService {
    private TransData transData;
    private TransProcessListener listener;
    private @WalletTransType int walletTransType;

    @IntDef({WalletTransType.QR_INQUIRY, WalletTransType.SALE_INQUIRY, WalletTransType.CANCEL_INQUIRY,
            WalletTransType.REVERSAL_NORMAL, WalletTransType.REVERSAL_BYPASS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WalletTransType {
        int QR_INQUIRY = 0;
        int SALE_INQUIRY = 1;
        int CANCEL_INQUIRY = 2;
        int REVERSAL_NORMAL = 3;
        int REVERSAL_BYPASS = 4;
    }

    public AlipayWechatTransService(@WalletTransType int walletTransType, TransData transData, TransProcessListener listener) {
        this.transData = transData;
        this.listener = listener;
        this.walletTransType = walletTransType;
    }

    public int process() {
        switch (walletTransType) {
            case WalletTransType.QR_INQUIRY://get QR
                return transQRInquiry();
            case WalletTransType.SALE_INQUIRY://sale
                return transSaleInquiry();
            case WalletTransType.CANCEL_INQUIRY://void transaction
                return transCancelInquiry();
            case WalletTransType.REVERSAL_NORMAL://reversal after press "CANCEL"
                return reversalHandleCase();
            case WalletTransType.REVERSAL_BYPASS://reversal GET QR
                return reversalWithoutHandleCase();
        }
        return TransResult.ERR_NOT_SUPPORT_TRANS;
    }

    private int transQRInquiry() {
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        setTransSuccessListener(data -> {
            ResponseCode responseCode = data.getResponseCode();
            if (responseCode != null) {
                String respCode = responseCode.getCode();
                if ("00".equals(respCode)) {
                    listener.onHideProgress();
                    transData.setDupReason("");
                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                    int ret = processDE63();
                    if (ret == TransResult.SUCC) {
                        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                        return ret;
                    }
                    transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    return ret;
                } else {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                    showErrorMsgWithResponseCode(responseCode, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
            listener.onHideProgress();
            return TransResult.ERR_USER_CANCEL;
        });

        setTransFailListener(ret -> {
            listener.onHideProgress();
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK){
                return TransResult.ERR_USER_CANCEL;
            } else {
                return TransResult.ERR_NO_RESPONSE;
            }
        });

        return online(transData);
    }

    private int transSaleInquiry() {
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));

        setTransSuccessListener(data -> {
            ResponseCode responseCode = data.getResponseCode();
            if (responseCode != null) {
                String respCode = responseCode.getCode();
                switch (respCode) {
                    case "00":
                        listener.onHideProgress();
                        transData.setDupReason("");
                        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                        int ret = processDE63();
                        if (ret == TransResult.SUCC) {
                            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            return ret;
                        }
                        transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                        return ret;
                    case "99":
                        listener.onHideProgress();
                        if (!transData.getLastTrans()) {
                            showErrorMsgWithResponseCode(responseCode, 2);
                        }
                        return TransResult.ERR_WALLET_RESP_UK;
                    default:
                        FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                        showErrorMsgWithResponseCode(responseCode, Constants.FAILED_DIALOG_SHOW_TIME);
                        break;
                }
            }
            listener.onHideProgress();
            return TransResult.ERR_ABORTED;
        });

        setTransFailListener(ret -> {
            /*listener.onHideProgress();
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND){
                return ret;
            } else {
                return TransResult.ERR_NO_RESPONSE;
            }*/
            listener.onShowErrMessage(TransResultUtils.getMessage(ret), Constants.FAILED_DIALOG_SHOW_TIME, true);
            listener.onHideProgress();
            return ret;
        });

        return online(transData);
    }

    private int transCancelInquiry() {
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        setTransSuccessListener(data -> {
            ResponseCode responseCode = data.getResponseCode();
            if (responseCode != null) {
                String respCode = responseCode.getCode();
                if ("00".equals(respCode)) {
                    listener.onHideProgress();
                    transData.setDupReason("");
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    return TransResult.SUCC;
                } else {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                    showErrorMsgWithResponseCode(responseCode, Constants.FAILED_DIALOG_SHOW_TIME);
                }
            }
            listener.onHideProgress();
            return TransResult.ERR_USER_CANCEL;
        });

        setTransFailListener(ret -> {
            listener.onHideProgress();
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer()); //no reversal for void trans.
            return TransResult.ERR_USER_CANCEL;
        });

        return online(transData);
    }

    private int reversalWithoutHandleCase() {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordQR(transData.getAcquirer());
        if (dupTransData == null) {
            return TransResult.SUCC;
        }

        initialReversal(dupTransData);

        setTransSuccessListener(data -> {
            listener.onHideProgress();
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(data.getAcquirer());
            return TransResult.ERR_USER_CANCEL;
        });

        setTransFailListener(ret -> {
            listener.onHideProgress();
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
            return TransResult.ERR_USER_CANCEL;
        });

        return online(dupTransData);
    }

    private int reversalHandleCase() {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordQR(transData.getAcquirer());
        if (dupTransData == null) {
            return TransResult.SUCC;
        }

        initialReversal(dupTransData);

        setTransSuccessListener(data -> {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(data.getAcquirer());
            /*ResponseCode responseCode = data.getResponseCode();
            if (responseCode != null && "00".equals(responseCode.getCode())) {
                listener.onHideProgress();
                return TransResult.ERR_USER_CANCEL;
            } else {
                listener.onShowErrMessage(Utils.getString(R.string.pls_contact_kbiz), Constants.FAILED_DIALOG_SHOW_TIME, true);
                listener.onHideProgress();
            }
            return TransResult.ERR_ABORTED;*/
            listener.onHideProgress();
            return TransResult.ERR_USER_CANCEL;
        });

        setTransFailListener(ret -> {
            FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
            listener.onShowErrMessage(Utils.getString(R.string.pls_contact_kbiz), Constants.FAILED_DIALOG_SHOW_TIME, true);
            listener.onHideProgress();
            return TransResult.ERR_ABORTED;
        });

        return online(dupTransData);
    }

    private void initialReversal(TransData dupTransData) {
        long transNo = dupTransData.getTraceNo();
        long stanNo = dupTransData.getStanNo();
        String dupReason = dupTransData.getDupReason();

        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setStanNo(stanNo);
        dupTransData.setTraceNo(transNo);
        dupTransData.setDupReason(dupReason);
        dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }
    }

    private int online(TransData onlineTransData) {
        Online online = new WalletOnline();
        int ret = online.online(onlineTransData, listener);
        if (ret == TransResult.SUCC) {
            return transSuccessListener.onSuccess(onlineTransData);
        } else {
            if (walletTransType != WalletTransType.REVERSAL_BYPASS
                    && walletTransType != WalletTransType.REVERSAL_NORMAL) {
                transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            }
            return transFailListener.onFail(ret);
        }
    }

    private class WalletOnline extends Online {
        @Override
        public void initReversalStatus(TransData transData) {
            if (dupPackager != null
                    && transData.getReversalStatus() == TransData.ReversalStatus.NORMAL
                    && transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE
                    && transData.getReferralStatus() != TransData.ReferralStatus.REFERRED
                    && transData.getTransType() != ETransType.PROMPT_ADV) {

                transData.setReversalStatus(TransData.ReversalStatus.PENDING);

                if (walletTransType == WalletTransType.QR_INQUIRY
                        || walletTransType == WalletTransType.CANCEL_INQUIRY)
                    FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                else
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
            }
        }
    }

    private int processDE63() {
        if (transData.getField63RecByte() != null) {
            Component.splitField63Wallet(transData, transData.getField63RecByte());
            return TransResult.SUCC;
        }
        return TransResult.ERR_NO_RESPONSE;
    }

    private void showErrorMsgWithResponseCode(ResponseCode responseCode, int timeout) {
//        if (("LE".equals(responseCode.getCode()) || "ER".equals(responseCode.getCode())) && transData.getField63RecByte() != null) {
//            listener.onShowErrMessage(Utils.getString(R.string.prompt_err_code) + transData.getResponseCode().getCode() + "\n" +
//                    transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
        if ("99".equals(responseCode.getCode()) && transData.getField60RecByte() != null) {
            listener.onShowErrMessage(Utils.getString(R.string.prompt_err_code) + transData.getResponseCode().getCode() + "\n" +
                    new String(transData.getField60RecByte()), timeout, true);
        } else {
            listener.onShowErrMessage(responseCode.getMessage(), timeout, true);
        }
    }

    private interface TransSuccessListener {
        int onSuccess(TransData lTransData);
    }

    private interface TransFailListener {
        int onFail(int ret);
    }

    private TransSuccessListener transSuccessListener;
    private TransFailListener transFailListener;

    private void setTransSuccessListener(TransSuccessListener transSuccessListener) {
        this.transSuccessListener = transSuccessListener;
    }

    private void setTransFailListener(TransFailListener transFailListener) {
        this.transFailListener = transFailListener;
    }
}
