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
package com.pax.pay.trans.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.eemv.enums.ETransResult;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.utils.ResponseCode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;

@DatabaseTable(tableName = "trans_data")
public class TransData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "id";
    public static final String STANNO_FIELD_NAME = "stan_no";
    public static final String TRACENO_FIELD_NAME = "trace_no";
    public static final String BATCHNO_FIELD_NAME = "batch_no";
    public static final String TYPE_FIELD_NAME = "type";
    public static final String STATE_FIELD_NAME = "state";
    public static final String OFFLINE_STATE_FIELD_NAME = "offline_state";
    public static final String AMOUNT_FIELD_NAME = "amount";
    public static final String REVERSAL_FIELD_NAME = "REVERSAL";
    public static final String SIGN_PATH = "sign_path";
    public static final String REF_NO = "ref_no";
    public static final String QR_SALE_STATUS = "qr_sale_status";
    public static final String QR_SALE_STATE = "qr_sale_state";
    public static final String APPR_CODE = "appr_code";
    public static final String WALLET_RETRY_FIELD_NAME = "wallet_retry";
    public static final String WALLET_NAME = "wallet_name";
    public static final String ADVICE_FIELD_NAME = "ADVICE";
    public static final String REFERRAL_FIELD_NAME = "REFERRAL";
    public static final String REDEEM_QTY_KBANK_FIELD_NAME = "product_qty";
    public static final String REDEEM_POINTS_KBANK_FIELD_NAME = "redeemed_point";
    public static final String REDEEM_AMOUNT_KBANK_FIELD_NAME = "redeemed_amount";
    public static final String REDEEM_CREDIT_KBANK_FIELD_NAME = "redeemed_credit";
    public static final String REDEEM_TOTAL_KBANK_FIELD_NAME = "redeemed_total";
    public static final String DCC_AMOUNT_KBANK_FIELD_NAME = "dcc_amount";
    public static final String DCC_CURRENCY_CODE_KBANK_FIELD_NAME = "dcc_currency_code";
    public static final String DCC_CURRENCY_NAME_KBANK_FIELD_NAME = "dcc_currency_name";
    public static final String DCC_REQUIRED_KBANK_FIELD_NAME = "dccRequired";
    public static final String TXN_SMALL_AMT = "isTxnSmallAmt";
    public static final String ESLIP_FORMAT_FIELD_NAME = "e_slip_format";
    public static final String ESLIP_RETRY_FIELD_NAME = "e_slip_retry";
    public static final String ESLIP_UPLOAD_FIELD_NAME = "e_slip_status";
    public static final String ERECEIPT_NEED_MANUAL_FIELD_NAME = "e_receipt_manual_print";
    public static final String ERECEIPT_PRINT_COUNT = "NumberOfErmPrintingCount";
    public static final String ECR_REF_SALE_ID = "ecrReferenceSaleID";
    public static final String CNO_FILED_NAME = "pan";
    public static final String ADJUST_AMOUNT_FIELD_NAME = "adjusted_amount";
    public static final String ADJUST_DCC_AMOUNT_FIELD_NAME = "adjusted_dcc_amount";
    public static final String MERCHANT_NAME_FILED_NAME = "merchant_name";
    public void setReversalStatus() {
    }

    public void setCurrency(String currency) {
    }

    public void setOrigTranState(ETransStatus transState) {
    }

    /**
     * 交易状态
     *
     * @author Steven.W
     */
    public enum ETransStatus {
        /**
         * 正常
         */
        NORMAL,
        /**
         * 已撤销
         */
        VOIDED,
        /**
         * 已调整
         */
        ADJUSTED,


        SALE_COMPLETED
    }

    /* 脱机上送失败原因 */

    public enum OfflineStatus {
        /**
         * offline not sent
         */
        OFFLINE_NOT_SENT(0),
        /**
         * offline sent
         */
        OFFLINE_SENT(1),
        /**
         * 脱机上送失败
         */
        OFFLINE_ERR_SEND(2),
        /**
         * 脱机上送平台拒绝(返回码非00)
         */
        OFFLINE_ERR_RESP(3),
        /**
         * 脱机上送未知失败原因
         */
        OFFLINE_ERR_UNKNOWN(0xff),
        /**
         * offline sending
         */
        OFFLINE_SENDING(4),
        /**
         * offline voided
         */
        OFFLINE_VOIDED(5),
        /**
         * offline adjusted
         */
        OFFLINE_ADJUSTED(6);

        private final int value;

        OfflineStatus(int value) {
            this.value = value;
        }
    }

    /**
     * 电子签名上送状态
     */
    public enum SignSendStatus {
        /**
         * 未上送
         */
        SEND_SIG_NO,
        /**
         * 上送成功
         */
        SEND_SIG_SUCC,
        /**
         * 上送失败
         */
        SEND_SIG_ERR,
    }

    public enum EnterMode {
        /**
         * 手工输入
         */
        MANUAL("M"),
        /**
         * 刷卡
         */
        SWIPE("S"),
        /**
         * 插卡
         */
        INSERT("I"),
        /**
         * IC卡回退
         */
        FALLBACK("F"),
        /**
         * 非接支付
         */
        CLSS("C"),
        /**
         * 扫码支付
         */
        QR("Q"),

        SP200("P");

        private String str;

        EnterMode(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum ReversalStatus {
        /**
         *
         */
        NORMAL,
        /**
         *
         */
        PENDING,
        /**
         *
         */
        REVERSAL,
        /**
         *
         */
        NOREVERSE,
    }

    public enum QrSaleStatus {
        /**
         * Only gen QR code, not call QR Sale Inquiry
         */
        GENERATE_QR("GEN_QR"),
        /**
         * QR Sale Inquiry successfully.
         */
        SUCCESS("00"),
        /**
         * QR Inquiry not successfully.
         */
        NOT_SUCCESS("QR_RESP_ER"),
        ;

        private String str;

        QrSaleStatus(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum QrSaleState {
        /**
         * Online Trans.
         */
        QR_SEND_ONLINE,
        /**
         * Offline Trans.
         */
        QR_SEND_OFFLINE,
    }

    public enum WalletRetryStatus {
        /**
         *
         */
        NORMAL,
        /**
         *
         */
        PENDING,
        /**
         *
         */
        RETRY_CHECK,
    }

    public enum AdviceStatus {
        /**
         *
         */
        NORMAL,
        /**
         *
         */
        PENDING,
        /**
         *
         */
        ADVICE,
    }

    public enum ReferralStatus {
        NORMAL,
        PENDING,
        REFERRED,
        REFERRED_SUCC,
        REFERRED_ERR_SEND,
    }

    public enum SettleUploadSource {
        NORMAL,
        FROM_FILE
    }

    public enum UploadStatus {
        NORMAL("Success"),
        PENDING("Pending"),
        NOT_UPLOAD_REQUIRED("Not Required"),
        UPLOAD_FAILED_MANUAL_PRINT("Fail"),
        ;

        private String str;

        UploadStatus(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum EPreAuthStatus {
        NONE,                   // use for detect non-preAuth-Trans
        NORMAL,
        VOIDED,                 // only host V2
        SALE_COMPLETE,
        SALE_COMPLETE_VOIDED
    }

    public static final String DUP_REASON_NO_RECV = "98";
    public static final String DUP_REASON_MACWRONG = "A0";
    public static final String DUP_REASON_OTHERS = "06";

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    @DatabaseField
    private String qrCode;

    // ============= 需要存储 ==========================
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    protected int id;
    @DatabaseField(canBeNull = false, columnName = STANNO_FIELD_NAME)
    protected long stanNo; // pos流水号
    @DatabaseField
    protected long origTransNo; // 原pos流水号
    @DatabaseField(canBeNull = false, columnName = TYPE_FIELD_NAME)
    protected ETransType transType; // 交易类型
    @DatabaseField
    protected ETransType origTransType; // 原交易类型
    @DatabaseField(canBeNull = false, columnName = STATE_FIELD_NAME)
    protected ETransStatus transState = ETransStatus.NORMAL; // 交易状态
    @DatabaseField(canBeNull = false)
    protected boolean isUpload = false; // 是否已批上送
    @DatabaseField(columnName = OFFLINE_STATE_FIELD_NAME)
    protected OfflineStatus offlineSendState = null; // 脱机上送失败类型 ：上送失败/平台拒绝
    @DatabaseField(canBeNull = false)
    protected int sendTimes; // 已批上送次数
    @DatabaseField
    protected String procCode; // 处理码，39域
    @DatabaseField(columnName = AMOUNT_FIELD_NAME)
    protected String amount; // 交易金额
    @DatabaseField
    protected String tipAmount; // 小费金额
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    protected Locale currency; // currency
    @DatabaseField(canBeNull = false, columnName = BATCHNO_FIELD_NAME)
    protected long batchNo; // 批次号
    @DatabaseField
    protected long origBatchNo; // 原批次号
    @DatabaseField(columnName = CNO_FILED_NAME)
    protected String pan; // 主账号-刷两张卡时为转出卡卡号
    @DatabaseField
    protected String dateTime; // 交易日期时间
    @DatabaseField
    protected String origDateTime; // 原交易日期时间
    @DatabaseField
    protected String settleDateTime; // 清算日期时间
    @DatabaseField
    protected String expDate; // 卡有效期
    @DatabaseField
    protected EnterMode enterMode; // 输入模式
    @DatabaseField
    protected String nii;     //Network International Identifier
    @DatabaseField(columnName = REF_NO)
    protected String refNo; // 系统参考号
    @DatabaseField
    protected String origRefNo; // 原系统参考号
    @DatabaseField(columnName = APPR_CODE)
    protected String authCode; // 授权码
    @DatabaseField
    protected String origAuthCode; // 原授权码
    @DatabaseField
    protected String issuerCode; // 发卡行标识码
    @DatabaseField
    protected String acqCode; // 收单机构标识码
    @DatabaseField(canBeNull = false)
    protected boolean hasPin; // 是否有输密码
    @DatabaseField
    protected String track1; // 磁道一信息
    @DatabaseField
    protected String track2; // 磁道二数据
    @DatabaseField
    protected String track3; // 磁道三数据
    @DatabaseField
    protected String dupReason; // 冲正原因
    @DatabaseField
    protected String reserved; // 保留域[field63]

    // =================EMV数据=============================
    @DatabaseField(canBeNull = false)
    protected boolean pinFree = false; // 免密
    @DatabaseField(canBeNull = false)
    protected boolean signFree = false; // 免签
    @DatabaseField(canBeNull = false)
    protected boolean isCDCVM = false; // CDCVM标识
    @DatabaseField(canBeNull = false)
    protected boolean isOnlineTrans = false; // 是否为联机交易
    // 电子签名专用
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    protected byte[] signData; // signData

    // 电子签名专用
    @DatabaseField(dataType = DataType.BYTE_ARRAY, columnName = SIGN_PATH)
    private byte[] signPath; // raw sign path

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Issuer.ID_FIELD_NAME)
    protected Issuer issuer;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Acquirer.ID_FIELD_NAME)
    protected Acquirer acquirer;

    // =================EMV数据=============================
    @DatabaseField
    protected ETransResult emvResult; // EMV交易的执行状态
    @DatabaseField
    protected String cardSerialNo; // 23 域，卡片序列号
    @DatabaseField
    protected String sendIccData; // IC卡信息,55域
    @DatabaseField
    protected String dupIccData; // IC卡冲正信息,55域
    @DatabaseField
    protected String tc; // IC卡交易证书(TC值)tag9f26,(BIN)
    @DatabaseField
    protected String arqc; // 授权请求密文(ARQC)
    @DatabaseField
    protected String arpc; // 授权响应密文(ARPC)
    @DatabaseField
    protected String tvr; // 终端验证结果(TVR)值tag95
    @DatabaseField
    protected String aid; // 应用标识符AID
    @DatabaseField
    protected String emvAppLabel; // 应用标签
    @DatabaseField
    protected String emvAppName; // 应用首选名称
    @DatabaseField
    protected String tsi; // 交易状态信息(TSI)tag9B
    @DatabaseField
    protected String atc; // 应用交易计数器(ATC)值tag9f36

    @DatabaseField(canBeNull = false, columnName = REVERSAL_FIELD_NAME)
    protected ReversalStatus reversalStatus = ReversalStatus.NORMAL;

    @DatabaseField
    private String phoneNum;

    @DatabaseField
    private String email;

    @DatabaseField
    private String qrRef2;

    @DatabaseField(columnName = QR_SALE_STATUS)
    private String qrSaleStatus;
    @DatabaseField(columnName = QR_SALE_STATE)
    private QrSaleState qrSaleState;

    @DatabaseField
    protected String qrBuyerCode;

    @DatabaseField(columnName = WALLET_RETRY_FIELD_NAME)
    private WalletRetryStatus walletRetryStatus;

    @DatabaseField(columnName = WALLET_NAME)
    protected String walletName;

    @DatabaseField
    protected String walletSlipInfo;

    @DatabaseField(canBeNull = false, columnName = ADVICE_FIELD_NAME)
    protected AdviceStatus adviceStatus = AdviceStatus.NORMAL;

    @DatabaseField
    protected String origAmount;

    @DatabaseField
    protected String transYear;

    @DatabaseField
    protected String walletPartnerID;
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    protected String[] reqMsg;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    protected String[] respMsg;

    @DatabaseField(canBeNull = false, columnName = TRACENO_FIELD_NAME)
    protected long traceNo;

    @DatabaseField
    protected long voidStanNo;

    @DatabaseField(canBeNull = false, columnName = REFERRAL_FIELD_NAME)
    protected ReferralStatus referralStatus = ReferralStatus.NORMAL;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    protected byte[] field61RecByte;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    protected byte[] field63Byte;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    protected String[] infoSlipLinePay;

    @DatabaseField
    protected int templateId;

    @DatabaseField
    protected String UDSN;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    protected byte[] field61Byte;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    protected ResponseCode responseCode;

    @DatabaseField(canBeNull = false, columnName = TXN_SMALL_AMT)
    protected boolean isTxnSmallAmt;

    @DatabaseField
    protected int numSlipSmallAmt;

    @DatabaseField(canBeNull = false)
    protected boolean pinVerifyMsg = false;

    @DatabaseField
    protected String qrType;

    @DatabaseField
    protected String qrID;

    @DatabaseField
    protected long OrigStanNo;

    @DatabaseField
    protected int clssTypeMode;

    @DatabaseField
    private String branchID;

    @DatabaseField
    protected String QRCurrency;

    @DatabaseField
    protected String TxnID;

    @DatabaseField
    protected String PayTime;

    @DatabaseField
    protected String ExchangeRate;

    @DatabaseField
    protected String AmountCNY;

    @DatabaseField
    protected String TxnAmount;

    @DatabaseField
    protected String BuyerUserID;

    @DatabaseField
    protected String BuyerLoginID;

    @DatabaseField
    protected String MerchantInfo;

    @DatabaseField
    protected String AppCode;

    @DatabaseField
    protected String Promocode;

    @DatabaseField
    protected String TxnNo;

    @DatabaseField
    protected String Fee;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    protected byte[] field63RecByte;

    @DatabaseField
    protected String productCode;

    @DatabaseField(columnName = REDEEM_QTY_KBANK_FIELD_NAME)
    protected int productQty;

    @DatabaseField(columnName = REDEEM_POINTS_KBANK_FIELD_NAME)
    protected int redeemedPoint;

    @DatabaseField(columnName = REDEEM_AMOUNT_KBANK_FIELD_NAME)
    protected String redeemedAmount;

    @DatabaseField
    protected String redeemedDiscountType;

    @DatabaseField(columnName = REDEEM_CREDIT_KBANK_FIELD_NAME)
    protected String redeemedCredit;

    @DatabaseField(columnName = REDEEM_TOTAL_KBANK_FIELD_NAME)
    protected String redeemedTotal;

    @DatabaseField
    protected int instalmentPaymentTerm;

    @DatabaseField
    protected String instalmentIPlanMode;

    @DatabaseField
    protected String instalmentPromotionKey;

    @DatabaseField
    protected String instalmentSerialNo;

    @DatabaseField(canBeNull = false)
    protected boolean instalmentPromoProduct;

    @DatabaseField(canBeNull = false, columnName = DCC_REQUIRED_KBANK_FIELD_NAME)
    protected boolean dccRequired;

    @DatabaseField(columnName = DCC_AMOUNT_KBANK_FIELD_NAME)
    protected String dccAmount;

    @DatabaseField
    protected String dccConversionRate;

    @DatabaseField(columnName = DCC_CURRENCY_CODE_KBANK_FIELD_NAME, dataType = DataType.BYTE_ARRAY)
    protected byte[] dccCurrencyCode;

    @DatabaseField(columnName = DCC_CURRENCY_NAME_KBANK_FIELD_NAME)
    protected String dccCurrencyName;

    @DatabaseField
    protected String dccTipAmount;

    @DatabaseField
    protected String origDccAmount;

    @DatabaseField(canBeNull = false)
    protected boolean isEcrProcess = false;

    @DatabaseField(columnName = ESLIP_FORMAT_FIELD_NAME, dataType = DataType.BYTE_ARRAY)
    protected byte[] eSlipFormat;
    @DatabaseField(columnName = ESLIP_RETRY_FIELD_NAME)
    protected int eReceiptRetry = 0;
    @DatabaseField(columnName = ESLIP_UPLOAD_FIELD_NAME)
    protected UploadStatus eReceiptUploadStatus;
    @DatabaseField(columnName = ERECEIPT_NEED_MANUAL_FIELD_NAME)
    private boolean isEReceiptManualPrint = false;
    @DatabaseField
    private boolean isEReceiptReprint = false;
    @DatabaseField
    protected String eReceiptUploadTimeDateTime;
    @DatabaseField
    private int NumberOfErmPrintingCount = 0;

    @DatabaseField
    protected String PosNo_ReceiptNo;
    @DatabaseField
    protected String CashierName;
    @DatabaseField
    protected String instalmentTerms;
    @DatabaseField
    protected String instalmentMonthDue;
    @DatabaseField
    protected String instalmentInterest;
    @DatabaseField
    protected String serialNum;
    @DatabaseField
    protected String mktCode;
    @DatabaseField
    protected String skuCode;
    @DatabaseField
    protected String redeemPoints;
    @DatabaseField
    protected boolean isBayInstalmentSpecific = false;
    @DatabaseField(columnName = ECR_REF_SALE_ID)
    protected String ecrReferenceSaleID;
    @DatabaseField
    protected boolean enableQrTag31 = false;
    @DatabaseField
    protected String qrSourceOfFund;
    @DatabaseField
    protected String field28;
    @DatabaseField
    protected String walletBankCode = "";
    @DatabaseField
    protected String walletVerifyPaySlipQRCode = "";

    @DatabaseField(columnName = ADJUST_AMOUNT_FIELD_NAME)
    protected String adjustedAmount;

    @DatabaseField(columnName = ADJUST_DCC_AMOUNT_FIELD_NAME)
    protected String adjustedDccAmount;

    @DatabaseField
    protected String channel = "";
	
    @DatabaseField(canBeNull = false, columnName = MERCHANT_NAME_FILED_NAME)
    protected String merchantName = "";

    @DatabaseField
    protected String preAuthTransDate = null;
    @DatabaseField
    protected EPreAuthStatus preAuthStatus = EPreAuthStatus.NONE;

    @DatabaseField
    protected String saleReference1 = null;

    @DatabaseField
    protected String saleReference2 = null;

    private String pin;
    @DatabaseField
    private String qrResult;
    private boolean isLastTrans = false;
    protected String header;
    protected String tpdu;
    protected byte[] field46Byte;
    protected String field48;
    protected String field60;
    protected String field61;
    protected String field62;
    protected String field63;
    protected String recvIccData;
    protected String field3;
    protected boolean isTransInqID = false;
    protected String origField63;
    protected boolean isPromptPayRetry = false;
    protected ETransStatus origTransState;


    // TODO: KiTt below val still not in clone medhod better change to byte array.
    protected byte[] field60Byte;
    protected byte[] field60RecByte;
    protected byte[] bytesField62;

    protected int remainingAmt;
    protected boolean isTopUpRefund;
    protected boolean isOnlinePin;

    protected boolean isVatb;
    protected byte[] REF1;
    protected byte[] REF2;
    protected String vatAmount;
    protected String taxAllowance;
    protected byte[] mercUniqueValue;
    protected byte[] campaignType;
    private boolean isReferralSentSuccess = false;
    protected boolean isRedeemPartial = false;
    private SettleUploadSource eReceiptUploadSource;
    private String eReceiptUploadSourcePath;

    public TransData origErmTransData;



    public TransData() {

    }

    //copy constructor, replace clone
    public TransData(TransData other) {
        this.id = other.id;
        this.stanNo = other.stanNo;
        this.origTransNo = other.origTransNo;
        this.transType = other.transType;
        this.origTransType = other.origTransType;
        this.transState = other.transState;
        this.isUpload = other.isUpload;
        this.offlineSendState = other.offlineSendState;
        this.sendTimes = other.sendTimes;
        this.procCode = other.procCode;
        this.amount = other.amount;
        this.tipAmount = other.tipAmount;
        this.batchNo = other.batchNo;
        this.origBatchNo = other.origBatchNo;
        this.pan = other.pan;
        this.dateTime = other.dateTime;
        this.origDateTime = other.origDateTime;
        this.settleDateTime = other.settleDateTime;
        this.expDate = other.expDate;
        this.nii = other.nii;
        this.refNo = other.refNo;
        this.origRefNo = other.origRefNo;
        this.authCode = other.authCode;
        this.origAuthCode = other.origAuthCode;
        this.issuerCode = other.issuerCode;
        this.acqCode = other.acqCode;
        this.hasPin = other.hasPin;
        this.track1 = other.track1;
        this.track2 = other.track2;
        this.track3 = other.track3;
        this.dupReason = other.dupReason;
        this.reserved = other.reserved;
        this.pinFree = other.pinFree;
        this.signFree = other.signFree;
        this.isCDCVM = other.isCDCVM;
        this.isOnlineTrans = other.isOnlineTrans;
        this.signData = other.signData;
        this.signPath = other.signPath;
        this.cardSerialNo = other.cardSerialNo;
        this.sendIccData = other.sendIccData;
        this.dupIccData = other.dupIccData;
        this.tc = other.tc;
        this.arqc = other.arqc;
        this.arpc = other.arpc;
        this.tvr = other.tvr;
        this.aid = other.aid;
        this.emvAppLabel = other.emvAppLabel;
        this.emvAppName = other.emvAppName;
        this.tsi = other.tsi;
        this.atc = other.atc;
        this.reversalStatus = other.reversalStatus;
        this.phoneNum = other.phoneNum;
        this.email = other.email;
        this.pin = other.pin;
        this.header = other.header;
        this.tpdu = other.tpdu;
        this.recvIccData = other.recvIccData;
        this.acquirer = other.acquirer;

        this.pinVerifyMsg = other.pinVerifyMsg;
        this.emvResult = other.emvResult;
        this.enterMode = other.enterMode;
        this.issuer = other.issuer;
        this.qrRef2 = other.qrRef2;
        this.isTransInqID = other.isTransInqID;
        this.qrSaleStatus = other.qrSaleStatus;
        this.qrSaleState = other.qrSaleState;
        this.origField63 = other.origField63;
        this.isPromptPayRetry = other.isPromptPayRetry;
        this.qrBuyerCode = other.qrBuyerCode;
        this.walletRetryStatus = other.walletRetryStatus;
        this.walletName = other.walletName;
        this.walletSlipInfo = other.walletSlipInfo;
        this.walletPartnerID = other.walletPartnerID;
        this.adviceStatus = other.adviceStatus;
        this.origAmount = other.origAmount;
        this.transYear = other.transYear;
        this.reqMsg = other.reqMsg;
        this.respMsg = other.respMsg;
        this.infoSlipLinePay = other.infoSlipLinePay;
        this.templateId = other.templateId;
        this.field61 = other.field61;
        this.traceNo = other.traceNo;
        this.voidStanNo = other.voidStanNo;
        this.remainingAmt = other.remainingAmt;
        this.UDSN = other.UDSN;

        this.field3 = other.field3;
        this.field48 = other.field48;
        this.field60 = other.field60;
        this.field62 = other.field62;
        this.field63 = other.field63;
        this.referralStatus = other.referralStatus;
        this.OrigStanNo = other.OrigStanNo;
        this.isTxnSmallAmt = other.isTxnSmallAmt;
        this.numSlipSmallAmt = other.numSlipSmallAmt;
        this.qrType = other.qrType;
        this.qrID = other.qrID;
        this.clssTypeMode = other.clssTypeMode;
        this.productCode = other.productCode;
        this.productQty = other.productQty;
        this.redeemedPoint = other.redeemedPoint;
        this.redeemedAmount = other.redeemedAmount;
        this.redeemedDiscountType = other.redeemedDiscountType;
        this.redeemedCredit = other.redeemedCredit;
        this.redeemedTotal = other.redeemedTotal;
        this.instalmentPaymentTerm = other.instalmentPaymentTerm;
        this.instalmentIPlanMode = other.instalmentIPlanMode;
        this.instalmentPromotionKey = other.instalmentPromotionKey;
        this.instalmentSerialNo = other.instalmentSerialNo;
        this.instalmentPromoProduct = other.instalmentPromoProduct;
        this.dccRequired = other.dccRequired;
        this.dccAmount = other.dccAmount;
        this.dccConversionRate = other.dccConversionRate;
        this.dccCurrencyCode = other.dccCurrencyCode;
        this.dccCurrencyName = other.dccCurrencyName;
        this.dccTipAmount = other.dccTipAmount;
        this.origDccAmount = other.origDccAmount;
        this.isEcrProcess = other.isEcrProcess;
        this.eSlipFormat = other.eSlipFormat;
        this.eReceiptRetry = other.eReceiptRetry;
        this.eReceiptUploadStatus = other.eReceiptUploadStatus;
        this.isEReceiptManualPrint = other.isEReceiptManualPrint;
        this.isEReceiptReprint = other.isEReceiptReprint;
        this.eReceiptUploadTimeDateTime = other.eReceiptUploadTimeDateTime;
        this.PosNo_ReceiptNo = other.PosNo_ReceiptNo;
        this.CashierName = other.CashierName;
        this.instalmentTerms = other.instalmentTerms;
        this.instalmentMonthDue = other.instalmentMonthDue;
        this.instalmentInterest = other.instalmentInterest;
        this.serialNum = other.serialNum;
        this.mktCode = other.mktCode;
        this.skuCode = other.skuCode;
        this.redeemPoints = other.redeemPoints;
        this.isBayInstalmentSpecific = other.isBayInstalmentSpecific;

        this.walletBankCode = other.walletBankCode;
        this.walletVerifyPaySlipQRCode = other.walletVerifyPaySlipQRCode;
        this.adjustedAmount = other.adjustedAmount;
        this.adjustedDccAmount = other.adjustedDccAmount;
        this.field28 = other.field28;
        this.origTransState = other.origTransState;
        this.preAuthTransDate = other.preAuthTransDate;
        this.preAuthStatus = other.preAuthStatus;
        this.saleReference1 = other.saleReference1;
        this.saleReference2 = other.saleReference2;
    }

    /**
     * id
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStanNo() {
        return stanNo;
    }

    public void setStanNo(long stanNo) {
        this.stanNo = stanNo;
    }

    public long getOrigTransNo() {
        return origTransNo;
    }

    public void setOrigTransNo(long origTransNo) {
        this.origTransNo = origTransNo;
    }

    public ETransType getTransType() {
        return transType;
    }

    public void setTransType(ETransType transType) {
        this.transType = transType;
    }

    public ETransType getOrigTransType() {
        return origTransType;
    }

    public void setOrigTransType(ETransType origTransType) {
        this.origTransType = origTransType;
    }

    public ETransStatus getTransState() {
        return transState;
    }

    public void setTransState(ETransStatus transState) {
        this.transState = transState;
    }

    public boolean isUpload() {
        return isUpload;
    }

    public void setUpload(boolean isUpload) {
        this.isUpload = isUpload;
    }

    public OfflineStatus getOfflineSendState() {
        return offlineSendState;
    }

    public void setOfflineSendState(OfflineStatus offlineSendState) {
        this.offlineSendState = offlineSendState;
    }

    public int getSendTimes() {
        return sendTimes;
    }

    public void setSendTimes(int sendTimes) {
        this.sendTimes = sendTimes;
    }

    public String getProcCode() {
        return procCode;
    }

    public void setProcCode(String procCode) {
        this.procCode = procCode;
    }

    public String getAmount() {
        return amount == null ? "000000000000" : amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(String tipAmount) {
        this.tipAmount = tipAmount;
    }

    public Locale getCurrency() {
        return currency;
    }

    public void setCurrency(Locale currency) {
        this.currency = currency;
    }
    public String getQrResult() {
        return qrResult;
    }

    public void setQrResult(String qrResult) {
        this.qrResult = qrResult;
    }
    public long getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(long batchNo) {
        this.batchNo = batchNo;
    }

    public long getOrigBatchNo() {
        return origBatchNo;
    }

    public void setOrigBatchNo(long origBatchNo) {
        this.origBatchNo = origBatchNo;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        if (pan == null) {
            this.pan = pan;
        } else {
            this.pan = pan.trim();
        }
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getOrigDateTime() {
        return origDateTime;
    }

    public void setOrigDateTime(String origDateTime) {
        this.origDateTime = origDateTime;
    }

    public String getSettleDateTime() {
        return settleDateTime;
    }

    public void setSettleDateTime(String settleDateTime) {
        this.settleDateTime = settleDateTime;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public EnterMode getEnterMode() {
        return enterMode;
    }

    public void setEnterMode(EnterMode enterMode) {
        this.enterMode = enterMode;
    }

    public String getNii() {
        return nii;
    }

    public void setNii(String nii) {
        this.nii = nii;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getOrigRefNo() {
        return origRefNo;
    }

    public void setOrigRefNo(String origRefNo) {
        this.origRefNo = origRefNo;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getOrigAuthCode() {
        return origAuthCode;
    }

    public void setOrigAuthCode(String origAuthCode) {
        this.origAuthCode = origAuthCode;
    }

    public String getIssuerCode() {
        return issuerCode;
    }

    public void setIssuerCode(String issuerCode) {
        this.issuerCode = issuerCode;
    }

    public String getAcqCode() {
        return acqCode;
    }

    public void setAcqCode(String acqCode) {
        this.acqCode = acqCode;
    }

    public boolean isHasPin() {
        return hasPin;
    }

    public void setHasPin(boolean hasPin) {
        this.hasPin = hasPin;
    }

    public String getTrack1() {
        return track1;
    }

    public void setTrack1(String track1) {
        this.track1 = track1;
    }

    public String getTrack2() {
        return track2;
    }

    public void setTrack2(String track2) {
        this.track2 = track2;
    }

    public String getTrack3() {
        return track3;
    }

    public void setTrack3(String track3) {
        this.track3 = track3;
    }

    public String getDupReason() {
        return dupReason;
    }

    public void setDupReason(String dupReason) {
        this.dupReason = dupReason;
    }

    public String getReserved() {
        return reserved;
    }

    public void setReserved(String reserved) {
        this.reserved = reserved;
    }

    public boolean isPinFree() {
        return pinFree;
    }

    public void setPinFree(boolean pinFree) {
        this.pinFree = pinFree;
    }

    public boolean isSignFree() {
        return signFree;
    }

    public void setSignFree(boolean signFree) {
        this.signFree = signFree;
    }

    public boolean isCDCVM() {
        return isCDCVM;
    }

    public void setCDCVM(boolean isCDCVM) {
        this.isCDCVM = isCDCVM;
    }

    public boolean isOnlineTrans() {
        return isOnlineTrans;
    }

    public void setOnlineTrans(boolean isOnlineTrans) {
        this.isOnlineTrans = isOnlineTrans;
    }

    public byte[] getSignData() {
        return signData;
    }

    public void setSignData(byte[] signData) {
        this.signData = signData;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public Acquirer getAcquirer() {
        return acquirer;
    }

    public void setAcquirer(Acquirer acquirer) {
        this.acquirer = acquirer;

        //TODO: KiTty
        // this is hack code not good example please remove it (if you have time :P)
        this.merchantName = MerchantProfileManager.INSTANCE.getCurrentMerchant();
    }

    /**
     * EMV交易的执行状态
     */
    public ETransResult getEmvResult() {
        return emvResult;
    }

    public void setEmvResult(ETransResult emvResult) {
        this.emvResult = emvResult;
    }

    public String getCardSerialNo() {
        return cardSerialNo;
    }

    public void setCardSerialNo(String cardSerialNo) {
        this.cardSerialNo = cardSerialNo;
    }

    public String getSendIccData() {
        return sendIccData;
    }

    public void setSendIccData(String sendIccData) {
        this.sendIccData = sendIccData;
    }

    public String getDupIccData() {
        return dupIccData;
    }

    public void setDupIccData(String dupIccData) {
        this.dupIccData = dupIccData;
    }

    public String getTc() {
        return tc;
    }

    public void setTc(String tc) {
        this.tc = tc;
    }

    public String getArqc() {
        return arqc;
    }

    public void setArqc(String arqc) {
        this.arqc = arqc;
    }

    public String getArpc() {
        return arpc;
    }

    public void setArpc(String arpc) {
        this.arpc = arpc;
    }

    public String getTvr() {
        return tvr;
    }

    public void setTvr(String tvr) {
        this.tvr = tvr;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getEmvAppLabel() {
        return emvAppLabel;
    }

    public void setEmvAppLabel(String emvAppLabel) {
        this.emvAppLabel = emvAppLabel;
    }

    public String getEmvAppName() {
        return emvAppName;
    }

    public void setEmvAppName(String emvAppName) {
        this.emvAppName = emvAppName;
    }

    public String getTsi() {
        return tsi;
    }

    public void setTsi(String tsi) {
        this.tsi = tsi;
    }

    public String getAtc() {
        return atc;
    }

    public void setAtc(String atc) {
        this.atc = atc;
    }

    /**
     * 个人密码(密文)
     */
    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    /**
     * 响应码
     */
    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getTpdu() {
        return tpdu;
    }

    public void setTpdu(String tpdu) {
        this.tpdu = tpdu;
    }

    public ReversalStatus getReversalStatus() {
        return reversalStatus;
    }

    public void setReversalStatus(ReversalStatus reversalStatus) {
        this.reversalStatus = reversalStatus;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public byte[] getField46() {
        return field46Byte;
    }

    public void setField46(byte[] field46Byte) {
        this.field46Byte = field46Byte;
    }

    public String getField48() {
        return field48;
    }

    public void setField48(String field48) {
        this.field48 = field48;
    }

    public String getField60() {
        return field60;
    }

    public void setField60(String field60) {
        this.field60 = field60;
    }

    public byte[] getField60RecByte() {
        return field60RecByte;
    }

    public void setField60RecByte(byte[] field60) {
        if (field60 != null) this.field60RecByte = field60.clone();
    }

    public String getField61() {
        return field61;
    }

    public void setField61(String field61) {
        this.field61 = field61;
    }

    public void setField61Byte(byte[] field61) {
        if (field61 != null) this.field61Byte = field61.clone();
    }

    public byte[] getField61Byte() {
        return field61Byte;
    }

    public byte[] getField61RecByte() {
        return field61RecByte;
    }

    public void setField61RecByte(byte[] field61) {
        if (field61 != null) this.field61RecByte = field61.clone();
    }

    public String getField62() {
        return field62;
    }

    public void setField62(String field62) {
        this.field62 = field62;
    }

    public String getField63() {
        return field63;
    }

    public void setField63(String field63) {
        this.field63 = field63;
    }

    public byte[] getField63Byte() {
        return field63Byte;
    }

    public void setField63Byte(byte[] field63) {
        if (field63 != null) this.field63Byte = field63.clone();
    }

    public byte[] getField63RecByte() {
        return field63RecByte;
    }

    public void setField63RecByte(byte[] field63) {
        if (field63 != null) this.field63RecByte = field63.clone();
    }

    public String getRecvIccData() {
        return recvIccData;
    }

    public void setRecvIccData(String recvIccData) {
        this.recvIccData = recvIccData;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public boolean isPinVerifyMsg() {
        return pinVerifyMsg;
    }

    public void setPinVerifyMsg(boolean pinVerifyMsg) {
        this.pinVerifyMsg = pinVerifyMsg;
    }

    public byte[] getSignPath() {
        return signPath;
    }

    public void setSignPath(byte[] signPath) {
        this.signPath = signPath;
    }

    public String getQrRef2() {
        return qrRef2;
    }

    public void setQrRef2(String qrRef2) {
        this.qrRef2 = qrRef2;
    }

    public String getQrSaleStatus() {
        return qrSaleStatus;
    }

    public void setQrSaleStatus(String qrSaleStatus) {
        this.qrSaleStatus = qrSaleStatus;
    }

    public boolean isTransInqID() {
        return isTransInqID;
    }

    public void setTransInqID(boolean transInqID) {
        isTransInqID = transInqID;
    }

    public QrSaleState getQrSaleState() {
        return qrSaleState;
    }

    public void setQrSaleState(QrSaleState qrSaleState) {
        this.qrSaleState = qrSaleState;
    }

    public String getOrigField63() {
        return origField63;
    }

    public void setOrigField63(String origField63) {
        this.origField63 = origField63;
    }

    public boolean getPromptPayRetry() {
        return isPromptPayRetry;
    }

    public void setPromptPayRetry(Boolean promptPayRetry) {
        isPromptPayRetry = promptPayRetry;
    }

    public String getQrBuyerCode() {
        return qrBuyerCode;
    }

    public void setQrBuyerCode(String qrBuyerCode) {
        this.qrBuyerCode = qrBuyerCode;
    }

    public byte[] getBytesField62() {
        return bytesField62;
    }

    public void setBytesField62(byte[] bytesField62) {
        this.bytesField62 = bytesField62;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public WalletRetryStatus getWalletRetryStatus() {
        return walletRetryStatus;
    }

    public void setWalletRetryStatus(WalletRetryStatus walletRetryStatus) {
        this.walletRetryStatus = walletRetryStatus;
    }

    public String getOrigAmount() {
        return origAmount;
    }

    public void setOrigAmount(String origAmount) {
        this.origAmount = origAmount;
    }

    public String getWalletSlipInfo() {
        return walletSlipInfo;
    }

    public void setWalletSlipInfo(String walletSlipInfo) {
        this.walletSlipInfo = walletSlipInfo;
    }

    public AdviceStatus getAdviceStatus() {
        return adviceStatus;
    }

    public void setAdviceStatus(AdviceStatus adviceStatus) {
        this.adviceStatus = adviceStatus;
    }

    public String getWalletPartnerID() {
        return walletPartnerID;
    }

    public void setWalletPartnerID(String walletPartnerID) {
        this.walletPartnerID = walletPartnerID;
    }

    public String getTransYear() {
        return transYear;
    }

    public void setTransYear(String transYear) {
        this.transYear = transYear;
    }

    public String[] getReqMsg() {
        return reqMsg;
    }

    public void setReqMsg(String[] reqMsg) {
        this.reqMsg = reqMsg;
    }

    public String[] getRespMsg() {
        return respMsg;
    }

    public void setRespMsg(String[] respMsg) {
        this.respMsg = respMsg;
    }

    public String[] getInfoSlipLinePay() {
        return infoSlipLinePay;
    }

    public void setInfoSlipLinePay(String[] infoSlipLinePay) {
        this.infoSlipLinePay = infoSlipLinePay;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public long getTraceNo() {
        return traceNo;
    }

    public void setTraceNo(long traceNo) {
        this.traceNo = traceNo;
    }

    public void setVoidStanNo(long voidStanNo) {
        this.voidStanNo = voidStanNo;
    }

    public long getVoidStanNo() {
        return voidStanNo;
    }

    public int getRemainingAmt() {
        return remainingAmt;
    }

    public void setRemainingAmt(int remainingAmt) {
        this.remainingAmt = remainingAmt;
    }

    public void isTopUpRefund(Boolean topUpRefund) {
        this.isTopUpRefund = topUpRefund;
    }

    public boolean isTopUpRefund() {
        return this.isTopUpRefund;
    }

    public String getUDSN() {
        return UDSN;
    }

    public void setUDSN(String UDSN) {
        this.UDSN = UDSN;
    }

    public ReferralStatus getReferralStatus() {
        return referralStatus;
    }

    public void setReferralStatus(ReferralStatus referralStatus) {
        this.referralStatus = referralStatus;
    }

    public long getOrigStanNo() {
        return OrigStanNo;
    }

    public void setOrigStanNo(long origStanNo) {
        OrigStanNo = origStanNo;
    }

    public void setOnlinePin(boolean onlinePin) {
        isOnlinePin = onlinePin;
    }

    public boolean isOnlinePin() {
        return isOnlinePin;
    }

    public boolean isReferralSentSuccess() {
        return isReferralSentSuccess;
    }

    public void setReferralSentSuccess(boolean referralSentSuccess) {
        isReferralSentSuccess = referralSentSuccess;
    }

    public boolean isTxnSmallAmt() {
        return isTxnSmallAmt;
    }

    public void setTxnSmallAmt(boolean txnSmallAmt) {
        isTxnSmallAmt = txnSmallAmt;
    }

    public int getNumSlipSmallAmt() {
        return numSlipSmallAmt;
    }

    public void setNumSlipSmallAmt(int numSlipSmallAmt) {
        this.numSlipSmallAmt = numSlipSmallAmt;
    }

    public void setQrID(String qrID) {
        this.qrID = qrID;
    }

    public String getQrID() {
        return qrID;
    }

    public void setLastTrans(boolean lastTrans) {
        isLastTrans = lastTrans;
    }

    public boolean getLastTrans() {
        return isLastTrans;
    }

    public String getBranchID() {
        return branchID;
    }

    public void setBranchID(String branchID) {
        this.branchID = branchID;
    }

    public int getClssTypeMode() {
        return clssTypeMode;
    }

    public void setClssTypeMode(int clssTypeMode) {
        this.clssTypeMode = clssTypeMode;
    }

    public void setQrType(String qrType) {
        this.qrType = qrType;
    }

    public String getQrType() {
        return qrType;
    }

    public void setQRCurrency(String QRCurrency) {
        this.QRCurrency = QRCurrency;
    }

    public String getQRCurrency() {
        return QRCurrency;
    }

    public void setTxnID(String TxnID) {
        this.TxnID = TxnID;
    }

    public String getTxnID() {
        return TxnID;
    }

    public void setPayTime(String PayTime) {
        this.PayTime = PayTime;
    }

    public String getPayTime() {
        return PayTime;
    }

    public void setExchangeRate(String ExchangeRate) {
        this.ExchangeRate = ExchangeRate;
    }

    public String getExchangeRate() {
        return ExchangeRate;
    }

    public void setTxnAmount(String TxnAmount) {
        this.TxnAmount = TxnAmount;
    }

    public String getTxnAmount() {
        return TxnAmount;
    }

    public void setAmountCNY(String AmountCNY) {
        this.AmountCNY = AmountCNY;
    }

    public String getAmountCNY() {
        return AmountCNY;
    }

    public void setBuyerUserID(String BuyerUserID) {
        this.BuyerUserID = BuyerUserID;
    }

    public String getBuyerUserID() {
        return BuyerUserID;
    }

    public void setBuyerLoginID(String BuyerLoginID) {
        this.BuyerLoginID = BuyerLoginID;
    }

    public String getBuyerLoginID() {
        return BuyerLoginID;
    }

    public void setMerchantInfo(String MerchantInfo) {
        this.MerchantInfo = MerchantInfo;
    }

    public String getMerchantInfo() {
        return MerchantInfo;
    }

    public void setAppCode(String AppCode) {
        this.AppCode = AppCode;
    }

    public String getAppCode() {
        return AppCode;
    }

    public void setPromocode(String Promocode) {
        this.Promocode = Promocode;
    }

    public String getPromocode() {
        return Promocode;
    }

    public void setTxnNo(String TxnNo) {
        this.TxnNo = TxnNo;
    }

    public String getTxnNo() {
        return TxnNo;
    }

    public void setFee(String Fee) {
        this.Fee = Fee;
    }

    public String getFee() {
        return Fee;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public int getProductQty() {
        return productQty;
    }

    public void setProductQty(int productQty) {
        this.productQty = productQty;
    }

    public int getRedeemedPoint() {
        return redeemedPoint;
    }

    public void setRedeemedPoint(int redeemedPoint) {
        this.redeemedPoint = redeemedPoint;
    }

    public String getRedeemedAmount() {
        return redeemedAmount;
    }

    public void setRedeemedAmount(String redeemedAmount) {
        this.redeemedAmount = redeemedAmount;
    }

    public String getRedeemedDiscountType() {
        return redeemedDiscountType;
    }

    public void setRedeemedDiscountType(String redeemedDiscountType) {
        this.redeemedDiscountType = redeemedDiscountType;
    }

    public String getRedeemedCredit() {
        return redeemedCredit;
    }

    public void setRedeemedCredit(String redeemedCredit) {
        this.redeemedCredit = redeemedCredit;
    }

    public String getRedeemedTotal() {
        return redeemedTotal;
    }

    public void setRedeemedTotal(String redeemedTotal) {
        this.redeemedTotal = redeemedTotal;
    }

    public int getInstalmentPaymentTerm() {
        return instalmentPaymentTerm;
    }

    public void setInstalmentPaymentTerm(int instalmentPaymentTerm) {
        this.instalmentPaymentTerm = instalmentPaymentTerm;
    }

    public String getInstalmentIPlanMode() {
        return instalmentIPlanMode;
    }

    public void setInstalmentIPlanMode(String instalmentIPlanMode) {
        this.instalmentIPlanMode = instalmentIPlanMode;
    }

    public String getInstalmentPromotionKey() {
        return instalmentPromotionKey;
    }

    public void setInstalmentPromotionKey(String instalmentPromotionKey) {
        this.instalmentPromotionKey = instalmentPromotionKey;
    }

    public boolean isInstalmentPromoProduct() {
        return instalmentPromoProduct;
    }

    public void setInstalmentPromoProduct(boolean instalmentPromoProduct) {
        this.instalmentPromoProduct = instalmentPromoProduct;
    }

    public String getInstalmentSerialNo() {
        return instalmentSerialNo;
    }

    public void setInstalmentSerialNo(String instalmentSerialNo) {
        this.instalmentSerialNo = instalmentSerialNo;
    }

    public boolean isDccRequired() {
        return dccRequired;
    }

    public void setDccRequired(boolean dccRequired) {
        this.dccRequired = dccRequired;
    }

    public String getDccAmount() {
        return dccAmount;
    }

    public void setDccAmount(String dccAmount) {
        this.dccAmount = dccAmount;
    }

    public String getDccConversionRate() {
        return dccConversionRate;
    }

    public void setDccConversionRate(String dccConversionRate) {
        this.dccConversionRate = dccConversionRate;
    }

    public byte[] getDccCurrencyCode() {
        return dccCurrencyCode;
    }

    public void setDccCurrencyCode(byte[] dccCurrencyCode) {
        this.dccCurrencyCode = dccCurrencyCode;
    }

    public String getDccCurrencyName() {
        return dccCurrencyName;
    }

    public void setDccCurrencyName(String dccCurrencyName) {
        this.dccCurrencyName = dccCurrencyName;
    }

    public String getDccTipAmount() {
        return dccTipAmount;
    }

    public void setDccTipAmount(String dccTipAmount) {
        this.dccTipAmount = dccTipAmount;
    }

    public String getOrigDccAmount() {
        return origDccAmount;
    }

    public void setOrigDccAmount(String origDccAmount) {
        this.origDccAmount = origDccAmount;
    }

    public boolean isEcrProcess() {
        return isEcrProcess;
    }

    public void setEcrProcess(boolean ecrProcess) {
        isEcrProcess = ecrProcess;
    }

    // VERIFONE-ERM
    protected String VF_ERCM_TerminalSerialNumber;
    protected String VF_ERCM_BankCode;
    protected String VF_ERCM_MerchantCode;
    protected String VF_ERCM_StoreCode;
    protected byte[] VF_ERCM_HeaderLogoImage;
    protected byte[] VF_ERCM_FooterLogoImage;
    protected byte[] VF_ERCM_LogoImageData;
    protected String VF_ERCM_Init_AcquirerIndex;
    protected String VF_ERCM_Init_AcquirerName;
    protected String VF_ERCM_Init_AcquirerNii;
    protected String VF_ERCM_Init_HeaderLogoFile;
    protected String VF_ERCM_Init_FooterLogoFile;
    protected String VF_ERCM_Init_SessionKeyFile;

    protected byte[] VF_ERCM_PBK_Exponent;
    protected byte[] VF_ERCM_PBK_Modulus;
    protected byte[] VF_ERCM_PBK_Hash;
    protected String VF_ERCM_PBK_Version;
    protected byte[] VF_ERCM_SessionKeyBlock;
    protected HashMap<String, Object> VF_ERCM_SessionKeyOutput;

    public void setSessionKeyOutput(HashMap<String, Object> exSessionKeyOutput) {
        this.VF_ERCM_SessionKeyOutput = exSessionKeyOutput;
    }

    public HashMap<String, Object> getSessionKeyOutput() {
        return this.VF_ERCM_SessionKeyOutput;
    }

    public void setInitHeaderLogoFile(String exHeaderLogoFile) {
        this.VF_ERCM_Init_HeaderLogoFile = exHeaderLogoFile;
    }

    public String getInitHeaderLogoFile() {
        return this.VF_ERCM_Init_HeaderLogoFile;
    }

    public void setInitFooterLogoFile(String exFooterLogoFile) {
        this.VF_ERCM_Init_FooterLogoFile = exFooterLogoFile;
    }

    public String getInitFooterLogoFile() {
        return this.VF_ERCM_Init_FooterLogoFile;
    }

    public void setInitSessionKeyFile(String exSessionKetFile) {
        this.VF_ERCM_Init_SessionKeyFile = exSessionKetFile;
    }

    public String getInitSessionKeyFile() {
        return this.VF_ERCM_Init_SessionKeyFile;
    }

    public void setInitAcquirerIndex(String exInitAcquirerIndex) {
        this.VF_ERCM_Init_AcquirerIndex = exInitAcquirerIndex;
    }

    public String getInitAcquirerIndex() {
        return this.VF_ERCM_Init_AcquirerIndex;
    }

    public void setInitAcquirerNii(String exInitAcquirerNii) {
        this.VF_ERCM_Init_AcquirerNii = exInitAcquirerNii;
    }

    public String getInitAcquirerNii() {
        return this.VF_ERCM_Init_AcquirerNii;
    }

    public void setInitAcquirerName(String exInitAcquirerName) {
        this.VF_ERCM_Init_AcquirerName = exInitAcquirerName;
    }

    public String getInitAcquirerName() {
        return this.VF_ERCM_Init_AcquirerName;
    }

    public void setERCMTerminalSerialNumber(String exERCMTerminalSerialNumber) {
        this.VF_ERCM_TerminalSerialNumber = exERCMTerminalSerialNumber;
    }

    public String getERCMTerminalSerialNumber() {
        return this.VF_ERCM_TerminalSerialNumber;
    }

    public void setERCMBankCode(String exBankCode) {
        this.VF_ERCM_BankCode = exBankCode;
    }

    public String getERCMBankCode() {
        return this.VF_ERCM_BankCode;
    }

    public void setERCMMerchantCode(String exMerchantCode) {
        this.VF_ERCM_MerchantCode = exMerchantCode;
    }

    public String getERCMMerchantCode() {
        return this.VF_ERCM_MerchantCode;
    }

    public void setERCMStoreCode(String exStoreCode) {
        this.VF_ERCM_StoreCode = exStoreCode;
    }

    public String getERCMStoreCode() {
        return this.VF_ERCM_StoreCode;
    }

    public void setERCMHeaderImagePath(byte[] exHeaderImagePath) {
        this.VF_ERCM_HeaderLogoImage = exHeaderImagePath;
    }

    public byte[] getERCMHeaderImagePath() {
        return this.VF_ERCM_HeaderLogoImage;
    }

    public void setERCMFooterImagePath(byte[] exFooterImagePath) {
        this.VF_ERCM_FooterLogoImage = exFooterImagePath;
    }

    public byte[] getERCMFooterImagePath() {
        return this.VF_ERCM_FooterLogoImage;
    }

    public void setERCMLogoImagePath(byte[] exLogoImagePath) {
        this.VF_ERCM_LogoImageData = exLogoImagePath;
    }

    public byte[] getERCMLogoImagePath() {
        return this.VF_ERCM_LogoImageData;
    }

    public void setPublickeyExponent(byte[] exExponentData) {
        this.VF_ERCM_PBK_Exponent = exExponentData;
    }

    public byte[] getPublickeyExponent() {
        return this.VF_ERCM_PBK_Exponent;
    }

    public void setPublickeyModulus(byte[] exModulusData) {
        this.VF_ERCM_PBK_Modulus = exModulusData;
    }

    public byte[] getPublickeyModulus() {
        return this.VF_ERCM_PBK_Modulus;
    }

    public void setPublickeyHash(byte[] exHashData) {
        this.VF_ERCM_PBK_Hash = exHashData;
    }

    public byte[] getPublickeyHash() {
        return this.VF_ERCM_PBK_Hash;
    }

    public void setPublicKeyVersion(String exPbkVers) {
        this.VF_ERCM_PBK_Version = exPbkVers;
    }

    public String getPublicKeyVersion() {
        return this.VF_ERCM_PBK_Version;
    }

    public void setSessionKeyBlock(byte[] exSessionKEyBlock) {
        this.VF_ERCM_SessionKeyBlock = exSessionKEyBlock;
    }

    public byte[] getSessionKeyBlock() {
        return this.VF_ERCM_SessionKeyBlock;
    }

    public byte[] geteSlipFormat() {
        return eSlipFormat;
    }

    public void seteSlipFormat(byte[] eSlipFormat) {
        this.eSlipFormat = eSlipFormat;
    }

    public int geteReceiptRetry() {
        return eReceiptRetry;
    }

    public void seteReceiptRetry(int eReceiptRetry) {
        this.eReceiptRetry = eReceiptRetry;
    }

    public UploadStatus geteReceiptUploadStatus() {
        return eReceiptUploadStatus;
    }

    public void seteReceiptUploadStatus(UploadStatus eReceiptUploadStatus) {
        this.eReceiptUploadStatus = eReceiptUploadStatus;
    }

    public SettleUploadSource geteReceiptUploadSource() {
        return eReceiptUploadSource;
    }

    public void seteReceiptUploadSource(SettleUploadSource eReceiptUploadSource) {
        this.eReceiptUploadSource = eReceiptUploadSource;
    }

    public String geteReceiptUploadSourcePath() {
        return eReceiptUploadSourcePath;
    }

    public void seteReceiptUploadSourcePath(String eReceiptUploadSource) {
        this.eReceiptUploadSourcePath = eReceiptUploadSource;
    }

    public boolean isEReceiptManualPrint() {
        return isEReceiptManualPrint;
    }

    public void setEReceiptManualPrint(boolean EReceiptManualPrint) {
        isEReceiptManualPrint = EReceiptManualPrint;
    }

    public boolean isEReceiptReprint() {
        return isEReceiptReprint;
    }

    public void setEReceiptReprint(boolean EReceiptReprint) {
        isEReceiptReprint = EReceiptReprint;
    }

    public String geteReceiptUploadDateTime() {
        return eReceiptUploadTimeDateTime;
    }

    public void seteReceiptUploadDateTime(String eReceiptUploadDateTime) {
        this.eReceiptUploadTimeDateTime = eReceiptUploadDateTime;
    }

    public TransTotal total;

    public TransTotal getSettlementTransTotal() {
        return total;
    }

    public void setSettleTransTotal(TransTotal exTransTotal) {
        total = exTransTotal;
    }

    public void setPosNo_ReceiptNo(String posNo_ReceiptNo) {
        PosNo_ReceiptNo = posNo_ReceiptNo;
    }

    public String getPosNo_ReceiptNo() {
        return PosNo_ReceiptNo;
    }

    public void setCashierName(String cashierName) {
        CashierName = cashierName;
    }

    public String getCashierName() {
        return CashierName;
    }

    public enum SettleErmReprintMode {None, HideReprint_HideSignature, HideReprint_ShowSignature, ShowReprint_HideSignature, ShowReprint_ShowSignature}

    private SettleErmReprintMode settleErmReprintMode = SettleErmReprintMode.ShowReprint_HideSignature;

    public void setSettlementErmReprintMode(SettleErmReprintMode mode) {
        settleErmReprintMode = mode;
    }

    public SettleErmReprintMode getSettlementErmReprintMode() {
        return settleErmReprintMode;
    }

    public void increaseNumberOfErmPrintingCount() {
        NumberOfErmPrintingCount += 1;
    }

    public void setNumberOfErmPrintingCount(int exErmPrintingCount) {
        NumberOfErmPrintingCount = exErmPrintingCount;
    }

    public int getNumberOfErmPrintingCount() {
        return NumberOfErmPrintingCount;
    }

    public void setIsVATB(boolean isVatb) {
        this.isVatb = isVatb;
    }

    public boolean isVatb() {
        return isVatb;
    }

    public void setREF1(byte[] REF1) {
        this.REF1 = REF1;
    }

    public void setREF2(byte[] REF2) {
        this.REF2 = REF2;
    }

    public void setVatAmount(String vatAmount) {
        this.vatAmount = vatAmount;
    }

    public void setTaxAllowance(String taxAllowance) {
        this.taxAllowance = taxAllowance;
    }

    public void setMercUniqueValue(byte[] mercUniqueValue) {
        this.mercUniqueValue = mercUniqueValue;
    }

    public void setCampaignType(byte[] campaignType) {
        this.campaignType = campaignType;
    }

    public byte[] getREF1() {
        return REF1;
    }

    public byte[] getREF2() {
        return REF2;
    }

    public String getVatAmount() {
        return vatAmount;
    }

    public String getTaxAllowance() {
        return taxAllowance;
    }

    public byte[] getMercUniqueValue() {
        return mercUniqueValue;
    }

    public byte[] getCampaignType() {
        return campaignType;
    }

    public String getInstalmentTerms() {
        return instalmentTerms;
    }

    public void setInstalmentTerms(String instalmentTerms) {
        this.instalmentTerms = instalmentTerms;
    }

    public String getInstalmentMonthDue() {
        return instalmentMonthDue;
    }

    public void setInstalmentMonthDue(String instalmentMonthDue) {
        this.instalmentMonthDue = instalmentMonthDue;
    }

    public String getInstalmentInterest() {
        return instalmentInterest;
    }

    public void setInstalmentInterest(String instalmentInterest) {
        this.instalmentInterest = instalmentInterest;
    }

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public String getMktCode() {
        return mktCode;
    }

    public void setMktCode(String mktCode) {
        this.mktCode = mktCode;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getRedeemPoints() {
        return redeemPoints;
    }

    public void setRedeemPoints(String redeemPoints) {
        this.redeemPoints = redeemPoints;
    }

    public boolean isBayInstalmentSpecific() {
        return isBayInstalmentSpecific;
    }

    public void setBayInstalmentSpecific(boolean bayInstalmentSpecific) {
        isBayInstalmentSpecific = bayInstalmentSpecific;
    }

    public boolean isRedeemPartial() {
        return isRedeemPartial;
    }

    public void setRedeemPartial(boolean redeemPartial) {
        isRedeemPartial = redeemPartial;
    }

    public void setReferenceSaleID(String refSaleID) {
        ecrReferenceSaleID = refSaleID;
    }

    public String getReferenceSaleID() {
        return ecrReferenceSaleID;
    }

    public String getQrSourceOfFund() {return qrSourceOfFund;}
    public void setQrSourceOfFund(String qrSourceOfFund) {this.qrSourceOfFund = qrSourceOfFund;}

    public boolean getEnableQrTag31() {return enableQrTag31;}
    public void setEnableQrTag31(boolean enableQrTag31) {this.enableQrTag31 = enableQrTag31;}

    public String getWalletBankCode() {return walletBankCode;}
    public void setWalletBankCode(String walletBankCode) {this.walletBankCode=walletBankCode;}

    public String getWalletVerifyPaySlipQRCode() {return walletVerifyPaySlipQRCode;}
    public void setWalletVerifyPaySlipQRCode(String walletVerifyPaySlipQRCode) {this.walletVerifyPaySlipQRCode=walletVerifyPaySlipQRCode;}

    public String getAdjustedAmount() {
        return adjustedAmount;
    }

    public void setAdjustedAmount(String adjustedAmount) {
        this.adjustedAmount = adjustedAmount;
    }

    public String getAdjustedDccAmount() {
        return adjustedDccAmount;
    }

    public void setAdjustedDccAmount(String adjustedDccAmount) {
        this.adjustedDccAmount = adjustedDccAmount;
    }

    public String getField28() {return field28;}
    public void setField28(String field28) {this.field28 = field28;}

    public ETransStatus getOrigTransState() {
        return origTransState;
    }

    public void setOrigTransState(ETransStatus origTransState) {
        this.origTransState = origTransState;
    }

    public String getChannel() {return channel;}
    public void setChannel(String channel) {this.channel = channel;}

    public String getMerchantName() {return merchantName;}
    public void setMerchantName(String merchantName) {this.merchantName = merchantName;}

    public String getPreAuthTransDate() {return preAuthTransDate;}
    public void setPreAuthTransDate(String preAuthTransDate) {this.preAuthTransDate = preAuthTransDate;}

    public EPreAuthStatus getPreAuthStatus() {return preAuthStatus;}
    public void setPreAuthStatus(EPreAuthStatus preAuthStatus) {this.preAuthStatus = preAuthStatus;}

    public String getSaleReference1() {
        return saleReference1;
    }

    public void setSaleReference1(String saleReference1) {
        this.saleReference1 = saleReference1;
    }

    public String getSaleReference2() {
        return saleReference2;
    }

    public void setSaleReference2(String saleReference2) {
        this.saleReference2 = saleReference2;
    }
}

