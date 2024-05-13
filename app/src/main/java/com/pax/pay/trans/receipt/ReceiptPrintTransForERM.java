package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;

import androidx.annotation.IntDef;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import th.co.bkkps.utils.Log;

public class ReceiptPrintTransForERM extends ReceiptPrintTrans {
    private boolean isSupportESignature;
    private boolean eReceiptPreSettlement;
    private boolean isKbankWalletTxn;
    private boolean printOneSlip;
    private @PrintType int printType;

    @IntDef({PrintType.DEFAULT, PrintType.SMARTPAY, PrintType.REDEEM, PrintType.BAY_INSTALLMENT, PrintType.AMEX_INSTALLMENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PrintType {
        int DEFAULT = 0;//normal credit, k+, AliPay, WeChat
        int SMARTPAY = 1;
        int REDEEM = 2;
        int BAY_INSTALLMENT = 3;
        int AMEX_INSTALLMENT = 4;
    }

    public Bitmap print(@PrintType int printType, TransData transData, boolean isRePrint, boolean eReceiptPreSettlement, boolean withoutPrint, PrintListener listener) {
        this.listener = listener;
        this.isReprint = isRePrint;
        this.eReceiptPreSettlement = eReceiptPreSettlement;
        this.printType = printType;

        return print(transData, isRePrint, eReceiptPreSettlement, withoutPrint, listener);
    }

    public Bitmap print(TransData transData, boolean isRePrint, boolean eReceiptPreSettlement, boolean withoutPrint, PrintListener listener) {
        Bitmap bitmap = null;
        String acquirerName = transData.getAcquirer() != null ? transData.getAcquirer().getName() : "";
        int receiptNum = getVoucherNum(acquirerName, transData);
        if (receiptNum > 0) {
            if (!withoutPrint) {
                if (listener != null)
                    listener.onShowMessage(null, Utils.getString(R.string.wait_print));
            }

            receiptNo = 0;
            receiptNum = handleVoucherNum(transData, receiptNum);

            for (; receiptNo < receiptNum; receiptNo++) {
                bitmap = generateBitmap(transData, receiptNo, receiptNum, acquirerName);

                if (withoutPrint) {
                    return bitmap;
                }

                if (bitmap == null) {
                    break;
                }

                int ret = printBitmap(bitmap);
                if (ret == -1) {
                    break;
                }

                if (printOneSlip) {
                    break;
                }

                if (receiptNum > 1 && receiptNum - 1 != receiptNo) {
                    PrintListener.Status result = null;
                    if (listener != null) {
                        result = listener.onPrintNext(Utils.getString(R.string.receipt_dlg_title), Utils.getString(R.string.receipt_dlg_body));
                    }
                    if (result == PrintListener.Status.CANCEL) {
                        break;
                    }
                }
            }
        }
        if (listener != null) {
            listener.onEnd();
        }

        return bitmap;
    }

    @Override
    protected Bitmap generateBitmap(TransData transData, int currentReceiptNo, int receiptNum, String acquirerName) {
        switch (printType) {
            case PrintType.DEFAULT:
                return super.generateBitmap(transData, currentReceiptNo, receiptNum, acquirerName);
            case PrintType.SMARTPAY:
                return new ReceiptGeneratorInstalmentKbankTrans(transData, currentReceiptNo, receiptNum, isReprint).generateBitmap();
            case PrintType.REDEEM:
                return new ReceiptGeneratorRedeemedTrans(transData, currentReceiptNo, receiptNum, isReprint).generateBitmap();
            case PrintType.BAY_INSTALLMENT:
                return new ReceiptGeneratorInstallmentBAYTrans(transData, currentReceiptNo, receiptNum, isReprint).generateBitmap();
            case PrintType.AMEX_INSTALLMENT:
                return new ReceiptGeneratorInstalmentAmexTrans(transData, currentReceiptNo, receiptNum, isReprint).generateBitmap();
        }
        return super.generateBitmap(transData, currentReceiptNo, receiptNum, acquirerName);
    }

    @Override
    protected int getVoucherNum(String acquirerName, TransData transData) {
        int receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);

        if ((isSupportESignature = Component.isAllowSignatureUpload(transData))) {
            if (eReceiptPreSettlement) {
                receiptNum = 1;//Print only merchant copy
            } else if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN) && !isReprint) {
                if (transData.geteReceiptUploadStatus() == TransData.UploadStatus.PENDING) {
                    receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD);
                } else {
                    receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP);
                }
            }
        }

        if (!eReceiptPreSettlement) {
            if (transData.getTransType() == ETransType.KBANK_REDEEM_INQUIRY) {
                return 1;
            }

            if (isKbankWalletTxn = (Constants.ACQ_KPLUS.equals(acquirerName)
                    || Constants.ACQ_WECHAT.equals(acquirerName)
                    || Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirerName)
                    || Constants.ACQ_ALIPAY.equals(acquirerName)
                    || Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirerName)
                    || Constants.ACQ_QR_CREDIT.equals(acquirerName))) {
                receiptNum = 1;
            } else if (transData.isTxnSmallAmt() && transData.geteReceiptUploadStatus() != TransData.UploadStatus.PENDING) {
                receiptNum = isReprint && transData.getNumSlipSmallAmt() == 0 ? 2 : transData.getNumSlipSmallAmt();
            }
        }

        Log.d(TAG, "ReceiptPrintTransForERM ---- NumbOfReceipt = " + receiptNum);

        return receiptNum;
    }

    @Override
    protected int handleVoucherNum(TransData transData, int receiptNum) {
        printOneSlip = false;
        if (transData.isTxnSmallAmt() && eReceiptPreSettlement) {
            receiptNo = 0; //print only merchant copy
            printOneSlip = true;
            return 2;
        } else {
            return super.handleVoucherNum(transData, receiptNum);
        }
    }
}
