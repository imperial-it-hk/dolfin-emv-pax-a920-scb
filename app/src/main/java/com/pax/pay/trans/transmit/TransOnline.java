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

import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.CardBinBlack;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.OfflineStatus;
import com.pax.pay.trans.model.TransData.ReferralStatus;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.trans.pack.PackIso8583;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.ResponseCode;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import th.co.bkkps.utils.ArrayListUtils;
import th.co.bkkps.utils.Log;

import static com.pax.pay.trans.component.Component.getPaddedNumber;
import static com.pax.pay.trans.component.Component.getPaddedStringRight;
import static com.pax.pay.trans.component.Component.incTraceNo;
import static com.pax.pay.trans.component.Component.transInit;

/**
 * 单独联机处理， 例如签到
 *
 * @author Steven.W
 */
public class TransOnline {
    private Online online = new Online();
    private boolean isSettleFail = false;
    private String respErrorCode;
    private String settledRefNo = null;

    public String getSettledRefNo() {
        return settledRefNo;
    }

    private boolean isDolfinIpp(Acquirer acquirer) { return acquirer.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT); }

    /**
     * 检查应答码
     *
     * @return {@link TransResult}
     */
    private int checkRspCode(TransData transData, TransProcessListener listener) {
        if (transData.getResponseCode() != null) {
            if (!"00".equals(transData.getResponseCode().getCode())) {
                if (listener != null) {
                    listener.onHideProgress();
                    listener.onShowErrMessage(transData.getResponseCode().getMessage(),
                            Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                return TransResult.ERR_HOST_REJECT;
            }
        }
        return TransResult.SUCC;
    }

    /**
     * 保存黑名单
     *
     * @param blackList the black list array
     */
    @SuppressWarnings("unused")
    private void writeBlack(byte[] blackList) {
        if (blackList == null)
            return;
        int loc = 0;
        while (loc < blackList.length) {
            int len = Integer.parseInt(new String(new byte[]{blackList[loc], blackList[loc + 1]}));
            byte[] cardNo = new byte[len];
            if (len + loc + 2 > blackList.length) {
                return;
            }
            System.arraycopy(blackList, loc + 2, cardNo, 0, len);
            CardBinBlack cardBinBlack = new CardBinBlack();
            cardBinBlack.setBin(new String(cardNo));
            cardBinBlack.setCardNoLen(cardNo.length);
            FinancialApplication.getCardBinDb().insertBlack(cardBinBlack);
            loc += 2 + len;
        }
    }

    public boolean isSettleFail() {
        return isSettleFail;
    }

    public String getRespErrorCode() {
        return respErrorCode;
    }

    private void handleSettleCaseFail(String resCode) {
        isSettleFail = !"00".equals(resCode);
        respErrorCode = resCode;
    }

    /**
     * 结算
     *
     * @return {@link TransResult}
     */
    public int settle(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        // 处理冲正
        ret = transmit.sendReversal(listener, total.getAcquirer());
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        ret = transmit.sendTcAdviceTrans(listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }

//        ret = transmit.sendOfflineTrans(listener, true, true);
        ret = transmit.sendOfflineTrans(listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = transmit.sendReferredTrans(listener, true);
        if (ret != TransResult.SUCC) {
            return ret;
        } else {
            TransTotal tmpTotal = FinancialApplication.getTransTotalDbHelper().calcTotal(total.getAcquirer(), true);
            tmpTotal.setAcquirer(total.getAcquirer());
            tmpTotal.setDateTime(total.getDateTime());
            total = tmpTotal;
        }

        ret = settleRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        ret = batchUp(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * 结算请求
     *
     * @return {@link TransResult}
     */
    private int settleRequest(TransTotal total, TransProcessListener listener) {

        TransData transData = transInit();
        transData.setTransType(ETransType.SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.SETTLE.getTransName());
        }

        String saleAmt;
        String saleNum;
        String refundAmt;
        String refundNum;

        String buf;
        saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        refundAmt = getPaddedNumber(total.getRefundTotalAmt(), 12);
        refundNum = getPaddedNumber(total.getRefundTotalNum(), 3);
        buf = saleNum + saleAmt + refundNum + refundAmt;
        if (!Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                && !Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName())) {//reserved filed for another transactions
            buf = getPaddedStringRight(buf, 90, '0');
//            buf += "000000000000000000000000000000000000000000000000000000000000";
        } else {//reserved field for AMEX
            buf = getPaddedStringRight(buf, 36, '0');
//            buf += "000000";
        }
        transData.setField63(buf);

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                // increase trace no. when settlement success,
                // if response code is 95 (batch upload occurs), trace no. will be increased after settlement end complete.

                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRED-BATCH-UPLOAD)");
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRED-BATCH-UPLOAD)");
                incTraceNo(null);
                return TransResult.SUCC_NOREQ_BATCH;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }


        settledRefNo = transData.getRefNo();
        Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        return TransResult.SUCC;
    }

    /**
     * 批上送
     *
     * @return {@link TransResult}
     */
    private int batchUp(TransTotal total, TransProcessListener listener) {
        int ret;
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.BATCH_UP.getTransName());
        // 获取交易记录条数
        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false);
        // If no transaction for batch upload, should do settlement end.
        if (cnt > 0) {
            // 获取交易重复次数
            int resendTimes = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_REDIAL_TIMES);
            int sendCnt = 0;
            final boolean[] left = new boolean[]{false};
            while (sendCnt < resendTimes + 1) {
                // 1)(对账平不送)全部磁条卡离线类交易，包括结算调整
                // 2)(对账平不送)基于PBOC标准的借/贷记IC卡脱机消费(含小额支付)成功交易
                // 3)(不存在)基于PBOC标准的电子钱包IC卡脱机消费成功交易 --- 不存在

                // 4)(对账平不送)全部磁条卡的请求类联机成功交易明细
                ret = new AllMagCardTransBatch(listener, new BatchUpListener() {

                    @Override
                    public void onLeftResult(boolean l) {
                        left[0] = l;
                    }
                }).process();
                if (ret != TransResult.SUCC) {
                    return ret;
                }
                // 5)(对账平不送)磁条卡和基于PBOC借/贷记标准IC卡的通知类交易明细，包括退货和预授权完成(通知)交易
                // 6)(对账平也送)为了上送基于PBOC标准的借/贷记IC卡成功交易产生的TC值，所有成功的IC卡借贷记联机交易明细全部重新上送
                // 7)(对账平也送)为了让发卡方了解基于PBOC标准的借/贷记IC卡脱机消费(含小额支付)交易的全部情况，上送所有失败的脱机消费交易明细
                // 8)(对账平也送)为了让发卡方防范基于PBOC标准的借/贷记IC卡风险交易，上送所有ARPC错但卡片仍然承兑的IC卡借贷记联机交易明细
                // 9)(不存在)为了上送基于PBOC标准的电子钱包IC卡成功圈存交易产生的TAC值，上送所有圈存确认的交易明细
                if (!left[0]) {
                    break;
                }
                left[0] = false;
                sendCnt++;
            }
        }
        // 10)(对账平也送)最后需上送批上送结束报文
        ret = batchUpEnd(total, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }
        return TransResult.SUCC;
    }

    /**
     * 结算结束
     *
     * @return {@link TransResult}
     */
    private int batchUpEnd(TransTotal total, TransProcessListener listener) {
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.SETTLE_END.getTransName());
        TransData transData = transInit();
        String f60 = getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6);//use current batch no. for each acquirer same as S500
//        f60 = "00" + getPaddedNumber(FinancialApplication.getAcqManager().getCurAcq().getCurrBatchNo(), 6);
//        f60 += "207";

        // Field 48 is not used based on message spec. for all acquirers in TH.
//        int batchUpNum = FinancialApplication.getController().get(Controller.BATCH_NUM);
//        transData.setField48(getPaddedNumber(batchUpNum, 4));
        transData.setField60(f60);
        transData.setTransType(ETransType.SETTLE_END);

        String saleAmt;
        String saleNum;
        String refundAmt;
        String refundNum;

        String buf;
        saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        refundAmt = getPaddedNumber(total.getRefundTotalAmt(), 12);
        refundNum = getPaddedNumber(total.getRefundTotalNum(), 3);
        buf = saleNum + saleAmt + refundNum + refundAmt;
        if (!Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())
                && !Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName())) {//reserved filed for another transactions
            buf = getPaddedStringRight(buf, 90, '0');
//            buf += "000000000000000000000000000000000000000000000000000000000000";
        } else {//reserved field for AMEX
            buf = getPaddedStringRight(buf, 36, '0');
//            buf += "000000";
        }
        transData.setField63(buf);

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ResponseCode responseCode = transData.getResponseCode();
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                return TransResult.SUCC;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }

        return TransResult.ERR_RECONCILE_FAILED;
    }

    /**
     * 脱机交易上送
     *
     * @param listener isOnline 是否为下一笔联机交易
     * @return {@link TransResult}
     */
    public int offlineTransSend(TransProcessListener listener, boolean isSendAllOfflineTrans, boolean isSettlement) {
        int sendMaxTime = FinancialApplication.getSysParam().get(SysParam.NumberParam.OFFLINE_TC_UPLOAD_TIMES);
        int maxOfflineNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.OFFLINE_TC_UPLOAD_NUM);

        final List<TransData.OfflineStatus> defFilter = new ArrayList<>();
        defFilter.add(TransData.OfflineStatus.OFFLINE_NOT_SENT);

        List<TransData> records = FinancialApplication.getTransDataDbHelper().findOfflineTransData(defFilter);
        List<TransData> notSendRecords = new ArrayList<>();
        if (records.isEmpty()) {
            return TransResult.SUCC;
        }

        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        if (acquirer == null || (!isSettlement && acquirer.isDisableTrickFeed())) {
            return TransResult.SUCC;
        }

        if (!isSettlement) {
            notSendRecords.add(records.get(0));
        } else {
            notSendRecords.addAll(records);
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.OFFLINE_TRANS_SEND.getTransName());
        }

        // 累计达到设置中“满足自动上送的累计笔数”，终端应主动拨号上送当前所有的离线类交易和IC卡脱机交易
        if (!isSendAllOfflineTrans && notSendRecords.size() < maxOfflineNum) {
            return TransResult.SUCC;
        }

        // 离线交易上送
        int ret = new OfflineTransProc(sendMaxTime, notSendRecords, listener).process();
        if (ret != TransResult.SUCC) {
            return ret;
        }
        // IC卡脱机交易上送

        return TransResult.SUCC;
    }

    public int offlineTransSend(TransProcessListener listener) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        List<Acquirer> acqs = new ArrayList<>();
        List<TransData.OfflineStatus> excludes = new ArrayList<>();
        acqs.add(acquirer);
        excludes.add(TransData.OfflineStatus.OFFLINE_SENT);
        excludes.add(TransData.OfflineStatus.OFFLINE_VOIDED);
        excludes.add(TransData.OfflineStatus.OFFLINE_ADJUSTED);
        List<TransData> records = FinancialApplication.getTransDataDbHelper().findAllOfflineTransData(acqs, excludes);
        if (records == null || (records != null && records.isEmpty())) {
            return TransResult.SUCC;
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_upload_offline));
        }

        int ret = new OfflineTransSendProc(records, listener).process();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * 回响功能
     *
     * @return {@link TransResultUtils}
     */
    public int echo(TransProcessListener listener) {
        TransData transData = transInit();
        int ret;
        transData.setTransType(ETransType.ECHO);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.ECHO.getTransName());
        }
        ret = online.online(transData, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }
        ret = checkRspCode(transData, listener);

        return ret;
    }

    /**
     * Send Offline trans. for PromptPay Settlement
     *
     * @param acquirer
     * @param listener
     */
    public int settlePromptPay(Acquirer acquirer, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalPromptpay(listener);
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        ret = transmit.sendAdvicePromptpay(null, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }

        Component.incTraceNo(null);// increase trace no. for settlement PromptPay
        List<TransData> qrSaleOfflineTrans = FinancialApplication.getTransDataDbHelper().findQRSaleTransData(acquirer, false, true, true);
        if (qrSaleOfflineTrans != null && !qrSaleOfflineTrans.isEmpty()) {
            if (listener != null) {
                listener.onUpdateProgressTitle(ETransType.SETTLE.getTransName());
            }
            int sendCount = 0;
            for (TransData record : qrSaleOfflineTrans) {
                sendCount++;
                if (listener != null) {
                    listener.onUpdateProgressTitle(ETransType.OFFLINE_TRANS_SEND.getTransName() + "[" + sendCount + "]");
                }

                TransData transData = new TransData(record);
                transInit(transData, acquirer);
                transData.setTraceNo(record.getTraceNo());
                transData.setField63(record.getQrRef2());
                transData.setQrSaleState(TransData.QrSaleState.QR_SEND_OFFLINE);
                transData.setTransType(ETransType.PROMPT_ADV);

                ret = this.online.online(transData, listener);

                if (listener != null) {
                    listener.onHideProgress();
                }

                if (ret != TransResult.SUCC) {
                    continue;
                } else {
                    if ("00".equals(transData.getResponseCode().getCode())) {
                        settledRefNo = transData.getRefNo();
                        Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                        Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                    }
                    record.setTransType(ETransType.BPS_QR_SALE_INQUIRY);
                    record.setQrSaleStatus(TransData.QrSaleStatus.SUCCESS.toString());
                    FinancialApplication.getTransDataDbHelper().updateTransData(record);
                }
            }
        }
        return ret;
    }

    /**
     * Settlement for MyPrompt transaction
     *
     * @return {@link TransResult}
     */
    public int settleMyPrompt(TransTotal total, TransProcessListener listener) {
        int ret = settleMyPromptRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * MyPrompt Settlement Request message
     *
     * @return {@link TransResult}
     */
    private int settleMyPromptRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.SETTLE_MYPROMPT);
        String currentBatch = Component.getPaddedNumber(total.getBatchNo(), 10);
        String totalAmt = Component.getPaddedNumber(total.getSaleTotalAmt(), 12);
        String totalTrans = Component.getPaddedNumber(total.getSaleVoidTotalNum(), 12);
        transData.setField63(currentBatch + totalAmt + totalTrans);

        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.SETTLE_MYPROMPT.getTransName());
        }

        transData.setField63(initBatchTotalsWallet(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        if ("00".equals(responseCode.getCode())) {
            settledRefNo = transData.getRefNo();
            Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRE-BATCH-UPLOAD)");
            Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRE-BATCH-UPLOAD)");
            Component.initField63Wallet(transData);
            total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());
            return TransResult.SUCC_NOREQ_BATCH;
        }
        if (listener != null) {
            listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail

        settledRefNo = transData.getRefNo();
        Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        return TransResult.ERR_HOST_REJECT;
    }

    /**
     * Settlement for Wallet transaction
     *
     * @return {@link TransResult}
     */
    public int settleWallet(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalWallet(listener);
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        ret = transmit.sendAdviceWallet(null, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = settleWalletRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        ret = batchUpWallet(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * Wallet Settlement Request message
     *
     * @return {@link TransResult}
     */
    private int settleWalletRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.SETTLE_WALLET);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.SETTLE_WALLET.getTransName());
        }

        transData.setField63(initBatchTotalsWallet(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRE-BATCH-UPLOAD)");
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRE-BATCH-UPLOAD)");
                Component.initField63Wallet(transData);
                total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());
                return TransResult.SUCC_NOREQ_BATCH;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }

        settledRefNo = transData.getRefNo();
        Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        return TransResult.SUCC;
    }

    /**
     * Wallet Settlement End Request message
     *
     * @return {@link TransResult}
     */
    private int batchUpEndWallet(TransTotal total, TransProcessListener listener) {
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.SETTLE_END_WALLET.getTransName());

        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.SETTLE_END_WALLET);

        transData.setField63(initBatchTotalsWallet(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ResponseCode responseCode = transData.getResponseCode();
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                Component.initField63Wallet(transData);
                total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());

                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                return TransResult.SUCC;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }

        return TransResult.ERR_RECONCILE_FAILED;
    }

    private String initBatchTotalsWallet(TransTotal total) {
        String saleAmt;
        String saleNum;
        String refundAmt;
        String refundNum;

        String buf;
        saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        if (total.getRefundTotalAmt() > 0) {
            refundAmt = getPaddedNumber(total.getRefundTotalAmt(), 12);
            refundNum = getPaddedNumber(total.getRefundTotalNum(), 3);
            buf = saleNum + saleAmt + refundNum + refundAmt;
        } else {
            buf = saleNum + saleAmt;
        }

        return getPaddedStringRight(buf, 90, '0');
    }

    /**
     * Batch Upload of Wallet trans
     *
     * @return {@link TransResult}
     */
    private int batchUpWallet(TransTotal total, TransProcessListener listener) {
        int ret;
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.BATCH_UP_WALLET.getTransName());

        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false);
        // If no transaction for batch upload, should do settlement end.
        if (cnt > 0) {
            int resendTimes = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_REDIAL_TIMES);
            int sendCnt = 0;
            final boolean[] left = new boolean[]{false};
            while (sendCnt < resendTimes + 1) {
                ret = new AllWalletTransBatch(listener, new BatchUpWalletListener() {

                    @Override
                    public void onLeftResult(boolean l) {
                        left[0] = l;
                    }
                }).process();
                if (ret != TransResult.SUCC) {
                    return ret;
                }
                if (!left[0]) {
                    break;
                }
                left[0] = false;
                sendCnt++;
            }
        }
        ret = batchUpEndWallet(total, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }
        return TransResult.SUCC;
    }

    public int referralTransSend(TransProcessListener listener, boolean isSettlement) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        List<TransData> records = FinancialApplication.getTransDataDbHelper().findAllReferredTrans(acquirer, false);
        if (records == null || (records != null && records.isEmpty())) {
            return TransResult.SUCC;
        }

        if (acquirer == null || (!isSettlement && acquirer.isDisableTrickFeed())) {
            return TransResult.SUCC;
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_referral));
        }

        int ret = new ReferralTransProc(records, listener).process();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        return TransResult.SUCC;
    }

    public int tcAdviceTransSend(TransProcessListener listener) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        List<TransData> records = FinancialApplication.getTransDataDbHelper().findAllAdviceRecord(acquirer);
        if (records == null || (records != null && records.isEmpty())) {
            return TransResult.SUCC;
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice));
        }

        int ret = new TcAdviceTransProc(records, listener).process();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        return TransResult.SUCC;
    }

    public int tcAdviceInstalmentTransSend(TransProcessListener listener) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();
        List<TransData> records = FinancialApplication.getTransDataDbHelper().findAllAdviceRecord(acquirer);
        if (records == null || records.isEmpty()) {
            return TransResult.SUCC;
        }

        if (listener != null) {
            listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice));
        }

        int ret = new TcAdviceInstalmentTransProc(records, listener).process();
        if (ret != TransResult.SUCC) {
            return ret;
        }

        return TransResult.SUCC;
    }

    /**********************************************************
     *                 KBANK WALLET SETTLEMNT
     *********************************************************/
    public int settleKbankPromptpay(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS));
        if (ret != TransResult.SUCC) {
            return ret;
        }

    /*ret = transmit.sendAdviceWallet(null, listener);
    if (ret != TransResult.SUCC) {
        return ret;
    }*/

        // for Promptpay (K+) settlement
        ret = settleKplusRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }
        /*ret = batchUpWallet(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }*/

        return TransResult.SUCC;
    }

    public int settleKbankAlipay(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY));
        if (ret != TransResult.SUCC) {
            return ret;
        }

    /*ret = transmit.sendAdviceWallet(null, listener);
    if (ret != TransResult.SUCC) {
        return ret;
    }*/

        // for Promptpay (K+) settlement
        ret = settleAlipayRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }
        /*ret = batchUpWallet(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }*/

        return TransResult.SUCC;
    }

    public int settleKbankAlipayBscanC(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ALIPAY_B_SCAN_C));
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = settleAlipayRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    public int settleKbankWechat(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT));
        if (ret != TransResult.SUCC) {
            return ret;
        }

    /*ret = transmit.sendAdviceWallet(null, listener);
    if (ret != TransResult.SUCC) {
        return ret;
    }*/

        // for Promptpay (K+) settlement
        ret = settleWechatRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }
        /*ret = batchUpWallet(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }*/

        return TransResult.SUCC;
    }

    public int settleKbankWechatBscanC(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WECHAT_B_SCAN_C));
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = settleWechatRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    public int settleKbankQRCredit(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversalQR(listener, FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_QR_CREDIT));
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = settleQRCreditRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    private int settleKplusRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.QR_KPLUS_SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.QR_KPLUS_SETTLE.getTransName());
        }

        //transData.setField63(BuildSettleKplusBit63(total));
        transData.setField63Byte(BuildSettleKplusBit63(total));
        //setBitData("63", BuildSettleKplusBit63(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        } else {
            if ("00".equals(transData.getResponseCode().getCode())) {
                settledRefNo = transData.getRefNo();
            }
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        //in case has batch upload
    /*if (!"95".equals(responseCode.getCode())) {
        if ("00".equals(responseCode.getCode())) {
            Component.initField63Wallet(transData);
            total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());
            return TransResult.SUCC_NOREQ_BATCH;
        }
        if (listener != null) {
            listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
        }
        handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
        return TransResult.ERR_HOST_REJECT;
    }

	return TransResult.SUCC;
	*/
        //in case no batch upload
        if ("00".equals(responseCode.getCode())) {
            return TransResult.SUCC_NOREQ_BATCH;
        } else {
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set calue to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }


    }

    private int settleAlipayRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.QR_KPLUS_SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.QR_KPLUS_SETTLE.getTransName());
        }

        //setBitData("63", BuildSettleAlipayBit63(total));
        transData.setField63Byte(BuildSettleAlipayBit63(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        //in case has batch upload
    /*if (!"95".equals(responseCode.getCode())) {
    if ("00".equals(responseCode.getCode())) {
        Component.initField63Wallet(transData);
        total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());
        return TransResult.SUCC_NOREQ_BATCH;
    }
    if (listener != null) {
        listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
    }
    handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
    return TransResult.ERR_HOST_REJECT;
    }

	return TransResult.SUCC;
	*/
        //in case no batch upload
        if ("00".equals(responseCode.getCode())) {
            settledRefNo = transData.getRefNo();
            Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            return TransResult.SUCC_NOREQ_BATCH;
        } else {
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set calue to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }


    }

    private int settleWechatRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.QR_KPLUS_SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.QR_KPLUS_SETTLE.getTransName());
        }

        transData.setField63Byte(BuildSettleWechatBit63(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        //in case has batch upload
    /*if (!"95".equals(responseCode.getCode())) {
    if ("00".equals(responseCode.getCode())) {
        Component.initField63Wallet(transData);
        total.setWalletSettleSlipInfo(transData.getWalletSlipInfo());
        return TransResult.SUCC_NOREQ_BATCH;
    }
    if (listener != null) {
        listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
    }
    handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
    return TransResult.ERR_HOST_REJECT;
    }

	return TransResult.SUCC;
	*/
        //in case no batch upload
        if ("00".equals(responseCode.getCode())) {
            settledRefNo = transData.getRefNo();
            Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            return TransResult.SUCC_NOREQ_BATCH;
        } else {
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set calue to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }


    }

    private int settleQRCreditRequest(TransTotal total, TransProcessListener listener) {
        TransData transData = transInit();

        Acquirer acquirer = total.getAcquirer();
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setTpdu("600" + acquirer.getNii() + "0000");
        transData.setAcquirer(acquirer);

        transData.setTransType(ETransType.QR_CREDIT_SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.QR_CREDIT_SETTLE.getTransName());
        }

        transData.setField63Byte(BuildSettleQRCreditBit63(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();

        //in case no batch upload
        if ("00".equals(responseCode.getCode())) {
            settledRefNo = transData.getRefNo();
            Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
            return TransResult.SUCC_NOREQ_BATCH;
        } else {
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set calue to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }
    }

    /**
     * Redeem Settlement
     *
     * @param total
     * @param listener
     * @return
     */
    public int settleRedeem(TransTotal total, TransProcessListener listener) {
        int ret;
        Transmit transmit = new Transmit();

        ret = transmit.sendReversal(listener, total.getAcquirer());
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        ret = settleRedeemRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        ret = batchUpRedeem(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * Settle Redeem Request
     *
     * @param total
     * @param listener
     * @return
     */
    private int settleRedeemRequest(TransTotal total, TransProcessListener listener) {

        TransData transData = transInit();
        transData.setTransType(ETransType.KBANK_REDEEM_SETTLE);
        if (listener != null) {
            listener.onUpdateProgressTitle(ETransType.KBANK_REDEEM_SETTLE.getTransName());
        }
        String f60 = getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6);//use current batch no. for each acquirer same as S500
        transData.setField60(f60);

        //todo generate field 63 as per specification
        transData.setField63(buildSettleRedeemBit63Str(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                // increase trace no. when settlement success,
                // if response code is 95 (batch upload occurs), trace no. will be increased after settlement end complete.
                settledRefNo = transData.getRefNo();
                incTraceNo(null);
                return TransResult.SUCC_NOREQ_BATCH;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }
        settledRefNo = transData.getRefNo();
        return TransResult.SUCC;
    }

    /**
     * Batch up Redeem
     *
     * @param total
     * @param listener
     * @return
     */
    private int batchUpRedeem(TransTotal total, TransProcessListener listener) {
        int ret;
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.KBANK_REDEEM_BATCH_UP.getTransName());
//        List<ETransType> list = new ArrayList<>();
//        list.add(ETransType.KBANK_REDEEM_VOID);
//        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false, list);
        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false);
        // If no transaction for batch upload, should do settlement end.
        if (cnt > 0) {
            // 获取交易重复次数
            int resendTimes = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_REDIAL_TIMES);
            int sendCnt = 0;
            final boolean[] left = new boolean[]{false};
            while (sendCnt < resendTimes + 1) {
                ret = new AllRedeemTransBatch(listener, new BatchUpRedeemListener() {

                    @Override
                    public void onLeftResult(boolean l) {
                        left[0] = l;
                    }
                }).process();
                if (ret != TransResult.SUCC) {
                    return ret;
                }
                if (!left[0]) {
                    break;
                }
                left[0] = false;
                sendCnt++;
            }
        }
        ret = settleEndRedeem(total, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }
        return TransResult.SUCC;
    }

    private int settleEndRedeem(TransTotal total, TransProcessListener listener) {
        if (listener != null)
            listener.onUpdateProgressTitle(ETransType.KBANK_REDEEM_SETTLE_END.getTransName());

        TransData transData = transInit();
        transData.setTransType(ETransType.KBANK_REDEEM_SETTLE_END);

        String f60 = getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6);//use current batch no. for each acquirer same as S500
        transData.setField60(f60);

        //todo generate field 63 as per specification
        transData.setField63(buildSettleRedeemBit63Str(total));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ResponseCode responseCode = transData.getResponseCode();
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
                return TransResult.SUCC;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }
        return TransResult.ERR_RECONCILE_FAILED;
    }

    public int settleDoflinInstal(TransTotal total, TransProcessListener listener) {
        Transmit transmit = new Transmit();

        int ret = transmit.sendReversal(listener, total.getAcquirer());
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * Instalment Settlement
     *
     * @param total
     * @param listener
     * @return
     */
    public int settleInstalment(TransTotal total, TransProcessListener listener) {
        Transmit transmit = new Transmit();

        int ret = transmit.sendReversal(listener, total.getAcquirer());
        if (ret == TransResult.ERR_ABORTED) {
            return ret;
        }

        if (total.getAcquirer().isEmvTcAdvice())
            ret = transmit.sendTcAdviceInstalmentTrans(listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ret = settleInstalmentRequest(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        ret = batchUpInstalment(total, listener);
        if (ret != TransResult.SUCC) {
            if (listener != null) {
                listener.onHideProgress();
            }
            return ret;
        }

        return TransResult.SUCC;
    }

    /**
     * Settle Instalment Request
     *
     * @param total
     * @param listener
     * @return
     */
    private int settleInstalmentRequest(TransTotal total, TransProcessListener listener) {

        TransData transData = transInit();
        transData.setTransType(ETransType.KBANK_SMART_PAY_SETTLE);
        if (listener != null) {
            String transName = ETransType.KBANK_SMART_PAY_SETTLE.getTransName();

            if (isDolfinIpp(total.getAcquirer()))
                transName = Utils.getString(R.string.trans_dolfin_instalment_settle);

            listener.onUpdateProgressTitle(transName);
        }
        String f60 = getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6);//use current batch no. for each acquirer same as S500
        transData.setField60(f60);

        String saleTotalNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        String saleTotalAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        String f63 = saleTotalNum + saleTotalAmt;
        transData.setField63(getPaddedStringRight(f63, 90, '0'));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ResponseCode responseCode = transData.getResponseCode();
        //AET-31
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                // increase trace no. when settlement success,
                // if response code is 95 (batch upload occurs), trace no. will be increased after settlement end complete.

                settledRefNo = transData.getRefNo();
                Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRED-BATCH-UPLOAD)");
                Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo + "  (NOT-REQUIRED-BATCH-UPLOAD)");
                incTraceNo(null);
                return TransResult.SUCC_NOREQ_BATCH;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }


        settledRefNo = transData.getRefNo();
        Log.d("Online", "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        Log.d(EReceiptUtils.TAG, "[" + transData.getTransType().name() + ".ReferencNo] = " + settledRefNo);
        return TransResult.SUCC;
    }

    /**
     * Batch up Instalment
     *
     * @param total
     * @param listener
     * @return
     */
    private int batchUpInstalment(TransTotal total, TransProcessListener listener) {
        int ret;
        if (listener != null) {
            String transName = ETransType.KBANK_SMART_PAY_BATCH_UP.getTransName();

            if (isDolfinIpp(total.getAcquirer()))
                transName = ETransType.DOLFIN_INSTALMENT_BATCH_UP.getTransName();

            listener.onUpdateProgressTitle(transName);
        }
//        List<ETransType> list = new ArrayList<>();
//        list.add(ETransType.KBANK_SMART_PAY_VOID);
//        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false, list);
        long cnt = FinancialApplication.getTransDataDbHelper().countOf(total.getAcquirer(), false);
        // If no transaction for batch upload, should do settlement end.
        if (cnt > 0) {
            // 获取交易重复次数
            int resendTimes = FinancialApplication.getSysParam().get(SysParam.NumberParam.COMM_REDIAL_TIMES);
            int sendCnt = 0;
            final boolean[] left = new boolean[]{false};
            while (sendCnt < resendTimes + 1) {
                ret = new AllInstalmentTransBatch(listener, new BatchUpInstalmentListener() {

                    @Override
                    public void onLeftResult(boolean l) {
                        left[0] = l;
                    }
                }).process();
                if (ret != TransResult.SUCC) {
                    return ret;
                }
                if (!left[0]) {
                    break;
                }
                left[0] = false;
                sendCnt++;
            }
        }
        ret = settleEndInstalment(total, listener);
        if (ret != TransResult.SUCC) {
            return ret;
        }
        return TransResult.SUCC;
    }

    private int settleEndInstalment(TransTotal total, TransProcessListener listener) {
        if (listener != null) {
            String transName = ETransType.KBANK_SMART_PAY_SETTLE_END.getTransName();

            if (isDolfinIpp(total.getAcquirer()))
                transName = Utils.getString(R.string.trans_dolfin_instalment_settle_end);

            listener.onUpdateProgressTitle(transName);
        }

        TransData transData = transInit();
        transData.setTransType(ETransType.KBANK_SMART_PAY_SETTLE_END);

        String f60 = getPaddedNumber(total.getAcquirer().getCurrBatchNo(), 6);//use current batch no. for each acquirer same as S500
        transData.setField60(f60);

        String saleTotalNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        String saleTotalAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        String f63 = saleTotalNum + saleTotalAmt;
        transData.setField63(getPaddedStringRight(f63, 90, '0'));

        int ret = online.online(transData, listener);
        if (listener != null) {
            listener.onHideProgress();
        }
        if (ret != TransResult.SUCC) {
            return ret;
        }

        ResponseCode responseCode = transData.getResponseCode();
        if (!"95".equals(responseCode.getCode())) {
            if ("00".equals(responseCode.getCode())) {
                settledRefNo = transData.getRefNo();
                return TransResult.SUCC;
            }
            if (listener != null) {
                listener.onShowErrMessage(responseCode.getMessage(), Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
            handleSettleCaseFail(responseCode.getCode());// Set value to handle for settlement fail
            return TransResult.ERR_HOST_REJECT;
        }

        return TransResult.ERR_RECONCILE_FAILED;
    }

    private byte[] BuildSettleKplusBit63(TransTotal total) {

        //For settlement Promptpay(K+)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;

        //Payment Txn count
        String saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        outputStream.write(saleNum.getBytes(), 0, saleNum.getBytes().length);

        //Payment Txn Amt
        String saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        outputStream.write(saleAmt.getBytes(), 0, saleAmt.getBytes().length);

        //Application Code (Promptpay K+)
        bytes = new byte[]{0x30, 0x33};
        outputStream.write(bytes, 0, bytes.length);

        return outputStream.toByteArray();
    }

    private byte[] BuildSettleAlipayBit63(TransTotal total) {

        //For settlement Alipay
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;

        //Payment Txn count
        String saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        outputStream.write(saleNum.getBytes(), 0, saleNum.getBytes().length);

        //Payment Txn Amt
        String saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        outputStream.write(saleAmt.getBytes(), 0, saleAmt.getBytes().length);

        //Application Code (Alipay)
        bytes = new byte[]{0x30, 0x31};
        outputStream.write(bytes, 0, bytes.length);

        return outputStream.toByteArray();
    }

    private byte[] BuildSettleWechatBit63(TransTotal total) {

        //For settlement Wechat
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;

        //Payment Txn count
        String saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        outputStream.write(saleNum.getBytes(), 0, saleNum.getBytes().length);

        //Payment Txn Amt
        String saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        outputStream.write(saleAmt.getBytes(), 0, saleAmt.getBytes().length);

        //Application Code (Promptpay Wechat)
        bytes = new byte[]{0x30, 0x32};
        outputStream.write(bytes, 0, bytes.length);

        return outputStream.toByteArray();
    }

    private byte[] BuildSettleQRCreditBit63(TransTotal total) {

        //For settlement QR Credit
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;

        //Payment Txn count
        String saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        outputStream.write(saleNum.getBytes(), 0, saleNum.getBytes().length);

        //Payment Txn Amt
        String saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        outputStream.write(saleAmt.getBytes(), 0, saleAmt.getBytes().length);

        //Application Code (QR Credit)
        bytes = new byte[]{0x30, 0x34};
        outputStream.write(bytes, 0, bytes.length);

        return outputStream.toByteArray();
    }

    private String buildSettleRedeemBit63Str(TransTotal total) {
        TransRedeemKbankTotal transRedeemKbankTotal = total.getTransRedeemKbankTotal() != null ? total.getTransRedeemKbankTotal() : null;
        if (transRedeemKbankTotal != null) {
            /*String fixDebitData = "000000000000000";//DEBIT-NBR = "000" + DEBIT-AMT = "000000000000"
            String creditNbr = getPaddedNumber(transRedeemKbankTotal.getProductCreditAllCard() + transRedeemKbankTotal.getVoucherCreditAllCard(), 3);
            String creditAmt = getPaddedNumber(transRedeemKbankTotal.getProductCreditTotal() + transRedeemKbankTotal.getVoucherCreditCredit(), 12);*/

            String debitNbr = getPaddedNumber(transRedeemKbankTotal.getProductCreditAllCard() + transRedeemKbankTotal.getVoucherCreditAllCard() + transRedeemKbankTotal.getDiscountAllCard(), 3);
            String debitAmt = getPaddedNumber(transRedeemKbankTotal.getProductCreditTotal() + transRedeemKbankTotal.getVoucherCreditCredit() + transRedeemKbankTotal.getDiscountCredit(), 12);
            String fixCreditData = "000000000000000"; //CREDIT-NBR = "000" + CREDIT-AMT = "000000000000"
            String productCount = getPaddedNumber(transRedeemKbankTotal.getProductAllCard(), 3);
            String productPoints = getPaddedNumber(transRedeemKbankTotal.getProductPoints(), 9);
            String voucherCount = getPaddedNumber(transRedeemKbankTotal.getVoucherAllCard() + transRedeemKbankTotal.getVoucherCreditAllCard(), 3);
            String voucherPoints = getPaddedNumber(transRedeemKbankTotal.getVoucherPoints() + transRedeemKbankTotal.getVoucherCreditPoints(), 9);
            String discountCount = getPaddedNumber(transRedeemKbankTotal.getDiscountAllCard(), 3);
            String discountPoints = getPaddedNumber(transRedeemKbankTotal.getDiscountPoints(), 9);

            return debitNbr + debitAmt + fixCreditData + productCount + productPoints + voucherCount + voucherPoints + discountCount + discountPoints;
        } else {
            return null;
        }
    }

    interface BatchUpListener {
        void onLeftResult(boolean left);
    }

    interface BatchUpWalletListener {
        void onLeftResult(boolean left);
    }

    interface BatchUpRedeemListener {
        void onLeftResult(boolean left);
    }

    interface BatchUpInstalmentListener {
        void onLeftResult(boolean left);
    }

    /**
     * 全部磁条卡的请求类联机成功交易明细上送
     */
    private class AllMagCardTransBatch {
        private final TransProcessListener listener;
        private final BatchUpListener batchUpListener;
        private boolean isFirst = true;
        private int ret = TransResult.SUCC;
        private boolean hasNext = true;

        AllMagCardTransBatch(TransProcessListener listener, BatchUpListener batchUpListener) {
            this.listener = listener;
            this.batchUpListener = batchUpListener;
        }

        private boolean needUpload(TransData transLog) {
            //Cz comment because Void trans, need to be populated in batch upload.
            return !(transLog.getTransType() == ETransType.PREAUTH /*|| transLog.getTransType() == ETransType.VOID*/ || transLog.isUpload() ||
                    (transLog.getTransState() == TransData.ETransStatus.ADJUSTED && transLog.getOfflineSendState() == OfflineStatus.OFFLINE_SENT));
        }

        private TransData genTranData(TransData transLog) {
//            transLog.setOrigTransType(transLog.getTransType());

            if (!needUpload(transLog))
                return null;
            TransData transData = transInit();
            // field 2
            String field2 = transLog.getPan();
            if (field2 != null) {
                transData.setPan(field2);
            }

            boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName()) ||
                    Constants.ACQ_AMEX_EPP.equals(transData.getAcquirer().getName()) ||
                    Constants.ISSUER_AMEX.equals(transLog.getIssuer().getName());
            boolean isVoid = transLog.getTransType() == ETransType.VOID;

            // field 4 If Void trans, the amount should be zero, Otherwise amount from original trans.
            transData.setAmount(isVoid ? "0" : transLog.getAmount());
            // All Host use the system stan.
//            if (!isAmex) {//AMEX, use the system stan.
//                //field 11
//                String field11 = getPaddedNumber(transLog.getStanNo(), 6);
//                transData.setStanNo(Utils.parseLongSafe(field11, -1));
//            }
            //field 12
            String dateTime = transLog.getDateTime();
            if (dateTime != null) {
                transData.setDateTime(dateTime);
            }
            //field 13
            String date = transLog.getExpDate();
            if (date != null) {
                transData.setExpDate(date);
            }
            //field 22
            transData.setEnterMode(transLog.getEnterMode());
            transData.setHasPin(transLog.isHasPin());
            //field 23
            String cardSerialNo = transLog.getCardSerialNo();
            if (cardSerialNo != null) {
                transData.setCardSerialNo(transLog.getCardSerialNo());
            }
            //field 24
            String nii = transLog.getNii();
            if (nii != null) {
                transData.setNii(nii);
            }
            //field 37
            String refNo = transLog.getRefNo();
            if (refNo != null) {
                transData.setRefNo(refNo);
            }
            //field 38
            String authCode = transLog.getAuthCode();
            if (authCode != null) {
                transData.setOrigAuthCode(authCode);
            }
            //field 39
            transData.setResponseCode(transLog.getResponseCode());
            //field 55
            String sendIccData = transLog.getSendIccData();
            if (sendIccData != null) {
                transData.setSendIccData(sendIccData);
            }
            //field 6, 10, 51
            if (transLog.isDccRequired()) {
                transData.setDccRequired(transLog.isDccRequired());
                transData.setDccAmount(transLog.getDccAmount());
                transData.setDccConversionRate(transLog.getDccConversionRate());
                transData.setDccCurrencyCode(transLog.getDccCurrencyCode());
            }

            //field 63
            byte[] field63Byte = transLog.getField63Byte();
            if (field63Byte != null) {
                transData.setField63Byte(field63Byte);
            }

            //field 48 - Instalment Amex
            String instalmentTerms = transLog.getInstalmentTerms();
            if (instalmentTerms != null) {
                transData.setInstalmentTerms(instalmentTerms);
            }

            transData.setOrigTransType(transLog.getTransType());
            transData.setOrigTransNo(transLog.getStanNo());//for AMEX, use in field 60
            transData.setTraceNo(transLog.getTraceNo());
            transData.setIssuer(transLog.getIssuer());
            transData.setTransType(ETransType.BATCH_UP);

            if (isVoid) {//for void trans, use processing code from original trans (Sale or Refund)
                transData.setProcCode(isAmex ? getProcCodeAmex(transLog.getOrigTransType()) : getProcCode(transLog.getOrigTransType().getProcCode(), transLog));
            } else {
                transData.setProcCode(isAmex ? getProcCodeAmex(transLog.getTransType()) : getProcCode(transLog.getTransType().getProcCode(), transLog));
            }

            transData.setReferralSentSuccess(isAmex && transLog.getReferralStatus() == ReferralStatus.REFERRED_SUCC);//only Amex, check transaction is sent referred trans. or not

            return transData;
        }

        private String getProcCode(String procCode, TransData origTrans) {
            boolean isRefund = origTrans.getTransType() == ETransType.REFUND || origTrans.getOrigTransType() == ETransType.REFUND;
            int iProcCode = Integer.parseInt(procCode);
            if (isRefund) {
                if (Constants.ACQ_UP.equals(origTrans.getAcquirer().getName())) {
                    return hasNext ? getPaddedNumber(iProcCode + 1, 6) : procCode;
                } else {
                    return hasNext ? getPaddedNumber(iProcCode + 4000 + 1, 6) : getPaddedNumber(iProcCode + 4000, 6);
                }
            }
            return hasNext ? getPaddedNumber(Integer.parseInt(procCode) + 1, 6) : procCode;
        }

        private String getProcCodeAmex(ETransType origTransType) {
            switch (origTransType) {
                case AMEX_INSTALMENT:
                case OFFLINE_TRANS_SEND:
                case SALE:
                    return hasNext ? getPaddedNumber(Integer.parseInt(PackIso8583.PROC_CODE_SALE_AMEX) + 1, 6) : PackIso8583.PROC_CODE_SALE_AMEX;
                case REFUND:
                    return hasNext ? getPaddedNumber(Integer.parseInt(PackIso8583.PROC_CODE_REFUND_AMEX) + 1, 6) : PackIso8583.PROC_CODE_REFUND_AMEX;
            }
            return "000000";
        }

        private boolean continueOnline(TransData transData) {
            if (transData == null)
                return true;
            ret = online.online(transData, listener, isFirst, false);
            isFirst = false;
            if (ret != TransResult.SUCC) {
                // If found error in batch upload, the another batch will be stopped sending for that acquirer.
                return false;
            }
            return true;
        }

        /**
         * @return {@link TransResult}
         */
        int process() {
            List<TransData> transDataList = FinancialApplication.getTransDataDbHelper().findAllTransData(FinancialApplication.getAcqManager().getCurAcq(), false, true, true);
            if (transDataList.isEmpty()) {
                return TransResult.ERR_NO_TRANS;
            }
            ArrayList<TransData> allTrans = new ArrayList<>(transDataList);
            ArrayListUtils.INSTANCE.removeAdjustState(allTrans);
            ArrayListUtils.INSTANCE.removePreAuthAndPreAuthCancel(allTrans);
            int transCnt = allTrans.size();

            isFirst = true;
            for (int cnt = 0; cnt < transCnt; cnt++) {
                int transIndex = cnt + 1;
                updateProgressTitle(transIndex, transCnt);
                hasNext = transIndex < transCnt;
                if (!continueOnline(genTranData(allTrans.get(cnt))))
                    break;
            }
            online.close();
            return ret;
        }

        private void updateProgressTitle(int cnt, int total) {
            if (listener != null)
                listener.onUpdateProgressTitle(ETransType.BATCH_UP.getTransName() + "[" + cnt + "/" + total + "]");
        }
    }

    /****************************************************************************
     * OfflineTransProc 离线交易上送处理
     ****************************************************************************/
    private class OfflineTransProc {
        private final int sendMaxTime;
        private final List<TransData> records;
        private final TransProcessListener listener;

        private int dupNum = 0;// 重发次数
        private boolean isLastTime = false;
        private int sendCount = 0;
        private TransData transData;
        private int result = TransResult.SUCC;

        OfflineTransProc(int sendMaxTime, List<TransData> records, TransProcessListener listener) {
            this.sendMaxTime = sendMaxTime;
            this.records = records;
            this.listener = listener;
        }

        private boolean isFilteredOfflineTran(TransData record) {
            // 跳过上送不成功的和应答码非"00"的交易
            return record.getOfflineSendState() == null || !record.getOfflineSendState().equals(OfflineStatus.OFFLINE_NOT_SENT);
        }

        private boolean uploadAll() {
            for (TransData record : records) { // 逐笔上送
                if (isFilteredOfflineTran(record))
                    continue;
                boolean isContinue = handleOnlineResult(record, uploadOne(record));
                if (!isContinue && result != TransResult.SUCC)
                    return false;
            }
            return true;
        }

        private int uploadOne(TransData record) {
            sendCount++;
            if (listener != null) {
                listener.onUpdateProgressTitle(ETransType.OFFLINE_TRANS_SEND.getTransName() + "[" + sendCount + "]");
            }
            transData = new TransData(record);
            transData.setTransType(ETransType.OFFLINE_TRANS_SEND);
            transInit(transData);
            transData.setStanNo(record.getStanNo());
            return online.online(transData, listener);
        }

        private boolean handleOnlineResult(TransData record, int ret) {
            return ret != TransResult.SUCC ? handleOnlineFailedCase(record, ret)
                    : handleOnlineSuccCase(record);
        }

        private boolean handleOnlineFailedCase(TransData record, int ret) {
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK
                    || ret == TransResult.ERR_MAC) {
                // 如果是发送数据时发生错误(连接错、发送错、数据包错、接收失败、MAC错)，则直接退出，不进行重发
                if (!record.getEmvResult().name().equalsIgnoreCase(ETransResult.OFFLINE_APPROVED.toString())) { //Don't show error for offline send
                    showErrMsg(TransResultUtils.getMessage(ret));
                }
                result = TransResult.ERR_ABORTED;
                return false;
            }

            // BCTC要求离线交易上送时，如果平台无应答要离线交易上送次数上送
            // 未达到上送次数，继续送， 如果已达到上送次数，但接收失败按失败处理，不再上送
            if (ret == TransResult.ERR_RECV && !isLastTime) {
                return true;
            }
            record.setOfflineSendState(OfflineStatus.OFFLINE_ERR_SEND);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        private boolean handleOnlineSuccCase(TransData record) {
            ResponseCode responseCode = transData.getResponseCode();
            // 返回码失败处理
            if ("A0".equals(responseCode.getCode())) {
                showErrMsg(responseCode.getMessage());
                result = TransResult.ERR_ABORTED;
                return false;
            }
            if (!"00".equals(responseCode.getCode()) && !"94".equals(responseCode.getCode())) { //AET-28
                showErrMsg(responseCode.getMessage());
                record.setOfflineSendState(OfflineStatus.OFFLINE_ERR_RESP);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return true;
            }

            record.setSettleDateTime(transData.getSettleDateTime() != null ? transData.getSettleDateTime() : "");
            record.setAuthCode(transData.getAuthCode() != null ? transData.getAuthCode() : "");
            record.setRefNo(transData.getRefNo());

            record.setAcqCode(transData.getAcqCode() != null ? transData.getAcqCode() : "");
            record.setIssuerCode(transData.getIssuerCode() != null ? transData.getIssuerCode() : "");

            record.setReserved(transData.getReserved() != null ? transData.getReserved() : "");

            record.setAuthCode(transData.getAuthCode());
            record.setOfflineSendState(OfflineStatus.OFFLINE_SENT);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        int process() {
            while (dupNum < sendMaxTime + 1) {
                sendCount = 0;
                if (dupNum == sendMaxTime) {
                    isLastTime = true;
                }
                if (!uploadAll()) {
                    return result;
                }
                dupNum++;
            }
            if (listener != null)
                listener.onHideProgress();
            return TransResult.SUCC;
        }

        private void showErrMsg(String str) {
            if (listener != null) {
                listener.onShowErrMessage(str, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }
    }

    /****************************************************************************
     * OfflineTransSendProc
     ****************************************************************************/
    private class OfflineTransSendProc {
        private final List<TransData> records;
        private final TransProcessListener listener;

        private int sendCount = 0;
        private int totalRecords = 0;
        private TransData transData;
        private int result = TransResult.SUCC;

        OfflineTransSendProc(List<TransData> records, TransProcessListener listener) {
            this.records = records;
            this.listener = listener;
            this.totalRecords = records != null && !records.isEmpty() ? records.size() : totalRecords;
        }

        private boolean isFilteredOfflineState(TransData record) {
            return record.getOfflineSendState() == null || record.getOfflineSendState() == OfflineStatus.OFFLINE_SENT;
        }

        private boolean uploadAll() {
            for (TransData record : records) {
                if (isFilteredOfflineState(record))
                    continue;
                boolean isContinue = handleOnlineResult(record, uploadOne(record));
                if (!isContinue)
                    return false;
            }
            return true;
        }

        private int uploadOne(TransData record) {
            sendCount++;
            if (listener != null) {
                listener.onUpdateProgressTitle(Utils.getString(R.string.trans_upload_offline) + "[" + sendCount + "/" + totalRecords + "]");
            }
            transData = new TransData(record);
            transInit(transData, record.getAcquirer());
            transData.setDateTime(record.getOrigDateTime());
            transData.setStanNo(record.getStanNo());
            transData.setTraceNo(record.getTraceNo());

            if (ETransType.OFFLINE_TRANS_SEND != record.getTransType()) {// sale below floor limit
                transData.setTransType(ETransType.OFFLINE_TRANS_SEND);
                transData.setOrigTransType(record.getTransType());
            }

            transData.setOfflineSendState(OfflineStatus.OFFLINE_SENDING);

            int ret = online.online(transData, listener);

            transData.setDateTime(record.getOrigDateTime());// set back the original date/time for use in void trans.
            transData.setTransType(record.getTransType());//set original transType

            return ret;
        }

        private boolean handleOnlineResult(TransData record, int ret) {
            result = ret;
            return ret != TransResult.SUCC ? handleOnlineFailedCase(record, ret)
                    : handleOnlineSuccCase(record);
        }

        private boolean handleOnlineFailedCase(TransData record, int ret) {
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK
                    || ret == TransResult.ERR_MAC || ret == TransResult.ERR_RECV) {
                showErrMsg(TransResultUtils.getMessage(ret));
                result = TransResult.ERR_ABORTED;

                OfflineStatus status;
                if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK) {
                    status = OfflineStatus.OFFLINE_ERR_SEND;
                } else if (ret == TransResult.ERR_RECV) {
                    status = OfflineStatus.OFFLINE_ERR_RESP;
                } else {
                    status = OfflineStatus.OFFLINE_ERR_UNKNOWN;
                }

                record.setOfflineSendState(status);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return false;
            }
            result = ret;
            record.setOfflineSendState(OfflineStatus.OFFLINE_ERR_UNKNOWN);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        private boolean handleOnlineSuccCase(TransData record) {
            if (transData.getResponseCode().getCode().equals("00")) {
                record.setRefNo(transData.getRefNo());
                record.setResponseCode(transData.getResponseCode());
                record.setOfflineSendState(OfflineStatus.OFFLINE_SENT);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                if (record.getTransType() == ETransType.ADJUST) {
                    TransData financialTran = FinancialApplication.getTransDataDbHelper().findTransDataByTraceNo(
                            record.getTraceNo(), true);
                    financialTran.setRefNo(record.getRefNo());
                    financialTran.setResponseCode(record.getResponseCode());
                    financialTran.setOfflineSendState(OfflineStatus.OFFLINE_SENT);
                    FinancialApplication.getTransDataDbHelper().updateTransData(financialTran);
                }
                return true;
            } else {
                record.setOfflineSendState(OfflineStatus.OFFLINE_ERR_SEND);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return false;
            }
        }

        int process() {
            if (!uploadAll()) {
                return TransResult.ERR_OFFLINE_UPLOAD_FAIL;
            }
            if (listener != null)
                listener.onHideProgress();
            return TransResult.SUCC;
        }

        private void showErrMsg(String str) {
            if (listener != null) {
                listener.onShowErrMessage(str, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }
    }

    /**
     * Process batch upload for wallet transaction
     */
    private class AllWalletTransBatch {
        private final TransProcessListener listener;
        private final BatchUpWalletListener batchUpListener;
        private boolean isFirst = true;
        private int ret = TransResult.SUCC;

        AllWalletTransBatch(TransProcessListener listener, BatchUpWalletListener batchUpListener) {
            this.listener = listener;
            this.batchUpListener = batchUpListener;
        }

        private TransData genTranData(TransData transLog) {
            Acquirer acquirer = transLog.getAcquirer();
            ETransType transType = transLog.getTransType();
            transLog.setOrigTransType(transLog.getTransType());

            TransData transData = transInit();

            transData.setBatchNo(acquirer.getCurrBatchNo());
            transData.setTpdu("600" + acquirer.getNii() + "0000");
            transData.setAcquirer(acquirer);

            // field 3 already in transInit

            // field 4
            transData.setAmount(transLog.getAmount());
            if (transType == ETransType.QR_VOID_WALLET)
                transData.setAmount("0");

            // field 11 already in transInit

            // field 12-13-17
            transData.setOrigDateTime(transLog.getDateTime());

            // field 24 set in PackWalletBatchUp

            // field 37
            String refNo = transLog.getRefNo();
            if (refNo != null) {
                transData.setRefNo(refNo);
            }

            // field 38
            String authCode = transLog.getAuthCode();
            if (authCode != null) {
                transData.setOrigAuthCode(authCode);
            }

            // field 41-42 set in packWalletBatchUp

            // field 62
            transData.setOrigTransNo(transLog.getTraceNo());
            if (transType == ETransType.QR_VOID_WALLET)
                transData.setOrigTransNo(transLog.getOrigTransNo());

            transData.setOrigTransType(transLog.getTransType());
            transData.setTransType(ETransType.BATCH_UP_WALLET);
            return transData;
        }

        private boolean continueOnline(TransData transData) {
            if (transData == null)
                return true;
            ret = online.online(transData, listener, isFirst, false);
            isFirst = false;
            if (ret != TransResult.SUCC) {
                // If found error in batch upload, the another batch will be stopped sending for that acquirer.
                return false;
            }
            return true;
        }

        /**
         * @return {@link TransResult}
         */
        int process() {
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_WALLET);
            List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllWalletTransData(false);
            if (allTrans.isEmpty()) {
                return TransResult.ERR_NO_TRANS;
            }
            int transCnt = allTrans.size();

            isFirst = true;
            for (int cnt = 0; cnt < transCnt; cnt++) {
                updateProgressTitle(cnt + 1, transCnt);
                if (!continueOnline(genTranData(allTrans.get(cnt))))
                    break;
            }
            online.close();
            return ret;
        }

        private void updateProgressTitle(int cnt, int total) {
            if (listener != null)
                listener.onUpdateProgressTitle(ETransType.BATCH_UP_WALLET.getTransName() + "[" + cnt + "/" + total + "]");
        }
    }

    /****************************************************************************
     * ReferralTransProc
     ****************************************************************************/
    private class ReferralTransProc {
        private final List<TransData> records;
        private final TransProcessListener listener;

        private boolean isLastTime = false;
        private int sendCount = 0;
        private TransData transData;
        private int result = TransResult.SUCC;

        ReferralTransProc(List<TransData> records, TransProcessListener listener) {
            this.records = records;
            this.listener = listener;
        }

        private boolean isFilteredReferralTrans(TransData record) {
            return record.getReferralStatus() == null || record.getReferralStatus().equals(ReferralStatus.NORMAL) || record.getReferralStatus().equals(ReferralStatus.REFERRED_SUCC);
        }

        private boolean uploadAll() {
            for (TransData record : records) {
                if (isFilteredReferralTrans(record))
                    continue;
                boolean isContinue = handleOnlineResult(record, uploadOne(record));
                if (!isContinue && result != TransResult.SUCC)
                    return false;
            }
            return true;
        }

        private int uploadOne(TransData record) {
            sendCount++;
            if (listener != null) {
                listener.onUpdateProgressTitle(Utils.getString(R.string.trans_referral) + "[" + sendCount + "]");
            }
            transData = new TransData(record);
            transInit(transData);
            transData.setReferralStatus(ReferralStatus.REFERRED);
            transData.setOrigAuthCode(record.getAuthCode());
            transData.setTraceNo(record.getTraceNo() + 1);

            //field 12 - 13
            String dateTime = record.getDateTime();
            if (dateTime != null) {
                transData.setDateTime(dateTime);
            }
            //field 14
            String expDate = record.getExpDate();
            if (expDate != null) {
                transData.setExpDate(expDate);
            }
            return online.online(transData, listener);
        }

        private boolean handleOnlineResult(TransData record, int ret) {
            return ret != TransResult.SUCC ? handleOnlineFailedCase(record, ret)
                    : handleOnlineSuccCase(record);
        }

        private boolean handleOnlineFailedCase(TransData record, int ret) {
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK
                    || ret == TransResult.ERR_MAC || ret == TransResult.ERR_RECV) {
                showErrMsg(TransResultUtils.getMessage(ret));
                result = TransResult.ERR_ABORTED;
                record.setReferralStatus(ReferralStatus.REFERRED_ERR_SEND);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return false;
            }
            result = ret;
            record.setReferralStatus(ReferralStatus.REFERRED_ERR_SEND);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        private boolean handleOnlineSuccCase(TransData record) {
            ResponseCode responseCode = transData.getResponseCode();
            if (!"00".equals(responseCode.getCode())) {
                if (record.getRefNo() == null) {
                    FinancialApplication.getTransDataDbHelper().deleteTransData(record.getId());
                    return true;
                }
            }
            record.setSettleDateTime(transData.getSettleDateTime() != null ? transData.getSettleDateTime() : "");
            record.setDateTime(transData.getDateTime());
            record.setStanNo(transData.getStanNo());
            record.setTraceNo(transData.getTraceNo());
            record.setAuthCode(transData.getAuthCode() != null ? transData.getAuthCode() : "");
            record.setRefNo(transData.getRefNo());
            record.setReferralStatus(ReferralStatus.REFERRED_SUCC);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        int process() {
            if (!uploadAll()) {
                return result;
            }
            if (listener != null)
                listener.onHideProgress();
            return TransResult.SUCC;
        }

        private void showErrMsg(String str) {
            if (listener != null) {
                listener.onShowErrMessage(str, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }
    }

    /****************************************************************************
     * TcAdviceTransProc
     ****************************************************************************/
    private class TcAdviceTransProc {
        private final List<TransData> records;
        private final TransProcessListener listener;

        private int sendCount = 0;
        private int totalRecords = 0;
        private TransData transData;
        private int result = TransResult.SUCC;

        TcAdviceTransProc(List<TransData> records, TransProcessListener listener) {
            this.records = records;
            this.listener = listener;
            this.totalRecords = records != null && !records.isEmpty() ? records.size() : totalRecords;
        }

        private boolean isFilteredTcAdviceTrans(TransData record) {
            return record.getAdviceStatus() == null || record.getAdviceStatus().equals(TransData.AdviceStatus.NORMAL);
        }

        private boolean uploadAll() {
            for (TransData record : records) {
                if (isFilteredTcAdviceTrans(record))
                    continue;
                boolean isContinue = handleOnlineResult(record, uploadOne(record));
                if (!isContinue && result != TransResult.SUCC)
                    return false;
            }
            return true;
        }

        private int uploadOne(TransData record) {
            sendCount++;
            if (listener != null) {
                listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice) + "[" + sendCount + "/" + totalRecords + "]");
            }

            transData = new TransData();
            transInit(transData, record.getAcquirer());
            transData.setTransType(ETransType.TCADVICE);

            transData.setPan(record.getPan());// field 2
            transData.setAmount(record.getAmount());// field 4
            transData.setDateTime(record.getDateTime());// field 12-13
            transData.setExpDate(record.getExpDate());// field 14
            transData.setEnterMode(record.getEnterMode());// field 22
            transData.setCardSerialNo(record.getCardSerialNo());// field 23
            transData.setOrigRefNo(record.getRefNo());// field 37
            transData.setOrigAuthCode(record.getAuthCode());// field 38
            transData.setResponseCode(record.getResponseCode());// field 39
            transData.setSendIccData(record.getSendIccData());// field 55
            transData.setOrigTransType(record.getTransType());// field 60
            transData.setOrigTransNo(record.getStanNo());// field 60
            transData.setTraceNo(record.getTraceNo()); // field 62

            transData.setAdviceStatus(TransData.AdviceStatus.ADVICE);
            return online.online(transData, listener);
        }

        private boolean handleOnlineResult(TransData record, int ret) {
            return ret != TransResult.SUCC ? handleOnlineFailedCase(record, ret)
                    : handleOnlineSuccCase(record);
        }

        private boolean handleOnlineFailedCase(TransData record, int ret) {
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK
                    || ret == TransResult.ERR_MAC || ret == TransResult.ERR_RECV) {
                showErrMsg(TransResultUtils.getMessage(ret));
                result = TransResult.ERR_ABORTED;
                record.setAdviceStatus(TransData.AdviceStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return false;
            }
            result = ret;
            record.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        private boolean handleOnlineSuccCase(TransData record) {
            record.setAdviceStatus(TransData.AdviceStatus.NORMAL);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return true;
        }

        int process() {
            if (!uploadAll()) {
                return result;
            }
            if (listener != null)
                listener.onHideProgress();
            return TransResult.SUCC;
        }

        private void showErrMsg(String str) {
            if (listener != null) {
                listener.onShowErrMessage(str, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }
    }

    /****************************************************************************
     * TcAdviceInstalmentTransProc
     ****************************************************************************/
    private class TcAdviceInstalmentTransProc {
        private final List<TransData> records;
        private final TransProcessListener listener;

        private int sendCount = 0;
        private int totalRecords = 0;
        private TransData transData;
        private int result = TransResult.SUCC;

        TcAdviceInstalmentTransProc(List<TransData> records, TransProcessListener listener) {
            this.records = records;
            this.listener = listener;
            this.totalRecords = records != null && !records.isEmpty() ? records.size() : totalRecords;
        }

        private boolean isFilteredTcAdviceTrans(TransData record) {
            return record.getAdviceStatus() == null || record.getAdviceStatus().equals(TransData.AdviceStatus.NORMAL);
        }

        private boolean uploadAll() {
            for (TransData record : records) {
                if (isFilteredTcAdviceTrans(record))
                    continue;
                boolean isContinue = handleOnlineResult(record, uploadOne(record));
                if (!isContinue && result != TransResult.SUCC)
                    return false;
            }
            return true;
        }

        private int uploadOne(TransData record) {
            sendCount++;
            if (listener != null) {
                listener.onUpdateProgressTitle(Utils.getString(R.string.trans_tcadvice) + "[" + sendCount + "/" + totalRecords + "]");
            }

            transData = new TransData();
            transInit(transData, record.getAcquirer());
            transData.setTransType(ETransType.KBANK_SMART_PAY_TCADVICE);

            transData.setIssuer(record.getIssuer());
            transData.setPan(record.getPan());// field 2
            transData.setAmount(record.getAmount());// field 4
            transData.setDateTime(record.getDateTime());// field 12-13
            transData.setExpDate(record.getExpDate());// field 14
            transData.setEnterMode(record.getEnterMode());// field 22
            transData.setCardSerialNo(record.getCardSerialNo());// field 23
            transData.setOrigRefNo(record.getRefNo());// field 37
            transData.setOrigAuthCode(record.getAuthCode());// field 38
            transData.setResponseCode(record.getResponseCode());// field 39
            transData.setSendIccData(record.getSendIccData());// field 55

            transData.setAdviceStatus(TransData.AdviceStatus.ADVICE);
            return online.online(transData, listener);
        }

        private boolean handleOnlineResult(TransData record, int ret) {
            return ret != TransResult.SUCC ? handleOnlineFailedCase(record, ret)
                    : handleOnlineSuccCase(record);
        }

        private boolean handleOnlineFailedCase(TransData record, int ret) {
            if (ret == TransResult.ERR_CONNECT || ret == TransResult.ERR_SEND || ret == TransResult.ERR_PACK
                    || ret == TransResult.ERR_MAC || ret == TransResult.ERR_RECV) {
                showErrMsg(TransResultUtils.getMessage(ret));
                result = TransResult.ERR_ABORTED;
                record.setAdviceStatus(TransData.AdviceStatus.PENDING);
                FinancialApplication.getTransDataDbHelper().updateTransData(record);
                return false;
            }
            result = ret;
            record.setAdviceStatus(TransData.AdviceStatus.PENDING);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return false;
        }

        private boolean handleOnlineSuccCase(TransData record) {
            record.setAdviceStatus(TransData.AdviceStatus.NORMAL);
            FinancialApplication.getTransDataDbHelper().updateTransData(record);
            return true;
        }

        int process() {
            if (!uploadAll()) {
                return result;
            }
            if (listener != null)
                listener.onHideProgress();
            return TransResult.SUCC;
        }

        private void showErrMsg(String str) {
            if (listener != null) {
                listener.onShowErrMessage(str, Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }
    }

    private class AllRedeemTransBatch {
        private final TransProcessListener listener;
        private final BatchUpRedeemListener batchUpListener;
        private boolean isFirst = true;
        private int ret = TransResult.SUCC;
        private boolean hasNext = true;

        AllRedeemTransBatch(TransProcessListener listener, BatchUpRedeemListener batchUpListener) {
            this.listener = listener;
            this.batchUpListener = batchUpListener;
        }

        private boolean needUpload(TransData transLog) {
            return (transLog.getTransType() == ETransType.KBANK_REDEEM_PRODUCT
                    || transLog.getTransType() == ETransType.KBANK_REDEEM_PRODUCT_CREDIT
                    || transLog.getTransType() == ETransType.KBANK_REDEEM_VOUCHER
                    || transLog.getTransType() == ETransType.KBANK_REDEEM_VOUCHER_CREDIT
                    || transLog.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT
                    || transLog.getTransType() == ETransType.KBANK_REDEEM_VOID);
        }

        private TransData genTranData(TransData transLog) {
            if (!needUpload(transLog))
                return null;
            TransData transData = transInit();
            // field 2
            String field2 = transLog.getPan();
            if (field2 != null) {
                transData.setPan(field2);
            }
            boolean isVoid = transLog.getTransType() == ETransType.VOID;
            // field 3
            if (isVoid) {//for void trans, use processing code from original trans
                transData.setProcCode(hasNext ? getPaddedNumber(Integer.parseInt(transLog.getOrigTransType().getProcCode()) + 1, 6) : transLog.getOrigTransType().getProcCode());
            } else {
                transData.setProcCode(hasNext ? getPaddedNumber(Integer.parseInt(transLog.getTransType().getProcCode()) + 1, 6) : transLog.getTransType().getProcCode());
            }
            // field 4
            transData.setAmount(transLog.getAmount());
            //field 12, 13
            String dateTime = transLog.getDateTime();
            if (dateTime != null) {
                transData.setDateTime(dateTime);
            }
            //field 14
            String date = transLog.getExpDate();
            if (date != null) {
                transData.setExpDate(date);
            }
            //field 22
            transData.setEnterMode(transLog.getEnterMode());
            transData.setHasPin(transLog.isHasPin());
            //field 23
            String cardSerialNo = transLog.getCardSerialNo();
            if (cardSerialNo != null) {
                transData.setCardSerialNo(transLog.getCardSerialNo());
            }
            //field 24
            String nii = transLog.getAcquirer().getNii();
            if (nii != null) {
                transData.setNii(nii);
            }
            //field 35
            String track2 = transLog.getTrack2();
            if (track2 != null) {
                transData.setTrack2(track2);
            }
            //field 37
            String refNo = transLog.getRefNo();
            if (refNo != null) {
                transData.setRefNo(refNo);
            }
            //field 38
            String authCode = transLog.getAuthCode();
            if (authCode != null) {
                transData.setOrigAuthCode(authCode);
            }
            //field 39
            ResponseCode respCode = transLog.getResponseCode();
            if (respCode != null) {
                transData.setResponseCode(respCode);
            }
            //field 63
            byte[] field63Byte = (isVoid) ? transLog.getField63Byte() : transLog.getField63RecByte();
            if (field63Byte != null) {
                transData.setField63Byte(field63Byte);
            }

            transData.setOrigTransType(transLog.getTransType());
            transData.setOrigTransNo(transLog.getStanNo());
            transData.setTraceNo(transLog.getTraceNo());
            transData.setIssuer(transLog.getIssuer());
            transData.setTransType(ETransType.KBANK_REDEEM_BATCH_UP);

            return transData;
        }

        private boolean continueOnline(TransData transData) {
            if (transData == null)
                return true;
            ret = online.online(transData, listener, isFirst, false);
            isFirst = false;
            // If found error in batch upload, the another batch will be stopped sending for that acquirer.
            return ret == TransResult.SUCC;
        }

        /**
         * @return {@link TransResult}
         */
        int process() {
//            List<ETransType> filterOut = new ArrayList<>();
//            filterOut.add(ETransType.KBANK_REDEEM_VOID);
//            List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransData(FinancialApplication.getAcqManager().getCurAcq(), false, filterOut, true);
            List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransData(FinancialApplication.getAcqManager().getCurAcq(), false);
            if (allTrans.isEmpty()) {
                return TransResult.ERR_NO_TRANS;
            }
            int transCnt = allTrans.size();

            isFirst = true;
            for (int cnt = 0; cnt < transCnt; cnt++) {
                int transIndex = cnt + 1;
                updateProgressTitle(transIndex, transCnt);
                hasNext = transIndex < transCnt;
                if (!continueOnline(genTranData(allTrans.get(cnt))))
                    break;
            }
            online.close();
            return ret;
        }

        private void updateProgressTitle(int cnt, int total) {
            if (listener != null)
                listener.onUpdateProgressTitle(ETransType.KBANK_REDEEM_BATCH_UP.getTransName() + "[" + cnt + "/" + total + "]");
        }
    }

    private class AllInstalmentTransBatch {
        private final TransProcessListener listener;
        private final BatchUpInstalmentListener batchUpListener;
        private boolean isFirst = true;
        private int ret = TransResult.SUCC;
        private boolean hasNext = true;

        AllInstalmentTransBatch(TransProcessListener listener, BatchUpInstalmentListener batchUpListener) {
            this.listener = listener;
            this.batchUpListener = batchUpListener;
        }


        private boolean needUpload(TransData transLog) {
            return (transLog.getTransType() == ETransType.KBANK_SMART_PAY
                    || transLog.getTransType() == ETransType.KBANK_SMART_PAY_VOID
                    || transLog.getTransType() == ETransType.DOLFIN_INSTALMENT
                    || transLog.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID);
        }

        private TransData genTranData(TransData transLog) {
            if (!needUpload(transLog))
                return null;
            TransData transData = transInit();
            // field 2
            String field2 = transLog.getPan();
            if (field2 != null) {
                transData.setPan(field2);
            }
            boolean isVoid = transLog.getTransType() == ETransType.VOID;
            // field 3
            if (isVoid) {//for void trans, use processing code from original trans
                transData.setProcCode(hasNext ? getPaddedNumber(Integer.parseInt(transLog.getOrigTransType().getProcCode()) + 1, 6) : transLog.getOrigTransType().getProcCode());
            } else {
                transData.setProcCode(hasNext ? getPaddedNumber(Integer.parseInt(transLog.getTransType().getProcCode()) + 1, 6) : transLog.getTransType().getProcCode());
            }
            // field 4
            transData.setAmount(isVoid ? "0" : transLog.getAmount());
            //field 12, 13
            String dateTime = transLog.getDateTime();
            if (dateTime != null) {
                transData.setDateTime(dateTime);
            }
            //field 14
            String date = transLog.getExpDate();
            if (date != null) {
                transData.setExpDate(date);
            }
            //field 22
            transData.setEnterMode(transLog.getEnterMode());
            transData.setHasPin(transLog.isHasPin());
            //field 23
            String cardSerialNo = transLog.getCardSerialNo();
            if (cardSerialNo != null) {
                transData.setCardSerialNo(transLog.getCardSerialNo());
            }
            //field 24
            String nii = transLog.getAcquirer().getNii();
            if (nii != null) {
                transData.setNii(nii);
            }
            //field 35
            String track2 = transLog.getTrack2();
            if (track2 != null) {
                transData.setTrack2(track2);
            }
            //field 37
            String refNo = transLog.getRefNo();
            if (refNo != null) {
                transData.setRefNo(refNo);
            }
            //field 38
            String authCode = transLog.getAuthCode();
            if (authCode != null) {
                transData.setOrigAuthCode(authCode);
            }
            //field 39
            ResponseCode respCode = transLog.getResponseCode();
            if (respCode != null) {
                transData.setResponseCode(respCode);
            }
            //field 55
            String sendIccData = transLog.getSendIccData();
            if (sendIccData != null) {
                transData.setSendIccData(sendIccData);
            }
            //field 61
            byte[] field61RecByte = transLog.getField61RecByte();
            if (field61RecByte != null) {
                byte[] f61 = (field61RecByte = (field61RecByte.length > 73 ? Arrays.copyOf(field61RecByte, 73) : field61RecByte));
                if (transLog.getField61Byte() != null) {
                    byte[] reqDE61 = Arrays.copyOfRange(transLog.getField61Byte(), 5, transLog.getField61Byte().length);
                    f61 = new byte[field61RecByte.length + reqDE61.length];
                    System.arraycopy(field61RecByte, 0, f61, 0, field61RecByte.length);
                    System.arraycopy(reqDE61, 0, f61, field61RecByte.length, reqDE61.length);
                }
                transData.setField61Byte(f61);
            }
            //field 63
            byte[] field63Byte = transLog.getField63Byte();
            if (field63Byte != null) {
                transData.setField63Byte(field63Byte);
            }

            transData.setOrigTransType(transLog.getTransType());
            transData.setOrigTransNo(transLog.getStanNo());
            transData.setTraceNo(transLog.getTraceNo());
            transData.setIssuer(transLog.getIssuer());
            transData.setTransType(ETransType.KBANK_SMART_PAY_BATCH_UP);

            if (isDolfinIpp(transLog.getAcquirer())) {
                transData.setTransType(ETransType.DOLFIN_INSTALMENT_BATCH_UP);
                transData.setField61Byte(transLog.getField61Byte());
            }
            return transData;
        }

        private boolean continueOnline(TransData transData) {
            if (transData == null)
                return true;
            ret = online.online(transData, listener, isFirst, false);
            isFirst = false;
            // If found error in batch upload, the another batch will be stopped sending for that acquirer.
            return ret == TransResult.SUCC;
        }

        /**
         * @return {@link TransResult}
         */
        int process() {
//            List<ETransType> filterOut = new ArrayList<>();
//            filterOut.add(ETransType.KBANK_SMART_PAY_VOID);
//            List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransData(FinancialApplication.getAcqManager().getCurAcq(), false, filterOut, true);
            List<TransData> allTrans = FinancialApplication.getTransDataDbHelper().findAllTransData(FinancialApplication.getAcqManager().getCurAcq(), false);
            if (allTrans.isEmpty()) {
                return TransResult.ERR_NO_TRANS;
            }
            int transCnt = allTrans.size();
            isFirst = true;
            for (int cnt = 0; cnt < transCnt; cnt++) {
                int transIndex = cnt + 1;
                TransData transData = genTranData(allTrans.get(cnt));
                updateProgressTitle(transData.getTransType(), transIndex, transCnt);
                hasNext = transIndex < transCnt;
                if (!continueOnline(transData))
                    break;
            }
            online.close();
            return ret;
        }

        private void updateProgressTitle(ETransType transType, int cnt, int total) {
            if (listener != null)
                listener.onUpdateProgressTitle(transType.getTransName() + "[" + cnt + "/" + total + "]");
        }
    }
}
