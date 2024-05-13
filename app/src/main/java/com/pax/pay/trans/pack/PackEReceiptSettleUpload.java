package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.dal.entity.ETermInfoKey;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEReceiptInfoUpload;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransRedeemKbankTotal;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.BitmapImageConverterUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.EReceiptUtils.BOL_Options;
import com.pax.pay.utils.EReceiptUtils.ConcatModes;
import com.pax.pay.utils.EReceiptUtils.EOL_Options;
import com.pax.pay.utils.EReceiptUtils.TextAlignment;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

import static com.pax.pay.trans.model.TransData.ETransStatus.ADJUSTED;
import static com.pax.pay.trans.model.TransData.ETransStatus.NORMAL;
import static com.pax.pay.trans.model.TransData.ETransStatus.VOIDED;

public class PackEReceiptSettleUpload extends PackEReceiptUpload {
    public PackEReceiptSettleUpload(PackListener listener) {
        super(listener);
    }

    private HashMap<Integer, Object> hash_eSettleReportFile = null;
    private boolean isCanReadSettleReport=false;

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            //todo check session key
            if (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ENC_DATA) == null ||
                    FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ORI_DATA) == null ||
                    transData.getERCMBankCode() == null ||
                    FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE)==false) {
                return "".getBytes();
            }

            if(transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && transData.geteReceiptUploadSourcePath() != null) {
                isCanReadSettleReport = true;
                hash_eSettleReportFile = new HashMap<Integer, Object>();
                String fName = transData.geteReceiptUploadSourcePath();
                int iPos = fName.indexOf("-");
                int Len = -1;
                int reerPos=0;
                int MercLen = 15;
                fName = fName.substring(iPos).replace("-","");
                Len=  3      ; String header = fName.substring(reerPos , reerPos + Len); reerPos+=Len ;
                if(header.substring(0,1).equals("1")) {MercLen=Integer.parseInt(header.substring(1,3));} else {MercLen=15;}
                Len=  3      ; hash_eSettleReportFile.put(24 , fName.substring(reerPos , reerPos + Len)); reerPos+=Len ;
                Len= 12      ; hash_eSettleReportFile.put(37 , fName.substring(reerPos , reerPos + Len)); reerPos+=Len ;
                Len=  8      ; hash_eSettleReportFile.put(41 , fName.substring(reerPos , reerPos + Len)); reerPos+=Len ;
                Len= MercLen ; hash_eSettleReportFile.put(42 , fName.substring(reerPos , reerPos + Len)); reerPos+=Len ;
                Len=  6      ; hash_eSettleReportFile.put(60 , fName.substring(reerPos , reerPos + Len)); reerPos+=Len ;
                hash_eSettleReportFile.put(61 ,EReceiptUtils.getUnSettleRawData(transData.geteReceiptUploadSourcePath()));
            } else if(transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && transData.geteReceiptUploadSourcePath() == null)  {
                return "".getBytes();
            }

            setMandatoryData(transData);

            return packERMTLEDData(transData);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        entity.setFieldValue("m", transData.getTransType().getMsgType());

        setBitData11(transData);
        setBitData12(transData);                               //12-13

        transData.setNii(transData.getInitAcquirerIndex());    //use external acquirer index to send as acquirer_id on ERCM host
        setBitData24(transData);

        //transData.setRefNo("000000000000");
        setBitData37(transData);//todo use ref no. from???

        setBitData41(transData);                                // override - Bit41
        setBitData42(transData);                                // override - Bit42

        setBitData46(transData);
        setBitData59(transData);//todo Payment media???
        setBitData60(transData);
        setBitData61Byte(transData);//todo Receipt image format

        setBitData63Byte(transData);


    }


    protected void setBitData24(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("24", (String)hash_eSettleReportFile.get(24));
        } else {
            setBitData("24", transData.getNii());
        }
    }
    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("37",(String)hash_eSettleReportFile.get(37));
        } else {
            setBitData("37", transData.getRefNo());
        }
    }
    @Override
    protected void setBitData41(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("41",(String)hash_eSettleReportFile.get(41));
        } else {
            Acquirer uploadAcquirer = FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName());
            setBitData("41", uploadAcquirer.getTerminalId());//todo Payment method
        }
    }
    @Override
    protected void setBitData42(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("42",(String)hash_eSettleReportFile.get(42));
        } else {
            Acquirer uploadAcquirer = FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName());
            setBitData("42", uploadAcquirer.getMerchantId());//todo Payment method
        }
    }
    @Override
    protected void setBitData59(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("59", "SETTLEMENT".getBytes());//todo Payment method
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {

        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("60",(String)hash_eSettleReportFile.get(60));
        } else {
            setBitData("60", Component.getPaddedNumber(transData.getAcquirer().getCurrBatchNo(), 6));
        }
    }

    @Override
    protected void setBitData61Byte(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.geteReceiptUploadSource() == TransData.SettleUploadSource.FROM_FILE && isCanReadSettleReport) {
            setBitData("61",(byte[])hash_eSettleReportFile.get(61));
        } else {
            setBitData("61", generateReceiptFormat(transData));

        }
    }

    @Override
    protected void setBitData63Byte(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", super.generateERTable(transData));
    }

    public byte[] generateReceiptFormat(TransData transData) {
        String SettlmentSlipData = PageToSlipFormat.getInstance().getSlipFormat();
        String[] tm_edc_appver   =  EReceiptUtils.TextSplitter(Utils.getTerminalAndAppVersion(),EReceiptUtils.MAX43_CHAR_PER_LINE);
        SettlmentSlipData = Tools.bcd2Str((EReceiptUtils.StringPadding(transData.getInitAcquirerIndex(),3,"0", Convert.EPaddingPosition.PADDING_LEFT) + new String(new byte[] {0x0D})).getBytes()) + SettlmentSlipData ;
        //SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0A, 0x0A, (byte)0xCE},Utils.getTerminalAndAppVersion().getBytes()));
        String lStr01 = SettlmentSlipData.substring(SettlmentSlipData.length()-4, SettlmentSlipData.length());
        String lStr02 = SettlmentSlipData.substring(SettlmentSlipData.length()-2, SettlmentSlipData.length());
        if (lStr01.equals("0D0A") || lStr02.equals("0A")) { SettlmentSlipData += "CE"; }

        int left=0;
        if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
            left = (EReceiptUtils.MAX43_CHAR_PER_LINE - tm_edc_appver[0].length()) / 2 ;
            SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0D, 0x0A, (byte)0xCE},(EReceiptUtils.StringPadding(tm_edc_appver[0], left+tm_edc_appver[0].length()," ", Convert.EPaddingPosition.PADDING_LEFT)).getBytes()));
            SettlmentSlipData += Tools.bcd2Str(new byte[]{0x0D, 0x0A});
        } else {
            for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                    SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0D, 0x0A, (byte)0xCE},(EReceiptUtils.StringPadding(tm_edc_appver[idx], left+tm_edc_appver[idx].length()," ", Convert.EPaddingPosition.PADDING_LEFT)).getBytes()));
                }
            }
            SettlmentSlipData += Tools.bcd2Str(new byte[]{0x0D, 0x0A});
        }

        byte[] tmp_uncompress_de61 = Tools.str2Bcd(SettlmentSlipData);
        BitmapImageConverterUtils.saveDataToFile(tmp_uncompress_de61,"/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/settle.slp");

        transData.seteSlipFormat(tmp_uncompress_de61);
        byte[] compressedData = gZipCompress(transData.geteSlipFormat());
        BitmapImageConverterUtils.saveDataToFile(compressedData,"/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/settle.gz");
        return compressedData;
    }//transData.seteSlipFormat(Tools.str2Bcd(slipData));

    public byte[] generateReceiptFormat(String InitAcquirerIndex) {
        String SettlmentSlipData = PageToSlipFormat.getInstance().getSlipFormat();
        String[] tm_edc_appver   =  EReceiptUtils.TextSplitter(Utils.getTerminalAndAppVersion(),EReceiptUtils.MAX43_CHAR_PER_LINE);
        SettlmentSlipData = Tools.bcd2Str((InitAcquirerIndex + new String(new byte[] {0x0D})).getBytes()) + SettlmentSlipData ;
        //SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0A, 0x0A, (byte)0xCE},Utils.getTerminalAndAppVersion().getBytes()));
        String lStr01 = SettlmentSlipData.substring(SettlmentSlipData.length()-4, SettlmentSlipData.length());
        String lStr02 = SettlmentSlipData.substring(SettlmentSlipData.length()-2, SettlmentSlipData.length());
        if (lStr01.equals("0D0A") || lStr02.equals("0A")) { SettlmentSlipData += "CE"; }

        int left=0;
        if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
            left = (EReceiptUtils.MAX43_CHAR_PER_LINE - tm_edc_appver[0].length()) / 2 ;
            SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0D, 0x0A, (byte)0xCE},(EReceiptUtils.StringPadding(tm_edc_appver[0], left+tm_edc_appver[0].length()," ", Convert.EPaddingPosition.PADDING_LEFT)).getBytes()));
            SettlmentSlipData += Tools.bcd2Str(new byte[]{0x0D, 0x0A});
        } else {
            for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                    SettlmentSlipData += Tools.bcd2Str(EReceiptUtils.getInstance().MergeArray(new byte[]{0x0D, 0x0A, (byte)0xCE},(EReceiptUtils.StringPadding(tm_edc_appver[idx], left+tm_edc_appver[idx].length()," ", Convert.EPaddingPosition.PADDING_LEFT)).getBytes()));
                }
            }
            SettlmentSlipData += Tools.bcd2Str(new byte[]{0x0D, 0x0A});
        }

        byte[] tmp_uncompress_de61 = Tools.str2Bcd(SettlmentSlipData);
        byte[] compressedData = gZipCompress(tmp_uncompress_de61);
        BitmapImageConverterUtils.saveDataToFile(tmp_uncompress_de61,"/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/settle.slp");
        BitmapImageConverterUtils.saveDataToFile(compressedData,     "/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/settle.gz");

        return compressedData;
    }

    protected String CreateSettlementEReceiptFormat__TOPS__(TransData transData) {
        StringBuilder settleSlipData = new StringBuilder();

        // Terminal information (TM)
        Acquirer acquirer           =  FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName());
        String tm_logo_index        =  EReceiptUtils.StringPadding(String.valueOf( acquirer.getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT);
        String tm_addr_ln_01        =  FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String tm_addr_ln_02        =  FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String tm_addr_ln_03        =  FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        String tm_host__name        =  acquirer.getName();
        String tm_terminalID        =  acquirer.getTerminalId();
        String tm_merchantID        =  acquirer.getMerchantId();
        String tm_edc_appver        =  FinancialApplication.getDal().getSys().getTermInfo().get(ETermInfoKey.MODEL) + " " + FinancialApplication.getVersion();

        // Separater (sp)
        String sp_01_dash_ln        = EReceiptUtils.StringPadding("",23,"-", Convert.EPaddingPosition.PADDING_LEFT);
        String sp_02_dash_ln        = EReceiptUtils.StringPadding("",41,"_", Convert.EPaddingPosition.PADDING_LEFT);
        String sp_03_eqal_ln        = EReceiptUtils.StringPadding("",23,"=", Convert.EPaddingPosition.PADDING_LEFT);
        String sp_04_chds_ln        = "----------- CARDHOLDER SIGNATURE ----------";
        String sp_05_dash_ln        = EReceiptUtils.StringPadding("",46,"-", Convert.EPaddingPosition.PADDING_LEFT);
        String sp_06_eqal_ln        = EReceiptUtils.StringPadding("",46,"=", Convert.EPaddingPosition.PADDING_LEFT);

        // Settlement (st)
        TransTotal total            = transData.getSettlementTransTotal();
        String st_batch__no         = EReceiptUtils.StringPadding(String.valueOf(total.getBatchNo()),6,"0", Convert.EPaddingPosition.PADDING_LEFT) ;
        String st_txn_title         = "SETTLEMENT REPORT" ;
        String st__dateTime         = total.getDateTime();
        String st_txn__date         = TimeConverter.convert(st__dateTime, Constants.TIME_PATTERN_TRANS, Constants.DATE_PATTERN_DISPLAY);
        String st_txn__time         = TimeConverter.convert(st__dateTime, Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY4);

        String st_sale_nb_txn       = Component.getPaddedNumber((total.getSaleTotalNum()),3);
        String st_rfnd_nb_txn       = Component.getPaddedNumber((total.getRefundTotalNum()),3);
        String st_void_sale_nb_txn  = Component.getPaddedNumber((total.getVoidTotalNum()),3);
        String st_void_refund_nb_txn= Component.getPaddedNumber((total.getRefundVoidTotalNum()),3);
        String st_sale__amt         = CurrencyConverter.convert(total.getSaleTotalAmt());
        String st_refund_amt        = ((total.getRefundTotalAmt() != 0.00) ? "- " : "" ) + CurrencyConverter.convert(total.getRefundTotalAmt());
        String st_void_sale_amt     = CurrencyConverter.convert(0 - total.getVoidTotalAmt()); //CurrencyConverter.convert(0 - total.getSaleVoidTotalAmt())
        String st_void_refund_amt   = CurrencyConverter.convert(total.getRefundVoidTotalAmt());

        String st_total_nb_txn      = Component.getPaddedNumber(total.getSaleTotalNum() + total.getRefundTotalNum() + total.getTopupTotalNum(),3);
        String st_total_amt         = CurrencyConverter.convert(total.getSaleTotalAmt() - total.getRefundTotalAmt() + total.getTopupTotalAmt());

        String SettleSlipInfos ="";
        EReceiptUtils instant = EReceiptUtils.getInstance();

        if (! (transData.getSettlementTransTotal().getAcquirer().getName().equals(Constants.ACQ_REDEEM) ||
                transData.getSettlementTransTotal().getAcquirer().getName().equals(Constants.ACQ_REDEEM_BDMS))) {
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_logo_index    , BOL_Options.None,             EOL_Options.AddCarriageReturn, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("HOST  : " + tm_host__name , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("TID   : " + tm_terminalID, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("MID   : " + tm_merchantID, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( st_txn_title      , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("BATCH : " +  st_batch__no, "CLOSED" , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( st_txn__date , st_txn__time, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("TOTALS-DEBIT/CR " , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(instant.AutoAddSpacebetween2String("SALE",         st_sale_nb_txn, 24)  ,          st_sale__amt, ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(instant.AutoAddSpacebetween2String("REFUND",       st_rfnd_nb_txn, 24) ,           st_refund_amt, ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(instant.AutoAddSpacebetween2String("VOID SALE",    st_void_sale_nb_txn, 24) ,      st_void_sale_amt, ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(instant.AutoAddSpacebetween2String("VOID REFUND",  st_void_refund_nb_txn, 24) ,    st_void_refund_amt, ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("GRAND TOTAL " , st_total_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "" , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "TOTALS BY CARD" , BOL_Options.NormalSizeWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "" , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
        }
        else if (transData.getSettlementTransTotal().getAcquirer().getName().equals(Constants.ACQ_REDEEM)){
            TransRedeemKbankTotal redeemTotal = total.getTransRedeemKbankTotal();
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_logo_index    , BOL_Options.None,             EOL_Options.AddCarriageReturn, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth,  EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("HOST  : " + tm_host__name , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("TID   : " + tm_terminalID, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("MID   : " + tm_merchantID, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( st_txn_title      , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("BATCH : " +  st_batch__no, "CLOSED" , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( st_txn__date , st_txn__time, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);

            // Product
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "PRODUCT"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getProductVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getProductMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getProductJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getProductOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getProductAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getProductItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getProductPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getProductRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getProductTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // Product + Credit
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "PRODUCT + Credit"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getProductCreditVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getProductCreditMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getProductCreditJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getProductCreditOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getProductCreditAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getProductCreditItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getProductCreditPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getProductCreditRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "CREDIT BHT" , String.valueOf(redeemTotal.getProductCreditCredit()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getProductCreditTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // VOUCHER
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "VOUCHER"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getVoucherVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getVoucherMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getVoucherJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getVoucherOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getVoucherAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getVoucherItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getVoucherPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getVoucherRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getVoucherTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // VOUCHER + CREDIT
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "VOUCHER + CREDIT"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getVoucherCreditVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getVoucherCreditMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getVoucherCreditJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getVoucherCreditOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getVoucherCreditAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getVoucherCreditItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getVoucherCreditPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getVoucherCreditRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "CREDIT BHT" , String.valueOf(redeemTotal.getVoucherCreditCredit()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getVoucherCreditTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // DISCOUNT
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "DISCOUNT"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getDiscountVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getDiscountMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getDiscountJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getDiscountOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getDiscountAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getDiscountItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getDiscountPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getDiscountRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getDiscountTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // DISCOUNT + CREDIT
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "DISCOUNT + CREDIT"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getDiscountVisa()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getDiscountMastercard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getDiscountJcb()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getDiscountOther()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getDiscountAllCard()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getDiscountItems()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getDiscountPoints()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getDiscountRedeem()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "CREDIT BHT" , String.valueOf(redeemTotal.getDiscountCredit()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getDiscountTotal()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            // TOTAL
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "REDEEM GRAND TOTAL"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.Center);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "VISA" , String.valueOf(redeemTotal.getVisaSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "MASTERCARD" , String.valueOf(redeemTotal.getMastercardSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "JCB" , String.valueOf(redeemTotal.getJcbSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "OTHER" , String.valueOf(redeemTotal.getOtherCardSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ALL CARD" , String.valueOf(redeemTotal.getAllCardsSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent("",          BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "ITEMS" , String.valueOf(redeemTotal.getItemSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "POINTS" , String.valueOf(redeemTotal.getPointsSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "REDEEM BHT" , String.valueOf(redeemTotal.getRedeemAmtSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "CREDIT BHT" , String.valueOf(redeemTotal.getCreditSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL BHT" , String.valueOf(redeemTotal.getTotalSum()), ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "*** SETTLEMENT ***"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  "CLOSED SUCCESSFULLY"    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth,    EOL_Options.AddNewLineFeed, TextAlignment.None);
        }

        //Log.d(EReceiptUtils.TAG, "[" + acquirer.getName() + "] HEADER SETTLEMENT REPORT : " + settleSlipData.toString());

        List<TransData.ETransStatus> filter = new ArrayList<>();
        filter.add(NORMAL);
        filter.add(ADJUSTED);
        List<Issuer> listIssuers = FinancialApplication.getAcqManager().findAllIssuers();
        List<Issuer> issuers = filterIssuers(listIssuers, filter, acquirer);
        //settleSlipData.append(generateTotalByCards(acquirer, issuers, filter));

        //EReceiptUtils instant = EReceiptUtils.getInstance();

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

        for (int isu_idx = 0; isu_idx < listIssuers.size() ; isu_idx ++ ){
            Issuer issuer = listIssuers.get(isu_idx);
            tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( issuer.getIssuerName() ,                          BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( issuer.getIssuerName() + "SUMMARY" ,      BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

                long[] tempObj1 = new long[2];
                if(acquirer.getName().equals(Constants.ACQ_KPLUS)){
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                    tempObjSmallAmt= FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false, true);
                } else if(acquirer.getName().equals(Constants.ACQ_ALIPAY)){
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false,true);
                }else if(acquirer.getName().equals(Constants.ACQ_WECHAT)){
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false,true);
                }else if(acquirer.getName().equals(Constants.ACQ_SMRTPAY) || acquirer.getName().equals(Constants.ACQ_SMRTPAY_BDMS)){
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false);
                    tempObjSmallAmt  = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false,true);
                } if(acquirer.getName().equals(Constants.ACQ_DOLFIN_INSTALMENT)){
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false);
                    tempObjSmallAmt  = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.DOLFIN_INSTALMENT, filter, issuer, false,true);
                } else {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
                    tempObj[0] = tempObj[0] + tempOffline[0];
                    tempObj[1] = tempObj[1] + tempOffline[1];
                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false,true);
                    tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false,true);
                    tempObjSmallAmt[0] = tempObjSmallAmt[0] + tempOffline[0];
                    tempObjSmallAmt[1] = tempObjSmallAmt[1] + tempOffline[1];
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
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE",  Component.getPaddedNumber(saleNum,3), 24), CurrencyConverter.convert(saleAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

                //sale void total
                tempObj1[0] = 0;
                tempObj1[1] = 0;
                if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
                }else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
                }else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
                }else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED);
                }else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED);
                }else {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
                    tempObj[0] = tempObj[0] + tempOffline[0];
                    tempObj[1] = tempObj[1] + tempOffline[1];
                }
                long voidNum = tempObj[0];
                long voidAmt = tempObj[1];
                saleVoidHostNum += voidNum;
                saleVoidHostAmt += voidAmt;


                //sale void small amt
                tempObj1[0] = 0;
                tempObj1[1] = 0;
                if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED,true);
                }else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED,true);
                }else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED,true);
                }else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED,true);
                }else if (Constants.ACQ_DOLFIN_INSTALMENT.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.DOLFIN_INSTALMENT, VOIDED,true);
                }else {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED, true);
                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED, true);
                    tempObj[0] = tempObj[0] + tempOffline[0];
                    tempObj[1] = tempObj[1] + tempOffline[1];
                }
                long voidSmallAmtNum = tempObj[0];
                long voidSmallAmt = tempObj[1];
                saleVoidSmallAmtHostNum += voidSmallAmtNum;
                saleVoidSmallAmtHostAmt += voidSmallAmt;
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SALE",  Component.getPaddedNumber(voidNum,3), 24), CurrencyConverter.convert(0 - voidAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

                // refund
                tempObj1[0] = 0;
                tempObj1[1] = 0;

                tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, filter , issuer, false);

                totalNum += tempObj[0];
                totalAmt = saleAmt - tempObj[1];
                refundNum = tempObj[0];
                refundAmt = tempObj[1];
                refundHostNum += refundNum;
                refundHostAmt += refundAmt;
                if(refundNum != 0.00) {
                    settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",  Component.getPaddedNumber(refundNum,3), 24), CurrencyConverter.convert(0 - refundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                }

                // void refund
                tempObj1[0] = 0;
                tempObj1[1] = 0;
                if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
                    tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
                } else  {
                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.REFUND, VOIDED);
                    tempObj[0] = tempObj[0];
                    tempObj[1] = tempObj[1];
                }
                long voidRefundNum = tempObj[0];
                long voidRefundAmt = tempObj[1];
                saleVoidRefundHostNum += voidRefundNum;
                saleVoidRefundHostAmt += voidRefundAmt;
                if (voidRefundNum != 0.00) {
                    settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",  Component.getPaddedNumber(voidRefundNum,3), 24), CurrencyConverter.convert(voidRefundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                }

                tempObj[0] = 0; tempObj[1] = 0;//default value for another acquirers
                tempObj1[0] = 0; tempObj1[1] = 0;

                topupNum = tempObj[0];
                topupAmt = tempObj[1];

                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL" , CurrencyConverter.convert(totalAmt) ,ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE SMALL TICKET",   Component.getPaddedNumber(tempSmallAmtNum,3), 24),             CurrencyConverter.convert(tempSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE NORMAL",         Component.getPaddedNumber((saleNum - tempSmallAmtNum),3), 24), CurrencyConverter.convert(saleAmt - tempSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",              Component.getPaddedNumber(refundNum,3), 24),                   CurrencyConverter.convert(0 - refundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SMALL TICKET",   Component.getPaddedNumber(voidSmallAmtNum,3), 24),             CurrencyConverter.convert(0 - voidSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID NORMAL",         Component.getPaddedNumber((voidNum - voidSmallAmtNum),3), 24), CurrencyConverter.convert(0 - (voidAmt- voidSmallAmt)), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",         Component.getPaddedNumber(voidRefundNum,3), 24),               CurrencyConverter.convert(voidRefundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "TOTAL" , CurrencyConverter.convert(totalAmt) ,ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

            }
        }

        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE", Component.getPaddedNumber(saleHostNum,3), 24),  CurrencyConverter.convert(saleHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND", Component.getPaddedNumber(refundHostNum,3), 24),  CurrencyConverter.convert(0 - refundHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SALE", Component.getPaddedNumber(saleVoidHostNum,3), 24),  CurrencyConverter.convert(0 - saleVoidHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND", Component.getPaddedNumber(saleVoidRefundHostNum,3), 24),  CurrencyConverter.convert(saleVoidRefundHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);

        tempObj[0] = 0; tempObj[1] = 0;//default value for another acquirers

        topupNum = tempObj[0];
        topupAmt = tempObj[1];

        // top up
        if (topupNum != 0.00) {
            String tmpTopUp = CurrencyConverter.convert(topupAmt);
            if(topupAmt != 0.00){  tmpTopUp = "- " + tmpTopUp; }
            settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("TOPUP", Component.getPaddedNumber(topupNum,3), 24),  tmpTopUp, ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        }

        long hostNum = saleHostNum + refundHostNum + topupNum + topupRefundNum;
        long hostAmt = saleHostAmt - refundHostAmt + topupAmt - topupRefundAmt;
        EcrData.instance.nBatchTotalSalesCount = saleHostNum;
        EcrData.instance.nBatchTotalSalesAmount = saleHostAmt;
        EcrData.instance.nBatchTotalRefundCount = refundHostNum;
        EcrData.instance.nBatchTotalRefundAmount = refundHostAmt;

        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,  BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "GRAND TOTAL" , CurrencyConverter.convert(hostAmt), ConcatModes.AddSpaceBetween2Content ,  BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE SMALL TICKET",   Component.getPaddedNumber(saleSmallAmtHostNum,3), 24),  CurrencyConverter.convert(saleSmallAmtHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE NORMAL",         Component.getPaddedNumber((saleHostNum - saleSmallAmtHostNum),3), 24),  CurrencyConverter.convert(saleHostAmt- saleSmallAmtHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",              Component.getPaddedNumber(refundHostNum,3), 24),  CurrencyConverter.convert(0 - refundHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SMALL TICKET",   Component.getPaddedNumber(saleVoidSmallAmtHostNum,3), 24),  CurrencyConverter.convert(0 - saleVoidSmallAmtHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID NORMAL",         Component.getPaddedNumber((saleVoidHostNum - saleVoidSmallAmtHostNum),3), 24),  CurrencyConverter.convert(0 - (saleVoidHostAmt - saleVoidSmallAmtHostAmt)), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",         Component.getPaddedNumber(saleVoidRefundHostNum,3), 24),  CurrencyConverter.convert(saleVoidRefundHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_05_dash_ln ,  BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( "GRAND TOTAL" , CurrencyConverter.convert(hostAmt), ConcatModes.AddSpaceBetween2Content ,  BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ; SettleSlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
        settleSlipData.append(SettleSlipInfos) ; SettleSlipInfos ="" ;

        return settleSlipData.toString();
    }

    private Object[] getRawResults(String[] value) {
        Object[] obj = new Object[]{0, 0, ""};
        if (value != null) {
            obj[0] = value[0] == null ? 0 : Utils.parseLongSafe(value[0], 0);
            obj[1] = value[1] == null ? 0 : Utils.parseLongSafe(value[1], 0);
            obj[2] = value[2];
        }
        return obj;
    }


//   public String generateTotalByCards(Acquirer acquirer, List<Issuer> listIssuers, List<TransData.ETransStatus> filter) {
//        StringBuilder SlipData = new StringBuilder();
//        String SlipInfos="";
//        EReceiptUtils instant = EReceiptUtils.getInstance();
//
//        // Separater (sp)
//        String sp_05_dash_ln        = EReceiptUtils.StringPadding("",46,"-", Convert.EPaddingPosition.PADDING_LEFT);
//        String sp_06_eqal_ln        = EReceiptUtils.StringPadding("",46,"=", Convert.EPaddingPosition.PADDING_LEFT);
//
//        long[] tempObj = new long[2];
//        long[] tempOff = new long[2];
//        long[] tempObjSmallAmt = new long[2];
//        long tempNum, tempAmt;
//        long refundNum, refundAmt;
//        long topupNum = 0, topupAmt = 0;
//        long topupRefundNum = 0, topupRefundAmt = 0;
//        long totalNum, totalAmt;
//        long saleHostNum = 0, saleHostAmt = 0;
//        long saleVoidHostNum = 0, saleVoidHostAmt = 0;
//        long refundHostNum = 0, refundHostAmt = 0;
//        long tempSmallAmtNum, tempSmallAmt;
//        long saleVoidRefundHostNum = 0, saleVoidRefundHostAmt = 0;
//        long saleSmallAmtHostNum = 0, saleSmallAmtHostAmt = 0;
//        long saleVoidSmallAmtHostNum = 0, saleVoidSmallAmtHostAmt = 0;
//
//        for (int isu_idx = 0; isu_idx < listIssuers.size() ; isu_idx ++ ){
//            Issuer issuer = listIssuers.get(isu_idx);
//            tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
//            if (tempObj[0] != 0 || tempObj[1] != 0) {
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( issuer.getIssuerName() ,                          BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( issuer.getIssuerName() + "SUMMARY" ,      BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
//
//                long[] tempObj1 = new long[2];
//                if(acquirer.getName().equals(Constants.ACQ_KPLUS)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false);
//                    tempObj[0] = tempObj[0];
//                    tempObj[1] = tempObj[1];
//                    tempObjSmallAmt= FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, filter, false, false, true);
//                } else if(acquirer.getName().equals(Constants.ACQ_ALIPAY)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false);
//                    tempObj[0] = tempObj[0];
//                    tempObj[1] = tempObj[1];
//                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, filter, false, false,true);
//                }else if(acquirer.getName().equals(Constants.ACQ_WECHAT)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false);
//                    tempObj[0] = tempObj[0];
//                    tempObj[1] = tempObj[1];
//                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, filter, false, false,true);
//                }else if(acquirer.getName().equals(Constants.ACQ_SMRTPAY) || acquirer.getName().equals(Constants.ACQ_SMRTPAY_BDMS)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false);
//                    tempObjSmallAmt  = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.KBANK_SMART_PAY, filter, issuer, false,true);
//                } else {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false);
//                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false);
//                    tempObj[0] = tempObj[0] + tempOffline[0];
//                    tempObj[1] = tempObj[1] + tempOffline[1];
//                    tempObjSmallAmt = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.SALE, filter, issuer, false,true);
//                    tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.OFFLINE_TRANS_SEND, filter, issuer, false,true);
//                    tempObjSmallAmt[0] = tempObjSmallAmt[0] + tempOffline[0];
//                    tempObjSmallAmt[1] = tempObjSmallAmt[1] + tempOffline[1];
//                }
//                long saleNum = tempObj[0];
//                long saleAmt = tempObj[1];
//                totalNum = tempObj[0];
//                totalAmt = tempObj[1];
//                saleHostNum += saleNum;
//                saleHostAmt += saleAmt;
//                tempSmallAmtNum = tempObjSmallAmt[0];
//                tempSmallAmt = tempObjSmallAmt[1];
//                saleSmallAmtHostNum += tempSmallAmtNum;
//                saleSmallAmtHostAmt += tempSmallAmt;
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE",  Component.getPaddedNumber(saleNum,3), 24), CurrencyConverter.convert(saleAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//                //sale void total
//                tempObj1[0] = 0;
//                tempObj1[1] = 0;
//                if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED);
//                }else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED);
//                }else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED);
//                }else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED);
//                }else {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED);
//                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED);
//                    tempObj[0] = tempObj[0] + tempOffline[0];
//                    tempObj[1] = tempObj[1] + tempOffline[1];
//                }
//                long voidNum = tempObj[0];
//                long voidAmt = tempObj[1];
//                saleVoidHostNum += voidNum;
//                saleVoidHostAmt += voidAmt;
//
//
//                //sale void small amt
//                tempObj1[0] = 0;
//                tempObj1[1] = 0;
//                if (Constants.ACQ_KPLUS.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY, VOIDED,true);
//                }else if (Constants.ACQ_ALIPAY.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_ALIPAY, VOIDED,true);
//                }else if (Constants.ACQ_WECHAT.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.QR_INQUIRY_WECHAT, VOIDED,true);
//                }else if (Constants.ACQ_SMRTPAY.equals(acquirer.getName()) || Constants.ACQ_SMRTPAY_BDMS.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.KBANK_SMART_PAY, VOIDED,true);
//                }else {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.SALE, VOIDED, true);
//                    long[] tempOffline = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.OFFLINE_TRANS_SEND, VOIDED, true);
//                    tempObj[0] = tempObj[0] + tempOffline[0];
//                    tempObj[1] = tempObj[1] + tempOffline[1];
//                }
//                long voidSmallAmtNum = tempObj[0];
//                long voidSmallAmt = tempObj[1];
//                saleVoidSmallAmtHostNum += voidSmallAmtNum;
//                saleVoidSmallAmtHostAmt += voidSmallAmt;
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SALE",  Component.getPaddedNumber(voidNum,3), 24), CurrencyConverter.convert(0 - voidAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//
//                // refund
//                tempObj1[0] = 0;
//                tempObj1[1] = 0;
//                if(acquirer.getName().equals(Constants.ACQ_LINEPAY)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_LINEPAY, filter, false, false);
//                } else if (acquirer.getName().equals(Constants.ACQ_BSS_HOST)) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_REFUND, filter, false, false);
//                    tempOff = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_OFFLINE_REFUND, filter, false, false);
//                    tempObj[0] = tempObj[0] + tempOff[0];
//                    tempObj[1] = tempObj[1] + tempOff[1];
//                } else {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND, filter , issuer, false);
//                }
//                totalNum += tempObj[0];
//                totalAmt = saleAmt - tempObj[1];
//                refundNum = tempObj[0];
//                refundAmt = tempObj[1];
//                refundHostNum += refundNum;
//                refundHostAmt += refundAmt;
//                if(refundNum != 0.00) {
//                    SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",  Component.getPaddedNumber(refundNum,3), 24), CurrencyConverter.convert(0 - refundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                }
//
//
//                // void refund
//                tempObj1[0] = 0;
//                tempObj1[1] = 0;
//                if (Constants.ACQ_WALLET.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
//                    tempObj1 = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_WALLET, VOIDED);
//                } else if (Constants.ACQ_LINEPAY.equals(acquirer.getName())) {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.REFUND_LINEPAY, VOIDED);
//                }else {
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, issuer, ETransType.REFUND, VOIDED);
//                    tempObj[0] = tempObj[0];
//                    tempObj[1] = tempObj[1];
//                }
//                long voidRefundNum = tempObj[0];
//                long voidRefundAmt = tempObj[1];
//                saleVoidRefundHostNum += voidRefundNum;
//                saleVoidRefundHostAmt += voidRefundAmt;
//                if (voidRefundNum != 0.00) {
//                    SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",  Component.getPaddedNumber(voidRefundNum,3), 24), CurrencyConverter.convert(voidRefundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                }
//
//
//                // top up
//                tempObj[0] = 0; tempObj[1] = 0;//default value for another acquirers
//                tempObj1[0] = 0; tempObj1[1] = 0;
//                if(acquirer.getName().equals(Constants.ACQ_LINEPAY)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.TOPUP_LINEPAY, filter, false, false);
//                    tempObj[0] = tempObj[0];
//                    tempObj[1] = tempObj[1];
//                }  else if(acquirer.getName().equals(Constants.ACQ_BSS_HOST)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_TOPUP, filter, false, false);
//                    tempOff = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_OFFLINE_TOPUP, filter, false, false);
//                    tempObj[0] = tempObj[0] + tempOff[0];
//                    tempObj[1] = tempObj[1] + tempOff[1];
//                }
//                topupNum = tempObj[0];
//                topupAmt = tempObj[1];
//                if (topupNum != 0.00) {
//                    String tmpTopUp = CurrencyConverter.convert(topupAmt);
//                    if(topupAmt != 0.00){  tmpTopUp = "- " + tmpTopUp; }
//                    SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("TOPUP",  Component.getPaddedNumber(topupNum,3), 24), CurrencyConverter.convert(topupAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                    totalNum += topupNum;
//                    totalAmt += topupAmt;
//                }
//
//                if(acquirer.getName().equals(Constants.ACQ_BSS_HOST)){
//                    tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_TOPUP_REFUND, filter, false, false);
//                    tempOff = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.BSS_RABBIT_OFFLINE_TOPUP_REFUND, filter, false, false);
//                    topupRefundNum = tempObj[0] + tempOff[0];
//                    topupRefundAmt = tempObj[1] + tempOff[1];
//
//                    if (topupRefundNum != 0.00) {
//                        String tmpTopUpRefund = CurrencyConverter.convert(topupRefundAmt);
//                        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("TOPUP REFUND",  Component.getPaddedNumber(topupRefundNum,3), 24), CurrencyConverter.convert(topupRefundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//                        totalNum += topupRefundNum;
//                        totalAmt -= topupRefundAmt;
//                    }
//                }
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "TOTAL" , CurrencyConverter.convert(totalAmt) ,ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE SMALL TICKET",   Component.getPaddedNumber(tempSmallAmtNum,3), 24),             CurrencyConverter.convert(tempSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE NORMAL",         Component.getPaddedNumber((saleNum - tempSmallAmtNum),3), 24), CurrencyConverter.convert(saleAmt - tempSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",              Component.getPaddedNumber(refundNum,3), 24),                   CurrencyConverter.convert(0 - refundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SMALL TICKET",   Component.getPaddedNumber(voidSmallAmtNum,3), 24),             CurrencyConverter.convert(0 - voidSmallAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID NORMAL",         Component.getPaddedNumber((voidNum - voidSmallAmtNum),3), 24), CurrencyConverter.convert(0 - (voidAmt- voidSmallAmt)), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",         Component.getPaddedNumber(voidRefundNum,3), 24),               CurrencyConverter.convert(voidRefundAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "TOTAL" , CurrencyConverter.convert(totalAmt) ,ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//                SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "" ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//            }
//        }
//
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE", Component.getPaddedNumber(saleHostNum,3), 24),  CurrencyConverter.convert(saleHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND", Component.getPaddedNumber(refundHostNum,3), 24),  CurrencyConverter.convert(0 - refundHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SALE", Component.getPaddedNumber(saleVoidHostNum,3), 24),  CurrencyConverter.convert(0 - saleVoidHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND", Component.getPaddedNumber(saleVoidRefundHostNum,3), 24),  CurrencyConverter.convert(saleVoidRefundHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//
//        tempObj[0] = 0; tempObj[1] = 0;//default value for another acquirers
//        if(acquirer.getName().equals(Constants.ACQ_LINEPAY)){
//            tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, ETransType.TOPUP_LINEPAY, filter, false, false);
//            tempObj[0] = tempObj[0];
//            tempObj[1] = tempObj[1];
//        }
//
//        topupNum = tempObj[0];
//        topupAmt = tempObj[1];
//
//        // top up
//        if (topupNum != 0.00) {
//            String tmpTopUp = CurrencyConverter.convert(topupAmt);
//            if(topupAmt != 0.00){  tmpTopUp = "- " + tmpTopUp; }
//            SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("TOPUP", Component.getPaddedNumber(topupNum,3), 24),  tmpTopUp, ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        }
//
//        long hostNum = saleHostNum + refundHostNum + topupNum + topupRefundNum;
//        long hostAmt = saleHostAmt - refundHostAmt + topupAmt - topupRefundAmt;
//        EcrData.instance.nBatchTotalSalesCount = saleHostNum;
//        EcrData.instance.nBatchTotalSalesAmount = saleHostAmt;
//        EcrData.instance.nBatchTotalRefundCount = refundHostNum;
//        EcrData.instance.nBatchTotalRefundAmount = refundHostAmt;
//
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,  BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "GRAND TOTAL" , CurrencyConverter.convert(hostAmt), ConcatModes.AddSpaceBetween2Content ,  BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE SMALL TICKET",   Component.getPaddedNumber(saleSmallAmtHostNum,3), 24),  CurrencyConverter.convert(saleSmallAmtHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("SALE NORMAL",         Component.getPaddedNumber((saleHostNum - saleSmallAmtHostNum),3), 24),  CurrencyConverter.convert(saleHostAmt- saleSmallAmtHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("REFUND",              Component.getPaddedNumber(refundHostNum,3), 24),  CurrencyConverter.convert(0 - refundHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID SMALL TICKET",   Component.getPaddedNumber(saleVoidSmallAmtHostNum,3), 24),  CurrencyConverter.convert(0 - saleVoidSmallAmtHostAmt), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID NORMAL",         Component.getPaddedNumber((saleVoidHostNum - saleVoidSmallAmtHostNum),3), 24),  CurrencyConverter.convert(0 - (saleVoidHostAmt - saleVoidSmallAmtHostAmt)), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( instant.AutoAddSpacebetween2String("VOID REFUND",         Component.getPaddedNumber(saleVoidRefundHostNum,3), 24),  CurrencyConverter.convert(saleVoidRefundHostAmt ), ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_05_dash_ln ,  BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( "GRAND TOTAL" , CurrencyConverter.convert(hostAmt), ConcatModes.AddSpaceBetween2Content ,  BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = instant.WrapContent( sp_06_eqal_ln ,                                   BOL_Options.HalSizefWidth, EOL_Options.AddCarriageReturn, TextAlignment.None);
//        SlipData.append(SlipInfos) ; SlipInfos ="" ;
//        //Log.d(EReceiptUtils.TAG, "[" + acquirer.getName() + "] TOTAL BY CARDS : " + SlipData.toString());
//        return SlipData.toString() + new String(new byte[]{0x0A});
//   }


    private List<Issuer> filterIssuers(List<Issuer> issuers, List<TransData.ETransStatus> filter, Acquirer acquirer) {
        List<Issuer> result = new ArrayList<>();
        for (Issuer issuer : issuers) {
            long[] tempObj = FinancialApplication.getTransDataDbHelper().countSumOf(acquirer, filter, issuer);
            if (tempObj[0] != 0 || tempObj[1] != 0) {
                result.add(issuer);
            }
        }
        return result;
    }
}
