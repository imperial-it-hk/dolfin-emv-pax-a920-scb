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
package com.pax.pay.trans.transmit;


import android.util.Log;

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.ECR.LawsonHyperCommClass;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.ResponseCode;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import static com.pax.pay.trans.model.ETransType.GET_QR_ALIPAY;
import static com.pax.pay.trans.model.ETransType.GET_QR_CREDIT;
import static com.pax.pay.trans.model.ETransType.GET_QR_KPLUS;
import static com.pax.pay.trans.model.ETransType.GET_QR_WECHAT;
import static com.pax.pay.trans.model.ETransType.QR_INQUIRY;
import static com.pax.pay.trans.model.ETransType.QR_VOID_ALIPAY;
import static com.pax.pay.trans.model.ETransType.QR_VOID_CREDIT;
import static com.pax.pay.trans.model.ETransType.QR_VOID_KPLUS;
import static com.pax.pay.trans.model.ETransType.QR_VOID_WECHAT;
import static com.pax.pay.trans.model.ETransType.QR_MYPROMPT_SALE;
import static com.pax.pay.trans.model.ETransType.QR_MYPROMPT_VOID;

import java.util.Arrays;

public class Transmit {

    private Online online = new Online();

    public int transmit(TransData transData, TransProcessListener listener) {
        int ret = 0;
        int i = 0;
        ETransType transType = transData.getTransType();

        if (Component.isAllowDynamicOffline(transData)) {
            return TransResult.OFFLINE_APPROVED;
        }

        // 处理冲正
        if (transType.isDupSendAllowed()) {
            ret = sendReversal(listener, transData.getAcquirer());
            if (ret != 0) {
                i = 3;
            } else {
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            }
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }


        // 只有平台返回密码错时， 才会下次循环
        for (int j = i; j < 3; j++) {
            if (j != 0) {
                // 输入密码
                if (listener != null) {
                    ret = listener.onInputOnlinePin(transData);
                    if (ret != 0) {
                        return TransResult.ERR_ABORTED;
                    }
                } else {
                    return TransResult.ERR_HOST_REJECT;
                }
                transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            }
            if (listener != null) {
                listener.onUpdateProgressTitle(transType.getTransName());
            }

            ret = online.online(transData, listener);
            if (ret == TransResult.SUCC) {
                ResponseCode responseCode = transData.getResponseCode();
                String retCode = responseCode.getCode();

                if ("00".equals(retCode)) {
                    // write transaction record
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setDupReason("");
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    return TransResult.SUCC;
                } else {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                    if (transType == ETransType.KBANK_DCC_GET_RATE) {
                        return TransResult.ERR_HOST_REJECT;
                    }

                    if ("55".equals(retCode)) {
                        if (listener != null) {
                            if (FinancialApplication.getAcqManager().getCurAcq().isEnableUpi()) {
                                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                                return TransResult.ERR_HOST_REJECT;
                            }
                            listener.onShowErrMessage(
                                    Utils.getString(R.string.err_password_reenter),
                                    Constants.FAILED_DIALOG_SHOW_TIME, true);
                        }
                        continue;
                    }
                    if (listener != null) {

                        if (transType == ETransType.LOAD_UPI_RSA) {
                            return TransResult.ERR_UPI_LOAD;
                        } else if (transType == ETransType.LOAD_UPI_TWK) {
                            return TransResult.ERR_UPI_LOGON;
                        } else {
                            // EDCBBLAND-383 [Change Request]Error message mapping with S500
                            if ("LE".equals(retCode)) {
                                listener.onShowErrMessage(Component.unpackBit63Credit(transData.getField63RecByte(), transData.getField63()), Constants.FAILED_DIALOG_SHOW_TIME, true);
                            } else {
                                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                            }

                            if (((transData.getIssuer() != null && transData.getIssuer().isReferral()) || Constants.ACQ_AMEX.equals(transData.getAcquirer())) && "01".equals(retCode)) {
                                //For AMEX or another issuers that are set Referral, when transaction is referred by host, need to do referred transaction
                                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                                transData.setDupReason("");
                                return TransResult.ERR_REFERRAL_CALL_ISSUER;
                            }
                        }
                    }
                    return TransResult.ERR_HOST_REJECT;
                }
            } else if (transData.getIssuer() != null && (Utils.parseLongSafe(transData.getAmount(), 0) <= transData.getIssuer().getFloorLimit())) {
                transData.setOrigAuthCode(Utils.getString(R.string.response_Y3_str));
                transData.setAuthCode(Utils.getString(R.string.response_Y3_str));
                // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                return TransResult.OFFLINE_APPROVED;
                //return EOnlineResult.FAILED;
            }

            if (transType != ETransType.LOADTMK && transType != ETransType.LOADTWK) {
                //send auto reversal
                if (sendAutoReversal(ret, transData, listener)) {
                    return TransResult.ERR_PROCESS_FAILED;
                }
            }

            break;
        }
        if (i == 3) {
            return TransResult.ERR_ABORTED;
        }

        return ret;
    }

    private boolean sendAutoReversal(int retCode, TransData transData, TransProcessListener listener) {
        TransData dupTransData;

        //Find trans that reversal status is pending.
        Boolean isNotNormalTrans = null;
        switch (transData.getAcquirer().getName()) {
            case Constants.ACQ_WALLET:
                isNotNormalTrans = true;
                dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordWallet();
                break;
            default:
                dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(transData.getAcquirer());
                break;
        }

        if (dupTransData != null && transData.getIssuer() != null && transData.getIssuer().isAutoReversal()) {
            listener.onShowErrMessage(TransResultUtils.getMessage(retCode), Constants.FAILED_DIALOG_SHOW_TIME, true);
            if (isNotNormalTrans != null) {
                if (isNotNormalTrans) {//true - Wallet
                    sendReversalWallet(listener);
                    return true;
                }
                //sendReversalLinePay(listener);//false - LinePay
            } else {
                sendReversal(listener, transData.getAcquirer());
            }
            return true;
        }

        return false;
    }

    /**
     * 冲正处理
     *
     * @return
     */
    public int sendReversal(TransProcessListener listener, Acquirer acquirer) {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecord(acquirer);
        if (dupTransData == null) {
            return TransResult.SUCC;
        }
        int ret = 0;
        long transNo = dupTransData.getTraceNo();
        long stanNo = dupTransData.getStanNo();
        String dupReason = dupTransData.getDupReason();
        Acquirer acq = dupTransData.getAcquirer();

        ETransType transType = dupTransData.getTransType();
        if (transType == ETransType.VOID) {
            dupTransData.setOrigAuthCode(dupTransData.getOrigAuthCode());
        } else {
            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
        }

        Component.transInit(dupTransData);

        dupTransData.setStanNo(stanNo);//for reversal, send original stan
        dupTransData.setTraceNo(transNo);
        dupTransData.setDupReason(dupReason);
        dupTransData.setAcquirer(acq);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);
        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecord(acq);
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecord();

        return ret;
    }

    /**
     * Send TC-ADVICE by acquirer
     *
     * @param acquirer
     * @param listener
     * @return
     */
    public int sendTcAdvice(TransData originalTransData, Acquirer acquirer, TransProcessListener listener) {
        if (originalTransData == null) {
            return TransResult.SUCC;
        }
        TransData dupTransData = new TransData();
        Component.transInit(dupTransData, acquirer);
        dupTransData.setTransType(ETransType.TCADVICE);

        dupTransData.setPan(originalTransData.getPan());// field 2
        dupTransData.setAmount(originalTransData.getAmount());// field 4
        dupTransData.setDateTime(originalTransData.getDateTime());// field 12-13
        dupTransData.setExpDate(originalTransData.getExpDate());// field 14
        dupTransData.setEnterMode(originalTransData.getEnterMode());// field 22
        dupTransData.setCardSerialNo(originalTransData.getCardSerialNo());// field 23
        dupTransData.setOrigRefNo(originalTransData.getRefNo());// field 37
        dupTransData.setOrigAuthCode(originalTransData.getAuthCode());// field 38
        dupTransData.setResponseCode(originalTransData.getResponseCode());// field 39
        dupTransData.setSendIccData(originalTransData.getSendIccData());// field 55
        dupTransData.setOrigTransType(originalTransData.getTransType());// field 60
        dupTransData.setOrigTransNo(originalTransData.getStanNo());// field 60
        dupTransData.setTraceNo(originalTransData.getTraceNo()); // field 62

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice));
        }

        dupTransData.setAdviceStatus(TransData.AdviceStatus.ADVICE);

        int ret = online.online(dupTransData, listener);
        if (ret == TransResult.SUCC) {
            //For TC-Advice, no need to check response code
            return TransResult.SUCC;
        }

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }

            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_tcadvice),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }

        return ret;
    }

    /**
     * Send TC-ADVICE by acquirer
     *
     * @param acquirer
     * @param listener
     * @return
     */
    public int sendTcAdviceInstalmentKbank(TransData originalTransData, Acquirer acquirer, TransProcessListener listener) {
        if (originalTransData == null) {
            return TransResult.SUCC;
        }
        TransData dupTransData = new TransData();
        Component.transInit(dupTransData, acquirer);
        dupTransData.setTransType(ETransType.KBANK_SMART_PAY_TCADVICE);

        dupTransData.setIssuer(originalTransData.getIssuer());
        dupTransData.setPan(originalTransData.getPan());// field 2
        dupTransData.setAmount(originalTransData.getAmount());// field 4
        dupTransData.setDateTime(originalTransData.getDateTime());// field 12-13
        dupTransData.setExpDate(originalTransData.getExpDate());// field 14
        dupTransData.setEnterMode(originalTransData.getEnterMode());// field 22
        dupTransData.setCardSerialNo(originalTransData.getCardSerialNo());// field 23
        dupTransData.setOrigRefNo(originalTransData.getRefNo());// field 37
        dupTransData.setOrigAuthCode(originalTransData.getAuthCode());// field 38
        dupTransData.setResponseCode(originalTransData.getResponseCode());// field 39
        dupTransData.setSendIccData(originalTransData.getSendIccData());// field 55

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice));
        }

        dupTransData.setAdviceStatus(TransData.AdviceStatus.ADVICE);

        int ret = online.online(dupTransData, listener);
        if (ret == TransResult.SUCC) {
            //For TC-Advice, no need to check response code
            return TransResult.SUCC;
        }

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }

            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_tcadvice),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }

        return ret;
    }

    /**
     * 脱机交易上送
     *
     * @param isOnline 是否为在下笔联机交易到来之前上送
     * @return
     */
    public int sendOfflineTrans(TransProcessListener listener, boolean isOnline, boolean isSettlement) {
        int ret = new TransOnline().offlineTransSend(listener, isOnline, isSettlement);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_ABORTED && listener != null) {
            listener.onShowErrMessage(
                    TransResultUtils.getMessage(ret),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        return ret;
    }

    public int transmitPromptPay(TransData transData, TransProcessListener listener) {
        int ret;
        ETransType transType = transData.getTransType();

        ret = sendReversal(listener, transData.getAcquirer());
        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        ret = sendAdvicePromptpay(null, listener);
        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        if (transData.getPromptPayRetry()) {
            ret = sendPromptPayTransWithRetry(transData, listener);
        } else if (transData.getTransType() == ETransType.PROMPTPAY_VOID) {
            ret = sendPromptPayVoid(transData, listener);
        } else {
            ret = sendPromptPayTrans(transData, listener);
        }

        return ret;
    }

    private int sendPromptPayTransWithRetry(TransData transData, TransProcessListener listener) {
        int ret, resTrans, i = 0;

        ETransType transType = transData.getTransType();

        while (true) {
            if (listener != null) {
                listener.onHideProgress();
                listener.onUpdateProgressTitle(transType.getTransName());
            }

            transData.setOrigField63(transData.getField63());
            ret = online.online(transData, listener);

            i++;

            if (ret == TransResult.SUCC) {
                ResponseCode responseCode = transData.getResponseCode();
                String retCode = responseCode.getCode();

                if ("00".equals(retCode)) {
                    // write transaction record
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setDupReason("");
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    return TransResult.SUCC;
                } else {
//                    if(retCode != "00"){ //"ER".equals(retCode)
                    if (i <= 4) {
                        resTrans = ret;
                        if (listener != null) {
                            ret = listener.onShowProgress(Utils.getString(R.string.err_promptpay_retry_again), transData.getAcquirer().getPromptRetryTimeout(), false, true);
                            if (ret == TransResult.ERR_USER_CANCEL) {
                                return ret;
                            }
                        }
//                            if (resTrans != TransResult.ERR_CONNECT && resTrans != TransResult.ERR_PACK && resTrans != TransResult.ERR_SEND) {
//                                Component.incStanNo(transData);
//                                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
//                            }
                        transData.setField63(transData.getOrigField63());
                        continue;
                    }
                    if (listener != null) {
                        transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                        listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
//                    }else {
//                        if(listener != null) {
//                            transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
//                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
//                            listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
//                        }
//                    }
                    return TransResult.ERR_HOST_REJECT;
                }
            }

            if (i <= 4) {
                resTrans = ret;
                if (listener != null) {
                    ret = listener.onShowProgress(Utils.getString(R.string.err_promptpay_retry_again), transData.getAcquirer().getPromptRetryTimeout(), false, true);
                    if (ret == TransResult.ERR_USER_CANCEL) {
                        return ret;
                    }
                }
//                if (resTrans != TransResult.ERR_CONNECT && resTrans != TransResult.ERR_PACK && resTrans != TransResult.ERR_SEND) {
//                    Component.incStanNo(transData);
//                    transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
//                }
                transData.setField63(transData.getOrigField63());
                continue;
            } else {
                break;
            }
        }

        return ret;
    }

    private int sendPromptPayTrans(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        transData.setOrigField63(transData.getField63());
        ret = online.online(transData, listener);

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                return TransResult.SUCC;
            } else {
                if (listener != null) {
                    transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    private int sendPromptPayVoid(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = online.online(transData, listener);
        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                initWalletAdviceStatus(transData);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                if (transType == ETransType.PROMPTPAY_VOID) {
                    ret = sendAdvicePromptpay(transData, listener);
                    if (ret != TransResult.SUCC) {
                        return TransResult.ERR_ADVICE;
                    }
                }
                return TransResult.SUCC;
            } else {
                transData.setReversalStatus(TransData.ReversalStatus.PENDING);
                if (transData.getField63() != null) {
                    listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                } else {
                    listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_HOST_REJECT;
            }
        }

        /*customize Promptpay error message*/
        if (ret == TransResult.ERR_RECV) {
            ret = TransResult.ERR_NO_RESPONSE;
        } else if (ret == TransResult.ERR_CONNECT) {
            ret = TransResult.ERR_COMMUNICATION;
        }

        return ret;
    }

    public int transmitWallet(TransData transData, TransProcessListener listener) {
        int ret;
        boolean doReversal = true;
        ETransType transType = transData.getTransType();

        if (transData.getWalletRetryStatus() == TransData.WalletRetryStatus.RETRY_CHECK) {
            doReversal = false;
        }

        if (doReversal && transType.isDupSendAllowed()) {
            ret = sendReversalWallet(listener);
            if (ret != 0) {
                return TransResult.ERR_ABORTED;
            } else {
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            }
        }

        if (doReversal) {
            ret = sendAdviceWallet(null, listener);
            if (ret != 0) {
                return TransResult.ERR_ABORTED;
            } else {
                transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            }
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = online.online(transData, listener);
        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();


            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                initWalletAdviceStatus(transData);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                if (transType == ETransType.QR_VOID_WALLET || transType == ETransType.REFUND_WALLET) {
                    ret = sendAdviceWallet(transData, listener);
                    if (ret != TransResult.SUCC) {
                        return TransResult.ERR_ADVICE;
                    }
                }
                return TransResult.SUCC;
            } else {
                if ("UK".equals(retCode) && (transType == ETransType.QR_SALE_WALLET || transType == ETransType.SALE_WALLET)) {//wallet response UK
                    initWalletRetryStatus(transData);
                    return TransResult.ERR_WALLET_RESP_UK;
                }
                FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(transData.getResponseCode().getCode())
                            && transData.getField63() != null) {
                        listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        if (ret == TransResult.ERR_RECV && transData.getWalletRetryStatus() == TransData.WalletRetryStatus.RETRY_CHECK) {
            //wallet retry check timeout
            initWalletRetryStatus(transData);
            return TransResult.ERR_WALLET_RESP_UK;
        }

        //send auto reversal
        if (sendAutoReversal(TransResult.ERR_NO_RESPONSE, transData, listener)) {
            return TransResult.ERR_PROCESS_FAILED;
        }

        /*customize Wallet error message*/
        if (ret == TransResult.ERR_RECV) {
            ret = TransResult.ERR_NO_RESPONSE;
        } else if (ret == TransResult.ERR_CONNECT) {
            ret = TransResult.ERR_COMMUNICATION;
        }

        return ret;
    }

    private void initWalletAdviceStatus(TransData transData) {
        if (transData.getAdviceStatus() != null
                && transData.getAdviceStatus() == TransData.AdviceStatus.NORMAL
                && (transData.getTransType() == ETransType.QR_VOID_WALLET
                || transData.getTransType() == ETransType.REFUND_WALLET)) {
            transData.setAdviceStatus(TransData.AdviceStatus.PENDING);
        }
    }

    private void initQrAdviceStatus(TransData transData) {
        if (transData.getAdviceStatus() != null
                && transData.getAdviceStatus() == TransData.AdviceStatus.NORMAL
                && (transData.getTransType() == ETransType.QR_VOID)) {
            transData.setAdviceStatus(TransData.AdviceStatus.PENDING);
        }
    }

    private void initWalletRetryStatus(TransData transData) {
        //if(transData.getWalletRetryStatus() != null
        ///        && transData.getWalletRetryStatus() == TransData.WalletRetryStatus.NORMAL){
        //    transData.setWalletRetryStatus(TransData.WalletRetryStatus.PENDING);
        //    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        //}
    }

    public int sendReversalMyPrompt(TransProcessListener listener) {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordMyPrompt();
        if (dupTransData == null) {
            return TransResult.SUCC;
        }
        int ret = 0;
        long stanNo = dupTransData.getStanNo();
        long traceNo = dupTransData.getTraceNo();
        String dupReason = dupTransData.getDupReason();

        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setStanNo(stanNo);
        dupTransData.setTraceNo(traceNo);
        dupTransData.setDupReason(dupReason);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);
        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
        return ret;
    }

    public int sendReversalWallet(TransProcessListener listener) {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordWallet();
        if (dupTransData == null) {
            return TransResult.SUCC;
        }
        int ret = 0;
        long stanNo = dupTransData.getStanNo();
        long traceNo = dupTransData.getTraceNo();
        String dupReason = dupTransData.getDupReason();

        /*ETransType transType = dupTransData.getTransType();
        if (transType == ETransType.VOID) {
            dupTransData.setOrigAuthCode(dupTransData.getOrigAuthCode());
        } else {
            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
        }*/
        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setStanNo(stanNo);
        dupTransData.setTraceNo(traceNo);
        dupTransData.setDupReason(dupReason);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);
        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
        return ret;
    }

    public int sendAdviceWallet(TransData dupTransData, TransProcessListener listener) {
        long orignalStan = 0;

        if (dupTransData == null) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);

            if (dupTransData == null) {
                return TransResult.SUCC;
            }

            long transNo = dupTransData.getTraceNo();
            int id = dupTransData.getId();
            String dateTime = dupTransData.getDateTime();
            TransData.ETransStatus transState = dupTransData.getTransState();

            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
            Component.transInit(dupTransData, dupTransData.getAcquirer());

            dupTransData.setTraceNo(transNo);
            dupTransData.setId(id);
            dupTransData.setDateTime(dateTime);
            dupTransData.setTransState(transState);
            orignalStan = dupTransData.getStanNo();
        } else {
            long stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
            if (stanNo == 0) {
                stanNo += 1;
                FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) stanNo);
            }
            orignalStan = dupTransData.getStanNo();
            dupTransData.setStanNo(stanNo);

        }

        String amount = dupTransData.getAmount();
        ResponseCode respCode = dupTransData.getResponseCode();

        int ret;
        ETransType transType = dupTransData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_advice));
        }

        if (transType == ETransType.QR_VOID_WALLET || dupTransData.getTransState() == TransData.ETransStatus.VOIDED) {
            dupTransData.setAmount("0");
            dupTransData.setTransType(ETransType.QR_VOID_WALLET);
            dupTransData.setOrigRefNo(dupTransData.getRefNo());
        }

        dupTransData.setAdviceStatus(TransData.AdviceStatus.ADVICE);
        ret = online.online(dupTransData, listener);

        dupTransData.setStanNo(orignalStan);
        dupTransData.setAmount(amount);//set back original amount value
        dupTransData.setTransType(transType);//set back original transType value

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = dupTransData.getResponseCode();
            dupTransData.setResponseCode(respCode);//set back response code original value
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
                dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                return TransResult.SUCC;
            } else {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(dupTransData.getResponseCode().getCode())
                            && dupTransData.getField63() != null) {
                        listener.onShowErrMessage(dupTransData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(dupTransData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }
        dupTransData.setResponseCode(respCode);//set back original response code value

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }

            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_advice),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);

        return ret;
    }

    public int sendAdvicePromptpay(TransData dupTransData, TransProcessListener listener) {
        long orignalStan = 0;
        String origDateTime;
        if (dupTransData == null) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
            dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);

            if (dupTransData == null) {
                return TransResult.SUCC;
            }

            long transNo = dupTransData.getTraceNo();
            int id = dupTransData.getId();
            origDateTime = dupTransData.getDateTime();
            String dateTime = dupTransData.getOrigDateTime();
            TransData.ETransStatus transState = dupTransData.getTransState();

            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
            Component.transInit(dupTransData, dupTransData.getAcquirer());

            dupTransData.setTraceNo(transNo);
            dupTransData.setId(id);
            dupTransData.setDateTime(dateTime);
            dupTransData.setTransState(transState);
            orignalStan = dupTransData.getStanNo();
        } else {
            long stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
            if (stanNo == 0) {
                stanNo += 1;
                FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) stanNo);
            }
            orignalStan = dupTransData.getStanNo();
            origDateTime = dupTransData.getDateTime();
            dupTransData.setDateTime(dupTransData.getOrigDateTime());
            dupTransData.setStanNo(stanNo);

        }

        String amount = dupTransData.getAmount();
        ResponseCode respCode = dupTransData.getResponseCode();

        int ret;
        ETransType transType = dupTransData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_advice));
        }

        if (transType == ETransType.PROMPTPAY_VOID || dupTransData.getTransState() == TransData.ETransStatus.VOIDED) {
            dupTransData.setAmount("0");
            dupTransData.setTransType(ETransType.PROMPTPAY_VOID);
            dupTransData.setOrigRefNo(dupTransData.getRefNo());
        }

        dupTransData.setAdviceStatus(TransData.AdviceStatus.ADVICE);
        ret = online.online(dupTransData, listener);

        dupTransData.setStanNo(orignalStan);
        dupTransData.setAmount(amount);//set back original amount value
        dupTransData.setTransType(transType);//set back original transType value
        dupTransData.setDateTime(origDateTime); //set back original date time value

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = dupTransData.getResponseCode();
            dupTransData.setResponseCode(respCode);//set back response code original value
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_PROMPT);
                dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                return TransResult.SUCC;
            } else {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(dupTransData.getResponseCode().getCode())
                            && dupTransData.getField63() != null) {
                        listener.onShowErrMessage(dupTransData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(dupTransData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }
        dupTransData.setResponseCode(respCode);//set back original response code value

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }

            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_advice),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);

        return ret;
    }

    public int sendReversalPromptpay(TransProcessListener listener) {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordPromptPay();
        if (dupTransData == null) {
            return TransResult.SUCC;
        }
        int ret = 0;
        long stanNo = dupTransData.getStanNo();
        long traceNo = dupTransData.getTraceNo();
        String dupReason = dupTransData.getDupReason();

        /*ETransType transType = dupTransData.getTransType();
        if (transType == ETransType.VOID) {
            dupTransData.setOrigAuthCode(dupTransData.getOrigAuthCode());
        } else {
            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
        }*/
        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setStanNo(stanNo);
        dupTransData.setTraceNo(traceNo);
        dupTransData.setDupReason(dupReason);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);
        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
        return ret;
    }

    public int sendReferredTrans(TransProcessListener listener, boolean isSettlement) {
        int ret = new TransOnline().referralTransSend(listener, isSettlement);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_ABORTED && listener != null) {
            listener.onShowErrMessage(
                    TransResultUtils.getMessage(ret),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        return ret;
    }

    /**
     * 冲正处理
     *
     * @return
     */
    public int sendIssuerScriptUpdate(TransProcessListener listener, TransData transData, byte[] f55) {
        int ret = 0;
        TransData dupTransData = new TransData(transData);

        dupTransData.setSendIccData(Utils.bcd2Str(f55));
        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        //update STAN
        dupTransData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        dupTransData.setTransType(ETransType.UPDATE_SCRIPT_RESULT);
        //listener.onUpdateProgressTitle(dupTransData.getTransType().getTransName());

        ret = online.online(dupTransData, listener);

                /*if (ret == TransResult.SUCC) {
                    String retCode = dupTransData.getResponseCode().getCode();
                    // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                    if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                        return TransResult.SUCC;
                    }
                }
                if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                    if (listener != null) {
                        listener.onShowErrMessage(
                                TransResultUtils.getMessage(ret),
                                Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }

                    return TransResult.ERR_ABORTED;
                }
                if (ret == TransResult.ERR_RECV) {
                    dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                    dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                    FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);

                }
            if (listener != null) {
                listener.onShowErrMessage(Utils.getString(R.string.err_update_script_result),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        return ret;*/
        return TransResult.SUCC; // Skip all process to check for result (mimic from S500)
    }

    public int sendTcAdviceTrans(TransProcessListener listener) {
        int ret = new TransOnline().tcAdviceTransSend(listener);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_ABORTED && listener != null) {
            listener.onShowErrMessage(
                    TransResultUtils.getMessage(ret),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        return ret;
    }

    public int sendTcAdviceInstalmentTrans(TransProcessListener listener) {
        int ret = new TransOnline().tcAdviceInstalmentTransSend(listener);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_ABORTED && listener != null) {
            listener.onShowErrMessage(
                    TransResultUtils.getMessage(ret),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        return ret;
    }

    public int transmitQRSale(TransData transData, TransProcessListener listener) {
        int ret;
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = sendReversalQRSale(listener);
        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        ret = sendAdviceQrSale(null, listener);
        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        String respCode;
        switch (transType) {
            case GET_QR_INFO:
                // Gen QR
                ret = sendQRSaleTrans(transData, listener);
                break;
            case QR_SALE_ALL_IN_ONE:
                if (transData.getLastTrans()) {
                    ret = sendQRSaleTrans(transData, listener);
                } else {
                    // Sale Inquiry with retry 3 time
                    ret = sendQRSaleTransWithRetry(transData, listener);
                }

                respCode = transData.getResponseCode() != null ? transData.getResponseCode().getCode() : null;
                if (ret != 0 && respCode == null) {
                    // Send Reversal
                    int iRet = sendReversalQRSale(listener);
                    if (iRet == 0) {
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    }
                }
                break;
            case STATUS_INQUIRY_ALL_IN_ONE:
                // Sale Inquiry
                ret = sendQRSaleTrans(transData, listener);

                respCode = transData.getResponseCode() != null ? transData.getResponseCode().getCode() : null;
                if (ret != 0 && respCode == null) {
                    // Send Reversal
                    int iRet = sendReversalQRSale(listener);
                    if (iRet == 0) {
                        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
                    }
                }
                break;
            case QR_VOID: // void
                // QR void flow
                ret = sendQRVoidTrans(transData, listener);
                break;
        }

        if ((transData.getTransType() == ETransType.QR_SALE_ALL_IN_ONE || transData.getTransType() == ETransType.STATUS_INQUIRY_ALL_IN_ONE) &&
                (ret == TransResult.ERR_RECV || ret == TransResult.ERR_HOST_REJECT || ret == TransResult.ERR_USER_CANCEL)) {
            Component.incTraceNo(transData);

        }
        return ret;
    }

    public int sendAdviceQrSale(TransData dupTransData, TransProcessListener listener) {
        long orignalStan = 0;
        String origDateTime;
        if (dupTransData == null) {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
            dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);

            if (dupTransData == null) {
                return TransResult.SUCC;
            }

            long transNo = dupTransData.getTraceNo();
            int id = dupTransData.getId();
            String dateTime = dupTransData.getOrigDateTime();
            origDateTime = dupTransData.getDateTime();
            TransData.ETransStatus transState = dupTransData.getTransState();

            dupTransData.setOrigAuthCode(dupTransData.getAuthCode());
            Component.transInit(dupTransData, dupTransData.getAcquirer());

            dupTransData.setTraceNo(transNo);
            dupTransData.setId(id);
            dupTransData.setDateTime(dateTime);
            dupTransData.setTransState(transState);
            orignalStan = dupTransData.getStanNo();
        } else {
            long stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
            if (stanNo == 0) {
                stanNo += 1;
                FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) stanNo);
            }
            orignalStan = dupTransData.getStanNo();
            origDateTime = dupTransData.getDateTime();
            dupTransData.setDateTime(dupTransData.getOrigDateTime());
            dupTransData.setStanNo(stanNo);

        }

        String amount = dupTransData.getAmount();
        ResponseCode respCode = dupTransData.getResponseCode();

        int ret;
        ETransType transType = dupTransData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_advice));
        }

        if (transType == ETransType.QR_VOID || dupTransData.getTransState() == TransData.ETransStatus.VOIDED) {
            dupTransData.setAmount("0");
            dupTransData.setTransType(ETransType.QR_VOID);
            dupTransData.setOrigRefNo(dupTransData.getRefNo());
        }

        dupTransData.setAdviceStatus(TransData.AdviceStatus.ADVICE);
        ret = online.online(dupTransData, listener);

        dupTransData.setStanNo(orignalStan);
        dupTransData.setAmount(amount);//set back original amount value
        dupTransData.setTransType(transType);//set back original transType value
        dupTransData.setDateTime(origDateTime); //set back original date time value

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = dupTransData.getResponseCode();
            dupTransData.setResponseCode(respCode);//set back response code original value
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QRC);
                dupTransData = FinancialApplication.getTransDataDbHelper().findAdviceRecord(acquirer);
                return TransResult.SUCC;
            } else {
                dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(dupTransData.getResponseCode().getCode()) && dupTransData.getField63() != null) {
                        listener.onShowErrMessage(dupTransData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(dupTransData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }
        dupTransData.setResponseCode(respCode);//set back original response code value

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }

            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_advice),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        dupTransData.setAdviceStatus(TransData.AdviceStatus.PENDING);
        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);

        return ret;
    }

    public int sendReversalQRSale(TransProcessListener listener) {

        if (listener != null) {
            listener.onHideProgress();
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordQRSale();
        if (dupTransData == null) {
            return TransResult.SUCC;
        }
        int ret = 0;
        long transNo = dupTransData.getTraceNo();
        long stanNo = dupTransData.getStanNo();
        String dupReason = dupTransData.getDupReason();

        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setTraceNo(transNo);
        dupTransData.setDupReason(dupReason);
        dupTransData.setStanNo(stanNo);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            dupTransData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
                    FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                //FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
        return ret;
    }

    private int sendQRSaleTrans(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();
        transData.setOrigStanNo(transData.getStanNo());

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        transData.setOrigField63(transData.getField63());
        ret = online.online(transData, listener);

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                if (transData.getTransType() != ETransType.GET_QR_INFO) {
                    // write transaction record
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
                    transData.setQrSaleState(TransData.QrSaleState.QR_SEND_ONLINE);
                    transData.setDupReason("");
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                }
            } else {
                if (listener != null && transData.getTransType() != ETransType.GET_QR_INFO) {
                    FinancialApplication.getTransDataDbHelper().deleteDupRecordQr();
                }

                if (("ER".equals(transData.getResponseCode().getCode()) || "UK".equals(transData.getResponseCode().getCode()))
                        && transData.getField63() != null) {
                    listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                } else {
                    listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    private int sendQRVoidTrans(TransData transData, TransProcessListener listener) {
        // QR void flow
        int ret;
        ret = online.online(transData, listener);
        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();
            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                initQrAdviceStatus(transData);
                TransData originTran = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(transData.getTraceNo(), false);
                originTran.setTransState(TransData.ETransStatus.VOIDED);
                FinancialApplication.getTransDataDbHelper().updateTransData(originTran);
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                ret = sendAdviceQrSale(transData, listener);
                if (ret != TransResult.SUCC) {
                    return TransResult.ERR_ADVICE;
                }

                return TransResult.SUCC;
            } else {
                if (listener != null) {
                    if ("ER".equals(transData.getResponseCode().getCode())
                            && transData.getField63() != null) {
                        listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }
        return ret;
    }

    private int sendQRSaleTransWithRetry(TransData transData, TransProcessListener listener) {
        int ret, resTrans, i = 0;

        ETransType transType = transData.getTransType();
        transData.setOrigStanNo(transData.getStanNo());

        while (true) {
            if (listener != null) {
                listener.onHideProgress();
                listener.onUpdateProgressTitle(transType.getTransName());
            }

            transData.setOrigField63(transData.getField63());
            transData.setStanNo(transData.getOrigStanNo());


            //delete transaction duplicate
            TransData dupTrans = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(transData.getTraceNo(), false);
            if (dupTrans != null) {
                FinancialApplication.getTransDataDbHelper().deleteTransData(dupTrans.getId());
            }

            ret = online.online(transData, listener);

            i++;

            if (ret == TransResult.SUCC) {
                ResponseCode responseCode = transData.getResponseCode();
                String retCode = responseCode.getCode();

                if ("00".equals(retCode)) {
                    // write transaction record
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
                    transData.setQrSaleState(TransData.QrSaleState.QR_SEND_ONLINE);
                    transData.setDupReason("");
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    return TransResult.SUCC;
                } else {
                    if ("ER".equals(retCode) || "UK".equals(retCode)) {
                        if (i < 4) {
                            transData.setField63(transData.getOrigField63());
                            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                            transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            if (listener != null) {
                                ret = listener.onShowProgress(Utils.getString(R.string.err_promptpay_retry_again), transData.getAcquirer().getPromptRetryTimeout(), false, true);
                                if (ret == TransResult.ERR_USER_CANCEL) {
                                    return ret;
                                }
                            }
                            continue;
                        }
                        if (listener != null) {
                            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                            transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        }
                    } else {
                        if (listener != null) {
                            transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                            listener.onShowErrMessage(transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                        }
                    }
                    return TransResult.ERR_HOST_REJECT;
                }
            }

            if (i < 4) {
                resTrans = ret;
                if (listener != null) {
                    transData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    ret = listener.onShowProgress(Utils.getString(R.string.err_promptpay_retry_again), transData.getAcquirer().getPromptRetryTimeout(), false, true);
                    if (ret == TransResult.ERR_USER_CANCEL) {
                        return ret;
                    }
                }
                transData.setField63(transData.getOrigField63());
                continue;
            } else {
                break;
            }
        }

        return ret;
    }

    public int transmitThaiQrVerifyPaySlip(TransData transData, TransProcessListener listener) {
        int ret = 0;
        ETransType transType = ETransType.QR_VERIFY_PAY_SLIP;

        if (listener!=null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));

        ret = online.online(transData, listener);
        if (ret==TransResult.SUCC) {
            if (transData.getResponseCode().getCode().equals("00")) {
                ret = extractDE63VerifyQRPaySlip(transData);
                if (ret == TransResult.SUCC) {
                    transData.setSignFree(true);
                    transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setQrSourceOfFund("Verify QR");
                    transData.setAuthCode(" ");
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                } else {
                    listener.onShowErrMessage( "Error code (" + ret + ")\n" + Utils.getString(ret) , Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
            } else {
                if (transData.getField60RecByte()!=null && transData.getField60RecByte().length > 0) {
                    listener.onShowErrMessage( "Resp.Code (" + transData.getResponseCode().getCode() + ")\n" + Tools.bytes2String(transData.getField60RecByte()) , Constants.FAILED_DIALOG_SHOW_TIME, true);
                } else {
                    listener.onShowErrMessage( "Resp.Code (" + transData.getResponseCode().getCode() + ")\n" + transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                ret = TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    public void mapControlLimitResponseCode() {

    }

    public int extractDE63VerifyQRPaySlip(TransData transData) {
        int ret = TransResult.ERR_MISSING_FIELD63;
        if (transData != null && transData.getField63RecByte() !=null && transData.getField63RecByte().length >0) {
            byte[] DE63 = transData.getField63RecByte();
            byte[] buff ;
            try {
                buff = Arrays.copyOfRange(DE63, 0, 32);
                //transData.setWalletPartnerID(Tools.bytes2String(buff));           // use original partner-id from EDC

                buff = Arrays.copyOfRange(DE63, 32, 96);
                //transData.setTxnID(Tools.bytes2String(buff));                     // Original Trans.ID generated from EDC

                buff = Arrays.copyOfRange(DE63, 96, 120);
                transData.setTxnNo(Tools.bytes2String(buff));

                buff = Arrays.copyOfRange(DE63, 120, 136);
                transData.setPayTime(Tools.bytes2String(buff));

                buff = Arrays.copyOfRange(DE63, 136, 139);
                transData.setWalletBankCode(Tools.bytes2String(buff));

                return TransResult.SUCC;
            } catch (Exception e) {
                e.printStackTrace();
                ret = TransResult.VERIFY_THAI_QR_PAY_RECEIPT_DE63_INVALID_LEN;
            }
        }

        return ret;
    }

    public int transmitMyPrompt(TransData transData, TransProcessListener listener) {
        int ret = 0;
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        return sendMyPromptTrans(transData, listener);
    }

    public int transmitKbankWallet(TransData transData, TransProcessListener listener) {
        int ret = 0;
        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        if (//transType == GET_QR_KPLUS                                                             // [EDCMGNT-1190]
                 transType == GET_QR_WECHAT
                || transType == GET_QR_ALIPAY
                || transType == GET_QR_CREDIT
                //|| transType == QR_VOID_KPLUS                                                     // [EDCMGNT-1190]
                || transType == QR_VOID_WECHAT
                || transType == QR_VOID_ALIPAY
                || transType == QR_VOID_CREDIT
                || transType == QR_MYPROMPT_SALE
                || transType == QR_MYPROMPT_VOID) {
            ret = sendReversalQR(listener, transData.getAcquirer());
        }
        if (ret != 0) {
            return TransResult.ERR_ABORTED;
        } else {
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        }

        switch (transType) {
            case QR_VOID_KPLUS:
            case QR_VOID_WECHAT:
            case QR_VOID_ALIPAY:
            case QR_VOID_CREDIT:
                ret = sendKBankQRVoidTrans(transData, listener);
                break;
            default:
                ret = sendQRTrans(transData, listener);
                break;
        }

        return ret;
    }

    private int sendQRTrans(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = online.online(transData, listener);

        // [EDCMGNT-1190]
        if (transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)
                && (transData.getTransType().equals(GET_QR_KPLUS) || transData.getTransType().equals(QR_INQUIRY) || transData.getTransType().equals(QR_VOID_KPLUS))) {
            // only THAIQR transaction -- set Reversal status as 'NORMAL' status to ensure NoReversal for THAIQR
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        }

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                // write transaction record
                if (transData.getTransType() != GET_QR_KPLUS
                        && transData.getTransType() != GET_QR_WECHAT
                        && transData.getTransType() != GET_QR_ALIPAY
                        && transData.getTransType() != GET_QR_CREDIT) {
                    transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                    transData.setDupReason("");
                    transData.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
                    Component.splitField63Wallet(transData, transData.getField63RecByte());
                    FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                    if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass ) {
                        if (transData.getTxnNo()!=null && !transData.getTxnNo().isEmpty()) {
                            EcrData.instance.qr_TransID = transData.getTxnNo().trim().getBytes();
                        }
                    }
                }
                return TransResult.SUCC;
            } else {
                if ("99".equals(retCode)
                        && (transType == ETransType.QR_INQUIRY
                        || transType == ETransType.QR_INQUIRY_ALIPAY
                        || transType == ETransType.QR_INQUIRY_WECHAT
                        || transType == ETransType.QR_INQUIRY_CREDIT)) {//KBANK response 99 (wait for inquiry)
                    transData.setAuthCode(null);
                    initWalletRetryStatus(transData);
                    if (transData.getLastTrans()) {
                        FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                    }
                    return TransResult.ERR_WALLET_RESP_UK;
                }
                FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(transData.getResponseCode().getCode())) {
                        listener.onShowErrMessage(Utils.getString(R.string.prompt_err_code) + transData.getResponseCode().getCode() + "\n" +
                                transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    private int sendKBankQRVoidTrans(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = online.online(transData, listener);

        // [EDCMGNT-1190]
        if (transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)
                && (transData.getTransType().equals(GET_QR_KPLUS) || transData.getTransType().equals(QR_INQUIRY) || transData.getTransType().equals(QR_VOID_KPLUS))) {
            // only THAIQR transaction -- set Reversal status as 'NORMAL' status to ensure NoReversal for THAIQR
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        }

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                transData.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                return TransResult.SUCC;
            } else {
                FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                if ("99".equals(retCode)
                        && (transType == ETransType.QR_INQUIRY
                        || transType == ETransType.QR_INQUIRY_ALIPAY
                        || transType == ETransType.QR_INQUIRY_WECHAT
                        || transType == ETransType.QR_INQUIRY_CREDIT)) {//KBANK response 99 (wait for inquiry)
                    transData.setAuthCode(null);
                    initWalletRetryStatus(transData);
                    return TransResult.ERR_WALLET_RESP_UK;
                }
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("ER".equals(transData.getResponseCode().getCode())) {
                        listener.onShowErrMessage(Utils.getString(R.string.prompt_err_code) + transData.getResponseCode().getCode() + "\n" +
                                transData.getField63(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    public int sendReversalQR(TransProcessListener listener, Acquirer acquirer) {
        TransData dupTransData = FinancialApplication.getTransDataDbHelper().findFirstDupRecordQR(acquirer);
        if (dupTransData == null) {
            return TransResult.SUCC;
        }

        int ret = 0;
        long transNo = dupTransData.getTraceNo();
        String dupReason = dupTransData.getDupReason();

        dupTransData.setOrigAuthCode(dupTransData.getAuthCode());

        Component.transInit(dupTransData, dupTransData.getAcquirer());

        dupTransData.setTraceNo(transNo);
        dupTransData.setDupReason(dupReason);

        int retry = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_REVERSAL_RETRY);
        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.prompt_reverse));
        }

        for (int i = 0; i < retry; i++) {
            //AET-126
            dupTransData.setReversalStatus(TransData.ReversalStatus.REVERSAL);
            dupTransData.setQrSaleStatus(TransData.QrSaleStatus.NOT_SUCCESS.toString());
            ret = online.online(dupTransData, listener);
            if (ret == TransResult.SUCC) {
                String retCode = dupTransData.getResponseCode().getCode();
                // 冲正收到响应码12或者25的响应码，应默认为冲正成功
                if ("00".equals(retCode) || "12".equals(retCode) || "25".equals(retCode)) {
//                    FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                    FinancialApplication.getTransDataDbHelper().deleteDupRecord(acquirer);
                    return TransResult.SUCC;
                }
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_OTHERS);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                continue;
            }
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
                if (listener != null) {
                    listener.onShowErrMessage(
                            TransResultUtils.getMessage(ret),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }

                return TransResult.ERR_ABORTED;
            }
            if (ret == TransResult.ERR_RECV) {
                dupTransData.setReversalStatus(TransData.ReversalStatus.PENDING);
                dupTransData.setDupReason(TransData.DUP_REASON_NO_RECV);
                //FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                break;
            }
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_reverse),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        //FinancialApplication.getTransDataDbHelper().deleteDupRecordWallet();
        return ret;
    }

    private int sendMyPromptTrans(TransData transData, TransProcessListener listener) {
        int ret;

        ETransType transType = transData.getTransType();

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        ret = online.online(transData, listener);

        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();

            if ("00".equals(retCode)) {
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");

                if (transType == ETransType.QR_MYPROMPT_SALE)  {
                    transData.setChannel(transData.getField63().substring(110,120).trim());
                } else if (transType == ETransType.QR_MYPROMPT_INQUIRY || transType == ETransType.QR_MYPROMPT_VERIFY) {
                    transData.setChannel(transData.getField63().substring(130,140).trim());
                }
                FinancialApplication.getTransDataDbHelper().updateTransData(transData);
                return TransResult.SUCC;
            } else {
                if ((transType == ETransType.QR_MYPROMPT_INQUIRY || transType == ETransType.QR_MYPROMPT_SALE)
                        && "99".equals(retCode)) {
                    return TransResult.PROMPT_INQUIRY;
                } else if (transType == ETransType.QR_MYPROMPT_VERIFY) {
                    Component.incTraceNo(transData);
                    return TransResult.PROMPT_INQUIRY;
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }

        return ret;
    }

    public int transmitKbankDolfin(TransData transData, TransProcessListener listener) {
        int ret;
        ETransType transType = transData.getTransType();

        ret = sendReversal(listener, transData.getAcquirer());
        if (ret != 0)
            return TransResult.ERR_ABORTED;

        if (listener != null) {
            listener.onUpdateProgressTitle(transType.getTransName());
        }

        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        ret = online.online(transData, listener);
        if (ret == TransResult.SUCC) {
            ResponseCode responseCode = transData.getResponseCode();
            String retCode = responseCode.getCode();
            if ("00".equals(retCode)) {
                // write transaction record
                transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
                transData.setDupReason("");
                FinancialApplication.getTransDataDbHelper().insertTransData(transData);
                ret = TransResult.SUCC;
            } else if ("99".equals(retCode)) {
                ret = TransResult.QR_PROCESS_INQUIRY;//TransResult.ERR_TIMEOUT;
            } else {
                //FinancialApplication.getTransDataDbHelper().deleteDupRecord(transData.getAcquirer());
                if (listener != null) {
                    // EDCBBLAND-383 [Change Request]Error message mapping with S500
                    if ("LE".equals(retCode)) {
                        listener.onShowErrMessage(Component.unpackBit63Credit(transData.getField63RecByte(), transData.getField63()), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                }
                ret = TransResult.ERR_HOST_REJECT;
            }

        }

        Log.d("Transmit", "transmitKbankDolfin " + ret);
        return ret;
    }

    public int sendOfflineTrans(TransProcessListener listener) {
        int ret = new TransOnline().offlineTransSend(listener);
        if (ret != TransResult.SUCC && ret != TransResult.ERR_ABORTED && listener != null) {
            listener.onShowErrMessage(
                    TransResultUtils.getMessage(ret),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        return ret;
    }

    /**
     * Send Upload Offline Txn
     *
     * @param listener
     * @return
     */
    public int sendOfflineNormalSale(TransData originalTransData, TransProcessListener listener) {
        if (originalTransData == null) {
            return TransResult.SUCC;
        }

        if (originalTransData.getAcquirer().isDisableTrickFeed()) {
            return TransResult.SUCC;
        }
        TransData dupTransData = new TransData(originalTransData);
        Component.transInit(dupTransData, originalTransData.getAcquirer());
        dupTransData.setDateTime(originalTransData.getOrigDateTime());
        // use original trace/stan no.
        dupTransData.setStanNo(originalTransData.getStanNo());
        dupTransData.setTraceNo(originalTransData.getTraceNo());

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_upload_offline));
        }

        if (ETransType.OFFLINE_TRANS_SEND != originalTransData.getTransType()) {// sale below floor limit
            dupTransData.setTransType(ETransType.OFFLINE_TRANS_SEND);
            dupTransData.setOrigTransType(originalTransData.getTransType());
        }
        dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_SENDING);

        int ret = online.online(dupTransData, listener);

        dupTransData.setDateTime(originalTransData.getOrigDateTime());// set back the original date/time for use in void trans.
        dupTransData.setTransType(originalTransData.getTransType());//set original transType

        if (ret == TransResult.SUCC) {
            //For Upload offline txn, no need to check response code
            if (dupTransData.getResponseCode().getCode().equals("00")) {
                dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_SENT);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
                if (originalTransData.getTransType() == ETransType.ADJUST) {
                    TransData financialTran = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(
                            originalTransData.getTraceNo(), true);
                    financialTran.setRefNo(dupTransData.getRefNo());
                    financialTran.setResponseCode(dupTransData.getResponseCode());
                    financialTran.setOfflineSendState(TransData.OfflineStatus.OFFLINE_SENT);
                    FinancialApplication.getTransDataDbHelper().updateTransData(financialTran);
                }
            } else {
                dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_ERR_SEND);
                FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            }
            return TransResult.SUCC;
        }

        if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_PACK || ret == TransResult.ERR_SEND) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_ERR_SEND);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            return TransResult.ERR_ABORTED;
        }
        if (ret == TransResult.ERR_RECV) {
            if (listener != null) {
                listener.onShowErrMessage(
                        TransResultUtils.getMessage(ret),
                        Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_ERR_RESP);
            FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);
            return ret;
        }
        if (listener != null) {
            listener.onShowErrMessage(Utils.getString(R.string.err_upload_offline),
                    Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        dupTransData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_ERR_UNKNOWN);
        FinancialApplication.getTransDataDbHelper().updateTransData(dupTransData);

        return ret;
    }
}
