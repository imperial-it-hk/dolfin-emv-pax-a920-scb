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
package com.pax.pay.trans.component;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.view.Gravity;

import com.pax.abl.utils.PanUtils;
import com.pax.dal.entity.EPedDesMode;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.ETermInfoKey;
import com.pax.dal.exceptions.PedDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.ClssInputParam;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.enums.ECvmResult;
import com.pax.eemv.enums.EFlowType;
import com.pax.eemv.utils.Tools;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransData.ETransStatus;
import com.pax.pay.trans.model.TransData.EnterMode;
import com.pax.pay.trans.model.TransDccKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.trans.receipt.IReceiptGenerator;
import com.pax.pay.trans.receipt.ReceiptKbankRedeemedTransDetail;
import com.pax.pay.utils.CountryCode;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.QrTag31Utils;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.sdk.Sdk;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

import static com.pax.pay.trans.model.TransData.ETransStatus.VOIDED;
import static com.pax.pay.utils.Utils.getString;

public class Component {

    private static final String TAG = "Component";

    private static final long MAX_TRANS_NO = 999999;
    private static final long MAX_BATCH_NO = 999999;

    private static final String TABLE_ID_TX = "TX";
    private static final String TABLE_VERSION_TX = "\02";
    private static final String CHARACTERS_PER_LINE = "34";
    private static final String MAX_DATA_SIZE = "0500";

    private static final String TABLE_ID_TM = "TM";
    private static final String TABLE_VERSION_TM = "\01";
    private static TransData transDataInstance;

    private Component() {
        //do nothing
    }

    /**
     * Transaction preprocessing, check whether to sign in,
     * whether to end, whether to continue batch uploading,
     * whether to support the transaction, and whether parameter download is required
     *
     * @param context   unused
     * @param transType {@link ETransType}
     * @return {@link TransResult}
     */
    public static int transPreDeal(final Context context, ETransType transType) {
        if (!isNeedPreDeal(transType)) {
            return TransResult.SUCC;
        }
        // TODO Battery level check

        // Determine if settlement is required
        int ret = checkSettle();
        if (ret != TransResult.SUCC) {
            return ret;
        }
        // batch upload breakpoint
        /*if (isNeedBatchUp()) {
            return TransResult.ERR_BATCH_UP_NOT_COMPLETED;
        }*/ // No need to check batch up status
        // Determine whether this transaction is supported based on the transaction type
        if (!isSupportTran(transType)) {
            return TransResult.ERR_NOT_SUPPORT_TRANS;
        }
        // Determine if there are parameters to download

        return ret;
    }

    /**
     * Get card reading method
     *
     * @param transType ：交易类型{@link com.pax.pay.trans.model.ETransType}
     * @return {@link SearchMode}
     */
    public static byte getCardReadMode(ETransType transType) {
        byte mode = transType.getReadMode();

        if (mode == 0) {
            return mode;
        }
        // does not support manual input
        if (!FinancialApplication.getAcqManager().getCurAcq().isEnableKeyIn()) {
            mode &= ~SearchMode.KEYIN;
        }
        // does not support QR pay
        if (!FinancialApplication.getAcqManager().getCurAcq().isEnableQR()) {
            mode &= ~SearchMode.QR;
        }
        return mode;
    }

    /**
     * Confirm whether the current transaction is preprocessed according to the transaction type and the reversal flag
     *
     * @param transType {@link ETransType}
     * @return true:需要预处理 false:不需要预处理 备注：签到，签退，结算，参数下发，公钥下载，冲正类不需要预处理,新增交易类型时，需修改添加交易类型判断
     */
    private static boolean isNeedPreDeal(ETransType transType) {
        return transType != ETransType.SETTLE;
    }

    /**
     * Check whether the settlement requirements are met
     *
     * @return 0：No need to settle 1: Settlement reminder, immediately
     *         2: Settlement reminder, later
     *         3: Settlement reminder, insufficient space
     */
    private static int checkSettle() {
        // Get the number of transactions
        long cnt = FinancialApplication.getTransDataDbHelper().countOf();
        // Get the maximum number of transactions allowed
        long maxCnt = FinancialApplication.getSysParam().get(SysParam.NumberParam.MAX_TRANS_COUNT, Integer.MAX_VALUE);

        // Get the maximum number of transactions allowed
        if (cnt >= maxCnt) {
            if (cnt >= maxCnt + 10) {
                return TransResult.ERR_NEED_SETTLE_NOW; // Billing reminder, immediately
            } else {
                return TransResult.ERR_NEED_SETTLE_LATER; // Billing reminder, later
            }
        }
        // Determine the storage space size
        if (!hasFreeSpace()) {
            return TransResult.ERR_NO_FREE_SPACE; // Insufficient storage space, need to settle
        }
        return TransResult.SUCC; // 不用结算
    }

    /**
     * 判断是否有剩余空间
     *
     * @return true: 有空间 false：无空间
     */
    @SuppressWarnings("deprecation")
    private static boolean hasFreeSpace() {
        File dataPath = Environment.getDataDirectory();
        StatFs dataFs = new StatFs(dataPath.getPath());
        long sizes = (long) dataFs.getFreeBlocks() * (long) dataFs.getBlockSize();
        long available = sizes / (1024 * 1024);
        return available > 1;
    }

    /**
     * 判断是否支持该交易
     *
     * @param transType {@link ETransType}
     */
    private static boolean isSupportTran(ETransType transType) {
        switch (transType) {
            case SALE:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.TTS_SALE);
            case VOID:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.TTS_VOID);
            case REFUND:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.TTS_REFUND);
            case PREAUTH:
                return FinancialApplication.getSysParam().get(SysParam.BooleanParam.TTS_PREAUTH);
            default:
                break;
        }

        return true;
    }

    /**
     * convert {@link TransData} to {@link InputParam} for EMV and CLSS
     *
     * @param transData {@link TransData}
     * @return {@link InputParam}
     */
    public static InputParam toInputParam(TransData transData) {
        InputParam inputParam = new InputParam();
        convertTransData2InputParam(transData, inputParam);
        return inputParam;
    }

    public static ClssInputParam toClssInputParam(TransData transData) {
        ClssInputParam inputParam = new ClssInputParam();
        convertTransData2InputParam(transData, inputParam);
        inputParam.setAmtZeroNoAllowedFlg(1);
        inputParam.setCrypto17Flg(true);
        inputParam.setStatusCheckFlg(false);
        inputParam.setReaderTTQ("32404000");//36008000
        inputParam.setDomesticOnly(false);
        List<ECvmResult> list = new ArrayList<>();
        list.add(ECvmResult.REQ_SIG);
        list.add(ECvmResult.OFFLINE_PIN);
        list.add(ECvmResult.CONSUMER_DEVICE);
        list.add(ECvmResult.REQ_ONLINE_PIN);
        inputParam.setCvmReq(list);
        inputParam.setEnDDAVerNo((byte) 0);
        return inputParam;
    }

    private static void convertTransData2InputParam(TransData transData, InputParam inputParam) {
        ETransType transType = transData.getTransType();

        String amount = transData.getAmount();
        if (amount == null || amount.isEmpty()) {
            amount = "0";
        }
        inputParam.setAmount(amount);
        inputParam.setCashBackAmount("0");
        inputParam.setPciTimeout(60 * 1000);
        if ((transData.getTransType().equals(ETransType.SALE))
                || (transData.getTransType().equals(ETransType.PREAUTH)
                || (transData.getTransType().equals(ETransType.PREAUTHORIZATION))
                || (transData.getTransType().equals(ETransType.GET_PAN)))) {
            if (transData.getEnterMode() == EnterMode.INSERT) {
                inputParam.setFlowType(EFlowType.COMPLETE);
            } else {
                inputParam.setFlowType(EFlowType.QPBOC);
            }
            inputParam.setEnableCardAuth(true);
            inputParam.setSupportCVM(true);

        } else {
            // 联机Q非消费，余额查询，预授权均走简单Q流程
            inputParam.setFlowType(EFlowType.SIMPLE);

            inputParam.setEnableCardAuth(false);
            inputParam.setSupportCVM(false);
        }
        byte[] procCode = FinancialApplication.getConvert()
                .strToBcd(transType.getProcCode(), EPaddingPosition.PADDING_RIGHT);
        inputParam.setTag9CValue(procCode[0]);

        // 根据交易类型判断是否强制联机
        inputParam.setForceOnline(true);

        inputParam.setTransDate(transData.getDateTime().substring(0, 8));
        inputParam.setTransTime(transData.getDateTime().substring(8));
        inputParam.setTransStanNo(Component.getPaddedNumber(transData.getStanNo(), 6));

    }

    public static Config genCommonEmvConfig() {
        Config cfg = new Config();
        Currency current = Currency.getInstance(CurrencyConverter.getDefCurrency());
        String currency = String.valueOf(
                CountryCode.getByCode(CurrencyConverter.getDefCurrency().getCountry()).getCurrencyNumeric());
        String country = String.valueOf(
                CountryCode.getByCode(CurrencyConverter.getDefCurrency().getCountry()).getNumeric());

        cfg.setCountryCode(country);
        cfg.setForceOnline(false);
        cfg.setGetDataPIN(true);
        cfg.setMerchCateCode("0000");
        cfg.setReferCurrCode(currency);
        cfg.setReferCurrCon(1000);
        cfg.setReferCurrExp((byte) current.getDefaultFractionDigits());
        cfg.setSurportPSESel(true);
        cfg.setTermType((byte) 0x22);
        cfg.setTransCurrCode(currency);
        cfg.setTransCurrExp((byte) current.getDefaultFractionDigits());
        cfg.setTermId(FinancialApplication.getAcqManager().getCurAcq().getTerminalId());
        cfg.setMerchId(FinancialApplication.getAcqManager().getCurAcq().getMerchantId());
        cfg.setMerchName(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN));
        cfg.setTermAIP("0800");
        cfg.setBypassPin(true); // 输密码支持bypass
        cfg.setBatchCapture((byte) 1);
        cfg.setUseTermAIPFlag(true);
        cfg.setBypassAllFlag(true);
        return cfg;
    }

    /**
     * 流水号+1
     */
    public static void incStanNo(TransData transData) {

        /*Please use this function before update EDC_STAN_NO parameter to avoid conflict
          with each transactions by add all validation rules here.

         flag will always be TRUE, its just to make sure that this function is used before update EDC_STAN_NO
        */

        long transNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
        if (transData != null) {
            ETransType transType = transData.getTransType();
            if (transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL/* && transType != ETransType.OFFLINE_TRANS_SEND*/
                    && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENDING) {
                if (transNo >= MAX_TRANS_NO) {
                    transNo = 0;
                }
                transNo++;
                FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) transNo, true);
            }
        } else {
            // for QR
            transNo++;
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) transNo, true);
        }
    }

    public static void incStanNo(long newStan) {

        /*Please use this function before update EDC_STAN_NO parameter to avoid conflict
          with each transactions by add all validation rules here.

        */

        long transNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
        if(newStan > transNo){
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) newStan, true);
        }
    }

    public static void incTraceNo(TransData transData) {

        /*Please use this function before update EDC_TRACE_NO parameter to avoid conflict
          with each transactions  by add all validation rules here.

         flag will always be TRUE, its just to make sure that this function is used before update EDC_TRACE_NO
        */

        long traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO);
        if (transData != null) {
            ETransType transType = transData.getTransType();
            if (transType != null) {
                if (transData.getReversalStatus() != TransData.ReversalStatus.REVERSAL
                        && transData.getAdviceStatus() != TransData.AdviceStatus.ADVICE
                        && transData.getReferralStatus() != TransData.ReferralStatus.REFERRED
                        && transData.getOfflineSendState() != TransData.OfflineStatus.OFFLINE_SENDING
                        /*&&(transType != ETransType.OFFLINE_TRANS_SEND)*/
                        && (transType != ETransType.VOID)
                        && (transType != ETransType.BATCH_UP)
                        && (transType != ETransType.QR_VOID_WALLET)
                        && (transType != ETransType.UPDATE_SCRIPT_RESULT)
                        && (transType != ETransType.SETTLE)
                        && (transType != ETransType.GET_QR_INFO)
                        && (transType != ETransType.QR_VOID)
                        && (transType != ETransType.GET_QR_KPLUS)
                        && (transType != ETransType.GET_QR_ALIPAY)
                        && (transType != ETransType.GET_QR_WECHAT)
                        && (transType != ETransType.GET_QR_CREDIT)
                        && (transType != ETransType.QR_VOID_KPLUS)
                        && (transType != ETransType.QR_VOID_ALIPAY)
                        && (transType != ETransType.QR_VOID_WECHAT)
                        && (transType != ETransType.QR_VOID_CREDIT)
                        && (transType != ETransType.KBANK_REDEEM_INQUIRY)
                        && (transType != ETransType.KBANK_REDEEM_VOID)
                        && (transType != ETransType.KBANK_REDEEM_SETTLE)
                        && (transType != ETransType.KBANK_REDEEM_BATCH_UP)
                        && (transType != ETransType.KBANK_SMART_PAY_VOID)
                        && (transType != ETransType.KBANK_SMART_PAY_SETTLE)
                        && (transType != ETransType.KBANK_SMART_PAY_BATCH_UP)
                        && (transType != ETransType.KBANK_DCC_GET_RATE)
                        && (transType != ETransType.QR_MYPROMPT_VOID)
                        && (transType != ETransType.DOLFIN_INSTALMENT_VOID)
                        && (transType != ETransType.DOLFIN_INSTALMENT_BATCH_UP)
                ) {

                    traceNo++;
                    FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) traceNo, true);
                }
            }
        } else {
            // for QR
            traceNo++;
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) traceNo, true);
        }
    }

    public static void incTraceNo(long newTrace) {

        /*Please use this function before update EDC_TRACE_NO parameter to avoid conflict
          with each transactions  by add all validation rules here.


        */

        long traceNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO);
        if(newTrace > traceNo){
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) newTrace, true);
        }
    }

    /**
     * 批次号+1
     */
    public static void incBatchNo() {
        int batchNo = FinancialApplication.getAcqManager().getCurAcq().getCurrBatchNo();
        if (batchNo >= MAX_BATCH_NO) {
            batchNo = 0;
        }
        batchNo++;

        FinancialApplication.getAcqManager().getCurAcq().setCurrBatchNo(batchNo);
        FinancialApplication.getAcqManager().updateAcquirer(FinancialApplication.getAcqManager().getCurAcq());
    }

    public static void incBatchNo(Acquirer acquirer) {
        int batchNo = acquirer.getCurrBatchNo();
        if (batchNo >= MAX_BATCH_NO) {
            batchNo = 0;
        }
        batchNo++;

        acquirer.setCurrBatchNo(batchNo);
        FinancialApplication.getAcqManager().updateAcquirer(acquirer);

        // update Merchant
        MerchantProfileManager.INSTANCE.updateMerchantAcqBatch(acquirer.getName(), batchNo);
    }

    public static String getPaddedNumber(long num, int digit) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setGroupingUsed(false);
        nf.setMaximumIntegerDigits(digit);
        nf.setMinimumIntegerDigits(digit);
        return nf.format(num);
    }

    public static String getPaddedString(String str, int maxLen, char ch) {
        return FinancialApplication.getConvert().stringPadding(str, ch, maxLen, IConvert.EPaddingPosition.PADDING_LEFT);
    }

    public static String getPaddedStringRight(String str, int maxLen, char ch) {
        return FinancialApplication.getConvert().stringPadding(str, ch, maxLen, EPaddingPosition.PADDING_RIGHT);
    }

    /**
     * 交易初始化
     *
     * @return {@link TransData}
     */
    public static TransData transInit() {
        TransData transData = new TransData();
        transInit(transData);
        return transData;
    }

    public static TransData getTransDataInstance() {
        return transDataInstance;
    }

    public static void setTransDataInstance(TransData transData) {
        transDataInstance = transData;
    }

    /**
     * 交易初始化
     *
     * @param transData {@link TransData}
     */
    public static void transInit(TransData transData) {
        Acquirer acquirer = FinancialApplication.getAcqManager().getCurAcq();

        transData.setStanNo(getStanNo());
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
        transData.setHeader("");
        transData.setTpdu("600" + acquirer.getNii() + "8000");
        // 冲正原因
        transData.setDupReason("06");
        transData.setTransState(ETransStatus.NORMAL);
        transData.setAcquirer(FinancialApplication.getAcqManager().getCurAcq());
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setTraceNo(getTraceNo());
        transData.setReferralStatus(TransData.ReferralStatus.NORMAL);
    }

    /**
     * 交易初始化
     *
     * @param transData {@link TransData}
     */
    public static void transInit(TransData transData, Acquirer acquirer) {

        transData.setStanNo(getStanNo());
        transData.setBatchNo(acquirer.getCurrBatchNo());
        transData.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
        transData.setHeader("");
        transData.setTpdu("600" + acquirer.getNii() + "8000");
        // 冲正原因
        transData.setDupReason("06");
        transData.setTransState(ETransStatus.NORMAL);
        transData.setAcquirer(acquirer);
        transData.setCurrency(CurrencyConverter.getDefCurrency());
        transData.setTraceNo(getTraceNo());
        transData.setReferralStatus(TransData.ReferralStatus.NORMAL);
    }

    // 获取流水号
    private static long getStanNo() {
        long transNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
        if (transNo == 0) {
            transNo += 1;
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_STAN_NO, (int) transNo);
        }
        return transNo;
    }

    private static long getTraceNo() {
        long transNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO);
        if (transNo == 0) {
            transNo += 1;
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) transNo);
        }
        return transNo;
    }


    /**
     * 是否免签
     *
     * @param transData {@link TransData}
     * @return true: 免签 false: 需要签名
     */
    public static boolean isSignatureFree(TransData transData, CTransResult transResult) {
        if (EnterMode.QR == transData.getEnterMode())
            return true;

        if (transData.getAid() != null && !(transData.getAid().contains(Constants.UP_AID_PREFIX) || transData.getAid().contains(Constants.TBA_AID_PREFIX))) {
            // Not UPI QuickPass and TPN, return false
            return false;
        }

        if (transResult != null && transResult.getCvmResult() == ECvmResult.CONSUMER_DEVICE) {
            return true;
        }

        if (!FinancialApplication.getSysParam().get(SysParam.BooleanParam.QUICK_PASS_TRANS_SIGN_FREE_FLAG)) {
            return false;
        }

        int limitAmount = FinancialApplication.getSysParam().get(SysParam.NumberParam.QUICK_PASS_TRANS_SIGN_FREE_AMOUNT);
        String amount = transData.getAmount().replace(".", "");
        ETransType transType = transData.getTransType();
        if (!(ETransType.SALE.equals(transType) || ETransType.PREAUTH.equals(transType))) {
            return false;
        }
        return Utils.parseLongSafe(amount, 0) <= limitAmount;
    }

    /**
     * 磁道加密
     *
     * @param trackData track data
     * @return encrypted track data
     */
    @SuppressWarnings("unused")
    public static String encryptTrack(String trackData) {
        if (trackData == null || trackData.isEmpty()) {
            return null;
        }
        String temp = trackData;
        int len = temp.length();
        if (len % 2 > 0) {
            temp += "0";
        }
        byte[] tb = new byte[8];
        byte[] bTrack = FinancialApplication.getConvert().strToBcd(temp, EPaddingPosition.PADDING_LEFT);
        System.arraycopy(bTrack, bTrack.length - 9, tb, 0, 8);
        byte[] block = new byte[8];
        try {
            block = FinancialApplication.getDal().getPed(EPedType.INTERNAL).calcDes(Constants.INDEX_TDK, tb,
                    EPedDesMode.ENCRYPT);
        } catch (PedDevException e) {
            Log.e(TAG, "", e);
        }
        System.arraycopy(block, 0, bTrack, bTrack.length - 9, 8);
        return Utils.bcd2Str(bTrack).substring(0, len);
    }

    /**
     * check whether the Neptune is installed, if not, display prompt
     */
    public static boolean neptuneInstalled(Context context, DialogInterface.OnDismissListener noNeptuneDismissListener) {
        if (!Sdk.isPaxDevice()) {
            return true;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo("com.pax.ipp.neptune", 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "", e);
        }

        if (packageInfo == null) {
            CustomAlertDialog dialog = new CustomAlertDialog(context, CustomAlertDialog.ERROR_TYPE, 5);
            dialog.setContentText(context.getString(R.string.please_install_neptune));
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
            dialog.setOnDismissListener(noNeptuneDismissListener);
            return false;
        }
        return true;
    }

    public static boolean isDemo() {
        String commType = FinancialApplication.getSysParam().get(SysParam.StringParam.COMM_TYPE);
        return SysParam.Constant.CommType.DEMO.toString().equals(commType);
    }

    public static void initField63Wallet(TransData transData) {
        String strField63 = "";
        byte[] bytesField63;
        if (transData.getTransType() == ETransType.SETTLE_WALLET || transData.getTransType() == ETransType.SETTLE_END_WALLET) {
            bytesField63 = transData.getField61RecByte();
        } else {
            bytesField63 = transData.getField63RecByte();
        }
        int index = 0;

        if (bytesField63 != null) {
            int lenField = bytesField63.length;
            for (int i = 0; i < lenField; i++) {

                String strhex = Tools.byteToStringHex(bytesField63[i]);

                if (strhex.equals("FC") || strhex.equals("FB")) {
                    if (index == 0) {
                        byte[] arr = Arrays.copyOfRange(bytesField63, 5, i - 1);
                        transData.setWalletName(Tools.bytes2String(arr));
                    } else {
                        byte[] arr = Arrays.copyOfRange(bytesField63, index + 2, i);
                        strField63 += Tools.bytes2String(arr);
                    }
                    index = i;

                    int size = Integer.parseInt(Tools.byteToStringHex(bytesField63[i + 1]));
                    if (strhex.equals("FC")) {
                        strField63 += paddingSpace(size);
                    } else if (strhex.equals("FB")) {
                        strField63 += paddingSpace(size).replace(' ', '-');
                    }
                }

            }
            int lastIndex = index + 2;
            if (lastIndex != lenField) {// For Wallet Settlement
                byte[] arr = Arrays.copyOfRange(bytesField63, lastIndex, lenField);
                strField63 += Tools.bytes2String(arr);
            }
        }
        transData.setWalletSlipInfo(strField63);
    }

    private static String paddingSpace(int size) {
        String space = String.format("%1$-" + size + "s", "");
        return space;
    }

    public static String initPrintText() {
        String characterPerLine = Tools.hexToAscii(CHARACTERS_PER_LINE);
        String maxDataSize = Tools.hexToAscii(MAX_DATA_SIZE);
        String txData = TABLE_ID_TX + TABLE_VERSION_TX + characterPerLine + maxDataSize;
        String lengthtxDataAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(txData).length, 4)));
        return lengthtxDataAscii + txData;
    }

    public static String initTerminalInformation() {
        // terminal information
        String currentDateTime = Tools.hexToAscii(Device.getTime(Constants.TIME_PATTERN_TRANS));

        // get serial number EDC.
        Map<ETermInfoKey, String> termInfo = FinancialApplication.getDal().getSys().getTermInfo();
        String serial = termInfo.get(ETermInfoKey.SN);

        // get app version.
        String version = FinancialApplication.getApp().getVersion();

        // separator in termianl information
        String termsn = Tools.hexToAscii("1f");

        String tmData = TABLE_ID_TM + TABLE_VERSION_TM + currentDateTime + serial + termsn + version;
        String lengthtmDataAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(tmData).length, 4)));
        return lengthtmDataAscii + tmData;
    }

    public static void generateTransDetail(TransData transData, IPage page, int fontsize) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();
        boolean isOfflineTransSend = ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType();
        boolean isRefund = ETransType.REFUND == transType || ETransType.REFUND == transData.getOrigTransType();
        String type = (!isOfflineTransSend) ? transType.toString() : transType.getTransName().toUpperCase();
        if (ETransType.VOID == transType) {
            type = (!isOfflineTransSend) ? (!isRefund) ? "SALE(VOID)" : "REFUND(VOID)" : "OFFLINE(VOID)";
        }

        if (!isOfflineTransSend && transData.getOfflineSendState() != null) {
            type = Utils.getString(R.string.trans_offline).toUpperCase() + " " + type;
        }

        // AET-18
        // transaction NO/transaction type/auth

        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);
        String authCode = (!isOfflineTransSend) ? (transData.getAuthCode() == null ? "" : transData.getAuthCode()) : transData.getOrigAuthCode();
        temp = authCode;
        temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);// TRACE

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setFontSize(fontsize).setGravity(Gravity.LEFT))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(type).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setWeight(0.3f).setFontSize(fontsize));
        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setWeight(0.3f).setFontSize(fontsize));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END).setFontSize(fontsize));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setGravity(Gravity.END).setFontSize(fontsize));

        // amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setFontSize(fontsize));
        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }

    public static void generateTransDetailPromptPay(TransData transData, IPage page, int fontSize) {
        String temp;

        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();
        //String state = transData.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE ? Utils.getString(R.string.state_qr_offline) : Utils.getString(R.string.state_qr_online);
        String state = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        } else {
            state = transData.getQrSaleState() == TransData.QrSaleState.QR_SEND_OFFLINE ? Utils.getString(R.string.state_qr_offline) : Utils.getString(R.string.state_qr_online);
        }
        String type = transType.getTransName().toUpperCase() + " " + "(" + state + ")";

        // AET-18
        // transaction NO/transaction type/amount
        if (transType.isSymbolNegative() || transState == TransData.ETransStatus.VOIDED) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }

        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit().setText(type).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        // trace no/appr code
        temp = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        String temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);

        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + " " + temp2).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_appr_code) + " " + temp).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        //AET-125
        page.addLine()
                .addUnit(page.createUnit().setText(formattedDate).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(formattedTime).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        temp2 = transData.getQrRef2() == null ? "" : transData.getQrRef2();
        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_ref2) + " " + temp2).setFontSize(fontSize).setWeight(3.0f));

        temp2 = transData.getRefNo() == null ? "" : transData.getRefNo();
        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_trans_id) + " " + temp2).setFontSize(fontSize).setWeight(3.0f));

        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }

    public static void generateTransDetailWallet(TransData transData, IPage page, int fontSize) {
        String temp;

        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();

        String state = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        }
        String type = !state.isEmpty() ? transType.getTransName().toUpperCase() + "(" + state + ")" : transType.getTransName().toUpperCase();


        // trace no/appr code
        temp = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        String temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);

        // date/time
        String formattedDateTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY3);

        page.addLine()
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.LEFT).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(formattedDateTime).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        page.addLine()
                .addUnit(page.createUnit().setText(type).setGravity(Gravity.LEFT).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        temp = transData.getQrBuyerCode() != null ? PanUtils.maskCardNo(transData.getQrBuyerCode(), transData.getIssuer().getPanMaskPattern()) : "";
        temp2 = transData.getWalletName();

        if (temp != "") {
            page.addLine()
                    .addUnit(page.createUnit().setText(temp).setFontSize(fontSize).setWeight(3.0f))
                    .addUnit(page.createUnit().setText(Utils.getString(R.string.history_detail_wallet_card)).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));
        } else {
            temp = transData.getRefNo() != null ? transData.getRefNo() : "";
            page.addLine()
                    .addUnit(page.createUnit().setText(temp).setFontSize(fontSize).setWeight(3.0f))
                    .addUnit(page.createUnit().setText(Utils.getString(R.string.history_detail_ref_no)).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));
        }

        page.addLine()
                .addUnit(page.createUnit().setText(temp2).setFontSize(fontSize).setWeight(3.0f).setGravity(Gravity.LEFT));

        // transaction NO/transaction type/amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }

        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontSize).setWeight(3.0f).setGravity(Gravity.RIGHT));

        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }

    public static void generateTransDetailWalletKbank(TransData transData, IPage page, int fontSize) {
        String temp;

        ETransType transType = transData.getTransType();
        TransData.ETransStatus transState = transData.getTransState();

        String state = "", strTransType = "", strIssuer = "";
        if (transState != null && transState != TransData.ETransStatus.NORMAL) {
            state = transState == TransData.ETransStatus.VOIDED ? "VOID" : transState.toString().toUpperCase();
        }
        if (transType != null && transType.getTransName() != null) {
            String[] sp = transType.getTransName().contains(" ") ? transType.getTransName().split(" ") : new String[0];
            if (Constants.ACQ_QR_CREDIT.equals(transData.getAcquirer().getName())) {
                strIssuer = Utils.getString(R.string.trans_qr_credit) + " " + (transData.getMerchantInfo() != null ? transData.getMerchantInfo().trim() : "");
                strTransType = transType == ETransType.QR_INQUIRY_CREDIT ? Utils.getString(R.string.trans_sale) : Utils.getString(R.string.trans_void);
            } else {
                if (sp.length > 1) {
                    strIssuer = sp[0];
                    strTransType = sp[1];
                }
            }
        }
        String type = !state.isEmpty() ? strTransType.toUpperCase() + "(" + state + ")" : strTransType.toUpperCase();


        // trace no/appr code
        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);
        String authCode = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        temp = authCode;
        String temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setGravity(Gravity.LEFT).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontSize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(type).setGravity(Gravity.LEFT).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setFontSize(fontSize).setWeight(1.0f));

        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setGravity(Gravity.LEFT).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setFontSize(fontSize).setWeight(1.0f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.LEFT).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        //temp = transData.getQrBuyerCode() != null ? PanUtils.maskCardNo(transData.getQrBuyerCode(), transData.getIssuer().getPanMaskPattern()) : "";
        //temp2 = transData.getWalletName();

        //KBank Requirement, not show ref no for KPLUS, ALIPAY, WECHAT
        //temp = transData.getRefNo();
        temp = "";

        switch (transData.getAcquirer().getName()) {
            case Constants.ACQ_KPLUS:
//                String promocode = transData.getPromocode() != null ? transData.getPromocode().trim() : "";
//                strIssuer = "2".equalsIgnoreCase(promocode) ? "Promptpay" : Utils.getString(R.string.receipt_kplus);
                strIssuer = (transData.getQrSourceOfFund() != null) ? transData.getQrSourceOfFund().trim() : "-";
                break;
            case Constants.ACQ_ALIPAY:
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                strIssuer = Utils.getString(R.string.receipt_alipay);
                break;
            case Constants.ACQ_WECHAT:
            case Constants.ACQ_WECHAT_B_SCAN_C:
                strIssuer = Utils.getString(R.string.receipt_wechat);
                break;
            case Constants.ACQ_QR_CREDIT:
                temp = transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : "";
                break;
        }

        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(strIssuer).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.5f));

        // transaction NO/transaction type/amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }

        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontSize).setWeight(3.0f).setGravity(Gravity.RIGHT));

        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }


    public static void generateTransDetailKbankRedeem(TransData transData, IPage page, int fontSize) {
        String temp;
        String temp2;
        boolean isVoid = transData.getTransType() == ETransType.KBANK_REDEEM_VOID || transData.getTransState() == TransData.ETransStatus.VOIDED;

        String authCode = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        temp = authCode;

        // TRANS. NAME
        ETransType transType = isVoid ? transData.getOrigTransType() : transData.getTransType();
        String strTransType = "";
        if (transType != null) {
            if (transType == ETransType.KBANK_REDEEM_DISCOUNT) {
                strTransType = "89999".equals(transData.getRedeemedDiscountType()) ? Utils.getString(R.string.trans_kbank_redeem_discount_var) : Utils.getString(R.string.trans_kbank_redeem_discount_fix);
            } else {
                strTransType = transType.getTransName();
            }
            strTransType = isVoid ? strTransType.replaceFirst(" ", " " + Utils.getString(R.string.receipt_redeem_void) + " ") : strTransType;
        }

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setGravity(Gravity.LEFT).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontSize).setWeight(0.3f));

        page.addLine().addUnit(page.createUnit()
                .setText(strTransType.toUpperCase())
                .setGravity(Gravity.START)
                .setFontSize(fontSize));
        page.addLine().addUnit(page.createUnit()
                .setText(authCode)
                .setGravity(Gravity.END)
                .setFontSize(fontSize));

        // AET-18
        // transaction NO/auth
        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);

        temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);// TRACE
        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setFontSize(fontSize).setWeight(0.3f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END).setFontSize(fontSize));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6).setFontSize(fontSize))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setGravity(Gravity.END).setFontSize(fontSize));

        // Redeemed Info.
        ReceiptKbankRedeemedTransDetail.generateDetail(page, transData, false, true, false, 20);

        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }

    public static void generateTransDetailInstalmentKbank(TransData transData, IPage page, int fontsize) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        boolean isVoid = transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID
                        || transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID
                        || transData.getTransState() == TransData.ETransStatus.VOIDED ;
        String transTypeValue = isVoid ? Component.getTransByIPlanMode(transData) + "(VOID)" : Component.getTransByIPlanMode(transData);

        // AET-18
        // transaction NO/transaction type/auth
        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);
        String authCode = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        temp = authCode;
        temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);// TRACE

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setFontSize(fontsize).setGravity(Gravity.LEFT))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(transTypeValue).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setFontSize(fontsize).setGravity(Gravity.END));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setFontSize(fontsize).setGravity(Gravity.END));

        // amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize).setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit().setFontSize(fontsize).setText(" "));

        return;
    }

    public static void generateTransDetailDccKbank(List<TransData> transDataList, IPage page) {
        List<String[]> dccCurencys = FinancialApplication.getTransDataDbHelper().findAllDccCurrency();
        if (dccCurencys != null && dccCurencys.size() > 0) {

            for (String[] c : dccCurencys) {
                Log.e("menu", "dccCurencys = " + c[0]);
                Currency dccCurrency = Currency.getInstance(c[0]);
                page.addLine().addUnit(page.createUnit()
                        .setText(getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit().setText(dccCurrency.getDisplayName() + " (" + dccCurrency.getCurrencyCode() + ")").setGravity(Gravity.CENTER));
                page.addLine().addUnit(page.createUnit()
                        .setText(getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                for (TransData transData : transDataList) {
                    if (transData.getDccCurrencyName() != null && transData.getDccCurrencyName().equalsIgnoreCase(c[0])) {
                        page = buildDccTransDetails(transData, page);
                    }
                }
            }
        }

        return;
    }

    private static IPage buildDccTransDetails(TransData transData, IPage page) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        String type = transType.toString();
        if (type.equals("VOID")) {
            type = "SALE(VOID)";
        }

        // AET-18
        // transaction NO/transaction type/auth
        temp = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);// TRACE
        page.addLine()
                .addUnit(page.createUnit().setText(temp2 + "  " + type))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setWeight(0.3f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setGravity(Gravity.END));

        // amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END));

        //dcc rate
        double exRate = transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0;
        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_dcc_ex_rate)))
                .addUnit(page.createUnit().setText(String.format(Locale.getDefault(), "%.4f", exRate)).setGravity(Gravity.END));

        long amount = Utils.parseLongSafe(transData.getDccAmount(), 0);
        if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED)
            amount = -amount;

        String currencyNumeric = Tools.bytes2String(transData.getDccCurrencyCode());
        temp = CurrencyConverter.convert(amount, currencyNumeric);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit().setText(" "));

        return page;
    }

    public static void generateTotalDetailMainInfo(IPage page, Acquirer acquirer, String title, int fontSize, int acqNum) {
        // Change acquirer name Prompt Pay on slip
        String acqName = acquirer.getName();

        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST : " + acquirer.getNii())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(acqName)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        if (acqNum != 0 && acqNum != 999) {
            // TID/BATCH
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short) + " " + acquirer.getTerminalId())
                            .setFontSize(fontSize));

            // MER
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short) + " " + acquirer.getMerchantId())
                            .setFontSize(fontSize)
                            .setGravity(Gravity.LEFT));
        }
        // title
        page.addLine().addUnit(page.createUnit().setText("< " + title + " >").setFontSize(fontSize).setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        return;
    }

    public static void generateTotalDetailMainInfoAudit(IPage page, Acquirer acquirer, String title, int fontSize, boolean isAllAcq) {
        // Change acquirer name Prompt Pay on slip
        String acqName = acquirer.getName();

        //HOST
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("HOST : " + acquirer.getNii())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(acqName)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        if (isAllAcq) {
            // TID/BATCH
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_terminal_code_short) + " " + acquirer.getTerminalId())
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_batch_num_short) + " " + Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                            .setFontSize(fontSize)
                            .setGravity(Gravity.END));

            // MER
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_merchant_code_short) + " " + acquirer.getMerchantId())
                            .setFontSize(fontSize)
                            .setGravity(Gravity.LEFT));
        }

        // title
        page.addLine().addUnit(page.createUnit().setText("< " + title + " >").setFontSize(fontSize).setGravity(Gravity.CENTER));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setGravity(Gravity.CENTER));

        return;
    }

    public static void generateTotalByIssuer(IPage page, Acquirer acquirer, List<ETransStatus> filter, List<Issuer> listIssuers, int fontSize) {
        long[] tempObj = new long[2];
        long[] tempOff = new long[2];
        long tempNum, tempAmt;
        long refundNum, refundAmt;
        long topupNum = 0, topupAmt = 0;
        long topupRefundNum = 0, topupRefundAmt = 0;
        long totalNum, totalAmt;
        long saleHostNum = 0, saleHostAmt = 0;
        long saleVoidHostNum = 0, saleVoidHostAmt = 0;
        long refundHostNum = 0, refundHostAmt = 0;

        for (Issuer issuer : listIssuers) {
            tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                // issuer
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_card_issue) + " : " + issuer.getIssuerName())
                                .setFontSize(fontSize));
                page.addLine().addUnit(page.createUnit().setText(" "));

                long[] tempObj1 = new long[2];
                if (acquirer.getName().equals(Constants.ACQ_QR_PROMPT)) {//Modified by Cz to support PromptPay.
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, false);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, true);
                    long[] obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
                    tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
                    tempObj[0] = tempObj[0] + tempOffline[0] + obj1[0] + tempObj1[0];
                    tempObj[1] = tempObj[1] + tempOffline[1] + obj1[1] + tempObj1[1];
                } else if (acquirer.getName().equals(Constants.ACQ_QRC)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
                    long[] tempInq = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);
                    tempObj[0] = tempObj[0] + tempInq[0];
                    tempObj[1] = tempObj[1] + tempInq[1];
                } else if (acquirer.getName().equals(Constants.ACQ_KPLUS)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                } else if (acquirer.getName().equals(Constants.ACQ_WECHAT)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                }  else if (acquirer.getName().equals(Constants.ACQ_WECHAT_B_SCAN_C)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                } else if (acquirer.getName().equals(Constants.ACQ_SMRTPAY) || acquirer.getName().equals(Constants.ACQ_SMRTPAY_BDMS)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false);
                } else if (acquirer.getName().equals(Constants.ACQ_AMEX_EPP)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, issuer, false);
                } else if (acquirer.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false);
                } else {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
                    tempObj[0] = tempObj[0] + tempOffline[0];
                    tempObj[1] = tempObj[1] + tempOffline[1];
                }
                tempNum = tempObj[0];
                tempAmt = tempObj[1];
                totalNum = tempObj[0];
                totalAmt = tempObj[1];
                saleHostNum += tempNum;
                saleHostAmt += tempAmt;

                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                .setFontSize(fontSize))
                        .addUnit(page.createUnit()
                                .setText(" : " + tempNum)
                                .setFontSize(fontSize)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(tempAmt))
                                .setFontSize(fontSize)
                                .setGravity(Gravity.END));

                tempObj1[0] = 0;
                tempObj1[1] = 0;
                //sale void total
                if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED);
                    tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED);
                } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED);
                    tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED);
                } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
                } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
                } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, VOIDED);
                } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
                } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, VOIDED);
                } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED);
                } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName()) ) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED);
                } else {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
                    tempObj[0] = tempObj[0] + tempOffline[0];
                    tempObj[1] = tempObj[1] + tempOffline[1];
                }
                long voidNum = tempObj[0];
                long voidAmt = tempObj[1];
                saleVoidHostNum += voidNum;
                saleVoidHostAmt += voidAmt;

                // void
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_void).toUpperCase())
                                .setFontSize(fontSize))
                        .addUnit(page.createUnit()
                                .setText(" : " + voidNum)
                                .setFontSize(fontSize)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - voidAmt))
                                .setFontSize(fontSize)
                                .setGravity(Gravity.END));


                tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, filter, issuer, false);

                totalNum += tempObj[0];
                totalAmt = tempAmt - tempObj[1];
                tempNum = tempObj[0];
                tempAmt = tempObj[1];
                refundHostNum += tempNum;
                refundHostAmt += tempAmt;

                // refund
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                .setFontSize(fontSize))
                        .addUnit(page.createUnit()
                                .setText(" : " + tempNum)
                                .setFontSize(fontSize)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - tempAmt))
                                .setFontSize(fontSize)
                                .setGravity(Gravity.END));

                tempObj[0] = 0;
                tempObj[1] = 0;//default value for another acquirers

                topupNum = tempObj[0];
                topupAmt = tempObj[1];

                // top up
                if (topupNum != 0.00) {
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(" : " + topupNum)
                                    .setFontSize(fontSize)
                                    .setWeight(3.0f));
                    String tmpTopUp = CurrencyConverter.convert(topupAmt);
                    if (topupAmt != 0.00) {
                        tmpTopUp = "- " + tmpTopUp;
                    }
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(tmpTopUp)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    totalNum += topupNum;
                    totalAmt += topupAmt;
                }

                page.addLine().addUnit(page.createUnit()
                        .setText("---------")
                        .setGravity(Gravity.END));

                // total
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TOTAL")
                                .setFontSize(fontSize))
                        .addUnit(page.createUnit()
                                .setText(" : " + totalNum)
                                .setFontSize(fontSize)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(totalAmt))
                                .setFontSize(fontSize)
                                .setGravity(Gravity.END));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));
            }
        }

        /*=================== HOST TOTAL ===================*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_totals))
                        .setFontSize(fontSize));
        page.addLine().addUnit(page.createUnit().setText(" "));
        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + saleHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + saleVoidHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - saleVoidHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + refundHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - refundHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        tempObj[0] = 0;
        tempObj[1] = 0;//default value for another acquirers

        topupNum = tempObj[0];
        topupAmt = tempObj[1];

        // top up
        if (topupNum != 0.00) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(" : " + topupNum)
                            .setFontSize(fontSize)
                            .setWeight(3.0f));
            String tmpTopUp = CurrencyConverter.convert(topupAmt);
            if (topupAmt != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setFontSize(fontSize)
                            .setGravity(Gravity.END));
        }

        page.addLine().addUnit(page.createUnit()
                .setText("---------")
                .setGravity(Gravity.END));

        long hostNum = saleHostNum + refundHostNum + topupNum + topupRefundNum;
        long hostAmt = saleHostAmt - refundHostAmt + topupAmt - topupRefundAmt;

        EcrData.instance.nBatchTotalSalesCount = saleHostNum;
        EcrData.instance.nBatchTotalSalesAmount = saleHostAmt;

        EcrData.instance.nBatchTotalRefundCount = refundHostNum;
        EcrData.instance.nBatchTotalRefundAmount = refundHostAmt;

        // total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL")
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + hostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(hostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return;
    }

    public static List<Bitmap> generateTotalByIssuerBitmapArray(Acquirer acquirer, List<ETransStatus> filter, List<Issuer> listIssuers, int fontSize, List<Bitmap> bitmaps, IImgProcessing imgProcessing) {
        final int MAX_PAGE = 30;
        int transNo = 0, j, tempSize;
        int tranSize = listIssuers != null ? listIssuers.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_PAGE);

        long[] tempObj = new long[2];
        long[] tempOff = new long[2];
        long tempNum, tempAmt;
        long refundNum, refundAmt;
        long topupNum = 0, topupAmt = 0;
        long topupRefundNum = 0, topupRefundAmt = 0;
        long totalNum, totalAmt;
        long saleHostNum = 0, saleHostAmt = 0;
        long saleVoidHostNum = 0, saleVoidHostAmt = 0;
        long refundHostNum = 0, refundHostAmt = 0;

        IPage page;
        for (int i = 1; i <= totalPage; i++) {
            page = Device.generatePage();
            tempSize = (tempSize = i * MAX_PAGE) > tranSize ? tranSize : tempSize;
            for (j = transNo; j < tempSize; j++) {
                Issuer issuer = listIssuers.get(j);
                tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
                if (tempObj[0] != 0 || tempObj[1] != 0) {
                    // issuer
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_card_issue) + " : " + issuer.getIssuerName())
                                    .setFontSize(fontSize));
                    page.addLine().addUnit(page.createUnit().setText(" "));

                    long[] tempObj1 = new long[2];
                    if (acquirer.getName().equals(Constants.ACQ_QR_PROMPT)) {//Modified by Cz to support PromptPay.
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, false);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, true);
                        long[] obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
                        tempObj[0] = tempObj[0] + tempOffline[0] + obj1[0] + tempObj1[0];
                        tempObj[1] = tempObj[1] + tempOffline[1] + obj1[1] + tempObj1[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_QRC)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
                        long[] tempInq = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);
                        tempObj[0] = tempObj[0] + tempInq[0];
                        tempObj[1] = tempObj[1] + tempInq[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_KPLUS)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_WECHAT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_WECHAT_B_SCAN_C)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_SMRTPAY) || acquirer.getName().equals(Constants.ACQ_SMRTPAY_BDMS)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false);
                    } else if (acquirer.getName().equals(Constants.ACQ_AMEX_EPP)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, issuer, false);
                    } else if (acquirer.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
                        tempObj[0] = tempObj[0] + tempOffline[0];
                        tempObj[1] = tempObj[1] + tempOffline[1];
                    }
                    tempNum = tempObj[0];
                    tempAmt = tempObj[1];
                    totalNum = tempObj[0];
                    totalAmt = tempObj[1];
                    saleHostNum += tempNum;
                    saleHostAmt += tempAmt;

                    // sale
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(" : " + tempNum)
                                    .setFontSize(fontSize)
                                    .setWeight(3.0f));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(tempAmt))
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    tempObj1[0] = 0;
                    tempObj1[1] = 0;
                    //sale void total
                    if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED);
                    } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED);
                    } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
                    } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
                    } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, VOIDED);
                    } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
                    } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, VOIDED);
                    } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED);
                    } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.AMEX_INSTALMENT, VOIDED);
                    } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
                        tempObj[0] = tempObj[0] + tempOffline[0];
                        tempObj[1] = tempObj[1] + tempOffline[1];
                    }
                    long voidNum = tempObj[0];
                    long voidAmt = tempObj[1];
                    saleVoidHostNum += voidNum;
                    saleVoidHostAmt += voidAmt;

                    // void
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_void).toUpperCase())
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(" : " + voidNum)
                                    .setFontSize(fontSize)
                                    .setWeight(3.0f));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - voidAmt))
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));


                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, filter, issuer, false);

                    totalNum += tempObj[0];
                    totalAmt = tempAmt - tempObj[1];
                    tempNum = tempObj[0];
                    tempAmt = tempObj[1];
                    refundHostNum += tempNum;
                    refundHostAmt += tempAmt;

                    // refund
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(" : " + tempNum)
                                    .setFontSize(fontSize)
                                    .setWeight(3.0f));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - tempAmt))
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    tempObj[0] = 0;
                    tempObj[1] = 0;//default value for another acquirers

                    topupNum = tempObj[0];
                    topupAmt = tempObj[1];

                    // top up
                    if (topupNum != 0.00) {
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(" : " + topupNum)
                                        .setFontSize(fontSize)
                                        .setWeight(3.0f));
                        String tmpTopUp = CurrencyConverter.convert(topupAmt);
                        if (topupAmt != 0.00) {
                            tmpTopUp = "- " + tmpTopUp;
                        }
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(tmpTopUp)
                                        .setFontSize(fontSize)
                                        .setGravity(Gravity.END));

                        totalNum += topupNum;
                        totalAmt += topupAmt;
                    }

                    page.addLine().addUnit(page.createUnit()
                            .setText("---------")
                            .setGravity(Gravity.END));

                    // total
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TOTAL")
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(" : " + totalNum)
                                    .setFontSize(fontSize)
                                    .setWeight(3.0f));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(totalAmt))
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setGravity(Gravity.CENTER));
                }
            }
            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }

        page = Device.generatePage();
        /*=================== HOST TOTAL ===================*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_totals))
                        .setFontSize(fontSize));
        page.addLine().addUnit(page.createUnit().setText(" "));
        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + saleHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + saleVoidHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - saleVoidHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + refundHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - refundHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        tempObj[0] = 0;
        tempObj[1] = 0;//default value for another acquirers

        topupNum = tempObj[0];
        topupAmt = tempObj[1];

        // top up
        if (topupNum != 0.00) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(" : " + topupNum)
                            .setFontSize(fontSize)
                            .setWeight(3.0f));
            String tmpTopUp = CurrencyConverter.convert(topupAmt);
            if (topupAmt != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setFontSize(fontSize)
                            .setGravity(Gravity.END));
        }

        page.addLine().addUnit(page.createUnit()
                .setText("---------")
                .setGravity(Gravity.END));

        long hostNum = saleHostNum + refundHostNum + topupNum + topupRefundNum;
        long hostAmt = saleHostAmt - refundHostAmt + topupAmt - topupRefundAmt;

        EcrData.instance.nBatchTotalSalesCount = saleHostNum;
        EcrData.instance.nBatchTotalSalesAmount = saleHostAmt;

        EcrData.instance.nBatchTotalRefundCount = refundHostNum;
        EcrData.instance.nBatchTotalRefundAmount = refundHostAmt;

        // total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL")
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + hostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(hostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        bitmaps.add(imgProcessing.pageToBitmap(page, 384));

        return bitmaps;
    }

    public static void generateTotalWalletByIssuer(IPage page, Acquirer acquirer, List<ETransStatus> filter, List<Issuer> listIssuers, int fontSize) {
        long[] tempObj;
        long tempNum, tempAmt;
        long totalNum, totalAmt;
        long saleHostNum = 0, saleHostAmt = 0;
        long refundHostNum = 0, refundHostAmt = 0;

        for (Issuer issuer : listIssuers) {
            tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                // issuer
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.receipt_card_issue) + " : " + issuer.getName())
                                .setFontSize(fontSize));
                page.addLine().addUnit(page.createUnit().setText(" "));

                List<ETransStatus> statuses = new ArrayList<>();
                statuses.add(ETransStatus.VOIDED);
                statuses.addAll(filter);
                List<String[]> tempList = FinancialApplication.getTransDataDbHelper().countSumOfWallet(acquirer, statuses, null);

                for (String[] tempObjs : tempList) {
                    Object[] rawResults = getRawResults(tempObjs);
                    if (rawResults != null) {
                        String walletName = (String) rawResults[2];

                        if (walletName == null)
                            continue;

                        page.addLine()
                                .adjustTopSpace(10)
                                .addUnit(page.createUnit()
                                        .setText(walletName)
                                        .setFontSize(fontSize));
                        page.addLine().addUnit(page.createUnit().setText(" "));

                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOfWallet(acquirer, ETransType.QR_SALE_WALLET, filter, issuer, walletName);
                        long[] obj1 = FinancialApplication.getTransDataDbHelper().countSumOfWallet(acquirer, ETransType.SALE_WALLET, filter, issuer, walletName);

                        tempNum = tempObj[0] + obj1[0];
                        tempAmt = tempObj[1] + obj1[1];
                        totalNum = tempNum;
                        totalAmt = tempAmt;
                        saleHostNum += tempNum;
                        saleHostAmt += tempAmt;

                        // sale
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(" : " + tempNum)
                                        .setFontSize(fontSize)
                                        .setWeight(3.0f));
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(CurrencyConverter.convert(tempAmt))
                                        .setFontSize(fontSize)
                                        .setGravity(Gravity.END));

                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOfWallet(acquirer, ETransType.REFUND_WALLET, filter, issuer, walletName);
                        totalNum += tempObj[0];
                        totalAmt = tempAmt - tempObj[1];
                        tempNum = tempObj[0];
                        tempAmt = tempObj[1];
                        refundHostNum += tempNum;
                        refundHostAmt += tempAmt;


                        // refund
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(" : " + tempNum)
                                        .setFontSize(fontSize)
                                        .setWeight(3.0f));
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(CurrencyConverter.convert(0 - tempAmt))
                                        .setFontSize(fontSize)
                                        .setGravity(Gravity.END));

                        page.addLine().addUnit(page.createUnit()
                                .setText("---------")
                                .setGravity(Gravity.END));

                        // total
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText("TOTAL")
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(" : " + totalNum)
                                        .setFontSize(fontSize)
                                        .setWeight(3.0f));
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(CurrencyConverter.convert(totalAmt))
                                        .setFontSize(fontSize)
                                        .setGravity(Gravity.END));
                    }
                }

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));
            }
        }

        /*=================== HOST TOTAL ===================*/
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_host_totals))
                        .setFontSize(fontSize));
        page.addLine().addUnit(page.createUnit().setText(" "));

        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + saleHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + refundHostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - refundHostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText("---------")
                .setGravity(Gravity.END));

        long hostNum = saleHostNum + refundHostNum;
        long hostAmt = saleHostAmt - refundHostAmt;

        // total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL")
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(" : " + hostNum)
                        .setFontSize(fontSize)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(hostAmt))
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        return;
    }

    public static void generateDccTotal(IPage page, Acquirer acquirer) {
        List<TransDccKbankTotal> totals = FinancialApplication.getTransTotalDbHelper().calTotalDccGroupByCurrency(acquirer);
        if (!totals.isEmpty()) {
            for (TransDccKbankTotal t : totals) {
                Currency dccCurrency = Currency.getInstance(t.getCurrencyCode());
                page.addLine().addUnit(page.createUnit()
                        .setText(dccCurrency.getDisplayName() + " (" + dccCurrency.getCurrencyCode() + ")")
                        .setFontSize(IReceiptGenerator.FONT_NORMAL_26)
                        .setTextStyle(Typeface.BOLD).setGravity(Gravity.CENTER));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_one_line))
                        .setGravity(Gravity.CENTER));

                page.addLine().addUnit(page.createUnit().setText(" "));

                // sale
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + t.getSaleTotalNum())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(t.getSaleTotalAmt()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(t.getSaleDccTotalAmt(), t.getCurrencyNumericCode()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));

                // void
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_void).toUpperCase())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + t.getSaleVoidTotalNum())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - t.getSaleVoidTotalAmt()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - t.getSaleDccVoidTotalAmt(), t.getCurrencyNumericCode()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));

                // refund
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + t.getRefundTotalNum())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - t.getRefundTotalAmt()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(0 - t.getRefundTotalAmt(), t.getCurrencyNumericCode()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));

                page.addLine().addUnit(page.createUnit()
                        .setText("---------")
                        .setGravity(Gravity.END));

                // total
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText("TOTAL")
                                .setFontSize(IReceiptGenerator.FONT_NORMAL))
                        .addUnit(page.createUnit()
                                .setText(" : " + t.getSaleTotalNum())
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setWeight(3.0f));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(t.getSaleTotalAmt()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));
                page.addLine()
                        .addUnit(page.createUnit()
                                .setText(CurrencyConverter.convert(t.getSaleDccTotalAmt(), t.getCurrencyNumericCode()))
                                .setFontSize(IReceiptGenerator.FONT_NORMAL)
                                .setGravity(Gravity.END));

                page.addLine().addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_double_line))
                        .setGravity(Gravity.CENTER));
            }
        }
    }


    private static Object[] getRawResults(String[] value) {
        Object[] obj = new Object[]{0, 0, ""};
        if (value != null) {
            obj[0] = value[0] == null ? 0 : Utils.parseLongSafe(value[0], 0);
            obj[1] = value[1] == null ? 0 : Utils.parseLongSafe(value[1], 0);
            obj[2] = value[2];
        }
        return obj;
    }


    public static void initInfoSlip(TransData transData) {

        byte[] bytesField63 = transData.getField63RecByte();

        ArrayList<String> infoSlip = new ArrayList<String>();

        byte[] arrLen = Arrays.copyOfRange(bytesField63, 0, 2);
        int infoLen = Integer.parseInt(byteArrToStringHex(arrLen));

        byte[] arrTemplateId = Arrays.copyOfRange(bytesField63, 4, 6);
        int TemplateId = Integer.parseInt(byteArrToStringHex(arrTemplateId), 16);
        transData.setTemplateId(TemplateId);

        byte[] arrInfoSlip = Arrays.copyOfRange(bytesField63, 6, infoLen + 2);

        for (int i = 0; i < arrInfoSlip.length; i++) {
            int inthex = Integer.parseInt(Tools.byteToStringHex(arrInfoSlip[i]));
            int lenStart = i + 1;
            int lenEnd = inthex + i + 1;
            byte[] infoPerLine = Arrays.copyOfRange(arrInfoSlip, lenStart, lenEnd);
            String strAscii = ConvertArrByteToASCII(infoPerLine);
            infoSlip.add(strAscii);
            i += inthex;
        }

        // get remaining balance
        int remaining = 0;
        byte[] arrBA = Arrays.copyOfRange(bytesField63, bytesField63.length - 9, bytesField63.length);
        byte[] arrNameBA = Arrays.copyOfRange(arrBA, 0, 2);
        String nameBA = ConvertArrByteToASCII(arrNameBA);
        if (nameBA.equals("BA")) {
            byte[] arrRemaining = Arrays.copyOfRange(arrBA, 4, arrBA.length);
            remaining = Integer.parseInt(byteArrToStringHex(arrRemaining));

            int balNeg = Integer.parseInt(Tools.byteToStringHex(arrBA[3]));
            if (balNeg == 1) {
                remaining = remaining * -1;
            }
        } else {
            remaining = 0;
        }
        transData.setRemainingAmt(remaining);

        transData.setInfoSlipLinePay(infoSlip.toArray(new String[0]));

    }

    private static String ConvertArrByteToASCII(byte[] ArrByte) {
        String temp = "";
        for (int ii = 0; ii < ArrByte.length; ii++) {
            String strhex = Tools.byteToStringHex(ArrByte[ii]);
            if (strhex.equals("FC")) {
                int spaceLen = Integer.parseInt(Tools.byteToStringHex(ArrByte[ii + 1]));
                temp += paddingSpace(spaceLen);
                ii++;
            } else {
                temp += Tools.bytes2String(new byte[]{ArrByte[ii]});

            }
        }
        return temp;
    }

    private static String byteArrToStringHex(byte[] b) {
        String strHex = "";
        for (int i = 0; i < b.length; i++) {
            strHex += String.format("%02X", b[i]);
        }
        return strHex;
    }

    public static boolean isPrintSlipLinaPay() {
        int receiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_NUM_OF_SLIP_LINEPAY);
        if (receiptNum == 0) {
            return false;
        } else {
            return true;
        }
    }

    public static void generateGrandTotal(IPage page, int FONT_NORMAL, TransTotal transTotal) {
        long grandSaleNum = 0, grandSaleAmt = 0;
        long grandSaleVoidNum = 0, grandSaleVoidAmt = 0;
        long grandRefundNum = 0, grandRefundAmt = 0;
        long grandTopUpNum = 0, grandTopUpAmt = 0;
        long grandTopUpRefundNum = 0, grandTopUpRefundAmt = 0;

        TransTotal total = null;
        total = (transTotal != null) ? transTotal : FinancialApplication.getTransTotalDbHelper().calcTotal(true);
        if (total != null) {
            grandSaleNum = total.getSaleTotalNum();
            grandSaleAmt = total.getSaleTotalAmt();
            grandSaleVoidNum = total.getSaleVoidTotalNum();
            grandSaleVoidAmt = total.getSaleVoidTotalAmt();
            grandRefundNum = total.getRefundTotalNum();
            grandRefundAmt = total.getRefundTotalAmt();
            grandTopUpNum = total.getTopupTotalNum();
            grandTopUpAmt = total.getTopupTotalAmt();
            grandTopUpRefundNum = total.getTopupVoidTotalNum();
            grandTopUpRefundAmt = total.getTopupVoidTotalAmt();
        }

        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_grand_totals))
                        .setFontSize(FONT_NORMAL));
        page.addLine().addUnit(page.createUnit().setText(" "));

        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(" : " + grandSaleNum)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(grandSaleAmt))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // void
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(" : " + grandSaleVoidNum)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - grandSaleVoidAmt))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(" : " + grandRefundNum)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - grandRefundAmt))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        // top up
        if (grandTopUpNum != 0.00) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + grandTopUpNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
            String tmpTopUp = CurrencyConverter.convert(grandTopUpAmt);
            if (grandTopUpAmt != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
        }

        // top up refund
        if (grandTopUpRefundNum != 0.00) {
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TOPUP REFUND")
                            .setFontSize(FONT_NORMAL))
                    .addUnit(page.createUnit()
                            .setText(" : " + grandTopUpRefundNum)
                            .setFontSize(FONT_NORMAL)
                            .setWeight(3.0f));
            String tmpTopUpRefund = CurrencyConverter.convert(grandTopUpRefundAmt);
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(tmpTopUpRefund)
                            .setFontSize(FONT_NORMAL)
                            .setGravity(Gravity.END));
        }

        page.addLine().addUnit(page.createUnit()
                .setText("---------")
                .setGravity(Gravity.END));

        long grandNum = grandSaleNum + grandRefundNum + grandTopUpNum + grandTopUpRefundNum;
        long grandAmt = grandSaleAmt - grandRefundAmt + grandTopUpAmt - grandTopUpRefundAmt;

        // total
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(" : " + grandNum)
                        .setFontSize(FONT_NORMAL)
                        .setWeight(3.0f));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(grandAmt))
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));

        return;
    }

    public static void generateTransDetailQRSale(TransData transData, IPage page, int fontSize) {
        String temp;

        ETransType transType = transData.getTransType();
        String type = Component.findQRType(transData);

        String cardID = transData.getQrType();
        String refName = Utils.getString(R.string.receipt_qr_id);
        String ref = transData.getQrID();

        if (cardID.equals("01")) {
            refName = Utils.getString(R.string.receipt_ref2);
            ref = transData.getQrRef2();
        }

        if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
            type += "(VOID)";
        }

        // AET-18
        // transaction NO/transaction type/amount
        if (transType.isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        // date/time
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);

        page.addLine()
                .addUnit(page.createUnit().setText(type).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        // trace no/appr code
        temp = transData.getAuthCode() == null ? "" : transData.getAuthCode();
        String temp2 = Component.getPaddedNumber(transData.getTraceNo(), 6);

        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_trans_no_short_sharp) + " " + temp2).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_appr_code) + " " + temp).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        //AET-125
        page.addLine()
                .addUnit(page.createUnit().setText(formattedDate).setFontSize(fontSize).setWeight(3.0f))
                .addUnit(page.createUnit().setText(formattedTime).setGravity(Gravity.END).setFontSize(fontSize).setWeight(3.0f));

        if (ref != null) {
            page.addLine()
                    .addUnit(page.createUnit().setText(refName + " " + ref).setFontSize(fontSize).setWeight(3.0f));
        }

        temp2 = transData.getRefNo() == null ? "" : transData.getRefNo();
        page.addLine()
                .addUnit(page.createUnit().setText(Utils.getString(R.string.receipt_trans_id) + " " + temp2).setFontSize(fontSize).setWeight(3.0f));

        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }


    public static boolean chkSettlementStatus(String acqName) {
        if (acqName != null) {
            switch (acqName) {
                case Constants.ACQ_DCC:
                    return FinancialApplication.getController().get(Controller.SETTLE_DCC_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_REDEEM:
                    return FinancialApplication.getController().get(Controller.SETTLE_REDEEM_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_REDEEM_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_REDEEM_BDMS_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_SMRTPAY:
                    return FinancialApplication.getController().get(Controller.SETTLE_SMARTPAY_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_SMRTPAY_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_SMARTPAY_BDMS_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_AMEX:
                    return FinancialApplication.getController().get(Controller.SETTLE_AMEX_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_KBANK: // Visa, Master, JCB
                    return FinancialApplication.getController().get(Controller.SETTLE_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_KBANK_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_KBANK_BDMS_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_UP: // UnionPay, TPN, Diners
                    return FinancialApplication.getController().get(Controller.SETTLE_UP_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_WALLET:
                    return FinancialApplication.getController().get(Controller.SETTLE_WALLET_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_AMEX_EPP:
                    return FinancialApplication.getController().get(Controller.SETTLE_AMEX_EPP_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_ALIPAY:
                    return FinancialApplication.getController().get(Controller.SETTLE_ALIPAY_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    return FinancialApplication.getController().get(Controller.SETTLE_ALIPAY_B_SCAN_C_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_WECHAT:
                    return FinancialApplication.getController().get(Controller.SETTLE_WECHAT_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    return FinancialApplication.getController().get(Controller.SETTLE_WECHAT_B_SCAN_C_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_KPLUS:
                    return FinancialApplication.getController().get(Controller.SETTLE_KPLUS_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_BAY_INSTALLMENT:
                    return FinancialApplication.getController().get(Controller.SETTLE_BAY_INSTALLMENT_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_QR_CREDIT:
                    return FinancialApplication.getController().get(Controller.SETTLE_QR_CREDIT_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_DOLFIN:
                    return FinancialApplication.getController().get(Controller.SETTLE_DOLFIN_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_SCB_IPP:
                    return FinancialApplication.getController().get(Controller.SETTLE_SCB_INSTALLMENT_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_SCB_REDEEM:
                    return FinancialApplication.getController().get(Controller.SETTLE_SCB_REDEEM_STATUS) != Controller.Constant.WORKED;
                case Constants.ACQ_KCHECKID:
                    return FinancialApplication.getController().get(Controller.SETTLE_KCHECKID_STATUS) != Controller.Constant.WORKED;
				case Constants.ACQ_MY_PROMPT:
                    return FinancialApplication.getController().get(Controller.SETTLE_MY_PROMPT_STATUS) != Controller.Constant.WORKED;                    
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    return FinancialApplication.getController().get(Controller.SETTLE_DOLFIN_INSTALMENT_STATUS) != Controller.Constant.WORKED;
            }
        }

        return false;
    }

    public static int getSettlementStatusByAcquirer(String acqName) {
        if (acqName != null) {
            switch (acqName) {
                case Constants.ACQ_DCC:
                    return FinancialApplication.getController().get(Controller.SETTLE_DCC_STATUS) ;
                case Constants.ACQ_REDEEM:
                    return FinancialApplication.getController().get(Controller.SETTLE_REDEEM_STATUS) ;
                case Constants.ACQ_REDEEM_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_REDEEM_BDMS_STATUS) ;
                case Constants.ACQ_SMRTPAY:
                    return FinancialApplication.getController().get(Controller.SETTLE_SMARTPAY_STATUS) ;
                case Constants.ACQ_SMRTPAY_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_SMARTPAY_BDMS_STATUS) ;
                case Constants.ACQ_AMEX:
                    return FinancialApplication.getController().get(Controller.SETTLE_AMEX_STATUS);
                case Constants.ACQ_KBANK: // Visa, Master, JCB
                    return FinancialApplication.getController().get(Controller.SETTLE_STATUS);
                case Constants.ACQ_KBANK_BDMS:
                    return FinancialApplication.getController().get(Controller.SETTLE_KBANK_BDMS_STATUS);
                case Constants.ACQ_UP: // UnionPay, TPN, Diners
                    return FinancialApplication.getController().get(Controller.SETTLE_UP_STATUS);
                case Constants.ACQ_WALLET:
                    return FinancialApplication.getController().get(Controller.SETTLE_WALLET_STATUS);
                case Constants.ACQ_AMEX_EPP:
                    return FinancialApplication.getController().get(Controller.SETTLE_AMEX_EPP_STATUS);
                case Constants.ACQ_ALIPAY:
                    return FinancialApplication.getController().get(Controller.SETTLE_ALIPAY_STATUS);
                case Constants.ACQ_ALIPAY_B_SCAN_C:
                    return FinancialApplication.getController().get(Controller.SETTLE_ALIPAY_B_SCAN_C_STATUS);
                case Constants.ACQ_WECHAT:
                    return FinancialApplication.getController().get(Controller.SETTLE_WECHAT_STATUS);
                case Constants.ACQ_WECHAT_B_SCAN_C:
                    return FinancialApplication.getController().get(Controller.SETTLE_WECHAT_B_SCAN_C_STATUS);
                case Constants.ACQ_KPLUS:
                    return FinancialApplication.getController().get(Controller.SETTLE_KPLUS_STATUS);
                case Constants.ACQ_BAY_INSTALLMENT:
                    return FinancialApplication.getController().get(Controller.SETTLE_BAY_INSTALLMENT_STATUS);
                case Constants.ACQ_QR_CREDIT:
                    return FinancialApplication.getController().get(Controller.SETTLE_QR_CREDIT_STATUS);
                case Constants.ACQ_DOLFIN:
                    return FinancialApplication.getController().get(Controller.SETTLE_DOLFIN_STATUS);
                case Constants.ACQ_SCB_IPP:
                    return FinancialApplication.getController().get(Controller.SETTLE_SCB_INSTALLMENT_STATUS);
                case Constants.ACQ_SCB_REDEEM:
                    return FinancialApplication.getController().get(Controller.SETTLE_SCB_REDEEM_STATUS);
                case Constants.ACQ_KCHECKID:
                    return FinancialApplication.getController().get(Controller.SETTLE_KCHECKID_STATUS);
                case Constants.ACQ_MY_PROMPT:
                    return FinancialApplication.getController().get(Controller.SETTLE_MY_PROMPT_STATUS);                    
                case Constants.ACQ_DOLFIN_INSTALMENT:
                    return FinancialApplication.getController().get(Controller.SETTLE_DOLFIN_INSTALMENT_STATUS);
            }
        }

        return Controller.Constant.NO_ACQUIRER;
    }

    public static void setSettleStatus(int settle_stateVal, String acquirerName) {
        String key = "";
        switch (acquirerName) {
            case "All Acquirers":
                setSettleStatusAllAcqs(settle_stateVal);
                return;
            case Constants.ACQ_DCC:
                key = Controller.SETTLE_DCC_STATUS;
                break;
            case Constants.ACQ_REDEEM:
                key = Controller.SETTLE_REDEEM_STATUS;
                break;
            case Constants.ACQ_REDEEM_BDMS:
                key = Controller.SETTLE_REDEEM_BDMS_STATUS;
                break;
            case Constants.ACQ_SMRTPAY:
                key = Controller.SETTLE_SMARTPAY_STATUS;
                break;
            case Constants.ACQ_SMRTPAY_BDMS:
                key = Controller.SETTLE_SMARTPAY_BDMS_STATUS;
                break;
            case Constants.ACQ_AMEX:
                key = Controller.SETTLE_AMEX_STATUS;
                break;
            case Constants.ACQ_KBANK: // Visa, Master, JCB
                key = Controller.SETTLE_STATUS;
                break;
            case Constants.ACQ_KBANK_BDMS:
                key = Controller.SETTLE_KBANK_BDMS_STATUS;
                break;
            case Constants.ACQ_UP: // UnionPay, TPN, Diners
                key = Controller.SETTLE_UP_STATUS;
                break;
            case Constants.ACQ_WALLET:
                key = Controller.SETTLE_WALLET_STATUS;
                break;
            case Constants.ACQ_AMEX_EPP:
                key = Controller.SETTLE_AMEX_EPP_STATUS;
                break;
            case Constants.ACQ_ALIPAY:
                key = Controller.SETTLE_ALIPAY_STATUS;
                break;
            case Constants.ACQ_ALIPAY_B_SCAN_C:
                key = Controller.SETTLE_ALIPAY_B_SCAN_C_STATUS;
                break;
            case Constants.ACQ_WECHAT:
                key = Controller.SETTLE_WECHAT_STATUS;
                break;
            case Constants.ACQ_WECHAT_B_SCAN_C:
                key = Controller.SETTLE_WECHAT_B_SCAN_C_STATUS;
                break;
            case Constants.ACQ_KPLUS:
                key = Controller.SETTLE_KPLUS_STATUS;
                break;
            case Constants.ACQ_BAY_INSTALLMENT:
                key = Controller.SETTLE_BAY_INSTALLMENT_STATUS;
                break;
            case Constants.ACQ_QR_CREDIT:
                key = Controller.SETTLE_QR_CREDIT_STATUS;
                break;
            case Constants.ACQ_DOLFIN:
                key = Controller.SETTLE_DOLFIN_STATUS;
                break;
            case Constants.ACQ_SCB_IPP:
                key = Controller.SETTLE_SCB_INSTALLMENT_STATUS;
                break;
            case Constants.ACQ_SCB_REDEEM:
                key = Controller.SETTLE_SCB_REDEEM_STATUS;
                break;
            case Constants.ACQ_KCHECKID:
                key = Controller.SETTLE_KCHECKID_STATUS;
                break;
            case Constants.ACQ_MY_PROMPT:
                key = Controller.SETTLE_MY_PROMPT_STATUS;
                break;
            case Constants.ACQ_DOLFIN_INSTALMENT:
                key = Controller.SETTLE_DOLFIN_INSTALMENT_STATUS;
                break;
        }
        FinancialApplication.getController().set(key, settle_stateVal);
    }

    private static void setSettleStatusAllAcqs(int value) {
        FinancialApplication.getController().set(Controller.SETTLE_DCC_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_REDEEM_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_REDEEM_BDMS_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_SMARTPAY_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_SMARTPAY_BDMS_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_AMEX_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_KBANK_BDMS_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_UP_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_WALLET_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_BBL_BSS_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_LINEPAY_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_AMEX_EPP_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_ALIPAY_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_WECHAT_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_KPLUS_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_BAY_INSTALLMENT_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_QR_CREDIT_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_DOLFIN_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_SCB_INSTALLMENT_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_SCB_REDEEM_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_KCHECKID_STATUS, value);
        FinancialApplication.getController().set(Controller.SETTLE_MY_PROMPT_STATUS, value);
    }

    public static String unpackBit63Credit(byte[] field63, String strField63) {
        String errorMsg = "";
        int len63 = field63 != null ? field63.length : 0;
        if (len63 > 0) {
            errorMsg = strField63.trim();
            if (len63 >= 44) {
                byte[] f63 = new byte[len63 - 4];
                System.arraycopy(field63, 4, f63, 0, len63 - 4);
                errorMsg = Tools.bytes2String(f63).trim();
            }
        }
        return errorMsg;
    }

    public static Bitmap getImageFromInternalFile(String fileName) {
        Bitmap image = null;
        String path = loadString(Constants.DN_PARAM_SLIP_LOGO_PATH);
        try {
            if (path != null) {
                File f = new File(path, fileName);
                image = BitmapFactory.decodeStream(new FileInputStream(f));
            } else {
                AssetManager am = FinancialApplication.getApp().getResources().getAssets();
//                InputStream is = am.open("kbank+bangHosp-small.png");
//                InputStream is = am.open("bps_receipt_logo.jpg");
                try {
                    InputStream is = am.open("kBank_Default.jpg");
                    image = BitmapFactory.decodeStream(is);
                    is.close();
                } catch (Exception ex) {
                    Log.d(TAG, ex.getMessage());
                }
            }
            if (image != null && image.getWidth() > 385) {
                Bitmap scaled = resizeBitmap(image, 385, image.getHeight());
                image = scaled;
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        return image;

    }

    public static Bitmap getImageFromInternalFile(String mainLogo, String hostLogo) {
        Bitmap image = null;
        String mainLogoPath = loadString(Constants.DN_PARAM_SLIP_LOGO_PATH);
        try {
            ContextWrapper cw = new ContextWrapper(FinancialApplication.getApp());
            File imageDir = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File fHostLogo = new File(imageDir, hostLogo + ".bmp");
            String HostNII = hostLogo.split("_")[0];
            String Hostname = hostLogo.split("_")[1];

            if (MerchantProfileManager.INSTANCE.isMultiMerchantEnable()) {
                if(MerchantProfileManager.INSTANCE.getCurrentMerchantLogo() != null){
                    Bitmap defMercLogo = null;

                    File defaultMerchantLogo = new File(MerchantProfileManager.INSTANCE.getCurrentMerchantLogo());
                    if (defaultMerchantLogo.exists()) { defMercLogo = BitmapFactory.decodeStream(new FileInputStream(defaultMerchantLogo)); }

                    if (fHostLogo.exists()) {
                        image = BitmapFactory.decodeStream(new FileInputStream(fHostLogo));
                    } else {
                        if (defMercLogo != null) {
                            image = defMercLogo;
                        } else {
                            AssetManager am = FinancialApplication.getApp().getResources().getAssets();
                            InputStream is = am.open("kBank_Default.jpg");
                            image = BitmapFactory.decodeStream(is);
                            is.close();
                        }
                    }
                } else {
                    image = null;
                }
            } else {
                if(image == null){
                    if (fHostLogo.exists()) {
                        image = BitmapFactory.decodeStream(new FileInputStream(fHostLogo));
                    } else {
                        AssetManager am = FinancialApplication.getApp().getResources().getAssets();
                        InputStream is = null;
                        if (Hostname.contains(Constants.ACQ_SMRTPAY)) {
                            is = am.open("logo_kbank_smartpay.bmp");
                        } else if (Hostname.equals(Constants.ACQ_KPLUS)) {
                            is = am.open("logo_kbank_kplus.bmp");
                        } else if (Hostname.equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                            is = am.open("logo_kbank_dolfin_smartpay.bmp");
                        }else {
                            if (mainLogoPath != null) {
                                File f = new File(mainLogoPath, mainLogo);
                                is = new FileInputStream(f);
                            }
                            if (is == null) {
                                is = am.open("kBank_Default.jpg");
                            }
                        }
                        image = BitmapFactory.decodeStream(is);
                        is.close();
                    }
                }
            }

            if (image != null && image.getWidth() > 385) {
                float wPercen = (float) (256 * 100 / image.getWidth());
                int newHeight = (int) (wPercen * image.getHeight() / 100);
                image = Bitmap.createScaledBitmap(image, 385, newHeight, true);
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        return image;

    }

    public static Bitmap getSmartPayFromInternalFile(String hostLogo) {
        Bitmap image = null;
        String mainLogoPath = loadString(Constants.DN_PARAM_SLIP_LOGO_PATH);
        try {
            ContextWrapper cw = new ContextWrapper(FinancialApplication.getApp());
            File imageDir = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File fHostLogo = new File(imageDir, hostLogo + ".bmp");

            if (fHostLogo.exists()) {
                image = BitmapFactory.decodeStream(new FileInputStream(fHostLogo));
            } else {
                AssetManager am = FinancialApplication.getApp().getResources().getAssets();
                InputStream is = am.open("logo_kbank_smartpay.bmp");
                image = BitmapFactory.decodeStream(is);
                is.close();
            }

            if (image != null && image.getWidth() > 385) {
                float wPercen = (float) (256 * 100 / image.getWidth());
                int newHeight = (int) (wPercen * image.getHeight() / 100);
                image = Bitmap.createScaledBitmap(image, 385, newHeight, true);
            }


        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        return image;

    }

    public static Bitmap getImageFromInternalFile(String downloadPath, String downloadFileName, String defaultFileName) {
        Bitmap image = null;
        String path = loadString(downloadPath);
        try {
            if (path != null) {
                File f = new File(path, downloadFileName);
                image = BitmapFactory.decodeStream(new FileInputStream(f));
            } else if (defaultFileName != null) {
                AssetManager am = FinancialApplication.getApp().getResources().getAssets();
                InputStream is = am.open(defaultFileName);
                image = BitmapFactory.decodeStream(is);
                is.close();
            }

        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        if (image != null && image.getWidth() > 385) {
            Bitmap scaled = resizeBitmap(image, 385, image.getHeight());
            image = scaled;
        }

        return image;

    }

    public static Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }


    private static String loadString(String stringName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        String path = prefs.getString(stringName, null);
        return path;
    }

    public static TransData copyOrigTransDataWallet(TransData transData, TransData origTransData) {
        initTransDataWallet(transData, origTransData);

        ETransType origTransType = origTransData.getTransType();

        transData.setOrigTransType(origTransType);
        transData.setOrigDateTime(origTransData.getDateTime());
        transData.setOrigRefNo((origTransType == ETransType.QR_SALE_WALLET ||
                origTransType == ETransType.QR_INQUIRY_ALIPAY ||
                origTransType == ETransType.QR_INQUIRY_WECHAT ||
                origTransType == ETransType.QR_INQUIRY_CREDIT) ? origTransData.getRefNo() : origTransData.getOrigRefNo());
        transData.setOrigAuthCode(origTransData.getAuthCode());
        transData.setOrigTransNo(origTransData.getTraceNo());
        transData.setOrigAmount(origTransData.getAmount());
        transData.setAmount(origTransData.getAmount());
        transData.setQrBuyerCode(origTransData.getQrBuyerCode());
        transData.setAuthCode(origTransData.getAuthCode());
        transData.setTraceNo(origTransData.getTraceNo());

        //For KBANK E-Wallet
        transData.setQRCurrency(origTransData.getQRCurrency());
        transData.setWalletPartnerID(origTransData.getWalletPartnerID());
        transData.setTxnID(origTransData.getTxnID());
        transData.setPayTime(origTransData.getPayTime());
        transData.setExchangeRate(origTransData.getExchangeRate());
        transData.setAmountCNY(origTransData.getAmountCNY());
        transData.setTxnAmount(origTransData.getTxnAmount());
        transData.setBuyerUserID(origTransData.getBuyerUserID());
        transData.setBuyerLoginID(origTransData.getBuyerLoginID());
        transData.setMerchantInfo(origTransData.getMerchantInfo());
        transData.setAppCode(origTransData.getAppCode());
        transData.setPromocode(origTransData.getPromocode());
        transData.setTxnNo(origTransData.getTxnNo());
        transData.setFee(origTransData.getFee());

        // for QR-TAG-31
        transData.setEnableQrTag31(origTransData.getEnableQrTag31());
        transData.setQrSourceOfFund(origTransData.getQrSourceOfFund());
        //for Alipay and Wechat pay
        transData.setQrResult(origTransData.getQrResult());
	    //for My Prompt
        transData.setQrCode(origTransData.getQrCode());

        return transData;
    }

    private static TransData initTransDataWallet(TransData transData, TransData origTransData) {
//        AcqManager acqManager = FinancialApplication.getAcqManager();
//        Acquirer acquirer = acqManager.findAcquirer(Constants.ACQ_WALLET);
//        Issuer issuer = acqManager.findIssuer(Constants.ISSUER_WALLET);

        transInit(transData, origTransData.getAcquirer());

        transData.setIssuer(origTransData.getIssuer());
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        transData.setWalletRetryStatus(TransData.WalletRetryStatus.NORMAL);
        transData.setAdviceStatus(TransData.AdviceStatus.NORMAL);
        return transData;
    }

    public static String findQRType(TransData transData) {
        String cardID = transData.getQrType();
        String qrType = "";
        if (cardID.equals("01")) {
            qrType = Utils.getString(R.string.receipt_qr_prmptpay);
        } else if (cardID.equals("03")) {
            qrType = Utils.getString(R.string.receipt_qr_visa);
        } else if (cardID.equals("04")) {
            qrType = Utils.getString(R.string.receipt_qr_master);
        } else if (cardID.equals("05")) {
            qrType = Utils.getString(R.string.receipt_qr_unionpay);
        } else if (cardID.equals("06")) {
            qrType = Utils.getString(R.string.receipt_qr_tpn);
        } else if (cardID.equals("07")) {
            qrType = Utils.getString(R.string.receipt_qr_itmx);
        } else if (cardID.equals("08")) {
            qrType = Utils.getString(R.string.receipt_qr_jcb);
        }
        return qrType;
    }

    public static TransData initTextOnSlipQRVisa(TransData transData) {
        byte[] temp;
        byte[] field63 = transData.getField63RecByte();

        for (int ii = 0; ii < field63.length; ii++) {
            temp = Arrays.copyOfRange(field63, ii, ii + 2);
            int length = Integer.parseInt(Tools.bcd2Str(temp));

            temp = Arrays.copyOfRange(field63, ii + 2, ii + 4);
            String tableID = Tools.bytes2String(temp);

            if (tableID.equals("TX")) {

                temp = Arrays.copyOfRange(field63, ii + 4, ii + 5);
                String cardID = Tools.bcd2Str(temp);
                transData.setQrType(cardID);

                String qrType = "";
                if (cardID.equals("01")) {
                    qrType = "PROMPTPAY";
                } else if (cardID.equals("03")) {
                    qrType = "QR-VISA";
                } else if (cardID.equals("04")) {
                    qrType = "QR-MASTER";
                } else if (cardID.equals("05")) {
                    qrType = "QR-UPI";
                } else if (cardID.equals("06")) {
                    qrType = "QR-TPN";
                } else if (cardID.equals("07")) {
                    qrType = "QR-ITMX";
                } else if (cardID.equals("08")) {
                    qrType = "QR-JCB";
                }
                transData.getIssuer().setIssuerName(qrType);

                byte[] bytesSlip = Arrays.copyOfRange(field63, ii + 5, ii + length + 2);
                String strField63 = "";
                int index = 0;
                if (bytesSlip != null) {
                    int lenField = bytesSlip.length;
                    for (int i = 0; i < lenField; i++) {

                        String strhex = Tools.byteToStringHex(bytesSlip[i]);

                        if (strhex.equals("FC") || strhex.equals("FB")) {
                            if (index == 0) {
                                byte[] arr = Arrays.copyOfRange(bytesSlip, 1, i);
                                strField63 += Tools.bytes2String(arr);
                            } else {
                                byte[] arr = Arrays.copyOfRange(bytesSlip, index + 2, i);
                                strField63 += Tools.bytes2String(arr);
                            }
                            index = i;

                            int size = Integer.parseInt(Tools.byteToStringHex(bytesSlip[i + 1]));
                            if (strhex.equals("FC")) {
                                strField63 += paddingSpace(size);
                            } else if (strhex.equals("FB")) {
                                strField63 += paddingSpace(size).replace(' ', '-');
                            }
                        }

                    }
                    int lastIndex = index + 2;
                    if (lastIndex != lenField) {// For Wallet Settlement
                        byte[] arr = Arrays.copyOfRange(bytesSlip, lastIndex, lenField);
                        strField63 += Tools.bytes2String(arr);
                    }
                }
                transData.setWalletSlipInfo(strField63);
                ii = ii + length + 2;
            } else {
                temp = Arrays.copyOfRange(field63, ii + 3, ii + 23);
                String cardID = Tools.bytes2String(temp);
                transData.setQrID(cardID);

                temp = Arrays.copyOfRange(field63, ii + 23, ii + 43);
                String qrRef = Tools.bytes2String(temp);
                ii = ii + 43;

                if (field63.length > ii) {
                    temp = Arrays.copyOfRange(field63, ii, field63.length);
                    String cusPan = Tools.bytes2String(temp);
                    transData.setPan(cusPan);
                    ii = field63.length;
                } else {
                    transData.setPan(transData.getRefNo());
                }
            }
        }
        return transData;
    }

    public static TransData chkTxnIsSmallAmt(TransData transData) {
        // check acquirer & issuer is enabled small amount flag or not.
        Issuer issuer = transData.getIssuer();
        ETransType transType = transData.getTransType();
        if ((transType == ETransType.SALE || transType == ETransType.OFFLINE_TRANS_SEND ||
                transType == ETransType.REFUND || transType == ETransType.PREAUTH ||
                transType == ETransType.PREAUTHORIZATION || transType == ETransType.ADJUST || transType == ETransType.SALE_COMPLETION)
                && transData.getAcquirer() != null && transData.getAcquirer().isEnableSmallAmt()
                && issuer != null && issuer.isEnableSmallAmt()) {
            // If enabled, need to keep flag for each transaction to support reprint slip process.
            transData.setTxnSmallAmt(Utils.parseLongSafe(transData.getAmount(), 0) <= issuer.getSmallAmount());
            transData.setNumSlipSmallAmt(issuer.getNumberOfReceipt());
        }

        return transData;
    }

    public static TransData saveOfflineTransNormalSale(TransData transData) {
        chkTxnIsSmallAmt(transData);
        transData.setOnlineTrans(false);
        transData.setOrigDateTime(transData.getDateTime());
        transData.setOfflineSendState(TransData.OfflineStatus.OFFLINE_NOT_SENT);
        transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        FinancialApplication.getTransDataDbHelper().insertTransData(transData);
        //increase stan/trace no.
        Component.incStanNo(transData);
        Component.incTraceNo(transData);
        return transData;
    }

    public static String getReceiptTxtSmallAmt(String issuerBrand) {
        String resString = "receipt_small_amt_";
        if (issuerBrand != null) {
            if (issuerBrand.contains("-")) {
                String issuer = issuerBrand.substring(0, issuerBrand.indexOf("-")).toLowerCase();
                resString += issuer;
            } else {
                resString += issuerBrand.toLowerCase();
            }

            int resId = Utils.getResId(resString, "string");
            if (resId > 0)
                return Utils.getString(resId);
        }
        return null;
    }

    public static String getTransByIPlanMode(TransData transData) {
        String resString = "trans_instalment_";
        String iPlanMode = transData.getInstalmentIPlanMode();
        String trans = Utils.getString(R.string.trans_instalment);
        if (transData.getAcquirer().getName().equals(Constants.ACQ_DOLFIN_INSTALMENT))
            trans = Utils.getString(R.string.trans_dolfin);
        if (iPlanMode != null) {
            resString += iPlanMode;
        }
        int resId = Utils.getResId(resString, "string");
        if (resId > 0)
            trans = trans + " " + Utils.getString(resId);
        return trans;
    }

    /**
     * find host by issuer
     *
     * @param transData transData
     */
    public static Acquirer setAcqFromIssuer(TransData transData) {
        Acquirer acquirer = null;
        AcqManager acqManager = FinancialApplication.getAcqManager();
        TransTypeMapping transMapping = acqManager.findTransMapping(transData.getTransType(), transData.getIssuer(), TransTypeMapping.FIRST_PRIORITY);
        if (transMapping != null) {
            acquirer = transMapping.getAcquirer();
            acqManager.setCurAcq(acquirer);
            FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, acquirer.getName());

            transData.setBatchNo(acquirer.getCurrBatchNo());
            transData.setTpdu("600" + acquirer.getNii() + "8000");
            transData.setAcquirer(acquirer);
            Log.i(TAG, "setAcqFromIssuer[transType=" + transMapping.getTransType() + ", acquirer=" + transMapping.getAcquirer().getName() + "]");
        }
        return acquirer;
    }

    public static boolean isDccRequired(TransData transData, String pan, byte[] appCurrency) {
        return FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_DCC).isEnable()
                && !chkSettlementStatus(Constants.ACQ_DCC)
                && (Constants.ISSUER_VISA.equals(transData.getIssuer().getName())
                    || Constants.ISSUER_MASTER.equals(transData.getIssuer().getName()))
                && ETransType.OFFLINE_TRANS_SEND != transData.getTransType()
                && (ETransType.SALE == transData.getTransType()
                    || ETransType.SALE_COMPLETION == transData.getTransType())
                && !isLocalCardRange(pan, appCurrency)
                && transData.getEnterMode() != EnterMode.CLSS   //Kbank requirement no have DCC on contactless
                && !isAllowDynamicOffline(transData);   //Kbank requirement disable DCC if dynamic offline is active
    }

    private static boolean isLocalCardRange(String pan, byte[] appCurrency) {
        if (appCurrency != null) {
            return Arrays.equals(appCurrency, new byte[]{0x07, 0x64});
        }

        List<CardRange> matchedCardRangesMaster = FinancialApplication.getAcqManager().findCardRange(pan, "MASTERCARD");
        List<CardRange> matchedCardRangesVisa = FinancialApplication.getAcqManager().findCardRange(pan, "VISA-CARD");
        boolean isLocalCard = true;

        if (matchedCardRangesMaster != null
                && matchedCardRangesMaster.size() == 1) {
            isLocalCard = false;
        }

        if (matchedCardRangesVisa != null
                && matchedCardRangesVisa.size() == 1) {
            isLocalCard = false;
        }

        return isLocalCard;
    }

    public static void insertTransTypeMapping() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<TransTypeMapping> mappings = Utils.readObjFromJSON("transtype_mapping.json", TransTypeMapping.class);
        for (TransTypeMapping map : mappings) {
            Acquirer acquirer = acqManager.findAcquirer(map.getAcquirerName());
            Issuer issuer = acqManager.findIssuer(map.getIssuerName());
            if (acquirer != null && issuer != null) {
                Log.i(TAG, "typemapping: [" + map.getTransType() + ", " + acquirer.getName() + ", " + issuer.getName() + ", " + map.getPriority() + "]");
                acqManager.insertTransMapping(map.getTransType(), acquirer, issuer, map.getPriority());
            }
        }
    }

    public static TransData splitField63Wallet(TransData transData, byte[] field63RecByte) {
        byte[] temp;
        temp = Arrays.copyOfRange(field63RecByte, 0, 3);
        transData.setQRCurrency(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 3, 35);
        transData.setWalletPartnerID(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 35, 99);
        transData.setTxnID(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 99, 115);
        transData.setPayTime(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 115, 127);
        transData.setExchangeRate(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 127, 139);
        transData.setAmountCNY(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 139, 151);
        transData.setTxnAmount(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 151, 167);
        transData.setBuyerUserID(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 167, 187);
        transData.setBuyerLoginID(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 187, 315);
        transData.setMerchantInfo(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 315, 317);
        transData.setAppCode(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 317, 341);
        transData.setPromocode(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 341, 365);
        transData.setTxnNo(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 365, 389);
        transData.setFee(Tools.bytes2String(temp));

        temp = Arrays.copyOfRange(field63RecByte, 389, 789);
        transData.setQrCode(Tools.bytes2String(temp).trim());

        return transData;
    }

    public static List<Bitmap> generateTotalByThaiQRSourceOfFundBitmappArrayTOPS (Acquirer acquirer, List<TransData> transDataList, int fontSize, List<Bitmap> bitmaps, IImgProcessing imgProcessing) {

        if (acquirer==null) {return bitmaps;}
        if (transDataList.isEmpty()) {return bitmaps;}

        // Declaration
        IPage page = null;



        Map<String, List<TransData>> sourceOfFundLists = QrTag31Utils.Companion.getDistinctSourceOfFund(transDataList);
        if (sourceOfFundLists!=null && sourceOfFundLists.size()>0) {
            HashMap<String, Long> totalSummaryHashMap = new HashMap<>();
            List<Float> weigth = new ArrayList<Float>(Arrays.asList(2.0f, 1.0f, 2.0f));           // for majority type printing
            List<String> transTypes = new ArrayList<String>(Arrays.asList(
                    getString(R.string.report_printing_sale).toUpperCase(),
                    getString(R.string.report_printing_void_sale).toUpperCase(),
                    getString(R.string.report_printing_refund).toUpperCase(),
                    getString(R.string.report_printing_void_refund).toUpperCase(),
                    getString(R.string.report_printing_total).toUpperCase(),
                    getString(R.string.report_printing_sale_small_ticket).toUpperCase(),
                    getString(R.string.report_printing_sale_normal).toUpperCase(),
                    getString(R.string.report_printing_refund).toUpperCase(),
                    getString(R.string.report_printing_void_small_ticket).toUpperCase(),
                    getString(R.string.report_printing_void_normal).toUpperCase(),
                    getString(R.string.report_printing_void_refund).toUpperCase(),
                    getString(R.string.report_printing_total).toUpperCase()));
            int index = 0;

            for (String SOF : sourceOfFundLists.keySet()) {
                // get all source-of-fund transData
                List<TransData> transList = sourceOfFundLists.get(SOF);

                // calculate hash map
                HashMap<String, Long> printHashMap = new HashMap<>();
                printHashMap.put("TRANS__SALE_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS__SALE_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS__RFUN_COUNT", 0L);
                printHashMap.put("TRANS__RFUN_AMOUNT", 0L);
                printHashMap.put("TRANS_VSALE_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_VOID_KPLUS), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_VSALE_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_VOID_KPLUS), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_VRFUN_COUNT", 0L);
                printHashMap.put("TRANS_VRFUN_AMOUNT", 0L);
                printHashMap.put("TRANS_TOTAL_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_TOTAL_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS__SALE_SMTK_COUNT", 0L);
                printHashMap.put("TRANS__SALE_SMTK_AMOUNT", 0L);
                printHashMap.put("TRANS__SALE_NORM_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS__SALE_NORM_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS__RFUN__SUB_COUNT", 0L);
                printHashMap.put("TRANS__RFUN__SUB_AMOUNT", 0L);
                printHashMap.put("TRANS_VSALE_SMTK_COUNT", 0L);
                printHashMap.put("TRANS_VSALE_SMTK_AMOUNT", 0L);
                printHashMap.put("TRANS_VSALE_NORM_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_VOID_KPLUS), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_VSALE_NORM_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_VOID_KPLUS), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_VRFUN__SUB_COUNT", 0L);
                printHashMap.put("TRANS_VRFUN__SUB_AMOUNT", 0L);
                printHashMap.put("TRANS_TOTAL_SUB_COUNT", QrTag31Utils.Companion.getRecordCountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));
                printHashMap.put("TRANS_TOTAL_SUB_AMOUNT", QrTag31Utils.Companion.getSummaryAmountByTransType(acquirer, Arrays.asList(ETransType.QR_INQUIRY), Arrays.asList(ETransStatus.NORMAL), transList));

                appendHashMapSummaryData(totalSummaryHashMap, printHashMap);

                // Generate Pages
                generateThaiQrTag31PrintingByHashMap(bitmaps, imgProcessing, SOF, fontSize, transTypes, printHashMap, weigth, false);
            }

            // Generate Pages
            generateThaiQrTag31PrintingByHashMap(bitmaps, imgProcessing, "", fontSize, transTypes, totalSummaryHashMap, weigth, true);
        }

        return bitmaps;
    }

    private static void generateThaiQrTag31PrintingByHashMap(List<Bitmap> bitmaps, IImgProcessing imgProcessing, String sourceOfFund, int fontSize, List<String> transTypes, Map<String,Long> hashmap, List<Float> weigth, Boolean isGrandTotal) {
        IPage page = null;
        int index = 0;

        // pritning config
        float fullSizeOneLine = 1.2f;
        float fullSizeDoubleLine = 0.8f;
        float hfWidthSize = 0.6f;
        String singleLine = getString(R.string.receipt_one_line);
        String doubleLine = getString(R.string.receipt_double_line);

        page = Device.generatePage(true);
        for (String type : transTypes) {

            if (index==0 ) {
                if (!isGrandTotal) {
                    page.addLine().addUnit(page.createUnit().setScaleX(fullSizeOneLine).setGravity(Gravity.CENTER).setText(singleLine));
                    page.addLine().addUnit(page.createUnit().setFontSize(fontSize).setText(sourceOfFund));
                    page.addLine().addUnit(page.createUnit().setScaleX(fullSizeDoubleLine).setGravity(Gravity.CENTER).setText(doubleLine));
                    page.addLine().addUnit(page.createUnit().setScaleX(fullSizeOneLine).setGravity(Gravity.CENTER).setText(singleLine));
                    page.addLine().addUnit(page.createUnit().setFontSize(fontSize).setText(sourceOfFund + "-SUMMARY"));
                } else {
                    page.addLine().addUnit(page.createUnit().setScaleX(fullSizeOneLine).setGravity(Gravity.CENTER).setText(" "));
                    page.addLine().addUnit(page.createUnit().setScaleX(fullSizeOneLine).setGravity(Gravity.CENTER).setText(" "));
                }
            }

            List<String> keys = new ArrayList<>(hashmap.keySet());
            long transCount = 0L ;
            long transAmount = 0L ;
            boolean isTotalLine = (type.equals(getString(R.string.report_printing_total).toUpperCase()));

            if (type.equals(getString(R.string.report_printing_sale).toUpperCase())) {
                transCount = hashmap.get("TRANS__SALE_COUNT");
                transAmount = hashmap.get("TRANS__SALE_AMOUNT");
            } else if (type.equals(getString(R.string.report_printing_sale_normal).toUpperCase())) {
                transCount = hashmap.get("TRANS__SALE_NORM_COUNT");
                transAmount = hashmap.get("TRANS__SALE_NORM_AMOUNT");
            } else if (type.equals(getString(R.string.report_printing_void_sale).toUpperCase())) {
                transCount = hashmap.get("TRANS_VSALE_COUNT");
                transAmount = hashmap.get("TRANS_VSALE_AMOUNT");
            } else if (type.equals(getString(R.string.report_printing_void_normal).toUpperCase())) {
                transCount = hashmap.get("TRANS_VSALE_NORM_COUNT");
                transAmount = hashmap.get("TRANS_VSALE_NORM_AMOUNT");
            } else if (type.equals(getString(R.string.report_printing_total).toUpperCase())) {
                if (isGrandTotal) {
                    transCount = hashmap.get("TRANS_TOTAL_SUB_COUNT");
                    transAmount = hashmap.get("TRANS_TOTAL_SUB_AMOUNT");
                } else {
                    transCount = hashmap.get("TRANS_TOTAL_COUNT");
                    transAmount = hashmap.get("TRANS_TOTAL_AMOUNT");
                }
            }

            String dispTransTypeText = ((isGrandTotal && isTotalLine) ? "GRAND " : "") + type;
            String dispTransCountText = (isTotalLine) ? " " : Component.getPaddedNumber(transCount, 3);
            String dispTransAmountText = CurrencyConverter.convert(transAmount);

            if (isTotalLine) { page.addLine().addUnit(page.createUnit().setScaleX(fullSizeOneLine).setGravity(Gravity.CENTER).setText(singleLine)); }

            page.addLine()
                    .addUnit(page.createUnit().setWeight(2.0f).setScaleX(hfWidthSize).setFontSize(fontSize).setText(dispTransTypeText))
                    .addUnit(page.createUnit().setWeight(1.0f).setScaleX(hfWidthSize).setFontSize(fontSize).setGravity(Gravity.CENTER).setText(dispTransCountText))
                    .addUnit(page.createUnit().setWeight(2.0f).setScaleX(hfWidthSize).setFontSize(fontSize).setGravity(Gravity.END).setText(dispTransAmountText));

            if (isTotalLine) { page.addLine().addUnit(page.createUnit().setScaleX(fullSizeDoubleLine).setGravity(Gravity.CENTER).setText(doubleLine)); }

            index +=1;
        }

        bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        PageToSlipFormat.getInstance().isSettleMode = true;
        PageToSlipFormat.getInstance().Append(page);
    }

    private static HashMap<String, Long> appendHashMapSummaryData(HashMap<String, Long> srcMap, HashMap<String, Long> appendFromMap) {
        List<String> keys = new ArrayList<>(appendFromMap.keySet());
        for (String key : keys) {
            if (!srcMap.containsKey(key)) {
                srcMap.put(key, appendFromMap.get(key));
            } else {
                srcMap.put(key, srcMap.get(key) + appendFromMap.get(key));
            }
        }

        return srcMap;
    }

    private static long castNull(Long val) {
        long retVal = 0L;
        if (val != null) {
            retVal = val;
        }

        return retVal;
    }

    public static List<Bitmap> generateTotalByIssuerBitmapArrayTOPS(Acquirer acquirer, List<ETransStatus> filter, List<Issuer> listIssuers, int fontSize, List<Bitmap> bitmaps, IImgProcessing imgProcessing) {
        final int MAX_PAGE = 30;
        int transNo = 0, j, tempSize;
        int tranSize = listIssuers != null ? listIssuers.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_PAGE);
        float halfWidthSize = 0.6f;
        float fullSizeOneLine = 1.2f;
        float fullSizeDoubleLine = 0.8f;


        long[] tempObj = new long[2];
        long[] tempOff = new long[2];
        long[] tempObjSmallAmt = new long[2];
        long tempNum, tempAmt;
        long refundNum, refundAmt;
        long topupNum = 0, topupAmt = 0;
        long topupRefundNum = 0, topupRefundAmt = 0;
        long totalNum, totalAmt;
        long saleHostNum = 0, saleHostAmt = 0;
        long saleVoidHostNum = 0, saleVoidHostAmt = 0;
        long refundHostNum = 0, refundHostAmt = 0;
        long tempSmallAmtNum, tempSmallAmt;
        long saleVoidRefundHostNum = 0, saleVoidRefundHostAmt = 0;
        long saleSmallAmtHostNum = 0, saleSmallAmtHostAmt = 0;
        long saleVoidSmallAmtHostNum = 0, saleVoidSmallAmtHostAmt = 0;

        IPage page;
        for (int i = 1; i <= totalPage; i++) {
            page = Device.generatePage(true);
            tempSize = (tempSize = i * MAX_PAGE) > tranSize ? tranSize : tempSize;
            for (j = transNo; j < tempSize; j++) {
                Issuer issuer = listIssuers.get(j);
                tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
                if (tempObj[0] != 0 || tempObj[1] != 0) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setScaleX(fullSizeOneLine));

                    String issuerName = issuer.getIssuerName();
                    if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                        issuerName = issuerName.replaceFirst("QRC_", "");
                    } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                        issuerName = Utils.getString(R.string.receipt_amex_instalment_issuer);
                    }

                    // issuer
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(issuerName)
                                    .setFontSize(fontSize));
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_double_line))
                            .setScaleX(fullSizeDoubleLine)
                            .setGravity(Gravity.CENTER));
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setScaleX(fullSizeOneLine));
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(issuerName + "-SUMMARY")
                                    .setFontSize(fontSize));

                    long[] tempObj1 = new long[2];
                    if (acquirer.getName().equals(Constants.ACQ_QR_PROMPT)) {//Modified by Cz to support PromptPay.
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, false);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, true);
                        long[] obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true);
                        tempObj[0] = tempObj[0] + tempOffline[0] + obj1[0] + tempObj1[0];
                        tempObj[1] = tempObj[1] + tempOffline[1] + obj1[1] + tempObj1[1];
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, false, true);
                        tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, filter, issuer, true, true);
                        obj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, true, false, true);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, filter, false, true, true);
                        tempObjSmallAmt[0] = tempObjSmallAmt[0] + tempOffline[0] + obj1[0] + tempObj1[0];
                        tempObjSmallAmt[1] = tempObjSmallAmt[1] + tempOffline[1] + obj1[1] + tempObj1[1];
                    } else if (acquirer.getName().equals(Constants.ACQ_QRC)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false);
                        long[] tempInq = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.STATUS_INQUIRY_ALL_IN_ONE, filter, true, false);
                        tempObj[0] = tempObj[0] + tempInq[0];
                        tempObj[1] = tempObj[1] + tempInq[1];
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_ALL_IN_ONE, filter, true, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_KPLUS)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, issuer, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, issuer, false, true);
                    }else if (acquirer.getName().equals(Constants.ACQ_MY_PROMPT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_MYPROMPT_SALE, filter, issuer, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_MYPROMPT_SALE, filter, issuer, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_ALIPAY_SCAN, filter, false, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_WECHAT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_WECHAT_B_SCAN_C)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_WECHAT_SCAN, filter, false, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_QR_CREDIT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, filter, issuer, false, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_CREDIT, filter, issuer, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_SMRTPAY) || acquirer.getName().equals(Constants.ACQ_SMRTPAY_BDMS)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_AMEX_EPP)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, issuer, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.AMEX_INSTALMENT, filter, issuer, false, true);
                    }  else if (acquirer.getName().equals(Constants.ACQ_BAY_INSTALLMENT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, filter, issuer, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BAY_INSTALMENT, filter, issuer, false, true);
                    } else if (acquirer.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false);
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false, true);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
                        long[] tempSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, issuer, false);
                        tempObj[0] = tempObj[0] + tempOffline[0] + tempSaleComp[0];
                        tempObj[1] = tempObj[1] + tempOffline[1] + tempSaleComp[1];
                        tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false, true);
                        tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false, true);
                        tempSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_COMPLETION, filter, issuer, false, true);
                        tempObjSmallAmt[0] = tempObjSmallAmt[0] + tempOffline[0] + tempSaleComp[0];
                        tempObjSmallAmt[1] = tempObjSmallAmt[1] + tempOffline[1] + tempSaleComp[1];
                    }
                    long saleNum = tempObj[0];
                    long saleAmt = tempObj[1];
                    totalNum = tempObj[0];
                    totalAmt = tempObj[1];
                    saleHostNum += saleNum;
                    saleHostAmt += saleAmt;
                    tempSmallAmtNum = tempObjSmallAmt[0];
                    tempSmallAmt = tempObjSmallAmt[1];
                    saleSmallAmtHostNum += tempSmallAmtNum;
                    saleSmallAmtHostAmt += tempSmallAmt;

                    // sale
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(saleNum, 3))
                                    .setFontSize(fontSize)
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(saleAmt))
                                    .setFontSize(fontSize)
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.END));


                    //sale void total
                    tempObj1[0] = 0;
                    tempObj1[1] = 0;
                    if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED);
                        tempObj[0] = tempObj[0] + tempObj1[0];
                        tempObj[1] = tempObj[1] + tempObj1[1];
                    } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED);
                        tempObj[0] = tempObj[0] + tempObj1[0];
                        tempObj[1] = tempObj[1] + tempObj1[1];
                    } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY, VOIDED);
                    } else if (Constants.ACQ_MY_PROMPT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_MYPROMPT_SALE, VOIDED);
                    } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
                    } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_ALIPAY_SCAN, VOIDED);
                    } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
                    } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_WECHAT_SCAN, VOIDED);
                    } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_CREDIT, VOIDED);
                    } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED);
                    } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.AMEX_INSTALMENT, VOIDED);
                    } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.BAY_INSTALMENT, VOIDED);
                    } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
                        long[] tempSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE_COMPLETION, VOIDED);
                        tempObj[0] = tempObj[0] + tempOffline[0] + tempSaleComp[0];
                        tempObj[1] = tempObj[1] + tempOffline[1] + tempSaleComp[1];
                    }
                    long voidNum = tempObj[0];
                    long voidAmt = tempObj[1];
                    saleVoidHostNum += voidNum;
                    saleVoidHostAmt += voidAmt;


                    //sale void small amt
                    tempObj1[0] = 0;
                    tempObj1[1] = 0;
                    if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_SALE_WALLET, VOIDED, true);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE_WALLET, VOIDED, true);
                    } else if (Constants.ACQ_QR_PROMPT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_SALE_INQUIRY, VOIDED, true);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BPS_QR_INQUIRY_ID, VOIDED, true);
                    } else if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY, VOIDED, true);
                    } else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_ALIPAY, VOIDED, true);
                    } else if (Constants.ACQ_ALIPAY_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_ALIPAY_SCAN, VOIDED, true);
                    } else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_WECHAT, VOIDED, true);
                    } else if (Constants.ACQ_WECHAT_B_SCAN_C.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_WECHAT_SCAN, VOIDED, true);
                    } else if (Constants.ACQ_QR_CREDIT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.QR_INQUIRY_CREDIT, VOIDED, true);
                    } else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED, true);
                    } else if (Constants.ACQ_AMEX_EPP.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.AMEX_INSTALMENT, VOIDED, true);
                    } else if (Constants.ACQ_BAY_INSTALLMENT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.BAY_INSTALMENT, VOIDED, true);
                    } else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED, true);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED, true);
                        long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED, true);
                        long[] tempSaleComp = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE_COMPLETION, VOIDED, true);
                        tempObj[0] = tempObj[0] + tempOffline[0] + tempSaleComp[0];
                        tempObj[1] = tempObj[1] + tempOffline[1] + tempSaleComp[1];
                    }
                    long voidSmallAmtNum = tempObj[0];
                    long voidSmallAmt = tempObj[1];
                    saleVoidSmallAmtHostNum += voidSmallAmtNum;
                    saleVoidSmallAmtHostAmt += voidSmallAmt;

                    // void
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.receipt_void_sale).toUpperCase())
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(voidNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - voidAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));


                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, filter, issuer, false);

                    totalNum += tempObj[0];
                    totalAmt = saleAmt - tempObj[1];
                    refundNum = tempObj[0];
                    refundAmt = tempObj[1];
                    refundHostNum += refundNum;
                    refundHostAmt += refundAmt;

                    // refund
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(refundNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - refundAmt))
                                    .setFontSize(fontSize)
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.END));


                    // void refund
                    tempObj1[0] = 0;
                    tempObj1[1] = 0;
                    if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
                        tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
                    } else {
                        tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.REFUND, VOIDED);
                        tempObj[0] = tempObj[0];
                        tempObj[1] = tempObj[1];
                    }
                    long voidRefundNum = tempObj[0];
                    long voidRefundAmt = tempObj[1];
                    saleVoidRefundHostNum += voidRefundNum;
                    saleVoidRefundHostAmt += voidRefundAmt;


                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(voidRefundNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(voidRefundAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    // top up
                    tempObj[0] = 0;
                    tempObj[1] = 0;//default value for another acquirers
                    tempObj1[0] = 0;
                    tempObj1[1] = 0;

                    topupNum = tempObj[0];
                    topupAmt = tempObj[1];
                    if (topupNum != 0.00) {
                        String tmpTopUp = CurrencyConverter.convert(topupAmt);
                        if (topupAmt != 0.00) {
                            tmpTopUp = "- " + tmpTopUp;
                        }
                        page.addLine()
                                .addUnit(page.createUnit()
                                        .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                                        .setWeight(2.0f)
                                        .setScaleX(halfWidthSize)
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(Component.getPaddedNumber(topupNum, 3))
                                        .setWeight(1.0f)
                                        .setScaleX(halfWidthSize)
                                        .setGravity(Gravity.CENTER)
                                        .setFontSize(fontSize))
                                .addUnit(page.createUnit()
                                        .setText(tmpTopUp)
                                        .setWeight(2.0f)
                                        .setScaleX(halfWidthSize)
                                        .setFontSize(fontSize)
                                        .setGravity(Gravity.END));

                        totalNum += topupNum;
                        totalAmt += topupAmt;
                    }
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setScaleX(fullSizeOneLine));

                    // total
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TOTAL")
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(totalAmt))
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));


                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_double_line))
                            .setScaleX(fullSizeDoubleLine)
                            .setGravity(Gravity.CENTER));

                    // sale small amount
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " SMALL TICKET")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(tempSmallAmtNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(tempSmallAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    // sale normal
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " NORMAL")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber((saleNum - tempSmallAmtNum), 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(saleAmt - tempSmallAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    //Refund
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(refundNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - refundAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    // void sale small amount
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_void).toUpperCase() + " SMALL TICKET")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(voidSmallAmtNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - voidSmallAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    // void sale
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_void).toUpperCase() + " NORMAL")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber((voidNum - voidSmallAmtNum), 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(0 - (voidAmt - voidSmallAmt)))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    // void refund
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(Component.getPaddedNumber(voidRefundNum, 3))
                                    .setWeight(1.0f)
                                    .setScaleX(halfWidthSize)
                                    .setGravity(Gravity.CENTER)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(voidRefundAmt))
                                    .setWeight(2.0f)
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_one_line))
                            .setScaleX(fullSizeOneLine));

                    // total
                    page.addLine()
                            .addUnit(page.createUnit()
                                    .setText("TOTAL")
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize))
                            .addUnit(page.createUnit()
                                    .setText(CurrencyConverter.convert(totalAmt))
                                    .setScaleX(halfWidthSize)
                                    .setFontSize(fontSize)
                                    .setGravity(Gravity.END));

                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_double_line))
                            .setScaleX(fullSizeDoubleLine)
                            .setGravity(Gravity.CENTER));


                    page.addLine().addUnit(page.createUnit()
                            .setText("\n"));
                }
            }
            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));

            PageToSlipFormat.getInstance().isSettleMode = true;
            PageToSlipFormat.getInstance().Append(page);
        }

        page = Device.generatePage(true);
        /*=================== HOST TOTAL ===================*/

        // sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleHostNum, 3))
                        .setFontSize(fontSize)
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleHostAmt))
                        .setFontSize(fontSize)
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        // refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(refundHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - refundHostAmt))
                        .setFontSize(fontSize)
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.END));

        // void sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.receipt_void_sale).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleVoidHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - saleVoidHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleVoidRefundHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleVoidRefundHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));


        tempObj[0] = 0;
        tempObj[1] = 0;//default value for another acquirers

        topupNum = tempObj[0];
        topupAmt = tempObj[1];

        // top up
        if (topupNum != 0.00) {
            String tmpTopUp = CurrencyConverter.convert(topupAmt);
            if (topupAmt != 0.00) {
                tmpTopUp = "- " + tmpTopUp;
            }
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.trans_topup).toUpperCase())
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(Component.getPaddedNumber(topupNum, 3))
                            .setWeight(1.0f)
                            .setScaleX(halfWidthSize)
                            .setGravity(Gravity.CENTER)
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(tmpTopUp)
                            .setWeight(2.0f)
                            .setScaleX(halfWidthSize)
                            .setFontSize(fontSize)
                            .setGravity(Gravity.END));
        }

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine));

        long hostNum = saleHostNum + refundHostNum + topupNum + topupRefundNum;
        long hostAmt = saleHostAmt - refundHostAmt + topupAmt - topupRefundAmt;

        EcrData.instance.nBatchTotalSalesCount = saleHostNum;
        EcrData.instance.nBatchTotalSalesAmount = saleHostAmt;

        EcrData.instance.nBatchTotalRefundCount = refundHostNum;
        EcrData.instance.nBatchTotalRefundAmount = refundHostAmt;

        // Grand total (Host Total)
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("GRAND TOTAL")
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(hostAmt))
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));


        // sale small amount
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " SMALL TICKET")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleSmallAmtHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleSmallAmtHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // sale normal
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_sale).toUpperCase() + " NORMAL")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber((saleHostNum - saleSmallAmtHostNum), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleHostAmt - saleSmallAmtHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        //Refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_refund).toUpperCase())
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(refundHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - refundHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void sale small amount
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " SMALL TICKET")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleVoidSmallAmtHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - saleVoidSmallAmtHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void sale
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " NORMAL")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber((saleVoidHostNum - saleVoidSmallAmtHostNum), 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(0 - (saleVoidHostAmt - saleVoidSmallAmtHostAmt)))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        // void refund
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(Utils.getString(R.string.trans_void).toUpperCase() + " REFUND")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(saleVoidRefundHostNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(saleVoidRefundHostAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_one_line))
                .setScaleX(fullSizeOneLine));

        // Grand total (Host Total)
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("GRAND TOTAL")
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(hostAmt))
                        .setScaleX(halfWidthSize)
                        .setFontSize(fontSize)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setScaleX(fullSizeDoubleLine)
                .setGravity(Gravity.CENTER));

//        page.addLine().addUnit(page.createUnit()
//                .setText("\n"));

        PageToSlipFormat.getInstance().isSettleMode = true;
        PageToSlipFormat.getInstance().Append(page);
        bitmaps.add(imgProcessing.pageToBitmap(page, 384));

        return bitmaps;
    }

    public static void generateTotalDetailMainInfoAuditTOPS(IPage page, Acquirer acquirer, String title, int fontSize, boolean isAllAcq) {
        float fullSizeOneLine = 1.2f;
        // Change acquirer name Prompt Pay on slip
        String acqName = acquirer.getName();

        //if (isAllAcq) {
            Bitmap logo;
            logo = Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME, acquirer.getNii() + "_" + acquirer.getName());
            page.addLine()
                    .addUnit(page.createUnit()
                            .setBitmap(logo)
                            .setGravity(Gravity.CENTER));
            page.addLine().addUnit(page.createUnit().setText(" "));


            //HOST
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("HOST")
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(": " + acqName)
                            .setWeight(3.0f)
                            .setFontSize(fontSize));

            // TID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("TID")
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(": " + acquirer.getTerminalId())
                            .setWeight(3.0f)
                            .setGravity(Gravity.LEFT)
                    );
            //MID
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("MID")
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(": " + acquirer.getMerchantId())
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));
            //BATCH No.
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("BATCH")
                            .setFontSize(fontSize))
                    .addUnit(page.createUnit()
                            .setText(": " + Component.getPaddedNumber(acquirer.getCurrBatchNo(), 6))
                            .setGravity(Gravity.LEFT)
                            .setWeight(3.0f));

            page.addLine().addUnit(page.createUnit()
                    .setText(" ")
                    .setGravity(Gravity.CENTER));

        //}

        // title
        page.addLine().addUnit(page.createUnit().setText(title).setFontSize(fontSize));

        return;
    }

    public static String unpackField63Dcc(TransData transData) {
        byte[] field63 = transData.getField63Byte();
        if (field63 != null && field63.length > 0) {
            byte[] temp = Arrays.copyOfRange(field63, 4, field63.length);
            String field63Str = Tools.bytes2String(temp);
            return field63Str != null ? field63Str.trim() : null;
        }
        return null;
    }

    public static void genAppVersiononSlip(IPage page) {
//        page.addLine()
//                .adjustTopSpace(5)
//                .addUnit(page.createUnit()
//                        .setText(Utils.getTerminalAndAppVersion())
//                        .setFontSize(18)
//                        .setGravity(Gravity.CENTER));

        String[] AppVersion;
        if (Utils.getTerminalAndAppVersion() != null) {
            try {
                AppVersion = EReceiptUtils.stringSplitter(Utils.getTerminalAndAppVersion(), EReceiptUtils.MAX42_CHAR_PER_LINE);
                if (AppVersion.length == 1 && AppVersion[0] != null) {
                    page.addLine().addUnit(page.createUnit()
                            .setText(AppVersion[0])
                            .setFontSize(18)
                            .setGravity(Gravity.CENTER));
                } else {
                    page.addLine().addUnit(page.createUnit()
                            .setText(AppVersion[0])
                            .setFontSize(18)
                            .setGravity(Gravity.CENTER));
                    page.addLine().addUnit(page.createUnit()
                            .setText(AppVersion[1])
                            .setFontSize(18)
                            .setGravity(Gravity.CENTER));
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage() + " : Error during reformat [EDC.ApplicationVersion]");
            }
        }
    }

    public static boolean isAllowSignatureUpload(TransData transData) {
        //todo currently, only support for normal sale and void transaction.
        Acquirer acqDownloadKey = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE);
        Acquirer acqUpload = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);

        try {
            Log.d(EReceiptUtils.TAG, "\t\t\t>> ERCM-MODE ENABLED\t\t= " + FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE));
            Log.d(EReceiptUtils.TAG, "\t\t\t>> E-SIGNATURE ENABLE\t\t= " + FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE));
            Log.d(EReceiptUtils.TAG, "\t\t\t>> ERCM-CURR-ACQUIRER NAME\t= " + transData.getAcquirer().getName());
            Log.d(EReceiptUtils.TAG, "\t\t\t>> ERCM-CURR-ACQUIRER UPLOAD STATUS\t= " + transData.getAcquirer().getEnableUploadERM());
            Log.d(EReceiptUtils.TAG, "\t\t\t>> ERCM-CURR-ACQUIRER TRANS TYPE\t= " + transData.getTransType().toString());
            Log.d(EReceiptUtils.TAG, "-----------------------------------------------------------------------");
        } catch (Exception ex){
            ex.printStackTrace();
        }

        boolean isCurrentHostEnableUploadERM = transData.getAcquirer().getEnableUploadERM();
        return FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE)
                && FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE)
                && (isCurrentHostEnableUploadERM)
                && (transData.getTransType().getRequireErmUpload())
                && (acqDownloadKey != null && acqDownloadKey.isEnable())
                && (acqUpload != null && acqUpload.isEnable());
//                //&& (isInitiatedHost)
//                && (transData.getTransType() == ETransType.SALE
//                || (transData.getTransType() == ETransType.REFUND)
//                || (transData.getTransType() == ETransType.AMEX_INSTALMENT)
//                || (transData.getTransType() == ETransType.VOID)
//                || transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT
//                || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER
//                || transData.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT
//                || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER_CREDIT
//                || transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT_CREDIT
//                || transData.getTransType() == ETransType.KBANK_REDEEM_VOID
//                || transData.getTransType() == ETransType.KBANK_SMART_PAY
//                || transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID
//                || transData.getTransType() == ETransType.QR_INQUIRY_WECHAT
//                || transData.getTransType() == ETransType.QR_VOID_WECHAT
//                || transData.getTransType() == ETransType.QR_INQUIRY_ALIPAY
//                || transData.getTransType() == ETransType.QR_VOID_ALIPAY
//                || transData.getTransType() == ETransType.QR_INQUIRY
//                || transData.getTransType() == ETransType.QR_VOID_KPLUS
//                || transData.getTransType() == ETransType.QR_INQUIRY_CREDIT
//                || transData.getTransType() == ETransType.QR_VERIFY_PAY_SLIP
//                || transData.getTransType() == ETransType.QR_VOID_CREDIT
//                || transData.getTransType() == ETransType.KCHECKID_DUMMY
//                || transData.getTransType() == ETransType.ADJUST
//                || transData.getTransType() == ETransType.OFFLINE_TRANS_SEND
//                || transData.getTransType() == ETransType.DOLFIN_INSTALMENT
//                || transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID
//        )
    }

    private static void genAuthCodeDynamicOffline(TransData transData) {
        String s66HHdd = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.S66_HH_DD_DISPLAY);
        transData.setAuthCode(s66HHdd);
        transData.setOrigAuthCode(transData.getAuthCode());
        transData.setResponseCode(FinancialApplication.getRspCode().parse("00"));
    }

    public static boolean isAllowDynamicOffline(TransData transData) {
        if (transData.getTransType() == ETransType.SALE && transData.getIssuer() != null
                && DynamicOffline.getInstance().isDynamicOfflineActive(transData.getIssuer().getIssuerBrand(), Utils.parseLongSafe(transData.getAmount(), 0))) {
            genAuthCodeDynamicOffline(transData);
            return true;
        }
        return false;
    }

    public static void generateTransDetailAmexInstalment(TransData transData, IPage page, int fontsize) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        ETransType origTransType = transData.getOrigTransType();
        boolean isRefund = ETransType.REFUND == transType || ETransType.REFUND == transData.getOrigTransType();
        String type = transType.getTransName().toUpperCase();
        if (ETransType.VOID == transType) {
            type = (!isRefund) ? "SALE(VOID)" : "REFUND(VOID)";
        }

        // AET-18
        // transaction NO/transaction type/auth
        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);
        String authCode = transData.getAuthCode() == null ? "" : transData.getAuthCode();

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setFontSize(fontsize).setGravity(Gravity.LEFT))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(type).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setGravity(Gravity.END).setFontSize(fontsize));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = Utils.getString(R.string.receipt_amex_instalment_issuer);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setGravity(Gravity.END).setFontSize(fontsize));

        // amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setGravity(Gravity.END).setFontSize(fontsize));
        page.addLine().addUnit(page.createUnit().setText(" "));

        return;
    }


    public static void generateTransDetailInstalmentBay(TransData transData, IPage page, int fontsize) {
        String temp;
        String temp2;
        ETransType transType = transData.getTransType();
        boolean isVoid = transType == ETransType.VOID || transData.getTransState() == TransData.ETransStatus.VOIDED;
        String transTypeValue = isVoid ? transData.getOrigTransType().getTransName() + "(VOID)" : transType.getTransName();

        // AET-18
        // transaction NO/transaction type/auth
        String stanNo = Component.getPaddedNumber(transData.getStanNo(), 6);
        String traceNo = Component.getPaddedNumber(transData.getTraceNo(), 6);
        String batchNo = Component.getPaddedNumber(transData.getBatchNo(), 6);
        String authCode = transData.getAuthCode() == null ? "" : transData.getAuthCode();

        page.addLine()
                .addUnit(page.createUnit().setText(transData.getAcquirer().getName().toUpperCase()).setFontSize(fontsize).setGravity(Gravity.LEFT))
                .addUnit(page.createUnit().setText(transData.getAcquirer().getNii()).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(transTypeValue).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(authCode).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        page.addLine()
                .addUnit(page.createUnit().setText(traceNo + "               " + stanNo).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(batchNo).setGravity(Gravity.END).setFontSize(fontsize).setWeight(0.3f));

        // date/time
        temp = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.DATE_PATTERN_DISPLAY);

        temp2 = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY4);
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setFontSize(fontsize).setGravity(Gravity.END));

        //card NO/card type
        temp = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());
        temp2 = transData.getIssuer().getName();
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setWeight(6).setFontSize(fontsize))
                .addUnit(page.createUnit().setText(temp2).setWeight(5).setFontSize(fontsize).setGravity(Gravity.END));

        // amount
        if (transType.isSymbolNegative()) {
            temp = CurrencyConverter.convert(0 - Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        } else {
            temp = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
        }
        page.addLine()
                .addUnit(page.createUnit().setText(temp).setFontSize(fontsize).setGravity(Gravity.END));
        page.addLine().addUnit(page.createUnit().setFontSize(fontsize).setText(" "));

        return;
    }

    public static int generateKeyId() {
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();
        List<Integer> keyIdList = new ArrayList<>();
        for (Acquirer acq : acquirers) {
            if (acq.getKeyId() > 0) {
                keyIdList.add(acq.getKeyId());
            }
        }

        int generatedKeyId = 0;

        for (int i = 1; i < 15; i++) {
            if (!keyIdList.isEmpty() && keyIdList.contains(i)) {
                continue;
            } else {
                generatedKeyId = i;
                break;
            }
        }

        return generatedKeyId;
    }

}
