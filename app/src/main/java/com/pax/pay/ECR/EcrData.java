package com.pax.pay.ECR;

import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.EcrAuditReportPrintingModel;
import com.pax.pay.trans.model.EcrSettlementModel;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.ResponseCode;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import th.co.bkkps.linkposapi.model.SettleHostData;
import th.co.bkkps.linkposapi.service.LinkPOSMessage;
import th.co.bkkps.utils.Log;

import static com.pax.pay.trans.component.Component.getPaddedNumber;
import static com.pax.pay.trans.component.Component.getPaddedStringRight;

public enum EcrData {
    instance;
    //public static final Pattern RESTXT_REGEX_PATTERN = Pattern.compile("[^\\p{ASCII}]");
    public static final Pattern RESTXT_REGEX_PATTERN = Pattern.compile("[^A-Za-z0-9_.]");
    //PosNo_ReceiptNo
    public byte[] PosNo_ReceiptNo = new byte[10];
    /**************************************************************************************************/
    /*                                ECR Data For HyperCom Protocol                                  */
    // USER_ID
    public byte[] User_ID = new byte[6];
    //Cashier Name
    public byte[] CashierName = new byte[10];
    // For HyperCom settlement
    public long nBatchTotalSalesCount;
    public long nBatchTotalSalesAmount;
    public long nBatchTotalRefundCount;
    public long nBatchTotalRefundAmount;
    public boolean isEcrProcess;
    // For HyperCom audit report
    public String nDateTime;
    public byte[] T1C_MemberID = new byte[20];
    public byte[] T1C_CardRefFlag = new byte[1];
    public boolean isOnProcessing = false;
    /**************************************************************************************************/

    // mResponseCode
    public byte[] RespCode = new byte[]{0, 0};
    //FieldType = "02", Response Text from host or EDC
    public byte[] HyperComRespText = new byte[40];
    //FieldType = "D0", Merchant Name and Address
    byte[] MerName = new byte[23];
    byte[] MerAddress = new byte[23];
    byte[] MerAddress1 = new byte[23];
    //FieldType = "03", Transaction Date
    public byte[] DateByte = new byte[6];
    //FieldType = "04", Transaction Time
    public byte[] TimeByte = new byte[6];
    //FieldType = "56", Approval Code
    public byte[] ApprovalCode = new byte[6];
    //FieldType = "56", Invoice/Trace Number
    public byte[] TraceNo = new byte[6];
    //FieldType = "16", Terminal Identification Number (TID)
    public byte[] TermID = new byte[8];
    //FieldType = "D1", Merchant Number (MID)
    public byte[] MerID = new byte[15];
    //FieldType = "D2", Card Issuer Name
    public byte[] CardIssuerName = new byte[10];
    //FieldType = "30", Card Number
    public byte[] HyperComCardNo = new byte[19];
    public byte[] qr_TransID = new byte[19];
    //FieldType = "31", Expiry Date
    public byte[] ExpDate = new byte[4];
    //FieldType = "50", Batch Number
    public byte[] BatchNo = new byte[6];
    //FieldType = "D3", Retrieval Reference
    public byte[] RefNo = new byte[12];
    //FieldType = "D4", Card Issuer ID
    public byte[] CardIssuerID = new byte[2];
    //FieldType = 0x48 0x4E, HN-NII
    public byte[] HYPER_COM_HN_NII = new byte[3];
    //FieldType = "H1"-Batch total sales count
    public byte[] BatchTotalSalesCount = new byte[3];
    //FieldType = "H2"-Batch total sales amount
    public byte[] BatchTotalSalesAmount = new byte[12];
    //Batch total Refund count
    public byte[] BatchTotalRefundCount = new byte[3];
    //Batch total Refund Amount
    public byte[] BatchTotalRefundAmount = new byte[12];
    byte[] padding = new byte[6];
    byte[] padding2 = new byte[11];


    //FieldType = "D2", Card Issuer Name
    public byte[] qr_IssuerName = new byte[10];                 // For QR-SALE of Lawson
    // FieldType = "45" :  Merchant Number
    public byte[] merchantNumber = new byte[1];
    // FieldType = "65" :  Trace/Invoce
    public byte[] traceInvoice = new byte[6];
    // FieldType = "A1" :  Wallet Type
    public byte[] walletType = new byte[2];
    // FieldType = "FG" :  Signature ImageData
    public byte[] signatureImgData = null;
    // FieldType = "FH" :  Data for Kisosk/Pos Print AID
    public byte[] kioskPos_AID = new byte[7];
    // FieldType = "FI" :  Data for Kisosk/Pos Print TVR
    public byte[] kioskPos_TVR = new byte[5];
    // FieldType = "FJ" :  Data for Kisosk/Pos Print TSI
    public byte[] kioskPos_TSI = new byte[2];
    // FieldType = "FK" :  Data for Kisosk/Pos Print TSI
    public byte[] kioskPos_PrintSignatureBox = new byte[1];
    // FieldType = "HI" :  Host Index
    public byte[] hostIndex = new byte[3];
    public byte[] qr_hostIndex = new byte[3];
    // FieldType = "HO" :  Single Batch Total
    private EcrAuditReportPrintingModel AuditReportTotal = null;
    public byte[] singleBatchTotal = new byte[0];
    // FieldType = "HN" :  Single Batch Total
    public byte[] singleHostNII = new byte[3];
    // FieldType = "R0" :  Data for Kisosk/Pos Print TSI
    public byte[] saleReferenceIDR0 = new byte[8];
    // FieldType = "R1" :  Data for Kisosk/Pos Print TSI
    public byte[] saleReferenceIDR1 = new byte[12];
    // FieldType = "zy" :  Setllement Batch Total
    private HashMap<String, EcrSettlementModel> batchTotalArrays = new HashMap<>();
    public byte[] settleBatchTotal = new byte[0];
    // FieldType = "zz" :  Setllement NII
    public byte[] settleNII = new byte[0];

    public byte[] settleRespCode = new byte[2];
    public byte[] settleTermId = new byte[8];
    public byte[] settleMerId = new byte[15];
    public byte[] settleBatchNo = new byte[6];
    public byte[] settleDate = new byte[6];
    public byte[] settleTime = new byte[6];
    public byte[] settleHN = new byte[3];
    public Map<Integer, SettleHostData> settleAllHostData = new HashMap<>();
    public boolean isOnHomeScreen = false;


    /**************************************************************************************************/
    /*                                 ECR Data For PosNet Protocol                                   */
    public byte[] transAmount = new byte[12];
    /**************************************************************************************************/

    //FieldType = "30", Stan Number
    public byte[] StanNo = new byte[6];
    byte[] HolderName = new byte[22];
    byte[] POSNET_HN_NII = new byte[4];
    byte[] PosNetRespText = new byte[16];
    //FieldType = "30", Card Number
    byte[] PosNetCardNo = new byte[20];
    byte[] RabbitBalanceAmount = new byte[12];
    byte[] RabbitID = new byte[8];
    byte[] RabbitTrace = new byte[6];
    byte[] REF1 = new byte[20];
    byte[] REF2 = new byte[20];
    byte[] VatAmount = new byte[10];
    byte[] TaxAllowance = new byte[10];
    byte[] MercUniqueValue = new byte[20];
    byte[] CampaignType = new byte[6];
    private String HostID;


/**************************************************************************************************/
    /*                              Public Function                                               */

    /**************************************************************************************************/
    public void setEcrData(TransData transData, SysParam sysParam, Acquirer acquirer, ActionResult result) {

        //FieldType = "02", Response Text from host or EDC
        Arrays.fill(RespCode, (byte) 0x00);
        ResponseCode mResponseCode = transData.getResponseCode();
        if (result.getRet() == TransResult.ERR_USER_CANCEL) {
            Utils.SaveArrayCopy(new byte[]{0, 0}, 0, RespCode, 0, RespCode.length);
        } else if (mResponseCode != null) {
            Log.d("EcrData:", "ResponseText:" + mResponseCode.getCode());
            if (result.getRet() == TransResult.ERR_PROCESS_FAILED) { // push card
                Utils.SaveArrayCopy(new byte[]{0x52, 0x42}, 0, RespCode, 0, RespCode.length);
            } else {
                Utils.SaveArrayCopy(mResponseCode.getCode().getBytes(), 0, RespCode, 0, RespCode.length);
            }
        } else if (transData.getTransType() == ETransType.SETTLE && result.getRet() == TransResult.SUCC) {
            Utils.SaveArrayCopy(new byte[]{'0', '0'}, 0, RespCode, 0, RespCode.length);
        } else {
            Utils.SaveArrayCopy(new byte[]{0x52, 0x42}, 0, RespCode, 0, RespCode.length);
        }

        //FieldType = "D0", Merchant Name and Address
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        Arrays.fill(MerName, (byte) 0x20);
        String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        Arrays.fill(MerAddress, (byte) 0x20);
        String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        Arrays.fill(MerAddress1, (byte) 0x20);

        if (merName != null) {
            Log.d("EcrData:", "merName:" + merName);
            Utils.SaveArrayCopy(merName.getBytes(), 0, MerName, 0, merName.getBytes().length);
            Log.d("EcrData:", ":merNameByte:" + Convert.getInstance().bcdToStr(MerName));
        }
        if (merAddress != null) {
            Log.d("EcrData:", ":merAddress:" + merAddress);

            Utils.SaveArrayCopy(merAddress.getBytes(), 0, MerAddress, 0, merAddress.getBytes().length);
            Log.d("EcrData:", ":merAddressByte:" + Convert.getInstance().bcdToStr(MerAddress));
        }
        if (merAddress1 != null) {
            Log.d("EcrData:", "merAddress1:" + merAddress1);
            Utils.SaveArrayCopy(merAddress1.getBytes(), 0, MerAddress1, 0, merAddress1.getBytes().length);
            Log.d("EcrData:", ":merAddress1Byte:" + Convert.getInstance().bcdToStr(MerAddress1));
        }

        //FieldType = "03", Transaction Date
        Arrays.fill(DateByte, (byte) 0x20);
        String formattedDate = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, "yyMMdd");
        if (formattedDate != null) {
            Log.d("EcrData:", "formattedDate:" + formattedDate);
            Utils.SaveArrayCopy(formattedDate.getBytes(), 0, DateByte, 0, DateByte.length);
        }

        //FieldType = "04", Transaction Time
        Arrays.fill(TimeByte, (byte) 0x20);
        String formattedTime = TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, "HHmmss");
        if (formattedTime != null) {
            Log.d("EcrData:", "formattedTime:" + formattedTime);
            Utils.SaveArrayCopy(formattedTime.getBytes(), 0, TimeByte, 0, TimeByte.length);
        }

        //FieldType = "56", Approval Code
        Arrays.fill(ApprovalCode, (byte) 0x00);
        if (transData.getAuthCode() != null) {
            Log.d("EcrData:", "Approval Code:" + transData.getAuthCode());
            Utils.SaveArrayCopy(transData.getAuthCode().getBytes(), 0, ApprovalCode, 0, ApprovalCode.length);
        }

        //FieldType = "56", Invoice Number
        Arrays.fill(TraceNo, (byte) 0x30);
        String strTraceNo = String.format("%06d", transData.getTraceNo());
        Log.d("EcrData:", "Invoice/Trace Number:" + strTraceNo);
        Utils.SaveArrayCopy(strTraceNo.getBytes(), 0, TraceNo, 0, TraceNo.length);


        //FieldType = "16", Terminal Identification Number (TID)
        Arrays.fill(TermID, (byte) 0x30);
        if (acquirer.getTerminalId() != null) {
            Log.d("EcrData:", "TID :" + acquirer.getTerminalId());
            Utils.SaveArrayCopy(acquirer.getTerminalId().getBytes(), 0, TermID, 0, TermID.length);
        }

        String getIssuerName = "";
        if (transData.getIssuer() != null && transData.getIssuer().getIssuerName() != null) {
            getIssuerName = transData.getIssuer().getIssuerName();
        }

        //FieldType = "D1", Merchant Number (MID)
        Arrays.fill(MerID, (byte) 0x30);
        if (acquirer.getMerchantId() != null) {
            String merchant = getIssuerName.equals(Constants.ISSUER_DOLFIN) && acquirer.getMerchantId().length() < 15
                    ? Convert.getInstance().stringPadding(acquirer.getMerchantId(), ' ', 15, Convert.EPaddingPosition.PADDING_RIGHT)
                    : acquirer.getMerchantId();
            Log.d("EcrData:", "MID:" + merchant);
            Utils.SaveArrayCopy(merchant.getBytes(), 0, MerID, 0, MerID.length);
        }

        //FieldType = "D2", Card Issuer Name
        Arrays.fill(CardIssuerName, (byte) 0x20);
        getIssuerName = "";
        if (transData.getIssuer() != null && transData.getIssuer().getIssuerName() != null) {
            if (transData.getIssuer().getIssuerName() != null) {
                getIssuerName = transData.getIssuer().getIssuerName();
                Log.d("EcrData:", "Card Issuer Name:" + transData.getIssuer().getIssuerName());
                Utils.SaveArrayCopy(transData.getIssuer().getIssuerName().getBytes(), 0, CardIssuerName, 0, (transData.getIssuer().getIssuerName().getBytes().length > 10) ? 10 : transData.getIssuer().getIssuerName().getBytes().length);
            }
        }

        //FieldType = "30", Card Number
        Arrays.fill(HyperComCardNo, (byte) 0x30);
        if (transData.getPan() != null || transData.getQrBuyerCode() != null || transData.getRefNo() != null) {
            if (transData.getIssuer() != null) {
                String strPanMask;

                if (getIssuerName.equals(Constants.ISSUER_PROMTPAY)
                        || Constants.ACQ_QRC.equals(transData.getAcquirer().getName())) {
                    strPanMask = transData.getPan();
                } else if (getIssuerName.equals(Constants.ISSUER_WALLET)) {
                    strPanMask = transData.getQrBuyerCode();
                } else if (Constants.ISSUER_DOLFIN.equalsIgnoreCase(getIssuerName)) {
                    strPanMask = Constants.PREFIX_CARD_NO_DOLFIN + transData.getRefNo();
                } else if (Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())) {
                    strPanMask = transData.getPan() != null ? transData.getPan().replaceAll("\\*", "X") : "X"; // already masking from AMEX API
                } else {
                    strPanMask = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern(), "X");
                }
                if (strPanMask != null) {
                    HyperComCardNo = new byte[(strPanMask.getBytes().length > 19) ? 19 : strPanMask.getBytes().length];
                    Log.d("EcrData:", "Card Number:" + strPanMask);
                    Utils.SaveArrayCopy(strPanMask.getBytes(), 0, HyperComCardNo, 0, HyperComCardNo.length);
                }
            }
            else {
                String pan = transData.getPan();
                String strPanMask = null;
                if (pan!=null && pan.length() < 16) {
                    strPanMask = PanUtils.maskCardNo(pan, "(?<=\\d{6})\\d(?=\\d{3})", "X");
                }
                else if (pan!=null && pan.length() >= 16 && pan.length() <= 19) {
                    strPanMask = PanUtils.maskCardNo(pan, "(?<=\\d{6})\\d(?=\\d{4})", "X");
                }
                else {
                    if (pan!=null) {
                        strPanMask = PanUtils.maskCardNo(pan.substring(0, 19), "(?<=\\d{6})\\d(?=\\d{4})", "X");
                    }
                }

                if (strPanMask != null) {
                    HyperComCardNo = new byte[(strPanMask.getBytes().length > 19) ? 19 : strPanMask.getBytes().length];
                    Log.d("EcrData:", "Card Number:" + strPanMask);
                    Utils.SaveArrayCopy(strPanMask.getBytes(), 0, HyperComCardNo, 0, HyperComCardNo.length);
                }
            }
        }

        //FieldType = "30", Card Number - QR Trans ID
        if ((transData.getTxnNo() != null && !transData.getTxnNo().trim().isEmpty()) ||
                (transData.getWalletPartnerID() != null && !transData.getWalletPartnerID().trim().isEmpty())) {
            if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                qr_TransID = transData.getTxnNo().trim().getBytes();
            } else {
                qr_TransID = transData.getWalletPartnerID().trim().getBytes();
            }
        }

        //FieldType = "02", Response Text from host or EDC
        Arrays.fill(HyperComRespText, (byte) 0x20);
        if (transData.getField63() != null
                && !(getIssuerName.equals(Constants.ISSUER_PROMTPAY)
                || getIssuerName.equals(Constants.ISSUER_WALLET))) {
            String asciiField63 = EcrData.instance.RESTXT_REGEX_PATTERN.matcher(transData.getField63()).replaceAll(" ");
            Log.d("EcrData:", "HyperComRespText" + asciiField63);
            Utils.SaveArrayCopy(asciiField63.getBytes(), 0, HyperComRespText, 0, asciiField63.getBytes().length > 40 ? 40 : asciiField63.getBytes().length);
        }
        Log.d("EcrData:", "HyperComRespText:" + Convert.getInstance().bcdToStr(HyperComRespText));

        //FieldType = "31", Expiry Date
        Arrays.fill(ExpDate, (byte) 'X');
//        if (transData.getExpDate() != null) {
//            Log.d("EcrData:", "Expiry Date:" + transData.getExpDate());
//            Utils.SaveArrayCopy(transData.getExpDate().getBytes(),0, ExpDate, 0, transData.getExpDate().getBytes().length);
//        }

        //FieldType = "50", Batch Number
        String strBatchNo = String.format("%06d", transData.getBatchNo());
        Log.d("EcrData:", "Batch Number:" + strBatchNo);
        Utils.SaveArrayCopy(strBatchNo.getBytes(), 0, BatchNo, 0, BatchNo.length);

        //FieldType = "D3", Retrieval Reference
        Arrays.fill(RefNo, (byte) 0x20);
        if (transData.getRefNo() != null) {
            Log.d("EcrData:", "Retrieval Ref:" + transData.getRefNo());
            Utils.SaveArrayCopy(transData.getRefNo().getBytes(), 0, RefNo, 0, RefNo.length);
        }

        //FieldType = "D4", Card Issuer ID
        Arrays.fill(CardIssuerID, (byte) 0x30);
        if (transData.getIssuer() != null) {
            String strCardID = String.format("%02d", transData.getIssuer().getIssuerID());
            Log.d("EcrData:", "Card Issuer ID:" + strCardID);
            Utils.SaveArrayCopy(strCardID.getBytes(), 0, CardIssuerID, 0, CardIssuerID.length);
        }


        //FieldType = 0x48 0x4E, HN-NII
        Arrays.fill(HYPER_COM_HN_NII, (byte) 0x30);
        String strNII = String.format("%03d", Integer.parseInt(acquirer.getNii()));
        Utils.SaveArrayCopy(strNII.getBytes(), 0, HYPER_COM_HN_NII, 0, HYPER_COM_HN_NII.length);
        Log.d("EcrData:", "HYPER_COM_NII:" + strNII);


/*************************************** For POSNet Protocol *************************************/
        //FieldType Response Text from host or EDC
        Arrays.fill(PosNetRespText, (byte) 0x20);
        //String asciiField63 = "12%3sd@f;a_-'./.wj#@!#";
        //asciiField63 = EcrData.instance.RESTXT_REGEX_PATTERN.matcher(asciiField63).replaceAll(" ");
        //Utils.SaveArrayCopy(asciiField63.getBytes(), 0, PosNetRespText,0,  asciiField63.getBytes().length > PosNetRespText.length ? PosNetRespText.length : asciiField63.getBytes().length );
//        if (transData.getField63() != null) {
//            String asciiField63 = EcrData.instance.RESTXT_REGEX_PATTERN.matcher(transData.getField63()).replaceAll(" ");
//            Utils.SaveArrayCopy(asciiField63.getBytes(), 0, PosNetRespText,0,  asciiField63.getBytes().length > PosNetRespText.length ? PosNetRespText.length : asciiField63.getBytes().length );
//        }
        Log.d("EcrData:", "PosNetComRespText:" + Convert.getInstance().bcdToStr(PosNetRespText));


        //FieldType Card Number
        Arrays.fill(PosNetCardNo, (byte) 0x20);
        if (transData.getPan() != null) {
            String strPanMask;

            strPanMask = PanUtils.maskCardNo(transData.getPan(), "(?<=\\d{6})\\d(?=\\d{4})", "X");

            if (transData.getIssuer() != null || transData.getQrBuyerCode() != null || transData.getRefNo() != null) {
                if (getIssuerName.equals(Constants.ISSUER_PROMTPAY)
                        || Constants.ACQ_QRC.equals(transData.getAcquirer().getName())) {
                    strPanMask = transData.getPan();
                } else if (getIssuerName.equals(Constants.ISSUER_WALLET)) {
                    strPanMask = transData.getQrBuyerCode();
                } else if (Constants.ISSUER_DOLFIN.equalsIgnoreCase(getIssuerName)) {
                    strPanMask = Constants.PREFIX_CARD_NO_DOLFIN + transData.getRefNo();
                } else if (Constants.ACQ_AMEX.equals(transData.getAcquirer().getName())) {
                    strPanMask = transData.getPan() != null ? transData.getPan().replaceAll("\\*", "X") : "X"; // already masking from AMEX API
                } else {
                    strPanMask = PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern(), "X");
                }

                Log.d("EcrData:", "Card Number:" + strPanMask);
            }
            else {
                String pan = transData.getPan();
                if (pan !=null && pan.length() < 16) {
                    strPanMask = PanUtils.maskCardNo(pan, "(?<=\\d{6})\\d(?=\\d{3})", "X");
                }
                else if (pan!=null && pan.length() >= 16 && pan.length() <= 19) {
                    strPanMask = PanUtils.maskCardNo(pan, "(?<=\\d{6})\\d(?=\\d{4})", "X");
                }
                else {
                    if (pan!=null) {
                        strPanMask = PanUtils.maskCardNo(pan.substring(0, 19), "(?<=\\d{6})\\d(?=\\d{4})", "X");
                    }
                }
            }
            Utils.SaveArrayCopy(strPanMask.getBytes(), 0, PosNetCardNo, 0, strPanMask.getBytes().length);
        }

        //Nii
        Arrays.fill(POSNET_HN_NII, (byte) 0x30);
        strNII = String.format("%04d", Integer.parseInt(acquirer.getNii()));
        Utils.SaveArrayCopy(strNII.getBytes(), 0, POSNET_HN_NII, 0, POSNET_HN_NII.length);
        Log.d("EcrData:", "POSNET_HN_NII:" + strNII);

        //FieldType = "H1"-Batch total sales count
        String strTmp = String.format("%03d", nBatchTotalSalesCount);
        Log.d("EcrData:", "Batch total sales count:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalSalesCount, 0, BatchTotalSalesCount.length);


        //FieldType = "H2"-Batch total sales amount
        strTmp = String.format("%012d", nBatchTotalSalesAmount);
        Log.d("EcrData:", "Batch total sales amount:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalSalesAmount, 0, BatchTotalSalesAmount.length);


        //Batch total Refund count
        strTmp = String.format("%03d", nBatchTotalRefundCount);
        Log.d("EcrData:", "Batch total  Refund count:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalRefundCount, 0, BatchTotalRefundCount.length);

        //Batch total Refund Amount
        strTmp = String.format("%012d", nBatchTotalRefundAmount);
        Log.d("EcrData:", "Batch total Refund Amount:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalRefundAmount, 0, BatchTotalRefundAmount.length);

        //Stan Number
        Arrays.fill(StanNo, (byte) 0x20);
        String strStan = String.format("%06d", transData.getStanNo());
        Log.d("EcrData:", "STAN:" + strStan);
        Utils.SaveArrayCopy(strStan.getBytes(), 0, StanNo, 0, (strStan.getBytes().length > StanNo.length) ? StanNo.length : strStan.getBytes().length);

        //Holder Name
        Arrays.fill(HolderName, (byte) 0x20);
        String xHolderName = String.format("%1$10s", "").replace(' ', 'X');
        Utils.SaveArrayCopy(xHolderName.getBytes(), 0, HolderName, 0, xHolderName.getBytes().length);
        Log.d("EcrData:", "Card Holder Name:" + xHolderName);
        /*if (transData.getTrack1() != null) {
            Utils.SaveArrayCopy(transData.getTrack1().getBytes(), 0, HolderName, 0, (transData.getTrack1().getBytes().length > HolderName.length) ? HolderName.length : transData.getTrack1().getBytes().length);
        }
        Log.d("EcrData:", "Card Holder Name:" + transData.getTrack1());*/

        Arrays.fill(transAmount, (byte) 0x20);
        String amount = transData.getAmount();
        Utils.SaveArrayCopy(amount.getBytes(), 0, transAmount, 0, transAmount.length);

        Arrays.fill(padding, (byte) 0x20);
        Arrays.fill(padding2, (byte) 0x20);

    }

    public void setEcrData(Acquirer acquirer, String hostID, ActionResult result) {

        //RESPONSE CODE
        Arrays.fill(RespCode, (byte) 0x00);
        if (result.getRet() == TransResult.SUCC) {
            Utils.SaveArrayCopy(new byte[]{'0', '0'}, 0, RespCode, 0, RespCode.length);
        } else {
            Utils.SaveArrayCopy(new byte[]{0x52, 0x42}, 0, RespCode, 0, RespCode.length);
        }

        //FieldType = "02", Response Text from host or EDC
        Arrays.fill(HyperComRespText, (byte) 0x20);
        if (result.getRet() == TransResult.ERR_HOST_NOT_FOUND) {
            Utils.SaveArrayCopy(Tools.string2Bytes(TransResultUtils.getMessage(result.getRet())), 0, HyperComRespText, 0, HyperComRespText.length);
            Log.d("EcrData:", "HyperComRespText:" + Convert.getInstance().bcdToStr(HyperComRespText));
        }

        //FieldType = "D0", Merchant Name and Address
        String merName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        Arrays.fill(MerName, (byte) 0x20);
        String merAddress = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        Arrays.fill(MerAddress, (byte) 0x20);
        String merAddress1 = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        Arrays.fill(MerAddress1, (byte) 0x20);

        if (merName != null) {
            Log.d("EcrData:", "merName:" + merName);
            Utils.SaveArrayCopy(merName.getBytes(), 0, MerName, 0, merName.getBytes().length);
            Log.d("EcrData:", ":merNameByte:" + Convert.getInstance().bcdToStr(MerName));
        }
        if (merAddress != null) {
            Log.d("EcrData:", ":merAddress:" + merAddress);

            Utils.SaveArrayCopy(merAddress.getBytes(), 0, MerAddress, 0, merAddress.getBytes().length);
            Log.d("EcrData:", ":merAddressByte:" + Convert.getInstance().bcdToStr(MerAddress));
        }
        if (merAddress1 != null) {
            Log.d("EcrData:", "merAddress1:" + merAddress1);
            Utils.SaveArrayCopy(merAddress1.getBytes(), 0, MerAddress1, 0, merAddress1.getBytes().length);
            Log.d("EcrData:", ":merAddress1Byte:" + Convert.getInstance().bcdToStr(MerAddress1));
        }

        //FieldType = "16", Terminal Identification Number (TID)
        Arrays.fill(TermID, (byte) 0x30);
        if (acquirer != null && acquirer.getTerminalId() != null) {
            Log.d("EcrData:", "TID :" + acquirer.getTerminalId());
            Utils.SaveArrayCopy(acquirer.getTerminalId().getBytes(), 0, TermID, 0, TermID.length);
        }

        //FieldType = "D1", Merchant Number (MID)
        Arrays.fill(MerID, (byte) 0x30);
        if (acquirer != null && acquirer.getMerchantId() != null) {
            Log.d("EcrData:", "MID:" + acquirer.getMerchantId());
            Utils.SaveArrayCopy(acquirer.getMerchantId().getBytes(), 0, MerID, 0, MerID.length);
        }

        //FieldType = "50", Batch Number
        Arrays.fill(BatchNo, (byte) 0x30);
        if (acquirer != null) {
            String strBatchNo = String.format(Locale.getDefault(), "%06d", acquirer.getCurrBatchNo());
            Log.d("EcrData:", "Batch Number:" + strBatchNo);
            Utils.SaveArrayCopy(strBatchNo.getBytes(), 0, BatchNo, 0, BatchNo.length);
        }

        //FieldType = "03", Transaction Date
        Arrays.fill(DateByte, (byte) 0x20);
        String deviceDateTime = Device.getTime(Constants.TIME_PATTERN_DISPLAY);
        String formattedDate = nDateTime != null ? TimeConverter.convert(nDateTime, Constants.TIME_PATTERN_DISPLAY, "yyMMdd") : TimeConverter.convert(deviceDateTime, Constants.TIME_PATTERN_DISPLAY, "yyMMdd");
        if (formattedDate != null) {
            Log.d("EcrData:", "formattedDate:" + formattedDate);
            Utils.SaveArrayCopy(formattedDate.getBytes(), 0, DateByte, 0, DateByte.length);
        }

        //FieldType = "04", Transaction Time
        Arrays.fill(TimeByte, (byte) 0x20);
        String formattedTime = nDateTime != null ? TimeConverter.convert(nDateTime, Constants.TIME_PATTERN_DISPLAY, "HHmmss") : TimeConverter.convert(deviceDateTime, Constants.TIME_PATTERN_DISPLAY, "HHmmss");
        if (formattedTime != null) {
            Log.d("EcrData:", "formattedTime:" + formattedTime);
            Utils.SaveArrayCopy(formattedTime.getBytes(), 0, TimeByte, 0, TimeByte.length);
        }

        //FieldType = 0x48 0x4E, HN-NII
        Arrays.fill(HYPER_COM_HN_NII, (byte) 0x30);
        String strNII = String.format(Locale.getDefault(), "%03d", Integer.parseInt(acquirer != null ? acquirer.getNii() : hostID));
        Utils.SaveArrayCopy(strNII.getBytes(), 0, HYPER_COM_HN_NII, 0, HYPER_COM_HN_NII.length);
        Log.d("EcrData:", "HYPER_COM_NII:" + strNII);


/*************************************** For POSNet Protocol *************************************/
        //FieldType Response Text from host or EDC
        Arrays.fill(PosNetRespText, (byte) 0x20);
        //String asciiField63 = "12%3sd@f;a_-'./.wj#@!#";
        //asciiField63 = EcrData.instance.RESTXT_REGEX_PATTERN.matcher(asciiField63).replaceAll(" ");
        //Utils.SaveArrayCopy(asciiField63.getBytes(), 0, PosNetRespText,0,  asciiField63.getBytes().length > PosNetRespText.length ? PosNetRespText.length : asciiField63.getBytes().length );
//        if (transData.getField63() != null) {
//            String asciiField63 = EcrData.instance.RESTXT_REGEX_PATTERN.matcher(transData.getField63()).replaceAll(" ");
//            Utils.SaveArrayCopy(asciiField63.getBytes(), 0, PosNetRespText,0,  asciiField63.getBytes().length > PosNetRespText.length ? PosNetRespText.length : asciiField63.getBytes().length );
//        }
        Log.d("EcrData:", "PosNetComRespText:" + Convert.getInstance().bcdToStr(PosNetRespText));

        //Nii
        Arrays.fill(POSNET_HN_NII, (byte) 0x30);
        strNII = String.format(Locale.getDefault(), "%04d", Integer.parseInt(acquirer != null ? acquirer.getNii() : hostID));
        Utils.SaveArrayCopy(strNII.getBytes(), 0, POSNET_HN_NII, 0, POSNET_HN_NII.length);
        Log.d("EcrData:", "POSNET_HN_NII:" + strNII);

        //FieldType = "H1"-Batch total sales count
        String strTmp = String.format(Locale.getDefault(), "%03d", nBatchTotalSalesCount);
        Log.d("EcrData:", "Batch total sales count:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalSalesCount, 0, BatchTotalSalesCount.length);


        //FieldType = "H2"-Batch total sales amount
        strTmp = String.format(Locale.getDefault(), "%012d", nBatchTotalSalesAmount);
        Log.d("EcrData:", "Batch total sales amount:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalSalesAmount, 0, BatchTotalSalesAmount.length);


        //Batch total Refund count
        strTmp = String.format(Locale.getDefault(), "%03d", nBatchTotalRefundCount);
        Log.d("EcrData:", "Batch total  Refund count:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalRefundCount, 0, BatchTotalRefundCount.length);

        //Batch total Refund Amount
        strTmp = String.format(Locale.getDefault(), "%012d", nBatchTotalRefundAmount);
        Log.d("EcrData:", "Batch total Refund Amount:" + strTmp);
        Utils.SaveArrayCopy(strTmp.getBytes(), 0, BatchTotalRefundAmount, 0, BatchTotalRefundAmount.length);

    }

    public enum appendTotalDataMode {SETTLE, AUDITREPORT}

    public void clearAuditReportTotal() {
        if (AuditReportTotal != null) {
            AuditReportTotal = null;
        }
    }

    public void appendAuditReportTotalData(EcrAuditReportPrintingModel extData, boolean autoAddExtraLinePosData) {
        if (extData != null) {
            if (AuditReportTotal == null) {
                AuditReportTotal = extData;
            } else {
                // update to Audit ReportTotal Data
                AuditReportTotal.captureSaleCount += extData.captureSaleCount;
                AuditReportTotal.captureSaleAmount += extData.captureSaleAmount;
                AuditReportTotal.captureRefundCount += extData.captureRefundCount;
                AuditReportTotal.captureRefundAmount += extData.captureRefundAmount;

                AuditReportTotal.debitSaleCount += extData.debitSaleCount;
                AuditReportTotal.debitSaleAmount += extData.debitSaleAmount;
                AuditReportTotal.debitRefundCount += extData.debitRefundCount;
                AuditReportTotal.debitRefundAmount += extData.debitRefundAmount;

                AuditReportTotal.AuthorizeSaleCount += extData.AuthorizeSaleCount;
                AuditReportTotal.AuthorizeSaleAmount += extData.AuthorizeSaleAmount;
                AuditReportTotal.AuthorizeRefundCount += extData.AuthorizeRefundCount;
                AuditReportTotal.AuthorizeRefundAmount += extData.AuthorizeRefundAmount;
            }

            ByteArrayOutputStream bAOS_Data_Tmp = new ByteArrayOutputStream();
            try {
                // Field - HO :  capture sale count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.captureSaleCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  capture sale Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.captureSaleAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  capture Refund count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.captureRefundCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  capture Refund Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.captureRefundAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());

                // Field - HO :  Debit sale count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.debitSaleCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Debit sale Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.debitSaleAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Debit Refund count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.debitRefundCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Debit Refund Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.debitRefundAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());

                // Field - HO :  Authorize sale count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.AuthorizeSaleCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Authorize sale Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.AuthorizeSaleAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Authorize Refund count
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.AuthorizeRefundCount, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                // Field - HO :  Authorize Refund Amount
                bAOS_Data_Tmp.write(Utils.getStringPadding(AuditReportTotal.AuthorizeRefundAmount, 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());

                if (autoAddExtraLinePosData) {
                    // Field - ZY :  unidentified data
                    bAOS_Data_Tmp.write(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30, 0x30});
                    bAOS_Data_Tmp.write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
                }

                if (bAOS_Data_Tmp.toByteArray().length == (90 + (autoAddExtraLinePosData ? 30 : 0))) {
                    // set AuditReport BatchTotal data
                    singleBatchTotal = bAOS_Data_Tmp.toByteArray();
                    singleHostNII = HyperComMsg.instance.data_field_HN_nii.getBytes();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.e("EcrData", "error on convert data EcrSettlementModel to byteArray");
            }
        }
    }

    private void checkDisableSettlementHost(int returnHostMaxLen) {
        String paddedHostIndexStr = "";
        boolean isUnidentifiedHost = false;
        for (int index = 1; index <= returnHostMaxLen; index++) {
            isUnidentifiedHost = false;
            paddedHostIndexStr = Utils.getStringPadding(index, 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
            if ((index >= 1 && index <= 6) || (index >= 10 && index < returnHostMaxLen)) {
                String hostName = LawsonHyperCommClass.getLawsonHostName(paddedHostIndexStr);
                Acquirer acquirer = FinancialApplication.getAcqManager().findActiveAcquirer(hostName);
                if (acquirer == null || !acquirer.isEnable()) {
                    isUnidentifiedHost = true;
                }
            } else {
                if (!batchTotalArrays.containsKey(paddedHostIndexStr)) {
                    isUnidentifiedHost = true;
                }
            }

            if (isUnidentifiedHost) {
                batchTotalArrays.put(paddedHostIndexStr, getDummySettlementModelForUnsupportedHost(paddedHostIndexStr));
            }
        }
    }


    public void clearSettlementTotalList() {
        if (batchTotalArrays != null) {
            batchTotalArrays.clear();
        }
        EcrData.instance.settleBatchTotal = new byte[0];
        EcrData.instance.settleNII = new byte[0];
    }

    public void appendSettlementData(EcrSettlementModel settData, boolean autoAddExtraLinkPosData) {
        // instant check
        if (batchTotalArrays == null) {
            batchTotalArrays = new HashMap<>();
        }
        checkDisableSettlementHost(13);

        // SettlementDataModel null check
        if (settData != null) {
            String tmpHostIndex = LawsonHyperCommClass.getLawsonHostIndex(settData._acquirer); //Utils.StringPadding(BatchTotalArrays.size(), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT) ;
            if (tmpHostIndex == null) {
                return;
            }

            settData.HostIndex = tmpHostIndex;
            batchTotalArrays.put(settData.HostIndex, settData);
        }
        // re-constuct output
        if (batchTotalArrays.size() > 0) {
            HashMap<String, byte[]> settleBatchTotal = new HashMap<>();
            ByteArrayOutputStream bAOS_Data_Raw = new ByteArrayOutputStream();
            ByteArrayOutputStream bAOS_Data_Tmp = new ByteArrayOutputStream();
            ByteArrayOutputStream bAOS_NII_Raw = new ByteArrayOutputStream();
            ByteArrayOutputStream bAOS_NII_Tmp = new ByteArrayOutputStream();

            //String[] keys =(String[]) BatchTotalArrays.keySet().toArray();
            String[] keys = new String[batchTotalArrays.size()];
            int index = 0;
            for (String localKey : batchTotalArrays.keySet()) {
                keys[index] = localKey;
                index++;
            }

            Arrays.sort(keys);
            for (String key : keys) {
                try {
                    bAOS_Data_Tmp.reset();
                    bAOS_NII_Tmp.reset();
                    // ZY - Field : Heading Host Index
                    bAOS_Data_Tmp.write(String.format("H%s", Utils.getStringPadding(batchTotalArrays.get(key).HostIndex, 3, "0", Convert.EPaddingPosition.PADDING_LEFT)).getBytes());
                    // ZY - Field : Heading Merchant Number
                    bAOS_Data_Tmp.write(String.format("M%s", Utils.getStringPadding(HyperComMsg.instance.data_field_45_merchant_number, 3, "0", Convert.EPaddingPosition.PADDING_LEFT)).getBytes());

                    // Field - ZY :  capture sale count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).captureSaleCount);
                    // Field - ZY :  capture sale Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).captureSaleAmount);
                    // Field - ZY :  capture Refund count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).captureRefundCount);
                    // Field - ZY :  capture Refund Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).captureRefundAmount);

                    // Field - ZY :  Debit sale count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).debitSaleCount);
                    // Field - ZY :  Debit sale Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).debitSaleAmount);
                    // Field - ZY :  Debit Refund count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).debitRefundCount);
                    // Field - ZY :  Debit Refund Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).debitRefundAmount);

                    // Field - ZY :  Authorize sale count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).AuthorizeSaleCount);
                    // Field - ZY :  Authorize sale Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).AuthorizeSaleAmount);
                    // Field - ZY :  Authorize Refund count
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).AuthorizeRefundCount);
                    // Field - ZY :  Authorize Refund Amount
                    bAOS_Data_Tmp.write(batchTotalArrays.get(key).AuthorizeRefundAmount);

                    if (autoAddExtraLinkPosData) {
                        // Field - ZY :  unidentified data
                        bAOS_Data_Tmp.write(new byte[]{0x30, 0x30, 0x30, 0x30, 0x30, 0x30});
                        bAOS_Data_Tmp.write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
                    }


                    // Field - ZZ :  Host Index
                    bAOS_NII_Tmp.write(Utils.getStringPadding(batchTotalArrays.get(key).HostIndex, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                    // Field - ZZ :  Host NII
                    bAOS_NII_Tmp.write(Utils.getStringPadding(batchTotalArrays.get(key).HostNII, 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes());
                    // Field - ZZ :  Host Settlement Response
                    bAOS_NII_Tmp.write(batchTotalArrays.get(key).HostSettlementResult);


                    if (bAOS_Data_Tmp.toByteArray().length == 128 && bAOS_NII_Tmp.toByteArray().length == 8) {
                        // set Settlement BatchTotal data
                        bAOS_Data_Raw.write(bAOS_Data_Tmp.toByteArray());
                        // Host NII
                        bAOS_NII_Raw.write(bAOS_NII_Tmp.toByteArray());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e("EcrData", "error on convert data EcrSettlementModel to byteArray");
                }
            }

            this.settleBatchTotal = bAOS_Data_Raw.toByteArray();
            settleNII = bAOS_NII_Raw.toByteArray();
        }
    }



    public static final byte[] SETTLE_SKIPPED = new byte[]{'N', 'A'};
    public static final byte[] SETTLE_SUCCESS = new byte[]{'0', '0'};
    public static final byte[] SETTLE_UNSUCCESS = new byte[]{'E', 'R'};

    public static void createSettlementEcrResponse(TransTotal tmptotal, byte[] settleResult) {
        if (EcrData.instance.isOnProcessing) {
            if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                try {
                    EcrSettlementModel model = new EcrSettlementModel();
                    String saleNum = (tmptotal != null) ? getPaddedNumber(tmptotal.getSaleTotalNum(), 3) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String saleAmt = (tmptotal != null) ? getPaddedNumber(tmptotal.getSaleTotalAmt(), 12) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String refundNum = (tmptotal != null) ? getPaddedNumber(tmptotal.getRefundTotalNum(), 3) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String refundAmt = (tmptotal != null) ? getPaddedNumber(tmptotal.getRefundTotalAmt(), 12) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);

                    model._acquirer = tmptotal.getAcquirer();
                    model.HostIndex = null;
                    model.HostNII = Utils.getStringPadding(tmptotal.getAcquirer().getNii(), 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    model.HostSettlementResult = settleResult;

                    model.captureSaleCount = saleNum.getBytes();
                    model.captureSaleAmount = saleAmt.getBytes();
                    model.captureRefundCount = refundNum.getBytes();
                    model.captureRefundAmount = refundAmt.getBytes();

                    model.debitSaleCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.debitSaleAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.debitRefundCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.debitRefundAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();

                    model.AuthorizeSaleCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.AuthorizeSaleAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.AuthorizeRefundCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
                    model.AuthorizeRefundAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();


                    EcrData.instance.appendSettlementData(model, true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e("SETT", "Error during create settlement ECR response for Lawson Protocol");
                }
            } else {
                Acquirer acquirer = tmptotal.getAcquirer();
                Map<String, Integer> mapHostIndex = LinkPOSMessage.Companion.getHostIndexByMerchant();
                if (mapHostIndex.containsKey(acquirer.getName())) {
                    Integer hostIndex = mapHostIndex.get(acquirer.getName());
                    if (hostIndex != null) {
                        if (EcrData.instance.settleAllHostData == null) {
                            EcrData.instance.settleAllHostData = new HashMap<>();
                        }
                        EcrData.instance.settleAllHostData.put(hostIndex, new SettleHostData(tmptotal, settleResult));
                    }
                }
            }
        }
    }

    public static void createAuditReportEcrResponse(TransTotal tmptotal) {
        if (EcrData.instance.isOnProcessing) {
            if (FinancialApplication.getEcrProcess().mHyperComm instanceof LawsonHyperCommClass) {
                try {
                    EcrAuditReportPrintingModel model = new EcrAuditReportPrintingModel();
                    String saleNum = (tmptotal != null) ? getPaddedNumber(tmptotal.getSaleTotalNum(), 3) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String saleAmt = (tmptotal != null) ? getPaddedNumber(tmptotal.getSaleTotalAmt(), 12) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String refundNum = (tmptotal != null) ? getPaddedNumber(tmptotal.getRefundTotalNum(), 3) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
                    String refundAmt = (tmptotal != null) ? getPaddedNumber(tmptotal.getRefundTotalAmt(), 12) : Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);

                    model.captureSaleCount = Long.parseLong(saleNum);
                    model.captureSaleAmount = Long.parseLong(saleAmt);
                    model.captureRefundCount = Long.parseLong(refundNum);
                    model.captureRefundAmount = Long.parseLong(refundAmt);

                    model.debitSaleCount = 0;
                    model.debitSaleAmount = 0;
                    model.debitRefundCount = 0;
                    model.debitRefundAmount = 0;

                    model.AuthorizeSaleCount = 0;
                    model.AuthorizeSaleAmount = 0;
                    model.AuthorizeRefundCount = 0;
                    model.AuthorizeRefundAmount = 0;

                    EcrData.instance.appendAuditReportTotalData(model, true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e("AUDITREPORT", "Error during create AuditReportTotalD ECR response for Lawson Protocol");
                }
            }
        }
    }


    private EcrSettlementModel getDummySettlementModelForUnsupportedHost(String hostIndex) {
        if (hostIndex == null) {
            return null;
        }
        EcrSettlementModel model = new EcrSettlementModel();
        model._acquirer = null;
        model.HostIndex = hostIndex;
        model.HostNII = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT);
        model.HostSettlementResult = EcrData.SETTLE_SKIPPED;

        model.captureSaleCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.captureSaleAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.captureRefundCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.captureRefundAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();

        model.debitSaleCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.debitSaleAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.debitRefundCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.debitRefundAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();

        model.AuthorizeSaleCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.AuthorizeSaleAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.AuthorizeRefundCount = Utils.getStringPadding("", 3, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();
        model.AuthorizeRefundAmount = Utils.getStringPadding("", 12, "0", Convert.EPaddingPosition.PADDING_LEFT).getBytes();

        return model;
    }

    public void setSettleSingleHostTotalData(TransTotal total, String respCode) {
        String saleNum = getPaddedNumber(total.getSaleTotalNum(), 3);
        String saleAmt = getPaddedNumber(total.getSaleTotalAmt(), 12);
        String refundNum = getPaddedNumber(total.getRefundTotalNum(), 3);
        String refundAmt = getPaddedNumber(total.getRefundTotalAmt(), 12);
        String batchTotal = saleNum + saleAmt + refundNum + refundAmt;
        batchTotal = getPaddedStringRight(batchTotal, 96, '0');

        //FieldType = "00", Resp Code
        Arrays.fill(settleRespCode, (byte) 0x00);
        respCode = respCode != null ? respCode : "ND";
        Log.d("EcrData:", "Settle Resp Code :" + respCode);
        Utils.SaveArrayCopy(respCode.getBytes(), 0, settleRespCode, 0, settleRespCode.length);

        //FieldType = "HO", Batch Totals
        Log.d("EcrData:", "Batch Totals :" + batchTotal);
        singleBatchTotal = batchTotal.getBytes();

        Acquirer acquirer = total.getAcquirer();
        if (acquirer != null) {
            //FieldType = "16", Terminal Identification Number (TID)
            Arrays.fill(settleTermId, (byte) 0x30);
            if (acquirer.getTerminalId() != null) {
                Log.d("EcrData:", "TID :" + acquirer.getTerminalId());
                Utils.SaveArrayCopy(acquirer.getTerminalId().getBytes(), 0, settleTermId, 0, settleTermId.length);
            }

            //FieldType = "D1", Merchant Number (MID)
            Arrays.fill(settleMerId, (byte) 0x30);
            if (acquirer.getMerchantId() != null) {
                Log.d("EcrData:", "MID:" + acquirer.getMerchantId());
                Utils.SaveArrayCopy(acquirer.getMerchantId().getBytes(), 0, settleMerId, 0, settleMerId.length);
            }

            //FieldType = "50", Batch Number
            Arrays.fill(settleBatchNo, (byte) 0x30);
            String strBatchNo = String.format(Locale.getDefault(), "%06d", acquirer.getCurrBatchNo());
            Log.d("EcrData:", "Batch Number:" + strBatchNo);
            Utils.SaveArrayCopy(strBatchNo.getBytes(), 0, settleBatchNo, 0, settleBatchNo.length);

            //FieldType = 0x48 0x4E, HN-NII
            Arrays.fill(settleHN, (byte) 0x30);
            String strNII = String.format(Locale.getDefault(), "%03d", Integer.parseInt(acquirer.getNii()));
            Utils.SaveArrayCopy(strNII.getBytes(), 0, settleHN, 0, settleHN.length);
            Log.d("EcrData:", "HYPER_COM_NII:" + strNII);
        }

        //FieldType = "03", Transaction Date
        Arrays.fill(settleDate, (byte) 0x20);
        String deviceDateTime = total.getDateTime();
        if (deviceDateTime != null) {
            String formattedDate = TimeConverter.convert(deviceDateTime, Constants.TIME_PATTERN_DISPLAY, "yyMMdd");
            Log.d("EcrData:", "formattedDate:" + formattedDate);
            Utils.SaveArrayCopy(formattedDate.getBytes(), 0, settleDate, 0, settleDate.length);
        }

        //FieldType = "04", Transaction Time
        Arrays.fill(settleTime, (byte) 0x20);
        if (deviceDateTime != null) {
            String formattedTime = TimeConverter.convert(deviceDateTime, Constants.TIME_PATTERN_DISPLAY, "HHmmss");
            Log.d("EcrData:", "formattedTime:" + formattedTime);
            Utils.SaveArrayCopy(formattedTime.getBytes(), 0, settleTime, 0, settleTime.length);
        }
    }

}
