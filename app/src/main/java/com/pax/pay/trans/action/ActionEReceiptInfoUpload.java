package com.pax.pay.trans.action;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.renderscript.ScriptGroup;

import com.google.gson.GsonBuilder;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.device.Device;
import com.pax.edc.BuildConfig;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.MultiAppErmUploadModel;
import com.pax.pay.trans.model.ReservedFieldHandle;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.trans.pack.PackEReceiptSettleUpload;
import com.pax.pay.trans.pack.PackEReceiptUpload;
import com.pax.pay.trans.receipt.ReceiptGeneratorInstalmentAmexTrans;
import com.pax.pay.trans.receipt.ReceiptGeneratorInstalmentKbankTrans;
import com.pax.pay.trans.receipt.ReceiptGeneratorRedeemedTrans;
import com.pax.pay.trans.receipt.ReceiptGeneratorTransTOPS;
import com.pax.pay.trans.receipt.ReceiptPrintTransForERM;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.BitmapImageConverterUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.EReceiptUtils.BOL_Options;
import com.pax.pay.utils.EReceiptUtils.ConcatModes;
import com.pax.pay.utils.EReceiptUtils.EOL_Options;
import com.pax.pay.utils.EReceiptUtils.TextAlignment;
import com.pax.pay.utils.TimeConverter;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kotlin.text.Charsets;
import th.co.bkkps.utils.BitmapUtils;
import th.co.bkkps.utils.Log;
import th.co.bkkps.utils.PageToSlipFormat;

import static com.pax.pay.trans.component.Component.transInit;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public class ActionEReceiptInfoUpload extends AAction {
    public ActionEReceiptInfoUpload (ActionStartListener listener){super(listener);}
    public void setParam (Context ex_context, TransData ex_transData, EReceiptType type){
        this.context=ex_context;
        this.transData=ex_transData;
        this.type = type;
    }

    public void setParam (Context ex_context, TransData ex_transData, EReceiptType type, String fileUniqueID, Acquirer acquirer){
        // use only multiapplication upload
        this.isFromExtApp = true;
        this.context=ex_context;
        this.transData=ex_transData;
        this.type = type;
        this.extAppUploadFileName = fileUniqueID;
        this.extAppAcquirer = acquirer;
        this.extETransType = ex_transData.getTransType();
    }

    public void setParam (Context ex_context, List<Acquirer> acquirers, EReceiptType type){
        this.context = ex_context;
        this.acquirers = acquirers;
        this.type = type;
    }

    public void setParam (TransTotal transTotal, Context ex_context, EReceiptType type, String ReferenceNo){
        this.total = transTotal;
        this.context = ex_context;
        this.type = type;
        this.referencNo_settlemented = ReferenceNo;
        Log.d(EReceiptUtils.TAG,"SettleActivity:ERCM\t     inner.ReferenceNo = " + this.referencNo_settlemented);
    }

    public void setParam (Context ex_context, EReceiptType type){
        this.context = ex_context;
        this.type = type;
        Log.d(EReceiptUtils.TAG,"SettleActivity:ERCM\t     settlement-from-file activated");
    }

    public enum EReceiptType {
        ERECEIPT,
        ERECEIPT_PRE_SETTLE,
        ERECEIPT_SETTLE,
        ERECEIPT_REPORT_FROM_FILE,
        ERECEIPT_UPLOAD_FROM_FILE
    }

    private Context context ;
    private TransData transData;
    private List<Acquirer> acquirers;
    private Online online = new Online();

    private final static int ESIGN_RETRY = 3;
    private final static int ERECEIPT_RETRY = 3;
    private EReceiptType type;
    private List<TransTotal> transTotals;
    private TransTotal total;
    private String referencNo_settlemented;

    // upload from External Application
    private String extAppUploadFileName;
    private Acquirer extAppAcquirer;
    private ETransType extETransType;
    private boolean isFromExtApp;

    private TransProcessListenerImpl transProcessListenerImpl;

    @Override
    protected void process() {
        Acquirer acqDownloadKey = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE);
        Acquirer acqUpload      = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
        if ((acqDownloadKey == null || !acqDownloadKey.isEnable()) || (acqUpload == null || !acqUpload.isEnable())) {
            setResult(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }

        try {
            if (extETransType == ETransType.KCHECKID_DUMMY) {
                Thread ermThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        transProcessListenerImpl = new TransProcessListenerImpl(context);

                        switch (type) {
                            case ERECEIPT:
                                uploadEReceipt();
                                break;
                            case ERECEIPT_UPLOAD_FROM_FILE:
                                // for external application
                                if (extETransType == ETransType.KCHECKID_DUMMY) {
                                    uploadEReceiptFromFile();
                                } else {
                                    uploadErmFromFile(transData, extAppUploadFileName, extETransType, extAppAcquirer);
                                }
                                break;
                            case ERECEIPT_PRE_SETTLE:
                                int ret = doPreSettlement();
                                setResult(new ActionResult(ret, null));
                                break;
                            case ERECEIPT_SETTLE:
                                uploadESettlement();
                                break;
                            case ERECEIPT_REPORT_FROM_FILE:
                                uploadESettlementFromFile();
                                break;
                        }
                    }
                });
                ermThread.start();
                ermThread.join(60*1000);
            } else {
                FinancialApplication.getApp().runInBackground(new Runnable() {

                    @Override
                    public void run() {
                        transProcessListenerImpl = new TransProcessListenerImpl(context);

                        switch (type) {
                            case ERECEIPT:
                                uploadEReceipt();
                                break;
                            case ERECEIPT_UPLOAD_FROM_FILE:
                                // for external application
                                if (extETransType == ETransType.KCHECKID_DUMMY) {
                                    uploadEReceiptFromFile();
                                } else {
                                    uploadErmFromFile(transData, extAppUploadFileName, extETransType, extAppAcquirer);
                                }
                                break;
                            case ERECEIPT_PRE_SETTLE:
                                int ret = doPreSettlement();
                                setResult(new ActionResult(ret, null));
                                break;
                            case ERECEIPT_SETTLE:
                                uploadESettlement();
                                break;
                            case ERECEIPT_REPORT_FROM_FILE:
                                uploadESettlementFromFile();
                                break;
                        }
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setResult(ActionResult result) {
        transProcessListenerImpl.onHideProgress();
        super.setResult(result);
    }

    private void uploadEReceipt() {
        initEReceiptStatus();

        if (transData.geteSlipFormat() == null || transData.geteSlipFormat().length == 0) {
            handleUploadFlag(false);
            if(transData.getAcquirer().isEnableUploadERM()) {transProcessListenerImpl.onShowErrMessage(TransResultUtils.getMessage(TransResult.ERCM_UPLOAD_FAIL), Constants.FAILED_DIALOG_SHOW_TIME, true);}
            setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
            return;
        }

        TransData eReceipt = initialEReceiptInfomationUpload();

        String transName = eReceipt.getTransType().getTransName();
        transProcessListenerImpl.onUpdateProgressTitle(transName);

        //online.setReplaceExternalField63(true);
        Log.i("Online"," >> SEND : >> Uploading E-Receipt & E-Signature to ERM");
        Log.i(EReceiptUtils.TAG," >> SEND : >> Uploading E-Receipt & E-Signature to ERM");
        int ret = online.online(eReceipt, transProcessListenerImpl);
        if (ret == TransResult.SUCC && eReceipt.getResponseCode() != null && eReceipt.getResponseCode().getCode().equals("00")) {
            Log.i("Online"," >> SEND : >> Result >> Upload successful");
            Log.i(EReceiptUtils.TAG," >> SEND : >> Result >>  Upload successful");
            transData.seteReceiptUploadStatus(TransData.UploadStatus.NORMAL);
            handleUploadFlag(true);
            setResult(new ActionResult(ret, null));
        } else {
            handleUploadFlag(false);
            if (ret == TransResult.SUCC && eReceipt.getResponseCode() != null && !eReceipt.getResponseCode().getCode().equals("00")) {
                //transProcessListenerImpl.onShowErrMessage(transName + "\n Error code: " + eReceipt.getResponseCode().toString(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                if (eReceipt.getResponseCode().getCode().equals("51")) {
                    Log.i("Online"," >> SEND : >> Result >>  Key-out-of-sync");
                    Log.i(EReceiptUtils.TAG," >> SEND : >> Result >>  Key-out-of-sync");
                    setResult(new ActionResult(TransResult.ERCM_UPLOAD_SESSIONKEY_RENEWAL_REQUIRED, null));
                } else {
                    Log.i("Online"," >> SEND : >> Result >>  Upload failed");
                    Log.i(EReceiptUtils.TAG," >> SEND : >> Result >>  Upload failed");
                    setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
                }
            } else {
                Log.i("Online"," >> SEND : >> Result >>  Upload failed");
                Log.i(EReceiptUtils.TAG," >> SEND : >> Result >>  Upload failed");
                //transProcessListenerImpl.onShowErrMessage(transName + " " + TransResultUtils.getMessage(TransResult.ERCM_UPLOAD_FAIL), Constants.FAILED_DIALOG_SHOW_TIME, true);
                setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
            }

        }
    }



    private void initEReceiptStatus() {
        transData.seteReceiptRetry(1);
        transData.seteReceiptUploadStatus(TransData.UploadStatus.PENDING);
        transData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(transData.getAcquirer().getId()),3,"0", Convert.EPaddingPosition.PADDING_LEFT));
        transData.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(transData.getAcquirer().getNii()),3,"0", Convert.EPaddingPosition.PADDING_LEFT));
        transData.setInitAcquirerName(transData.getAcquirer().getName());
        if(transData.geteSlipFormat() == null ) {
            /*--------------------------------------------------
                  E-Receipt/E-Slip format Generated
          --------------------------------------------------*/
            String slipData = "";
            String SignatureData  = "";
            boolean onGeneratefromPrintModule = true;
            //--------------------------------------------------

            if (onGeneratefromPrintModule) {
                Log.d(EReceiptUtils.TAG," Slip generate method : using printing module (new)");
                String acquireName = transData.getAcquirer().getName();
                switch (acquireName) {
                    case Constants.ACQ_SMRTPAY:
                    case Constants.ACQ_SMRTPAY_BDMS:
                    case Constants.ACQ_DOLFIN_INSTALMENT:
                        ReceiptGeneratorInstalmentKbankTrans receiptGeneratorInstalmentKbankTrans = new ReceiptGeneratorInstalmentKbankTrans(transData,0,2,false,false,true);
                        receiptGeneratorInstalmentKbankTrans.generateBitmap();
                        break;
                    case Constants.ACQ_REDEEM:
                    case Constants.ACQ_REDEEM_BDMS:
                        ReceiptGeneratorRedeemedTrans receiptGeneratorRedeemedTrans =  new ReceiptGeneratorRedeemedTrans(transData,0,2,false,false,true);
                        receiptGeneratorRedeemedTrans.generateBitmap();
                        break;
                    case Constants.ACQ_KPLUS:
                    case Constants.ACQ_ALIPAY:
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                    case Constants.ACQ_QR_CREDIT:
                        ReceiptGeneratorTransTOPS receiptGeneratorWalletTrans = new ReceiptGeneratorTransTOPS(transData, 0, 2, false, false, true);
                        if (acquireName.equals(Constants.ACQ_KPLUS) && transData.getTransType()==ETransType.QR_VERIFY_PAY_SLIP
                                && transData.getTransState() != TransData.ETransStatus.VOIDED) {
                            receiptGeneratorWalletTrans.generateWalletVerifyPaySlipReceiptBitmap();
                        } else {
                            receiptGeneratorWalletTrans.generateKbankWalletReceiptBitmap();
                        }
                        break;
                    case Constants.ACQ_AMEX_EPP:
                        ReceiptGeneratorInstalmentAmexTrans receiptGeneratorInstalmentAmexTrans = new ReceiptGeneratorInstalmentAmexTrans(transData, 0, 2, false, false, true);
                        receiptGeneratorInstalmentAmexTrans.generateBitmap();
                        break;
                    default:
                        ReceiptGeneratorTransTOPS receiptGeneratorTransTops = new ReceiptGeneratorTransTOPS(transData, 0, 2, false, false, true);
                        receiptGeneratorTransTops.generateBitmap();
                        break;
                }


                slipData = PageToSlipFormat.getInstance().getSlipFormat();
                // add suffix host format
                slipData = slipData.replace(Utils.bcd2Str("[$]".getBytes()), Utils.bcd2Str(new byte[] {0x0D,(byte)0xCE}));
            } else {
                Log.d(EReceiptUtils.TAG," Slip generate method : using dedicate slip generate function (old)");
                slipData = CreateVirtualSlipData(transData);
            }


            if (!(transData.getTransType() == ETransType.VOID)                    && !(transData.getTransType() == ETransType.KBANK_REDEEM_VOID)
                    && !(transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID) && !(transData.getTransType() == ETransType.QR_VOID_WECHAT)
                    && !(transData.getTransType() == ETransType.QR_VOID_ALIPAY)       && !(transData.getTransType() == ETransType.QR_VOID_KPLUS)
                    && !(transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID)) {
                SignatureData = (transData.getSignData() != null) ? getSignatureWithVerifoneFormat(transData.getSignData()) : "" ; ;       // Slip format generation was generated inside this function
                Log.d(EReceiptUtils.TAG, "Signature Data size : " + SignatureData.length() + " bytes." );
            } else {
                Log.d(EReceiptUtils.TAG, "Signature Data size : [skip--print--signature--on--void--trans]" );
            }

            if (onGeneratefromPrintModule) {
                Log.d(EReceiptUtils.TAG, "Replace [@SIGN_DATA] : " + SignatureData );
                slipData = slipData.replace(Utils.bcd2Str("[@SIGN_DATA]".getBytes()), SignatureData );
            } else {
                Log.d(EReceiptUtils.TAG, "Replace [@VF_FORMAT_SIGNATURE] : " + SignatureData );
                slipData = slipData.replace("[@VF_FORMAT_SIGNATURE]",SignatureData );
            }

            String Path = "/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data";
            String Filename = "/slipinfo.slp";
            BitmapImageConverterUtils.saveDataToFile(Tools.str2Bcd(slipData),Path, Filename);

            /*--------------------------------------------------
                 SET Slip Format to TransData
            --------------------------------------------------*/
            transData.seteSlipFormat(Tools.str2Bcd(slipData));
            transData.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
            FinancialApplication.getTransDataDbHelper().updateTransData(transData);
        }
        else {
            String Path = "/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data";
            String Filename = "/slipnxtu.slp";
            BitmapImageConverterUtils.saveDataToFile(transData.geteSlipFormat(),Path, Filename);
            Log.d(EReceiptUtils.TAG, " Skip SlipInfoGenerate :: Use exisiting eSlipFormat" );

            transData.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
        }
    }

    public String replaceErmHostIndex(@NonNull String slipData) {
        return slipData.replace(Utils.bcd2Str("[$]".getBytes()), Utils.bcd2Str(new byte[] {0x0D,(byte)0xCE}));
    }

    public String replaceTagBcd(@NonNull String slipData, String srcText, String newText) {
        return slipData.replace(Utils.bcd2Str(srcText.getBytes()), Utils.bcd2Str(newText.getBytes()));
    }

    public String replaceErmSignatureData(@NonNull String slipData, @NonNull String SignatureData) {
        return slipData.replace(Utils.bcd2Str("[@SIGN_DATA]".getBytes()), SignatureData );
    }

    public String getSignatureWithVerifoneFormat(byte[] exJbigImage) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();
        Bitmap bitmap = imgProcessing.jbigToBitmap(exJbigImage);

        //only Scale image when Width > 256 or Height > 128

        bitmap = BitmapUtils.clearBlank(bitmap, 0, Color.WHITE);

        if (bitmap.getWidth() > 256
            || bitmap.getHeight() > 128) {

            // check scale H or scale W
            float hPercen = (float)( 128 * 100 / bitmap.getHeight());
            float wPercen = (float)( 256 * 100 / bitmap.getWidth());

            if(hPercen < wPercen) {
                int newWidth = (int)(hPercen * bitmap.getWidth()/100);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, 128, true);
            } else {
                int newHeight = (int)(wPercen * bitmap.getHeight()/100);
                bitmap = Bitmap.createScaledBitmap(bitmap, 256, newHeight, true);
            }
        }
        // resize canvas
        Bitmap newCanvas = Bitmap.createBitmap(256, 128, Bitmap.Config.ARGB_8888);
        newCanvas.eraseColor(Color.WHITE);
        Bitmap resize = BitmapUtils.addOverlayToCenter(bitmap, newCanvas);

        // Set new image to Transdata after scale
        byte[] rezizeJbig = FinancialApplication.getGl().getImgProcessing().bitmapToJbig(resize, Constants.rgb2MonoAlgo);
        if (transData!=null) {
            transData.setSignData(rezizeJbig);
        }



        // flip
        Matrix matrix = new Matrix();
        matrix.postScale(1, -1, (int)(resize.getHeight()/2), (int)(resize.getWidth()/2));
        Bitmap flip = Bitmap.createBitmap(resize, 0, 0, resize.getWidth(), resize.getHeight(), matrix, true);

        byte[] monoBmp = FinancialApplication.getGl().getImgProcessing().bitmapToMonoBmp(flip, Constants.rgb2MonoAlgo);

        byte[] vfFormatSignature = BitmapImageConverterUtils.getSignatureVerifoneFormat(monoBmp);

        //BitmapImageConverterUtils.CreateVerifoneFormat(exBitmapImage);
        return Tools.bcd2Str(vfFormatSignature);
    }

    public String[] ReformatCardScheme(TransData transData) {
        boolean isWalletTrans             = (transData.getTransType() == ETransType.QR_INQUIRY_ALIPAY
                                                || transData.getTransType() == ETransType.QR_VOID_ALIPAY
                                                || transData.getTransType() == ETransType.QR_ALIPAY_SCAN
                                                || transData.getTransType() == ETransType.QR_INQUIRY_WECHAT
                                                || transData.getTransType() == ETransType.QR_WECHAT_SCAN
                                                || transData.getTransType() == ETransType.QR_VOID_WECHAT
                                                || transData.getTransType() == ETransType.QR_INQUIRY
                                                || transData.getTransType() == ETransType.QR_VERIFY_PAY_SLIP
                                                || transData.getTransType() == ETransType.QR_MYPROMPT_SALE
                                                || transData.getTransType() == ETransType.QR_MYPROMPT_INQUIRY
                                                || transData.getTransType() == ETransType.QR_MYPROMPT_VERIFY
                                                || transData.getTransType() == ETransType.QR_VOID_KPLUS
                                                || transData.getTransType() == ETransType.QR_INQUIRY_CREDIT
                                                || transData.getTransType() == ETransType.QR_VOID_CREDIT
                                                || transData.getTransType() == ETransType.DOLFIN_INSTALMENT
                                                || transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID );
        if (isWalletTrans) {
            if        (transData.getTransType() == ETransType.QR_INQUIRY)                   {  return new String [] {"SALE",        "KPLUS"} ;                         }
            else if   (transData.getTransType() == ETransType.QR_MYPROMPT_SALE)             {  return new String [] {"SALE",        "QR-MYPROMPT"} ;                   }        // Extra for QR-MYPROMPT Sale B-SCAN-C mode
            else if   (transData.getTransType() == ETransType.QR_MYPROMPT_INQUIRY)          {  return new String [] {"SALE",        "QR-MYPROMPT"} ;                   }        // Extra for QR-MYPROMPT Inquiry
            else if   (transData.getTransType() == ETransType.QR_MYPROMPT_VERIFY)           {  return new String [] {"VERIFY QR",   "QR-MYPROMPT"} ;                   }        // Extra for QR-MYPROMPT Verify QR
            else if   (transData.getTransType() == ETransType.QR_VERIFY_PAY_SLIP)           {  return new String [] {"VERIFY QR",   "KPLUS"} ;                         }
            else if   (transData.getTransType() == ETransType.QR_VOID_KPLUS)                {  return new String [] {"VOID",        "KPLUS"} ;                         }
            else if   (transData.getTransType() == ETransType.QR_INQUIRY_ALIPAY)            {  return new String [] {"SALE",        "ALIPAY CsB"} ;                    }
            else if   (transData.getTransType() == ETransType.QR_ALIPAY_SCAN)               {  return new String [] {"SALE",        "ALIPAY BsC"} ;                    }        // Extra for ALIPAY B-SCAN-C mode
            else if   (transData.getTransType() == ETransType.QR_VOID_ALIPAY)               {  return new String [] {"VOID",        "ALIPAY"} ;                        }
            else if   (transData.getTransType() == ETransType.QR_INQUIRY_WECHAT)            {  return new String [] {"SALE",        "WECHAT CsB"} ;                    }
            else if   (transData.getTransType() == ETransType.QR_WECHAT_SCAN)               {  return new String [] {"SALE",        "WECHAT BsC"} ;                    }        // Extra for WECHATPAY B-SCAN-C mode
            else if   (transData.getTransType() == ETransType.QR_VOID_WECHAT)               {  return new String [] {"VOID",        "WECHAT"} ;                        }
            else if   (transData.getTransType() == ETransType.QR_INQUIRY_CREDIT)            {  return new String [] {"SALE",        "QR_CREDIT"} ;                     }
            else if   (transData.getTransType() == ETransType.QR_VOID_CREDIT)               {  return new String [] {"VOID",        "QR_CREDIT"} ;                     }
            else if   (transData.getTransType() == ETransType.DOLFIN_INSTALMENT)            {  return new String [] {"SALE",        "DOLFIN-XPC"} ;                    }
            else if   (transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID)       {  return new String [] {"VOID",        "DOLFIN-XPC"} ;                    }
        }
        else {
            if        (transData.getTransType() == ETransType.SALE )                        {  return new String [] {"SALE",        transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.VOID)                         {  return new String [] {"VOID",        (transData.getOrigTransType() == ETransType.AMEX_INSTALMENT) ? Utils.getString(R.string.receipt_amex_instalment_issuer) : transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT
                        || transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT_CREDIT
                        || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER
                        || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER_CREDIT
                        || transData.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT)     {  return new String [] {"SALE",       transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.KBANK_REDEEM_VOID)             {  return new String [] {"VOID",       transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.KBANK_SMART_PAY)               {  return new String [] {"SALE",       transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID)          {  return new String [] {"VOID",       transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.REFUND)                        {  return new String [] {"REFUND",     transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.AMEX_INSTALMENT)               {  return new String [] {"SALE",       Utils.getString(R.string.receipt_amex_instalment_issuer)} ; }

            // GENERIC R2 : FEATURES
            else if   (transData.getTransType() == ETransType.PREAUTHORIZATION)              {  return new String [] {"PREAUTH",    transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.PREAUTHORIZATION_CANCELLATION) {  return new String [] {"CAN-PREAUTH",transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.SALE_COMPLETION)               {  return new String [] {"SALE-COMP",  transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.ADJUST)                        {  return new String [] {"TIP-ADJ",    transData.getIssuer().getName()} ; }
            else if   (transData.getTransType() == ETransType.OFFLINE_TRANS_SEND)            {  return new String [] {"OFFLINE",    transData.getIssuer().getName()} ; }
        }

        return null;
    }

    public String[] ReformatCardSchemeMultiApp(TransData transData) {
        if        (transData.getOrigTransType() == ETransType.SALE )                        {  return new String [] {"SALE",        transData.getIssuer().getName()} ; }
        else if   (transData.getOrigTransType() == ETransType.VOID)                         {  return new String [] {"VOID",        transData.getIssuer().getName()} ; }

        return null;
    }


    private String isNullCheck(String targetStr) {
        String returnStr="";
        if (targetStr == null) {
            return returnStr;
        }
        else {
            return targetStr;
        }
    }

    private String[] isNullCheck(String[] targetStr) {
        String[] returnStrArray =  new String[targetStr.length];
        for (int idx = 0 ; idx < targetStr.length ; idx++) {
            if (targetStr[idx] == null) {
                returnStrArray[idx] = "" ;
            } else {
                returnStrArray[idx] = targetStr[idx] ;
            }
        }
        return returnStrArray;
    }

    private String CreateVirtualSlipData (TransData transData) {
        StringBuilder strBuilder = new StringBuilder();
        String SlipInfos = "" ;
        try {
            if (transData.getAcquirer()==null)      {Log.w(EReceiptUtils.TAG, "[ERCM.CreateVirtualSlipData] -----> ACQUIER was missing !!" ); }
            if (transData.getIssuer()==null)        {Log.w(EReceiptUtils.TAG, "[ERCM.CreateVirtualSlipData] -----> ISSUER was missing !!" ); }
            if (transData.getTrack1()==null)        {Log.w(EReceiptUtils.TAG, "[ERCM.CreateVirtualSlipData] -----> CARDHOLDER-NAME was missing" ); }

            boolean isCreditDebitDisclaimer   = (transData.getTransType() == ETransType.VOID
                                                  || transData.getTransType() == ETransType.VOID );
            boolean isRedemptionTrans         = (transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_DISCOUNT
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_VOUCHER_CREDIT
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_PRODUCT_CREDIT
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_VOID );
            boolean isInstallmentTrans        = (transData.getTransType() == ETransType.KBANK_SMART_PAY
                                                  || transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID );
            boolean isVoidTrans               = (transData.getTransType() == ETransType.VOID
                                                  || transData.getTransType() == ETransType.KBANK_REDEEM_VOID
                                                  || transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID );
            boolean isWalletTrans             = (transData.getTransType() == ETransType.QR_INQUIRY_ALIPAY
                                                  || transData.getTransType() == ETransType.QR_VOID_ALIPAY
                                                  || transData.getTransType() == ETransType.QR_INQUIRY_WECHAT
                                                  || transData.getTransType() == ETransType.QR_VOID_WECHAT
                                                  || transData.getTransType() == ETransType.QR_INQUIRY
                                                  || transData.getTransType() == ETransType.QR_VOID_KPLUS );

            // Terminal information (TM)
            String tm_logo_index        =  isNullCheck(EReceiptUtils.StringPadding(String.valueOf( transData.getAcquirer().getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT));
            String tm_addr_ln_01        =  isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN));
            String tm_addr_ln_02        =  isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS));
            String tm_addr_ln_03        =  isNullCheck(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1));
            String tm_terminalID        =  isNullCheck(transData.getAcquirer().getTerminalId());
            String tm_merchantID        =  isNullCheck(transData.getAcquirer().getMerchantId());
            String[] tm_edc_appver      =  isNullCheck(EReceiptUtils.TextSplitter(Utils.getTerminalAndAppVersion(),EReceiptUtils.MAX43_CHAR_PER_LINE));
            //FinancialApplication.getDal().getSys().getTermInfo().get(ETermInfoKey.MODEL) + " " + FinancialApplication.getVersion();

            // Transaction information (TX)
            String tx_trace_numb        = isNullCheck(EReceiptUtils.StringPadding(String.valueOf(transData.getTraceNo()),6,"0", Convert.EPaddingPosition.PADDING_LEFT));
            String tx_batch_numb        = isNullCheck(EReceiptUtils.StringPadding(String.valueOf(transData.getBatchNo()),6,"0", Convert.EPaddingPosition.PADDING_LEFT));
            String tx_stan__numb        = isNullCheck(EReceiptUtils.StringPadding(String.valueOf(transData.getStanNo()), 6,"0", Convert.EPaddingPosition.PADDING_LEFT));
            String tx_cardscheme        = isNullCheck(ReformatCardScheme(transData)[1]); //transData.getIssuer().getName()
            String tx_txn___date        = isNullCheck(TransDateTime(ReturnDateTimeFormat.Date,transData, ReturnDateType.Normal));          //  06/04/2020
            String tx_txn_2_date        = isNullCheck(TransDateTime(ReturnDateTimeFormat.Date,transData, ReturnDateType.SmallAmount));     //  Apr 6, 2020
            String tx_txn_w_date        = isNullCheck(TransDateTime(ReturnDateTimeFormat.Date,transData, ReturnDateType.Wallet));          //  06/04/2020
            String tx_txn___time        = isNullCheck(TransDateTime(ReturnDateTimeFormat.Time,transData,ReturnDateType.Normal));
            String tx_appvl_code        = isNullCheck(transData.getAuthCode());
            String tx_ref___numb        = isNullCheck(transData.getRefNo());
            String tx_emv_applbl        = isNullCheck((isWalletTrans) ? "": transData.getEmvAppLabel());
            String tx_emv_aid_no        = isNullCheck((isWalletTrans) ? "": transData.getAid());
            String tx_tc____code        = isNullCheck((isWalletTrans) ? "": transData.getTc());
            String tx_tvr___code        = isNullCheck((isWalletTrans) ? "": transData.getTvr());
            String tx_trans_type        = isNullCheck(getTransType(transData));
            String tx_trans__amt        = isNullCheck(getTransAmount(transData).replace("฿",new String("THB")));
            String tx__signature        = "[@VF_FORMAT_SIGNATURE]"  ;        // fix this string use for replace with Signature compression with GZIP data

            // Separater (sp)
            String sp_01_dash_ln        = EReceiptUtils.StringPadding("",23,"-", Convert.EPaddingPosition.PADDING_LEFT);
            String sp_02_dash_ln        = EReceiptUtils.StringPadding("",41,"_", Convert.EPaddingPosition.PADDING_LEFT);
            String sp_03_eqal_ln        = EReceiptUtils.StringPadding("",23,"=", Convert.EPaddingPosition.PADDING_LEFT);
            String sp_04_chds_ln        = "----------- CARDHOLDER SIGNATURE ----------";
            String sp_05_dash_ln        = EReceiptUtils.StringPadding("",46,"-", Convert.EPaddingPosition.PADDING_LEFT);
            String sp_06_eqal_ln        = EReceiptUtils.StringPadding("",46,"=", Convert.EPaddingPosition.PADDING_LEFT);
            String sp_d7_sept_ln        = new String(new byte[] {(byte)0xD7});
            String sp_ce_sept_ln        = new String(new byte[] {(byte)0xCE});

            // CardholderData (CH)
            String   ch_card___pan = "";
            String   ch_card__mode = "";
            String[] ch_holdername = null;
            String   ch_exp___date = "";
            if (! isWalletTrans) {
                 ch_card___pan        = isNullCheck(PANTruncate(transData.getPan(), transData));
                 ch_card__mode        = "(" + isNullCheck(PANModeSuffix(transData))+")";
                 ch_holdername        = (transData.getTrack1() != null) ? EReceiptUtils.TextSplitter(transData.getTrack1().trim(),EReceiptUtils.MAX23_CHAR_PER_LINE) : null;
                 ch_exp___date        = "EXP:**/**";
            }

            if (isWalletTrans) {
                strBuilder = new StringBuilder();
                SlipInfos ="";

                /* =======================================================================================
                *                 DISPLAY FIELD           |     KPLUS    |     WECHAT    |     ALIPAY    |
                *  =======================================================================================
                *    1. TRAN ID                           |       Y      |        Y      |       Y       |
                *    2. ACC                               |              |               |       Y       |
                *    3. NO.                               |              |        Y      |       Y       |
                *  =======================================================================================
                * */

                // Extra for Wallet printing
                String tx_trans_wallet_transacId = "";
                String tx_trans_wallet_buyer_acc = "";
                String tx_trans_wallet_partnerId = "";
                String tx_trans_wallet___channel = "";
                String tx_trans_wallet___paytime = "";
//                tx_trans_type = ((transData.getTransState() == TransData.ETransStatus.VOIDED
//                                    || transData.getTransType() == ETransType.QR_VOID_KPLUS
//                                    || transData.getTransType() == ETransType.QR_VOID_ALIPAY
//                                    || transData.getTransType() == ETransType.QR_VOID_WECHAT) ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase());
                boolean isWalletVoidTrans = (transData.getTransType() == ETransType.QR_VOID_KPLUS || transData.getTransType() == ETransType.QR_VOID_ALIPAY || transData.getTransType() == ETransType.QR_VOID_WECHAT) ? true : false ;
                tx_trans_type = ((isWalletVoidTrans) ? Utils.getString(R.string.trans_void).toUpperCase() : Utils.getString(R.string.trans_sale).toUpperCase());
                tx_trans_type += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";
                if (transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)) {
                    tx_trans_wallet_transacId = (transData.getTxnNo() != null ? transData.getTxnNo().trim() : "");
                } else {
                    tx_trans_wallet_transacId = (transData.getWalletPartnerID() != null ? transData.getWalletPartnerID().trim() : "");
                    tx_trans_wallet_partnerId = transData.getWalletPartnerID();
                    if (transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY)||transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                        tx_trans_wallet_buyer_acc = ((transData.getBuyerLoginID() != null ? transData.getBuyerLoginID().trim() : ""));
                    }
                }
                // amount
                long amount = Utils.parseLongSafe(transData.getAmount(), 0);;
                if (transData.getTransType().isSymbolNegative()) {
                    amount = -amount;
                }
                //if (transData.getTransState() == TransData.ETransStatus.VOIDED) {
                if (isWalletVoidTrans) {
                    amount = -amount;
                }
                tx_trans__amt = CurrencyConverter.convert(amount, transData.getCurrency());

                // Channel
                switch (transData.getAcquirer().getName()) {
                    case Constants.ACQ_KPLUS:
                        //String promocode = transData.getPromocode() != null ? transData.getPromocode().trim():"";
                        //tx_trans_wallet___channel = "2".equalsIgnoreCase(promocode)? "Promptpay" :Utils.getString(R.string.receipt_kplus);
                        tx_trans_wallet___channel = (transData.getQrSourceOfFund() != null) ? transData.getQrSourceOfFund() : "-" ;
                        break;
                    case Constants.ACQ_ALIPAY:
                    case Constants.ACQ_ALIPAY_B_SCAN_C:
                        tx_trans_wallet___channel = Utils.getString(R.string.receipt_alipay);
                        break;
                    case Constants.ACQ_WECHAT:
                    case Constants.ACQ_WECHAT_B_SCAN_C:
                        tx_trans_wallet___channel = Utils.getString(R.string.receipt_wechat);
                        break;
                }

                //Pay Time
                if (transData.getPayTime() != null && !Constants.ACQ_KPLUS.equals(transData.getAcquirer().getName())) {
                    tx_trans_wallet___paytime = TimeConverter.convert(transData.getPayTime().trim(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
                } else {
//                    tx_trans_wallet___paytime = (transData.getTransState() == TransData.ETransStatus.VOIDED
//                            || transData.getTransType() == ETransType.QR_VOID_KPLUS
//                            || transData.getTransType() == ETransType.QR_VOID_ALIPAY
//                            || transData.getTransType() == ETransType.QR_VOID_WECHAT) ?
//                            TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
//                            TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
                    tx_trans_wallet___paytime = (isWalletVoidTrans) ?
                                                TimeConverter.convert(transData.getOrigDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY) :
                                                TimeConverter.convert(transData.getDateTime(), Constants.TIME_PATTERN_TRANS, Constants.TIME_PATTERN_DISPLAY);
                }

                Log.d(EReceiptUtils.TAG, " WALLET : ACQUIRER-NAME = " + transData.getAcquirer().getName());
                Log.d(EReceiptUtils.TAG, " WALLET : TRANS_TYPE = " + tx_trans_type);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_logo_index    , BOL_Options.None, EOL_Options.AddCarriageReturn,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("HOST  :" + transData.getAcquirer().getName() ,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TID   :" + tm_terminalID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MID   :" + tm_merchantID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("STAN  :" + tx_stan__numb ,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BATCH :" + tx_batch_numb,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRACE :" + tx_trace_numb,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_txn_2_date , tx_txn___time, ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP.CODE :" ,    tx_appvl_code       , ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                if (transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRAN ID  : ", tx_trans_wallet_transacId ,ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                }
                else {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRAN ID  : ", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(tx_trans_wallet_transacId, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("CHANNEL  : ", tx_trans_wallet___channel ,ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                // this config for alipay host only
                if(transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY) || transData.getAcquirer().getName().equals(Constants.ACQ_ALIPAY_B_SCAN_C)) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("ACC      : ", tx_trans_wallet_buyer_acc ,ConcatModes.MergeContentOnly, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BASE ", tx_trans__amt ,ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                if (! transData.getAcquirer().getName().equals(Constants.ACQ_KPLUS)) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("NO.", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(tx_trans_wallet_partnerId, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Pay Time :", tx_trans_wallet___paytime ,ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I ACKNOWLEDGE SATISFACTORY RECEIPT", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("OF RELATIVE GOODS/SERVICE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[0]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                } else {
                    for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                        if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[idx]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        }
                    }
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*** NO REFUND ***", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("--MERCHANT COPY--", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ;
            }
            else if (isRedemptionTrans) {
                HashMap<String,Object> RedeemInfos = new HashMap<String,Object>();
                RedeemInfos = getRedemptionTransType(transData);

                String tx_rdm_prod_code = (String)RedeemInfos.get("RDM_PROD_CODE");
                String tx_rdm_prod_name = (String)RedeemInfos.get("RDM_PROD_NAME");
                String tx_rdm_amount = (String)RedeemInfos.get("RDM_AMT");
                String tx_rdm_point = ((isVoidTrans && (String)RedeemInfos.get("RDM_POINT") != null) ? "-" : "") + (String)RedeemInfos.get("RDM_POINT");
                String tx_rdm_prod_qty = ((isVoidTrans && (String)RedeemInfos.get("RDM_PROD_QTY") != null) ? "-" : "") + (String)RedeemInfos.get("RDM_PROD_QTY");
                String tx_rdm_bal_point = (String)RedeemInfos.get("RDM_BAL_POINT");
                String tx_rdm_net_cre_amt = (String)RedeemInfos.get("RDM_NET_AMT");
                String tx_rdm_total_amt = ((isVoidTrans && (String)RedeemInfos.get("RDM_TOTAL_AMT") != null) ? "-" : "") + (String)RedeemInfos.get("RDM_TOTAL_AMT");
                String tx_rdm_discount_rate = (String)RedeemInfos.get("RDM_DISC_PERCENT");

                String tmpEnterMode = transData.getEnterMode().toString();
                if (tmpEnterMode.equals("I") && Constants.ISSUER_UP.equals(transData.getIssuer().getIssuerBrand())) {//todo improve later
                    ch_card__mode = "(F)";
                }

                strBuilder = new StringBuilder();
                SlipInfos ="";
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_logo_index    , BOL_Options.None, EOL_Options.AddCarriageReturn,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TID:   " + tm_terminalID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MID:   " + tm_merchantID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BATCH:  " + tx_batch_numb, "HOST: " + transData.getAcquirer().getName(),ConcatModes.AddSpaceBetween2Content ,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRACE:  " + tx_trace_numb,  "STAN: " + tx_stan__numb ,ConcatModes.AddSpaceBetween2Content ,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( sp_06_eqal_ln     , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_card___pan , ch_card__mode, ConcatModes.MergeContentOnly,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_cardscheme , ch_exp___date, ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_txn_2_date , tx_txn___time, ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP.CODE" , tx_appvl_code , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("REF.NO  " , tx_ref___numb , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="";

                switch ((String)RedeemInfos.get("TRANS_TYPE_CODE")) {
                    case "RDM_PROD_NORMAL":
                    case "RDM_PROD_CREDIT":
                        tx_trans_type = (((String)RedeemInfos.get("TRANS_TYPE_CODE")).equals("RDM_PROD_CREDIT") ? "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "PRODUCT + CR" : "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "PRODUCT");
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (transData.isEcrProcess()) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( " " + Utils.getString(R.string.receipt_pos_tran), BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_prod_name != null  && !tx_rdm_prod_name.equals("null") && tx_rdm_prod_code !=null  && !tx_rdm_prod_code.equals("null")) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "PRODUCT CODE",tx_rdm_prod_code , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "PRODUCT NAME" , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "  " + tx_rdm_prod_name.trim() , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        if (tx_rdm_point !=null  && !tx_rdm_point.equals("null"))        { strBuilder.append(SlipInfos); SlipInfos = "" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "REDEEM POINT", tx_rdm_point, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (((String)RedeemInfos.get("TRANS_TYPE_CODE")).equals("RDM_PROD_CREDIT")) {
                            if (tx_rdm_net_cre_amt !=null  && !tx_rdm_net_cre_amt.equals("null")) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "CREDIT AMOUNT",tx_rdm_net_cre_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        }
                        if (tx_rdm_prod_qty != null  && !tx_rdm_prod_qty.equals("null")) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "NO. OF ITEMS",tx_rdm_prod_qty , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_rdm_bal_point !=null  && !tx_rdm_bal_point.equals("null")) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "BAL. POINT",tx_rdm_bal_point , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_04_chds_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        break;
                    case "RDM_VOCH_NORMAL":
                    case "RDM_VOCH_CREDIT":
                        tx_trans_type = (((String)RedeemInfos.get("TRANS_TYPE_CODE")).equals("RDM_VOCH_CREDIT") ? "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "VOUCHER+CR" : "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "VOUCHER");
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (transData.isEcrProcess()) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( " " + Utils.getString(R.string.receipt_pos_tran), BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_prod_code != null  && !tx_rdm_prod_code.equals("null")) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "PRODUCT CODE",tx_rdm_prod_code , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_point !=null  && !tx_rdm_point.equals("null"))          { strBuilder.append(SlipInfos); SlipInfos = "" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "REDEEM POINT", tx_rdm_point, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (tx_rdm_amount !=null  && !tx_rdm_amount.equals("null"))        { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "REDEEM AMOUNT", isVoidTrans ? "-" + tx_rdm_amount : tx_rdm_amount, ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (((String)RedeemInfos.get("TRANS_TYPE_CODE")).equals("RDM_VOCH_CREDIT")) {
                            if (tx_rdm_net_cre_amt !=null  && !tx_rdm_net_cre_amt.equals("null")) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "CREDIT AMOUNT",tx_rdm_net_cre_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            if (tx_rdm_total_amt !=null  && !tx_rdm_total_amt.equals("null"))         { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "TOTAL",tx_rdm_total_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        } else {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_rdm_bal_point !=null  && !tx_rdm_bal_point.equals("null")) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "BAL. POINT",tx_rdm_bal_point , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_04_chds_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        break;
                    case "RDM_DISF_NORMAL":
                    case "RDM_DISV_NORMAL":
                        tx_trans_type = (((String)RedeemInfos.get("TRANS_TYPE_CODE")).equals("RDM_DISF_NORMAL") ? "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "DIS.% FIX PNT." : "REDEEM" + ((isVoidTrans) ? " VOID " : " " ) + "DIS.% VAR PNT.");
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (transData.isEcrProcess()) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( " " + Utils.getString(R.string.receipt_pos_tran), BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_prod_name != null  && !tx_rdm_prod_name.equals("null") && tx_rdm_prod_code !=null  && !tx_rdm_prod_code.equals("null")) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "PRODUCT CODE",tx_rdm_prod_code , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "PRODUCT NAME" , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( " " + tx_rdm_prod_name.trim(), BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        if (tx_rdm_point !=null  && !tx_rdm_point.equals("null"))                 { strBuilder.append(SlipInfos); SlipInfos = "" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "REDEEM POINT", tx_rdm_point, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (tx_rdm_net_cre_amt !=null  && !tx_rdm_net_cre_amt.equals("null"))     { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "CREDIT AMOUNT",tx_rdm_net_cre_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_discount_rate !=null  && !tx_rdm_discount_rate.equals("null")) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "DISCOUNT %",tx_rdm_discount_rate , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        if (tx_rdm_amount !=null  && !tx_rdm_amount.equals("null"))               { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "DISCOUNT AMT",tx_rdm_amount , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_rdm_total_amt !=null  && !tx_rdm_total_amt.equals("null"))         { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "TOTAL",tx_rdm_total_amt , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_06_eqal_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_rdm_bal_point !=null  && !tx_rdm_bal_point.equals("null")) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( "BAL. POINT",tx_rdm_bal_point , ConcatModes.AddSpaceBetween2Content , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_04_chds_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                        break;
                }

                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = tx__signature ;
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                // CardHolderName
                if (ch_holdername !=null) {
                    if(ch_holdername.length ==1 && ch_holdername[0] != null) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[0]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    } else {
                        for (int idx =0 ; idx <= ch_holdername.length-1; idx++){
                            if (ch_holdername[idx] != null && ch_holdername[idx] != ""){
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[idx]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            }
                        }
                    }
                }

                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I ACKNOWLEDGE SATISFACTORY RECEIPT", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("OF RELATIVE GOODS/SERVICE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[0]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                } else {
                    for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                        if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[idx]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        }
                    }
                }
                strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("*** NO REFUND ***", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("* TRUSTED TRANSACTION *", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MERCHANT COPY", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="";
            }
            else if (isInstallmentTrans) {
                HashMap<ReservedFieldHandle.FieldTables, byte[]> f61 = ReservedFieldHandle.unpackReservedField(transData.getField61RecByte(), ReservedFieldHandle.smtp_f61_response, true);
                HashMap<ReservedFieldHandle.FieldTables, byte[]> f63 = ReservedFieldHandle.unpackReservedField(transData.getField63RecByte(), ReservedFieldHandle.smtp_f63_response, true);

                boolean isDolfin = transData.getAcquirer().getName().equals(Constants.ACQ_DOLFIN_INSTALMENT);
                if (isDolfin) {
                    byte[] buf = transData.getField63RecByte();
                    f61 = ReservedFieldHandle.unpackReservedField(buf, ReservedFieldHandle.dolfinIpp_f63_response, true);
                    f63 = transData.isInstalmentPromoProduct()? f61: null;
                }

                String payment_term     = null;
                String int_rate         = null;
                String handing_fee      = null;
                String total_pay_amt    = null;
                String month_pay_amt    = null;
                boolean isSmartPayVoidTrans = (transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID ? true : false);
                boolean isIppVoid = (transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID
                                    || transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID);                                    
                if (f61 != null) {
                    payment_term = transData.getInstalmentPaymentTerm() + " MONTHS";
                    int_rate = new String(f61.get(ReservedFieldHandle.FieldTables.INTEREST_RATE));
                    handing_fee = new String(f61.get(ReservedFieldHandle.FieldTables.HANDING_FEE));
                    total_pay_amt = new String(f61.get(ReservedFieldHandle.FieldTables.TOTAL_PAY_AMOUNT));
                    month_pay_amt = new String(f61.get(ReservedFieldHandle.FieldTables.MONTH_PAY_AMOUNT));

                    long amount =0;
                    int_rate = String.format(Locale.getDefault(), "%.2f", Double.parseDouble(int_rate.substring(0, int_rate.length()-1))/100);
                    amount = Utils.parseLongSafe(handing_fee, 0);
                    handing_fee = CurrencyConverter.convert(amount, transData.getCurrency());

                    amount = Utils.parseLongSafe(total_pay_amt, 0);
                    total_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());

                    amount = Utils.parseLongSafe(month_pay_amt, 0);
                    month_pay_amt = CurrencyConverter.convert(amount, transData.getCurrency());
                }
                String sup_name         = null;
                String prod_name        = null;
                String model_name       = null;
                String serial_no        = null;
                if (f63 != null) {
                    sup_name = new String(f63.get(ReservedFieldHandle.FieldTables.SUPPLIER_NAME)).trim();
                    prod_name = new String(f63.get(ReservedFieldHandle.FieldTables.PRODUCT_NAME)).trim();
                    model_name = new String(f63.get(ReservedFieldHandle.FieldTables.MODEL_NAME)).trim();
                    serial_no = Component.getPaddedStringRight(transData.getInstalmentSerialNo(), 18, '9');
                }
                String tempTransType = Component.getTransByIPlanMode(transData);
                //if(transData.getTransState() == TransData.ETransStatus.VOIDED || transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID){
                if(isSmartPayVoidTrans || isIppVoid){
                    tempTransType = Utils.getString(R.string.trans_void).toUpperCase() + " " + tempTransType;
                }
                strBuilder = new StringBuilder();
                SlipInfos ="";
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_logo_index    , BOL_Options.None, EOL_Options.AddCarriageReturn,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TID:   " + tm_terminalID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MID:   " + tm_merchantID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BATCH:  " + tx_batch_numb, "HOST: " + transData.getAcquirer().getName(),ConcatModes.AddSpaceBetween2Content ,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRACE:  " + tx_trace_numb,  "STAN: " + tx_stan__numb ,ConcatModes.AddSpaceBetween2Content ,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( sp_06_eqal_ln     , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_card___pan , ch_card__mode, ConcatModes.MergeContentOnly,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_cardscheme , ch_exp___date, ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_txn_2_date , tx_txn___time, ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP.CODE" , tx_appvl_code , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("REF.NO  " , tx_ref___numb , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                if (transData.getEnterMode() == TransData.EnterMode.INSERT) {
                    if (tx_emv_applbl != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP: " + tx_emv_applbl, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                    if (tx_emv_aid_no != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("AID: " + tx_emv_aid_no, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                    if (tx_tvr___code != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("TVR: " + tx_tvr___code, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                    if (tx_tc____code != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("TC: " + tx_tc____code, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                }

                strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tempTransType       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                if (transData.isEcrProcess()) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( " " + Utils.getString(R.string.receipt_pos_tran), BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("AMT" ,   tx_trans__amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TERM:" ,     payment_term , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("INT(%):" ,   int_rate , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MGT. FEE:" , handing_fee , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TOT DUE:" ,  total_pay_amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MONTHLY:" ,  month_pay_amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                if (sup_name != null)   {strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Brand: " + sup_name , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                if (prod_name != null)  {strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Product: " + prod_name , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                if (model_name != null) {strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Model: " + model_name , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                if (serial_no != null)  {strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("S/N: " + serial_no , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None); }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_04_chds_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = tx__signature ;
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                // CardHolderName
                if (ch_holdername !=null) {
                    if(ch_holdername.length ==1 && ch_holdername[0] != null) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[0]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    } else {
                        for (int idx =0 ; idx <= ch_holdername.length-1; idx++){
                            if (ch_holdername[idx] != null && ch_holdername[idx] != ""){
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[idx]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            }
                        }
                    }
                }

                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I ACKNOWLEDGE SATISFACTORY RECEIPT", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("OF RELATIVE GOODS/SERVICE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[0]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                } else {
                    for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                        if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[idx]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        }
                    }
                }
                strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("*** NO REFUND ***", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                if(transData.getAcquirer().isEnableTle()) {
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("* TRUSTED TRANSACTION *", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MERCHANT COPY", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                strBuilder.append(SlipInfos) ; SlipInfos ="";
            }
            else {
                // Credit or Debit sale
                strBuilder= new StringBuilder();
                SlipInfos="";
                boolean printFullReceipt = true;
                boolean isCreditDebitVoidTrans = (transData.getTransType() == ETransType.VOID ? true : false);

                if (!printFullReceipt) {
                    // Small amount slip
                    String tx_small_amount_text = transData.isTxnSmallAmt() && !(transData.getEnterMode() == TransData.EnterMode.CLSS || transData.getEnterMode() == TransData.EnterMode.SP200) ? Component.getReceiptTxtSmallAmt(transData.getIssuer().getIssuerBrand()) : null;
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_logo_index    , BOL_Options.None, EOL_Options.AddCarriageReturn,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TID:" + tm_terminalID, "MID:" + tm_merchantID, ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRACE:" + tx_trace_numb, "BATCH:" + tx_batch_numb, ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_cardscheme, tx_txn_2_date + " " + tx_txn___time, ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_card___pan , ch_card__mode, ConcatModes.MergeContentOnly,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type, sp_ce_sept_ln + "APP.CODE : " + tx_appvl_code , ConcatModes.AddSpaceBetween2Content,BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BASE" , tx_trans__amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    if(transData.isPinVerifyMsg()){
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("(PIN VERIFY SUCCESS)", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    } else if (transData.isSignFree() || transData.isTxnSmallAmt()){
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    }


                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                    // CardHolderName
                    if (ch_holdername !=null) {
                        if(ch_holdername.length ==1 && ch_holdername[0] != null) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[0]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        } else {
                            for (int idx =0 ; idx <= ch_holdername.length-1; idx++){
                                if (ch_holdername[idx] != null && ch_holdername[idx] != ""){
                                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[idx]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                                }
                            }
                        }
                    }

                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I ACKNOWLEDGE SATISFACTORY RECEIPT", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("OF RELATIVE GOODS/SERVICE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[0]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    } else {
                        for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                            if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[idx]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            }
                        }
                    }
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*** NO REFUND ***", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    if(transData.getAcquirer().isEnableTle()) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("* TRUSTED TRANSACTION *", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    }
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("--MERCHANT COPY--", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ;
                } else {

                    // DCC is MASTERCARD detect
                    boolean isMasterCard = Constants.ISSUER_MASTER.equals(transData.getIssuer().getName());

                    // Fully print Normal Slip
                    String tx_small_amount_text = transData.isTxnSmallAmt() && !(transData.getEnterMode() == TransData.EnterMode.CLSS || transData.getEnterMode() == TransData.EnterMode.SP200) ? Component.getReceiptTxtSmallAmt(transData.getIssuer().getIssuerBrand()) : null;
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_logo_index    , BOL_Options.None, EOL_Options.AddCarriageReturn,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_01    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_02    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  tm_addr_ln_03    , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("HOST  : " + transData.getAcquirer().getName() ,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TID   : " + tm_terminalID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("MID   : " + tm_merchantID,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("STAN  : " + tx_stan__numb ,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("BATCH : " + tx_batch_numb,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRACE : " + tx_trace_numb,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( sp_05_dash_ln     , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_cardscheme , ch_exp___date, ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_card___pan , ch_card__mode, ConcatModes.MergeContentOnly,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_txn___date , tx_txn___time, ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    if (!tx_ref___numb.isEmpty()) { strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("REF.NO.  :" , tx_ref___numb , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);}
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP.CODE :" , tx_appvl_code , ConcatModes.AddSpaceBetween2Content,BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                    if (transData.getEnterMode() == TransData.EnterMode.INSERT || transData.getEnterMode() == TransData.EnterMode.CLSS || transData.getEnterMode() == TransData.EnterMode.SP200) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_emv_applbl != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("APP: " + tx_emv_applbl, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (tx_emv_aid_no != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("AID: " + tx_emv_aid_no, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (tx_tvr___code != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("TVR: " + tx_tvr___code, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        if (tx_tc____code != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("TC: " + tx_tc____code, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    }

                    if (transData.isDccRequired() == false) {
                        // Credit / Debit card sale
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_small_amount_text != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent(tx_small_amount_text, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TOTAL " , tx_trans__amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("      ",EReceiptUtils.StringPadding("",17,"=", Convert.EPaddingPosition.PADDING_LEFT),ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                        if (transData.isPinVerifyMsg()){
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("(PIN VERIFY SUCCESS)", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        } else {
                            if (transData.isTxnSmallAmt() || transData.isSignFree()) {
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            } else {
                                strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent(sp_04_chds_ln, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                            }
                        }
                    } else if (transData.isDccRequired() ==true) {
                        String currencyNumeric = Tools.bytes2String(transData.getDccCurrencyCode());
                        String currencyCode = Tools.bytes2String(transData.getDccCurrencyCode());
                        String markUp =  Component.unpackField63Dcc(transData);
                        double exRate = (transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0);
                        String exchangeRateStr = String.format(Locale.getDefault(), "%.4f", exRate );
                        long amount = Utils.parseLongSafe(transData.getDccAmount(), 0);
                        //if (transData.getTransType().isSymbolNegative() || transData.getTransState() == TransData.ETransStatus.VOIDED) {amount = -amount;}
                        if (transData.getTransType().isSymbolNegative() || isCreditDebitVoidTrans) {amount = -amount;}
                        String strAmt = CurrencyConverter.convert(amount, currencyNumeric);
                        String DCC_Total_checkmark_currencycode = ((!isMasterCard) ? "[/] " + strAmt : strAmt);

                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("CURRENCY CODE" , currencyCode , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tx_trans_type       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        if (tx_small_amount_text != null) { strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent(tx_small_amount_text, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed, TextAlignment.None); }
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TOTAL" , tx_trans__amt , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("      ",EReceiptUtils.StringPadding("",17,"=", Convert.EPaddingPosition.PADDING_LEFT),ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("EX. RATE" , exchangeRateStr , ConcatModes.AddSpaceBetween2Content, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TRANSACTION CURRENCY" , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("TOTAL" , DCC_Total_checkmark_currencycode , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(" " , "Margin = " + markUp + "%" , ConcatModes.AddSpaceBetween2Content, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                        String DCC_Disclaimer = "";
                        if (isMasterCard) {
                            //DCC_Disclaimer = Utils.getString(R.string.receipt_dcc_ack_mastercard);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I HAVE CHOSEN NOT TO USE THE MASTERCARD", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("CURRENCY CONVERSION METHOD AND I WILL", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("HAVE NO RECOURSE AGAINST MASTERCARD", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("CONCERNING THE CURRENCY CONVERSION OR ITS", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("DISCLOSURE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        } else {
                            //DCC_Disclaimer = Utils.getString(R.string.receipt_dcc_ack_visa, CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true),CurrencyConverter.getCurrencySymbol(currencyNumeric, true),"%");
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I acknowledged that I have been offered a ", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("choice of payment currencies including", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(String.format("%1$s and agreed to pay in %2$s", CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true), CurrencyConverter.getCurrencySymbol(currencyNumeric, true),"%"), BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Dynamic Currency Conversion (DCC) is", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Conducted by the merchant based on the", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(String.format("export sight bill plus %3$s International", CurrencyConverter.getCurrencySymbol(CurrencyConverter.getDefCurrency(), true), CurrencyConverter.getCurrencySymbol(currencyNumeric, true), markUp + "%"), BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Conversion Margin. Card holder expressly", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("agrees to the transaction receipt", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("information by marking the accept box [x]", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("below", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }

                        if (transData.isPinVerifyMsg()){
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("(PIN VERIFY SUCCESS)", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        } else {
                            if (transData.isTxnSmallAmt() || transData.isSignFree()) {
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("*NO SIGNATURE REQUIRED*", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            } else {
                                strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent(sp_04_chds_ln, BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                            }
                        }
                    }

                    if (! transData.isPinVerifyMsg()) {
                        if (!transData.isTxnSmallAmt() || !transData.isSignFree()) {
                            strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = tx__signature;
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                        }
                    }

                    if (transData.isDccRequired() && ! isMasterCard==true) {
                        strBuilder.append(SlipInfos); SlipInfos = ""; SlipInfos = EReceiptUtils.getInstance().WrapContent("[/] SIGN X: ", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed, TextAlignment.None);
                    }
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent(  sp_05_dash_ln    , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);

                    // CardHolderName
                    if (ch_holdername !=null) {
                        if(ch_holdername.length ==1 && ch_holdername[0] != null) {
                            strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[0]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                        } else {
                            for (int idx =0 ; idx <= ch_holdername.length-1; idx++){
                                if (ch_holdername[idx] != null && ch_holdername[idx] != ""){
                                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( ch_holdername[idx]       , BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                                }
                            }
                        }
                    }

                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("I ACKNOWLEDGE SATISFACTORY RECEIPT", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("OF RELATIVE GOODS/SERVICE", BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    if(tm_edc_appver.length ==1 && tm_edc_appver[0] != null) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[0]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    } else {
                        for (int idx =0 ; idx <= tm_edc_appver.length-1; idx++){
                            if (tm_edc_appver[idx] != null && tm_edc_appver[idx] != ""){
                                strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent( tm_edc_appver[idx]       , BOL_Options.HalSizefWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                            }
                        }
                    }
                    strBuilder.append(SlipInfos) ; SlipInfos =""; SlipInfos = EReceiptUtils.getInstance().WrapContent("*** NO REFUND ***", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);

                    if(transData.getAcquirer().isEnableTle()) {
                        strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("* TRUSTED TRANSACTION *", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    }

                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("--MERCHANT COPY--", BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.Center);
                    strBuilder.append(SlipInfos) ; SlipInfos ="";
                }

                if (transData.isEcrProcess() == true) {
                    String EcrTicketNumber = new String(EcrData.instance.User_ID);
                    String EcrUserName     = new String(EcrData.instance.CashierName);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("Ticket No. : ",EcrTicketNumber, ConcatModes.MergeContentOnly, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                    strBuilder.append(SlipInfos) ; SlipInfos ="" ; SlipInfos = EReceiptUtils.getInstance().WrapContent("USER Name  : ",EcrUserName,     ConcatModes.MergeContentOnly, BOL_Options.NormalSizeWidth, EOL_Options.AddNewLineFeed,TextAlignment.None);
                }
                strBuilder.append(SlipInfos) ; SlipInfos ="" ;

                return strBuilder.toString();
            }
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG,"-------------------> Error during [CreateVirtualSlipData]  :  " + ex.getMessage() + "     Line.No: " + ex.getStackTrace()[0].getFileName() + "." + ex.getStackTrace()[0].getMethodName() + "." + ex.getStackTrace()[0].getLineNumber());
            return "";
        }

        return strBuilder.toString();
    }
    private String[] cardholderNameReformat (String exCardholderName, int MaxLenInput) {
        String tmpCHD = exCardholderName.replace("."," ").replace("-"," ");
        String[] chd_name_splitter = tmpCHD.split(" ");
        int len = (tmpCHD.length()/MaxLenInput) + (((tmpCHD.length() % MaxLenInput) > 0) ? 1 : 0);
        String[] exportCardHolderName = new String[len];
        String innerLineStr = "";
        int cummulateLineLen =0; int exportIndex =0; int cummulateIndex =0; ; int lastIndexStrCutter =0; int currLoopLen = 0;
        for (int idx = 0 ; idx <= chd_name_splitter.length-1; idx ++) {
            currLoopLen = chd_name_splitter[idx].length() + ((idx==0) ? 0 : 1);
            cummulateLineLen += currLoopLen ;
            if (cummulateLineLen >= MaxLenInput) {
                cummulateIndex += cummulateLineLen - currLoopLen+1 ;
                cummulateLineLen=0;
                exportCardHolderName[exportIndex] = exCardholderName.substring(lastIndexStrCutter, cummulateIndex).replace("."," ");
                lastIndexStrCutter= cummulateIndex;
                exportIndex+=1;
            }

            if (idx == chd_name_splitter.length-1) {
                exportCardHolderName[exportIndex] = exCardholderName.substring(lastIndexStrCutter, exCardholderName.length()).replace("."," ");
                break;
            }
        }
        if (exportCardHolderName[1] == null ) {exportCardHolderName[1] = "";}

        return exportCardHolderName;
    }
    private HashMap<String,Object> getRedemptionTransType(TransData localTransData) {
        HashMap<String,Object> ReturnObj = new HashMap<String,Object>();
        HashMap<ReservedFieldHandle.FieldTables, byte[]> f63 = ReservedFieldHandle.unpackReservedField(localTransData.getField63RecByte(), ReservedFieldHandle.redeemed_response, true);
        ETransType transType = localTransData.getTransType() == ETransType.KBANK_REDEEM_VOID ? localTransData.getOrigTransType() : localTransData.getTransType();

        switch (transType) {
            case KBANK_REDEEM_PRODUCT:
            case KBANK_REDEEM_PRODUCT_CREDIT:
                ReturnObj.put("TRANS_TYPE_CODE", (transType==ETransType.KBANK_REDEEM_PRODUCT) ? "RDM_PROD_NORMAL" : "RDM_PROD_CREDIT");
                ReturnObj.put("TRANS_TYPE", (transType==ETransType.KBANK_REDEEM_PRODUCT) ? "REDEEM PRODUCT" : "REDEEM PRODUCT + CREDIT");
                ReturnObj.put("RDM_PROD_CODE",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_CD)));
                ReturnObj.put("RDM_PROD_NAME",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_NAME)));
                ReturnObj.put("RDM_POINT", String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_PT)))));
                ReturnObj.put("RDM_PROD_QTY", String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.QTY)))));
                ReturnObj.put("RDM_BAL_POINT",String.format(Locale.getDefault(),"%,d", Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.BAL_PT)))));
                if (transType == ETransType.KBANK_REDEEM_PRODUCT_CREDIT) {
                    long net_amt = Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT)), 0);
                    if (localTransData.getTransType() == ETransType.KBANK_REDEEM_VOID) {
                        net_amt = net_amt > 0 ? -(net_amt) : net_amt;
                    }
                    ReturnObj.put("RDM_NET_AMT", CurrencyConverter.convert(net_amt, transData.getCurrency()));
                }
                break;
            case KBANK_REDEEM_VOUCHER:
            case KBANK_REDEEM_VOUCHER_CREDIT:
                ReturnObj.put("TRANS_TYPE_CODE", (transType==ETransType.KBANK_REDEEM_VOUCHER_CREDIT) ? "RDM_VOCH_CREDIT" : "RDM_VOCH_NORMAL");
                ReturnObj.put("TRANS_TYPE", (transType==ETransType.KBANK_REDEEM_VOUCHER_CREDIT) ? "REDEEM VOUCHER + CREDIT" : "REDEEM VOUCHER");
                ReturnObj.put("RDM_PROD_CODE",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_CD)));
                ReturnObj.put("RDM_PROD_NAME",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_NAME)));
                ReturnObj.put("RDM_POINT",String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_PT)))));
                ReturnObj.put("RDM_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_AMT)),0), transData.getCurrency()));
                ReturnObj.put("RDM_BAL_POINT",String.format(Locale.getDefault(),"%,d", Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.BAL_PT)))));
                ReturnObj.put("RDM_TOTAL_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.SALES_AMT)),0), transData.getCurrency()));
                if (transType == ETransType.KBANK_REDEEM_VOUCHER_CREDIT) {
                    long net_amt = Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT)), 0);
                    if (localTransData.getTransType() == ETransType.KBANK_REDEEM_VOID) {
                        net_amt = net_amt > 0 ? -(net_amt) : net_amt;
                    }
                    ReturnObj.put("RDM_NET_AMT", CurrencyConverter.convert(net_amt, transData.getCurrency()));
                }
                break;
            case KBANK_REDEEM_DISCOUNT:
                if ("89999".equals(transData.getRedeemedDiscountType())) {
                    ReturnObj.put("TRANS_TYPE_CODE", "RDM_DISV_NORMAL");
                    ReturnObj.put("TRANS_TYPE","REDEEM DIS.% VAR POINT");
                } else {
                    ReturnObj.put("TRANS_TYPE_CODE", "RDM_DISF_NORMAL");
                    ReturnObj.put("TRANS_TYPE","REDEEM DIS.% FIX POINT");
                }
                ReturnObj.put("RDM_PROD_CODE",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_CD)));
                ReturnObj.put("RDM_PROD_NAME",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_NAME)));
                ReturnObj.put("RDM_POINT",String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_PT)))));

                long net_amt = Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT)), 0);
                if (localTransData.getTransType() == ETransType.KBANK_REDEEM_VOID) {
                    net_amt = net_amt > 0 ? -(net_amt) : net_amt;
                }
                ReturnObj.put("RDM_NET_AMT", CurrencyConverter.convert(net_amt, transData.getCurrency()));

                ReturnObj.put("RDM_DISC_PERCENT", String.format(Locale.getDefault(), "%.2f", Double.parseDouble(new String(f63.get(ReservedFieldHandle.FieldTables.BALANCE_RATE)))/100));
                ReturnObj.put("RDM_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_AMT)), 0), transData.getCurrency()));
                ReturnObj.put("RDM_TOTAL_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.SALES_AMT)),0), transData.getCurrency()));
                ReturnObj.put("RDM_BAL_POINT",String.format(Locale.getDefault(),"%,d", Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.BAL_PT)))));
                break;
            case KBANK_REDEEM_VOID:
                ReturnObj.put("TRANS_TYPE_CODE", "RDM_VOID_NORMAL");
                ReturnObj.put("RDM_PROD_CODE",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_CD)));
                ReturnObj.put("RDM_PROD_NAME",new String(f63.get(ReservedFieldHandle.FieldTables.PROD_NAME)));
                ReturnObj.put("RDM_POINT",String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_PT)))));
                ReturnObj.put("RDM_PROD_QTY", String.valueOf(Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.QTY)))));
                ReturnObj.put("RDM_BAL_POINT",String.format(Locale.getDefault(),"%,d", Integer.parseInt(new String(f63.get(ReservedFieldHandle.FieldTables.BAL_PT)))));

                long net_amtt = Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.NET_SALES_AMT)), 0);
                if (localTransData.getTransType() == ETransType.KBANK_REDEEM_VOID) {
                    net_amtt = net_amtt > 0 ? -(net_amtt) : net_amtt;
                }
                ReturnObj.put("RDM_NET_AMT", CurrencyConverter.convert(net_amtt, transData.getCurrency()));

                ReturnObj.put("RDM_DISC_PERCENT", String.format(Locale.getDefault(), "%.2f", Double.parseDouble(new String(f63.get(ReservedFieldHandle.FieldTables.BALANCE_RATE)))/100));
                ReturnObj.put("RDM_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.REDEEMED_AMT)), 0), transData.getCurrency()));
                ReturnObj.put("RDM_TOTAL_AMT", CurrencyConverter.convert(Utils.parseLongSafe(new String(f63.get(ReservedFieldHandle.FieldTables.SALES_AMT)),0), transData.getCurrency()));
                break;
            default:
                ReturnObj.put("TRANS_TYPE","-");
                break;
        }


        return ReturnObj;
    }

    private String PANTruncate (String PAN, TransData transData) {
        String panMask = "" ;

        panMask = (transData.getAcquirer().getNii().equals("003") )
                ? PanUtils.maskCardNo(transData.getPan(), Constants.PAN_MASK_PATTERN4)
                : PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern());

        //panMask = panMask.replace("*","X");
//        String tmpPan = "";
//        int tmpLen=0;
//        for (int idx=0 ; idx <= panMask.length()-1 ;) {
//            tmpLen = ((idx + 4 >panMask.length()) ? panMask.length()-idx : 4 );
//            tmpPan += ((idx==0) ? "" : " ") + panMask.substring(idx, idx + tmpLen);
//            idx+=tmpLen;
//        }
//        panMask = tmpPan;

        return panMask;
    }
    private String PANModeSuffix(TransData transData) {
        String tmpEnterMode = transData.getEnterMode().toString();
        if (tmpEnterMode.equals("I")) {
            tmpEnterMode = "C";
        } else if (transData.getEnterMode() == TransData.EnterMode.CLSS || tmpEnterMode.equals("P")) {
            tmpEnterMode = "CTLS";
        }
        return tmpEnterMode ;
    }
    private enum ReturnDateTimeFormat {Date, Time}
    private enum ReturnDateType{Normal, SmallAmount, Wallet}
    private String TransDateTime(ReturnDateTimeFormat retFormat, TransData transData, ReturnDateType types) {
        String returnDate = "";
        String calDateTime = (transData.getTransState() == TransData.ETransStatus.VOIDED) ? transData.getOrigDateTime() : transData.getDateTime() ;
        String TranDateTime = TimeConverter.convert(calDateTime, Constants.TIME_PATTERN_DISPLAY, Constants.DATE_PATTERN_DISPLAY);

        switch (retFormat) {
            case Date   :
                if (types==ReturnDateType.SmallAmount) {
                    returnDate =  getMonthShort(TranDateTime.substring(4,6)) + " " + Integer.parseInt(TranDateTime.substring(6,8))  + ", " + TranDateTime.substring(0,4);
                } else if (types==ReturnDateType.Normal)  {
                    returnDate =  TranDateTime.substring(6,8) + " " + getMonthShort(TranDateTime.substring(4,6)) + " " + TranDateTime.substring(2,4);
                } else if (types==ReturnDateType.Wallet)  {
                    returnDate =  TranDateTime.substring(0,4) + "/" + getMonthShort(TranDateTime.substring(4,6)) + "/" + TranDateTime.substring(6,8);
                }

                break;
            case Time   :
                returnDate =  TranDateTime.substring(8,10) + ":" + TranDateTime.substring(10,12) + ":" + TranDateTime.substring(12,14);
                break;
        }

        return returnDate;
    }
    private String getMonthShort(String MonthNumber) {
        switch (MonthNumber) {
            case "01" :  return "Jan" ;
            case "02" :  return "Feb" ;
            case "03" :  return "Mar" ;
            case "04" :  return "Apr" ;
            case "05" :  return "May" ;
            case "06" :  return "Jun" ;
            case "07" :  return "Jul" ;
            case "08" :  return "Aug" ;
            case "09" :  return "Sep" ;
            case "10" :  return "Oct" ;
            case "11" :  return "Nov" ;
            case "12" :  return "Dec" ;
            default: return null;
        }
    }
    private String getTransType (TransData transData){
        ETransType transType = transData.getTransType();
        TransData.ETransStatus transStatus = transData.getTransState();
        String temp = "";//transStatus.equals(TransData.ETransStatus.NORMAL) ? "" : " (" + transStatus.toString() + ")";
        String returnStr = "";
        if (transData.getAcquirer().getNii().equals("003") && transData.getReferralStatus() != null && transData.getReferralStatus() != TransData.ReferralStatus.NORMAL) {
            returnStr =  transType + Utils.getString(R.string.receipt_amex_call_issuer) + temp;
        } else if(transType == ETransType.VOID){
            temp = (! (ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType()))
                    ? Utils.getString(R.string.receipt_void_sale)
                    : Utils.getString(R.string.receipt_void_offline);
            returnStr =  temp;
        } else {
            temp = (! (ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType()))
                    ? (transType + temp)
                    : transType.getTransName().toUpperCase();
            returnStr =  temp;
        }

        /*if ((! (ETransType.OFFLINE_TRANS_SEND == transType || ETransType.OFFLINE_TRANS_SEND == transData.getOrigTransType())) && transData.getOfflineSendState() != null) {
            if (returnStr.lastIndexOf("VOID") != -1) {
                returnStr = returnStr.replace(" ", " " + Utils.getString(R.string.trans_offline).toUpperCase() + " ");
            } else {
                returnStr = Utils.getString(R.string.trans_offline).toUpperCase() + " " + returnStr;
            }
        }*/

        returnStr += transData.isEcrProcess() ? " " + Utils.getString(R.string.receipt_pos_tran) : "";

        return returnStr;
    }
    private String getTransAmount (TransData transData) {
        long amount = Utils.parseLongSafe(transData.getAmount(), 0);
        boolean isWalletVoidTrans       = (transData.getTransType() == ETransType.QR_VOID_ALIPAY || transData.getTransType() == ETransType.QR_VOID_WECHAT || transData.getTransType() == ETransType.QR_VOID_KPLUS) ? true : false;
        boolean isCreditDebitVoidTrans  = (transData.getTransType() == ETransType.VOID ) ? true : false;
        boolean isSmartPayVoidTrans     = (transData.getTransType() == ETransType.KBANK_SMART_PAY_VOID) ? true : false;
        boolean isRedeemVoidTrans       = (transData.getTransType() == ETransType.KBANK_REDEEM_VOID) ? true : false;
        boolean isDolfinIppVoidTrans    = (transData.getTransType() == ETransType.DOLFIN_INSTALMENT_VOID);

        if (transData.getTransType().isSymbolNegative()
                || isWalletVoidTrans
                || isCreditDebitVoidTrans
                || isSmartPayVoidTrans
                || isRedeemVoidTrans
                || isDolfinIppVoidTrans)
        {
            amount = -amount;
        }
        return CurrencyConverter.convert(amount, transData.getCurrency());
    }


    private void handleUploadFlag(boolean isUploadSuccess) {
        boolean isEnablePrintAfterTxn = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN);
        int eReceiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP);
        int edcReceiptNum = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_RECEIPT_NUM);
        int eReceiptNumUnableUpload = FinancialApplication.getSysParam().get(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD);
        if (isUploadSuccess) {
            boolean smallAmtWithMerchant = (transData.isTxnSmallAmt() && !(transData.getNumSlipSmallAmt() == 0 || transData.getNumSlipSmallAmt() == 1));
            boolean normalTxnPrnMerchant = (isEnablePrintAfterTxn && (eReceiptNum == 1 || eReceiptNum == 2)) || (!isEnablePrintAfterTxn && (edcReceiptNum == 1 || edcReceiptNum == 2));
            transData.setEReceiptManualPrint(smallAmtWithMerchant || normalTxnPrnMerchant);
        } else {
            boolean smallAmtWithMerchant = (transData.isTxnSmallAmt() && !(transData.getNumSlipSmallAmt() == 0 || transData.getNumSlipSmallAmt() == 1));
            boolean normalTxnPrnMerchant = !isEnablePrintAfterTxn && (edcReceiptNum == 1 || edcReceiptNum == 2);
            boolean notUploadPrnMerchant = isEnablePrintAfterTxn && (eReceiptNumUnableUpload == 1 || eReceiptNumUnableUpload == 2);
            if (isEnablePrintAfterTxn) {
                transData.setEReceiptManualPrint(notUploadPrnMerchant);
            } else {
                transData.setEReceiptManualPrint(smallAmtWithMerchant || normalTxnPrnMerchant);
            }
        }
        transData.setEReceiptReprint(transData.isEReceiptManualPrint());
        FinancialApplication.getTransDataDbHelper().updateTransData(transData);
    }

    private TransData initialEReceiptInfomationUpload () {
        TransData eReceiptTrans = new TransData(transData);
        eReceiptTrans.setTransType(ETransType.ERCEIPT_UPLOAD);
        eReceiptTrans.setAcquirer(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE));
        eReceiptTrans.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
        transInit(eReceiptTrans, eReceiptTrans.getAcquirer());
        return eReceiptTrans;
    }

    private TransData initialEReceiptInfomationUpload(TransData origTransData) {
        if (origTransData != null) {
            TransData eReceiptTrans = new TransData(origTransData);
            eReceiptTrans.setTransType(ETransType.ERCEIPT_UPLOAD);
            eReceiptTrans.setAcquirer(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE));
            eReceiptTrans.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
            transInit(eReceiptTrans, eReceiptTrans.getAcquirer());
            return eReceiptTrans;
        }
        return null;
    }

    private int doPreSettlement() {
        boolean isKeepTxnToPrnBeforeSettle = FinancialApplication.getSysParam().get(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE);

        int ret = uploadAllEReceipt(isKeepTxnToPrnBeforeSettle);
        if (ret != TransResult.SUCC) {
            return ret;
        }
        ret = uploadAllExternalReceipt(true);
        if (ret != TransResult.SUCC) {
            return ret;
        }

        List<TransData> failedUploads = FinancialApplication.getTransDataDbHelper().findAllEReceiptUploadFail(acquirers);
        if (failedUploads != null && !isKeepTxnToPrnBeforeSettle) {
            for (TransData transUpload : failedUploads) {
                if (transUpload.getTransState() == TransData.ETransStatus.VOIDED) {
                    TransData origTxnPrint = FinancialApplication.getTransDataDbHelper().findTransDataByStanNo(transUpload.getVoidStanNo(), false);
                    transUpload.setTransState(TransData.ETransStatus.NORMAL);//set for print only, not impact txn.
                    transUpload.setDateTime(origTxnPrint.getOrigDateTime());//set for print only, not impact txn.
                }
                switch (transUpload.getAcquirer().getName()) {
                    case Constants.ACQ_REDEEM:
                    case Constants.ACQ_REDEEM_BDMS:
                        saveEReceiptImage(Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.REDEEM, transUpload, transUpload.isEReceiptReprint(), true, (Activity) context));
                        break;
                    case Constants.ACQ_SMRTPAY:
                    case Constants.ACQ_SMRTPAY_BDMS:
                    case Constants.ACQ_DOLFIN_INSTALMENT:
                        saveEReceiptImage(Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.SMARTPAY, transUpload, transUpload.isEReceiptReprint(), true, (Activity) context));
                        break;
                    default:
                        saveEReceiptImage(Printer.printEReceiptSlip(ReceiptPrintTransForERM.PrintType.DEFAULT, transUpload, transUpload.isEReceiptReprint(), true, (Activity) context));
                        break;
                }
            }
        }

        return TransResult.SUCC;
    }


    private List<String> getSelectedAcquirersName() {
        ArrayList<String> selAcqNameList = new ArrayList<String>();
        if (acquirers != null || acquirers.size()>0) {
            for (Acquirer acq : acquirers) {
                selAcqNameList.add(acq.getName());
            }
        }

        return selAcqNameList;
    }

    private HashMap<String, HashMap<String, String>> getExternalAppSavedFiles() {
        HashMap<String, HashMap<String, String>> savedFileList = new HashMap<String, HashMap<String, String>>();
        List<String> acqSelectedNameList = getSelectedAcquirersName();
        if (acqSelectedNameList.size()==0) {
            return savedFileList;
        }

        String path = EReceiptUtils.getERM_ExternalAppRootDirectory(FinancialApplication.getApp().getApplicationContext());


        File rootDir = new File(path);
        if (rootDir.isDirectory() && rootDir.listFiles().length > 0) {
            for (File acqDir: rootDir.listFiles()) {
                HashMap<String, String> fileNames = new HashMap<String, String>();
                if (acqDir.isDirectory() && acqDir.listFiles().length > 0 && acqSelectedNameList.contains(acqDir.getName())) {
                    for (File uploadFile : acqDir.listFiles()) {
                        if (uploadFile.isFile() && uploadFile.getName().endsWith(".erm")) {
                            fileNames.put(uploadFile.getName(), acqDir.getName());
                        }
                    }

                    if (fileNames.size() > 0) {
                        savedFileList.put(acqDir.getAbsolutePath(), fileNames);
                    }
                }
            }
        }

        return savedFileList;
    }

    private ETransType getTransTypeforExternalAppUploadERCM(Acquirer acquirer) {
        if (acquirer.getName().equals(Constants.ACQ_KCHECKID)) {
            return ETransType.KCHECKID_DUMMY;
        } else {
            return  ETransType.ERM_MULTI_APP_UPLOAD;
        }
    }

    private int uploadAllExternalReceipt(boolean isFromMultiApp) {
        int returnResult = TransResult.ERCM_UPLOAD_FAIL;
        try {
            HashMap<String, HashMap<String, String>> savedUploadList = getExternalAppSavedFiles();
            if (savedUploadList.size() > 0) {
                int maxSize = savedUploadList.size();
                String[] keys = savedUploadList.keySet().toArray(new String[maxSize]);
                for (String path : keys) {
                    HashMap<String, String> fileNamesList = savedUploadList.get(path);
                    int fileCount = fileNamesList.size();
                    String[] fileNameKeys = fileNamesList.keySet().toArray(new String[fileCount]);
                    for (String targFileName: fileNameKeys) {
                        String fullName = path + File.separator + targFileName;
                        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(fileNamesList.get(targFileName));
                        ETransType transType = getTransTypeforExternalAppUploadERCM(acquirer);
                        try {
                            File file = new File(fullName);
                            byte[] byteStream = new byte[(int)file.length()];
                            FileInputStream fIpS = new FileInputStream(file);
                            fIpS.read(byteStream, 0, byteStream.length);
                            fIpS.close();

                            int result;
                            File config = null;
                            if (transType == ETransType.KCHECKID_DUMMY) {
                                initTransDataForExternalAppUploadErm(targFileName, acquirer, transType, byteStream);
                                result = trySendEReceiptfromFile(path, targFileName, acquirer, transType, isFromMultiApp);
                            } else {
                                config = new File(fullName.replace(".erm",".irm"));
                                byte[] configByteStream = new byte[(int)config.length()];
                                fIpS = new FileInputStream(config);
                                fIpS.read(configByteStream, 0, configByteStream.length);
                                fIpS.close();

                                String jsonTransData = new String(configByteStream, "UTF-8");
                                TransData paymentTransData = (new GsonBuilder().create().fromJson(jsonTransData, TransData.class));

                                result = uploadPreSettleErmFromFile(paymentTransData, targFileName.replace(".erm",""), transType, acquirer);
                            }

                            if (result==TransResult.SUCC) {
                                file.delete();
                                if (config!=null && config.exists()) {config.delete();}
                                returnResult = TransResult.SUCC;
                            } else {
                                returnResult = TransResult.ERCM_UPLOAD_FAIL;
                                Log.d(EReceiptUtils.TAG, "Failed during upload data of AcquirerName = " + acquirer.getName() + " file : " + fullName );
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(EReceiptUtils.TAG, "Error on sending upload ERM of AcquirerName = " + acquirer.getName() + " file : " + fullName );
                        }
                    }
                    if (returnResult==TransResult.ERCM_UPLOAD_FAIL) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
           e.printStackTrace();
           Log.e(EReceiptUtils.TAG, "Upload for external application using files failed");
        }

        return returnResult;
    }

    private void initTransDataForExternalAppUploadErm(String FileName, Acquirer targetAcq, ETransType transType, byte[] eReceiptData) {
        String localFileName = FileName.replace(".erm","");
        int iRefNo = localFileName.indexOf("REF") + 3 ;
        String refNo = localFileName.substring(iRefNo);

        TransData localTransData = new TransData();
        localTransData.setDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
        localTransData.setHeader("");
        //localTransData.setTpdu("600" + targetAcq.getNii() + "8000");
        localTransData.setDupReason("06");
        localTransData.setCurrency(CurrencyConverter.getDefCurrency());
        localTransData.setReferralStatus(TransData.ReferralStatus.NORMAL);

        localTransData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO, 1));
        localTransData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO, 1));
        localTransData.setBatchNo(targetAcq.getCurrBatchNo());
        localTransData.seteSlipFormat(eReceiptData);

        localTransData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(targetAcq.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
        localTransData.setInitAcquirerNii(targetAcq.getNii());
        localTransData.setInitAcquirerName(targetAcq.getName());
        localTransData.setPan("XXXXXXXXX0000");
        localTransData.setAmount("0000000000");
        localTransData.setRefNo(refNo);
        localTransData.setAuthCode("000000");
        localTransData.setAcquirer(targetAcq);
        localTransData.setReversalStatus(TransData.ReversalStatus.NORMAL);
        localTransData.setTransType(transType);
        localTransData.setTransState(TransData.ETransStatus.NORMAL);
        localTransData.setExpDate("XXXX");

        transData = localTransData;
    }

    private int uploadAllEReceipt(boolean isRequirePrnBeforeSettle) {

        int maxRetryAttempConnectErcm = 3 ;
        int curRetryAttempConnectErcm = 0 ;

        for (int i = 0; i < ERECEIPT_RETRY; i++) {
            List<TransData> allEReceiptPending = FinancialApplication.getTransDataDbHelper().findAllEReceiptPending(acquirers, ERECEIPT_RETRY);
            int sendCount = 0;
            if (allEReceiptPending != null && !allEReceiptPending.isEmpty()) {
                for (TransData origTransData : allEReceiptPending) {
                    if (curRetryAttempConnectErcm < maxRetryAttempConnectErcm){
                        sendCount++;
                        transProcessListenerImpl.onUpdateProgressTitle("E-Receipt Upload" + "-Retry(#" + (i+1) + ")" + "[" + sendCount + "/" + allEReceiptPending.size() + "]");
                        origTransData.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
                        TransData eReceiptOnline = initialEReceiptInfomationUpload(origTransData);
                        int ret = this.online.online(eReceiptOnline, transProcessListenerImpl);

                        origTransData.seteReceiptRetry(origTransData.geteReceiptRetry() + 1);
                        if (ret == TransResult.SUCC && eReceiptOnline.getResponseCode() != null && eReceiptOnline.getResponseCode().getCode().equals("00")) {
                            curRetryAttempConnectErcm=0;
                            origTransData.seteReceiptUploadStatus(TransData.UploadStatus.NORMAL);
                        } else {
                            if (curRetryAttempConnectErcm < 3)  {curRetryAttempConnectErcm+=1;}
                            if (origTransData.geteReceiptRetry() == ERECEIPT_RETRY || curRetryAttempConnectErcm == maxRetryAttempConnectErcm) {
                                origTransData.seteReceiptUploadStatus(TransData.UploadStatus.UPLOAD_FAILED_MANUAL_PRINT);
                                origTransData.setEReceiptManualPrint(!origTransData.isEReceiptManualPrint() ? isRequirePrnBeforeSettle : origTransData.isEReceiptManualPrint());
                            }
                        }
                    } else {
                        origTransData.seteReceiptUploadStatus(TransData.UploadStatus.UPLOAD_FAILED_MANUAL_PRINT);
                        origTransData.setEReceiptManualPrint(!origTransData.isEReceiptManualPrint() ? isRequirePrnBeforeSettle : origTransData.isEReceiptManualPrint());
                    }
                    FinancialApplication.getTransDataDbHelper().updateTransData(origTransData);
                    transProcessListenerImpl.onHideProgress();
                }

                if (curRetryAttempConnectErcm < maxRetryAttempConnectErcm){
                    i=ERECEIPT_RETRY;
                }
            }
        }
        return TransResult.SUCC;
    }

    private void saveEReceiptImage(Bitmap bitmap) {
        if (bitmap != null) {
            String subFolder = Device.getTime(Constants.DATE_PATTERN);
            String path = Environment.getExternalStorageDirectory().toString() + File.separator + "BPSeReceipt" + File.separator + subFolder;

            File dir = new File(path);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String time = Device.getTime(Constants.TIME_PATTERN_TRANS3);
            String fileName = "receipt-" + time + ".png";

            try (FileOutputStream fos = new FileOutputStream(path + File.separator + fileName)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e1) {
                Log.w(TAG, e1);
            }
        }
    }

    private boolean check_PendingUpload_eSettleReport(){
        try {
            String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();
            File dir = new File(path);
            if (dir.isDirectory() && dir.listFiles().length > 0) {
                Log.d(EReceiptUtils.TAG,"                      >> reUpload E-Settle-Report count : " + dir.listFiles().length);
                return true;
            } else {
                Log.d(EReceiptUtils.TAG,"                      >> reUpload E-Settle-Report wasn't found.");
            }
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG,"re-upload eSettleReport target external storage was missing [" + EReceiptUtils.getERM_UnsettleExternalStorageDirectory() + "]" );
        }
        return false;
    }

    private void uploadErmFromFile(TransData localTransData, String fName, ETransType transType, Acquirer acquirer) {
        try {
            int result = TransResult.ERCM_UPLOAD_FAIL;
            if (localTransData==null || fName==null || transType==null) {
                Log.e(EReceiptUtils.TAG, "TransData or Uploadfilename or input transtype was missing");
                setResult(new ActionResult(result,null));
            }

            String path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, extAppAcquirer);
            if (path==null) {
                Log.e(EReceiptUtils.TAG, "path was not found");
                setResult(new ActionResult(result,null));
            }

            File dir = new File(path);
            if (dir==null || !dir.exists() || !dir.isDirectory()) {
                Log.e(EReceiptUtils.TAG, "Target storage was not found");
                setResult(new ActionResult(result,null));
            }

            String fullPathName = path + File.separator + fName + ".erm";
            File targFileUpload = new File(fullPathName);
            if (!targFileUpload.exists() || !targFileUpload.isFile()) {
                Log.e(EReceiptUtils.TAG, "Target file was not found on '" + fullPathName);
                setResult(new ActionResult(result,null));
            }

            result = sendUploadErm(localTransData, fullPathName, transType, acquirer);
            if (transProcessListenerImpl != null) transProcessListenerImpl.onHideProgress();
            setResult(new ActionResult(result, null));

        } catch (Exception e) {
            Log.e(EReceiptUtils.TAG, "Failed to upload eReceipt to ERCM :: " + e.getMessage());
            setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL,null));
        }
    }

    private int uploadPreSettleErmFromFile(TransData localTransData, String fName, ETransType transType, Acquirer acquirer) {
        int result = TransResult.ERCM_UPLOAD_FAIL;
        try {
            if (localTransData==null || fName==null || transType==null) {
                Log.e(EReceiptUtils.TAG, "TransData or Uploadfilename or input transtype was missing");
                return result;
            }

            String path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, acquirer);
            if (path==null) {
                Log.e(EReceiptUtils.TAG, "path was not found");
                return result;
            }

            File dir = new File(path);
            if (dir==null || !dir.exists() || !dir.isDirectory()) {
                Log.e(EReceiptUtils.TAG, "Target storage was not found");
                return result;
            }

            String fullPathName = path + File.separator + fName + ".erm";
            File targFileUpload = new File(fullPathName);
            if (!targFileUpload.exists() || !targFileUpload.isFile()) {
                Log.e(EReceiptUtils.TAG, "Target file was not found on '" + fullPathName);
                return result;
            }

            result = sendUploadErm(localTransData, fullPathName, transType, acquirer);
            if (transProcessListenerImpl != null) transProcessListenerImpl.onHideProgress();
            return result;

        } catch (Exception e) {
            Log.e(EReceiptUtils.TAG, "Failed to upload eReceipt to ERCM :: " + e.getMessage());
        }
        return result;
    }

    private int uploadESettleReportFromFile(TransData localTransData, String fName, ETransType transType, Acquirer acquirer) {
        int result = TransResult.ERCM_UPLOAD_FAIL;
        try {
            if (localTransData==null || fName==null || transType==null) {
                Log.e(EReceiptUtils.TAG, "TransData or Uploadfilename or input transtype was missing");
                return result;
            }

            String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();
            if (path==null) {
                Log.e(EReceiptUtils.TAG, "path was not found");
                return result;
            }

            File dir = new File(path);
            if (dir==null || !dir.exists() || !dir.isDirectory()) {
                Log.e(EReceiptUtils.TAG, "Target storage was not found");
                return result;
            }

            String fileName = fName + ".erm";
            String fullPathName = path + File.separator + fileName;
            File targFileUpload = new File(fullPathName);
            if (!targFileUpload.exists() || !targFileUpload.isFile()) {
                Log.e(EReceiptUtils.TAG, "Target file was not found on '" + fullPathName);
                return result;
            }

            localTransData.setTransType(ETransType.ERCEIPT_SETTLE_UPLOAD);
            localTransData.seteReceiptUploadSource(TransData.SettleUploadSource.FROM_FILE);
            localTransData.seteReceiptUploadSourcePath(fileName);
            result = sendUploadErm(localTransData, fullPathName, transType, acquirer, true);
            if (transProcessListenerImpl != null) transProcessListenerImpl.onHideProgress();
            return result;

        } catch (Exception e) {
            Log.e(EReceiptUtils.TAG, "Failed to upload eReceipt to ERCM :: " + e.getMessage());
        }
        return result;
    }
    private int sendUploadErm(TransData paymentTrans, String fullPathName, ETransType transType, Acquirer acquirer) {
        return sendUploadErm(paymentTrans, fullPathName, transType, acquirer, false);
    }
    private int sendUploadErm(TransData paymentTrans, String fullPathName, ETransType transType, Acquirer acquirer, boolean isSettlementUpload) {
        TransProcessListener listener = new TransProcessListenerImpl(ActivityStack.getInstance().top());
        listener.onShowProgress("Upload E-Receipt : " + acquirer.getName(), Constants.FAILED_DIALOG_SHOW_TIME);

        File file = new File(fullPathName);
        boolean ermReadyUpload = isErmReadyForUpload(acquirer);
        if (!ermReadyUpload) { setResult(new ActionResult(TransResult.ERCM_INITIAL_PROCESS_FAILED, null));}

        Acquirer ermRMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);

        int result = -1;
        try {
            if (file!=null && file.exists() && file.isFile()) {
                TransData ermTrans = Component.transInit();
                ermTrans = setErmPrerequisiteInfo(paymentTrans, ermTrans, acquirer, ermRMS, isSettlementUpload);

                if (transProcessListenerImpl != null) transProcessListenerImpl.onUpdateProgressTitle(ermTrans.getTransType().getTransName());
                int ret = online.online(ermTrans, transProcessListenerImpl);
                Log.d("ERM-MULTIAPP", fullPathName + " upload result = " + result);
                if (ret == TransResult.SUCC && ermTrans.getResponseCode()!=null && ermTrans.getResponseCode().getCode().equals("00")) {
                    if (isSettlementUpload) {
                        DeleteTempSettlementFile(file.getName());
                    } else {
                        DeleteTempTransactionFile(this.context, acquirer, file.getName());
                    }

                    result = TransResult.SUCC;
                } else {
                    result = TransResult.ERCM_UPLOAD_FAIL;
                }
            } else{
                result = TransResult.ERCM_UPLOAD_FAIL;
            }
        } catch (Exception e) {
            result = TransResult.ERCM_UPLOAD_FAIL;
        }

        if (listener!=null) { listener.onHideProgress();}
        return result;
    }
    private TransData setErmPrerequisiteInfo(TransData paymentTrans, TransData ermTrans, Acquirer paymentAcquirer, Acquirer ermAcquirer) {
        return setErmPrerequisiteInfo(paymentTrans, ermTrans, paymentAcquirer, ermAcquirer, false);
    }
    private TransData setErmPrerequisiteInfo(TransData paymentTrans, TransData ermTrans, Acquirer paymentAcquirer, Acquirer ermAcquirer, boolean isSettlementUpload) {
        // ERM config
        ermTrans.setAcquirer(ermAcquirer);
        ermTrans.setTransType((isSettlementUpload) ? ETransType.ERCEIPT_SETTLE_UPLOAD : ETransType.ERCEIPT_UPLOAD_FOR_MULTI_APP);
        ermTrans.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
        ermTrans.setERCMMerchantCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE));
        ermTrans.setERCMStoreCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE));
        ermTrans.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));
        ermTrans.setTpdu("60" + getLeftPadding(ermAcquirer.getNii(),4, "0") + "8000");

        if (isSettlementUpload) {
            ermTrans.seteReceiptUploadSource(paymentTrans.geteReceiptUploadSource());
            ermTrans.seteReceiptUploadSourcePath(paymentTrans.geteReceiptUploadSourcePath());
            ermTrans.setAcquirer(ermAcquirer);
        }

        // using payment Acquirer
        ermTrans.setInitAcquirerIndex(getLeftPadding(paymentAcquirer.getId(),3,"0"));
        ermTrans.setInitAcquirerNii(getLeftPadding(paymentAcquirer.getNii(),3,"0"));
        ermTrans.setInitAcquirerName(paymentAcquirer.getName());

        // using payment TransData
        ermTrans.seteSlipFormat(paymentTrans.geteSlipFormat());
        ermTrans.setOrigTransType(paymentTrans.getTransType());
        ermTrans.setPan(paymentTrans.getPan());
        ermTrans.setAmount(paymentTrans.getAmount());
        ermTrans.setStanNo(paymentTrans.getStanNo());
        ermTrans.setTraceNo(paymentTrans.getTraceNo());
        ermTrans.setDateTime(paymentTrans.getDateTime());
        ermTrans.setExpDate(paymentTrans.getExpDate());
        ermTrans.setRefNo(paymentTrans.getRefNo());
        ermTrans.setAuthCode(paymentTrans.getAuthCode());
        ermTrans.setAuthCode(paymentTrans.getAuthCode());
        ermTrans.setIssuer(paymentTrans.getIssuer());
        ermTrans.setSignData(paymentTrans.getSignData());
        ermTrans.setPinVerifyMsg(paymentTrans.isPinVerifyMsg());
        ermTrans.setTxnSmallAmt(paymentTrans.isTxnSmallAmt());

        return ermTrans;
    }
    public boolean isErmReadyForUpload(Acquirer targetAcquirer) {
        if (!targetAcquirer.isEnable() || !targetAcquirer.isEnableUploadERM()) return false;

        Acquirer ermKMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
        if (ermKMS==null) return false;

        Acquirer ermRMS = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
        if (ermRMS==null) return false;

        String ermCodeBank = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE);
        if (ermCodeBank==null || ermCodeBank.isEmpty()) return false;

        String ermCodeMerc = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE);
        if (ermCodeMerc==null || ermCodeMerc.isEmpty()) return false;

        String ermCodeStore = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE);
        if (ermCodeStore==null || ermCodeStore.isEmpty()) return false;

        String ermKeyVersion = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION);
        if (ermKeyVersion==null || ermKeyVersion.isEmpty()) return false;

        try {
            EReceiptLogoMapping sskInfo = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerInfos(targetAcquirer.getNii(), targetAcquirer.getName());
            if (sskInfo==null ||sskInfo.getSessionKeyInfosFile() ==null) return false;
        } catch (Exception e) {
            return false;
        }

        return true;
    }
    private String getLeftPadding(int val, int maxlen, String padStr) {
        return EReceiptUtils.StringPadding(String.valueOf(val), maxlen, padStr, Convert.EPaddingPosition.PADDING_LEFT);
    }
    private String getLeftPadding(long val, int maxlen, String padStr) {
        return EReceiptUtils.StringPadding(String.valueOf(val), maxlen, padStr, Convert.EPaddingPosition.PADDING_LEFT);
    }
    private String getLeftPadding(String val, int maxlen, String padStr) {
        return EReceiptUtils.StringPadding(val, maxlen, padStr, Convert.EPaddingPosition.PADDING_LEFT);
    }

    private void uploadEReceiptFromFile() {
        try {
            String path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, extAppAcquirer);
            File uploadExTransStorage =  new File(path);
            if (uploadExTransStorage.exists() && uploadExTransStorage.isDirectory()) {
                String fileName = extAppUploadFileName + ".erm" ;
                File fileToBeUpload = new File(path + "/" + fileName);
                if (fileToBeUpload.exists() && fileToBeUpload.isFile()) {
                    if (extAppAcquirer != null) {
                        // set upload transType
                        ETransType transType = extETransType;
                        if (extAppAcquirer.getName().equals(Constants.ACQ_KCHECKID)) { transType = ETransType.KCHECKID_DUMMY; }

                        int result = trySendEReceiptfromFileKCheckID(path, fileName, extAppAcquirer, transType);
                        setResult(new ActionResult(result,null));
                    } else {
                        Log.e(EReceiptUtils.TAG, "missing acquirer for upload ERCM");
                        // TODO: define new error code for this method
                        setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL,null));
                    }
                } else {
                    Log.e(EReceiptUtils.TAG, "Target file was not found on '" + path + "/" + extAppUploadFileName + ".erm'");
                    // TODO: define new error code for this method
                    setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL,null));
                }
            } else {
                Log.e(EReceiptUtils.TAG, "Target storage was not found");
                // TODO: define new error code for this method
                setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL,null));
            }
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG, "Failed to upload eReceipt to ERCM :: " + ex.getMessage());
            // TODO: define new error code for this method
            setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL,null));
        }
    }

    private int trySendEReceiptfromFileKCheckID(String path, String fileName, Acquirer exAcq, ETransType transType) {
        TransProcessListenerImpl listener = new TransProcessListenerImpl(ActivityStack.getInstance().top());
        listener.onShowProgress("Upload E-Receipt : " + transData.getAcquirer().getName(), Constants.FAILED_DIALOG_SHOW_TIME);
        int result =-1;
        try {
            File fileToUpload = new File(path + "/" + fileName);
            if (fileToUpload!=null) {
                TransData eReceiptFromExAppTransData = new TransData();
                transInit(eReceiptFromExAppTransData);

                String fName = fileToUpload.getName().replace(".erm","");
                int fileLen = fName.length();
                String currHostBatchNum = EReceiptUtils.StringPadding(String.valueOf(exAcq.getCurrBatchNo()), 6, "0", Convert.EPaddingPosition.PADDING_LEFT);
                eReceiptFromExAppTransData.setTransType(ETransType.ERCEIPT_UPLOAD);
                eReceiptFromExAppTransData.seteSlipFormat(transData.geteSlipFormat());


                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
                eReceiptFromExAppTransData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(exAcq.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                eReceiptFromExAppTransData.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(exAcq.getNii()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                eReceiptFromExAppTransData.setInitAcquirerName(exAcq.getName());

                eReceiptFromExAppTransData.setOrigTransType(transType);
                eReceiptFromExAppTransData.setAcquirer(acquirer);
                eReceiptFromExAppTransData.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
                eReceiptFromExAppTransData.setTpdu("60" + EReceiptUtils.StringPadding(acquirer.getNii(),4,"0", Convert.EPaddingPosition.PADDING_LEFT) + "8000");
                eReceiptFromExAppTransData.setRefNo(transData.getRefNo());

                String transName = eReceiptFromExAppTransData.getTransType().getTransName();
                transProcessListenerImpl.onUpdateProgressTitle(transName);
                int ret = online.online(eReceiptFromExAppTransData, transProcessListenerImpl);
                if (ret == TransResult.SUCC && eReceiptFromExAppTransData.getResponseCode() != null && eReceiptFromExAppTransData.getResponseCode().getCode().equals("00")) {
                    DeleteTempTransactionFile(context, exAcq, fileToUpload.getName());
                    Log.d(EReceiptUtils.TAG, " E-RECEIPT--UPLOAD--SUCCESS--FILE   : " + fileToUpload.getName());
                    result = ret;
                } else {
                    Log.d(EReceiptUtils.TAG, " E-RECEIPT--UPLOAD--FAILED--FILE   : " + fileToUpload.getName());
                    result = ret;
                }
            } else {
                Log.e(TAG, "target upload file was missing");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "unable to upload ERCM with External File from externalApplication upload function.");
        }
        listener.onHideProgress();
        return result;
    }

    private int trySendEReceiptfromFile(String path, String fileName, Acquirer exAcq, ETransType transType) {
        return trySendEReceiptfromFile(path, fileName, exAcq, transType, false);
    }
    private int trySendEReceiptfromFile(String path, String fileName, Acquirer exAcq, ETransType transType, boolean isFromMultiAppUpload) {
        TransProcessListenerImpl listener = new TransProcessListenerImpl(ActivityStack.getInstance().top());
        listener.onShowProgress("Upload E-Receipt : " + transData.getAcquirer().getName(), Constants.FAILED_DIALOG_SHOW_TIME);
        int result =-1;
        try {
            File fileToUpload = new File(path + "/" + fileName);
            if (fileToUpload!=null) {
                Acquirer ermAcq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
                TransData eReceiptFromExAppTransData = new TransData();
                transInit(eReceiptFromExAppTransData);

                eReceiptFromExAppTransData = setErmPrerequisiteInfo(transData, eReceiptFromExAppTransData,exAcq, ermAcq);

                String fName = fileToUpload.getName().replace(".erm","");
                int fileLen = fName.length();
                String currHostBatchNum = EReceiptUtils.StringPadding(String.valueOf(exAcq.getCurrBatchNo()), 6, "0", Convert.EPaddingPosition.PADDING_LEFT);
                eReceiptFromExAppTransData.setTransType(ETransType.ERCEIPT_UPLOAD_FOR_MULTI_APP);
                Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
                eReceiptFromExAppTransData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(exAcq.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                eReceiptFromExAppTransData.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(exAcq.getNii()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                eReceiptFromExAppTransData.setInitAcquirerName(exAcq.getName());
                eReceiptFromExAppTransData.seteReceiptUploadDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));

                eReceiptFromExAppTransData.setOrigTransType(transType);
                eReceiptFromExAppTransData.setAcquirer(acquirer);
                eReceiptFromExAppTransData.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
                eReceiptFromExAppTransData.setTpdu("60" + EReceiptUtils.StringPadding(acquirer.getNii(),4,"0", Convert.EPaddingPosition.PADDING_LEFT) + "8000");
                eReceiptFromExAppTransData.setRefNo(transData.getRefNo());

                // note use as instant upload from multi-application need to upload ERM without originaltrans on mainApp.
                TransData refTransData = null;
                if (isFromMultiAppUpload) {
                    refTransData = readFileMultiAppTransInfo(path, fileName);
                }
                if (refTransData==null) {
                    refTransData = (isFromMultiAppUpload) ? transData : null;
                }
                eReceiptFromExAppTransData.origErmTransData = refTransData;

                if (refTransData!=null) {
                    eReceiptFromExAppTransData.seteSlipFormat(refTransData.geteSlipFormat());

                    String savPath = "/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data";
                    String savFileName = "/slipinfo_upload_externalapp.slp";
                    BitmapImageConverterUtils.saveDataToFile(eReceiptFromExAppTransData.geteSlipFormat(), savPath, savFileName);

                } else {
                    eReceiptFromExAppTransData.seteSlipFormat(transData.geteSlipFormat());
                }

                String transName = eReceiptFromExAppTransData.getTransType().getTransName();
                transProcessListenerImpl.onUpdateProgressTitle(transName);
                int ret = online.online(eReceiptFromExAppTransData, transProcessListenerImpl);
                if (ret == TransResult.SUCC && eReceiptFromExAppTransData.getResponseCode() != null && eReceiptFromExAppTransData.getResponseCode().getCode().equals("00")) {
                    DeleteTempTransactionFile(context, exAcq, fileToUpload.getName());
                    Log.d(EReceiptUtils.TAG, " E-RECEIPT--UPLOAD--SUCCESS--FILE   : " + fileToUpload.getName());
                    result = ret;
                } else {
                    Log.d(EReceiptUtils.TAG, " E-RECEIPT--UPLOAD--FAILED--FILE   : " + fileToUpload.getName());
                    result = TransResult.ERCM_UPLOAD_FAIL;
                }
            } else {
                Log.e(TAG, "target upload file was missing");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "unable to upload ERCM with External File from externalApplication upload function.");
        }
        listener.onHideProgress();
        return result;
    }

    private TransData readFileMultiAppTransInfo(String path, String filename) {
        try {
            File file = new File(path + File.separator + filename.replace(".erm", ".irm")) ;
            if (file!=null && file.isFile() && file.length()>0) {
                byte[] readBuffer = new byte[(int)file.length()];
                FileInputStream fIStream = new FileInputStream(file);
                fIStream.read(readBuffer, 0 , readBuffer.length);
                fIStream.close();

                String jsonTransInfo = new String(readBuffer, Charsets.UTF_8);
                TransData tmpTransData = new GsonBuilder().create().fromJson(jsonTransInfo, TransData.class);
                return tmpTransData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void uploadESettlementFromFile() {
        try {
            String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();                  // this function is include verify path already
            File settleExStorage = new File(path);
            if (settleExStorage.exists()) {
                if (settleExStorage.isDirectory()) {
                    Hashtable<String, String> SettleReportMaps = new Hashtable<String, String>();
                    String AcqNii  = "";
                    String AcqName = "";
                    boolean isAllowAdd =false;
                    if (settleExStorage.listFiles().length > 0) {
                        for (File localFile : settleExStorage.listFiles()) {
                            AcqNii  = localFile.getName().split("-")[0].substring(0,3);
                            AcqName = localFile.getName().split("-")[0].substring(3);
                            isAllowAdd = allowAddToHashtable(SettleReportMaps, AcqName, AcqNii);
                            if (isAllowAdd) { SettleReportMaps.put(AcqName, AcqNii);  }
                        }

                        if (SettleReportMaps.size() > 0) {
                            String key_AcqName = null;
                            String val_AcqNii  = null;
                            for (Map.Entry entry : SettleReportMaps.entrySet()) {
                                key_AcqName = (String) entry.getKey();
                                val_AcqNii  = (String) entry.getValue();
                                trySend_AlleSettleReport(key_AcqName, val_AcqNii, transProcessListenerImpl);
                            }

                            setResult(new ActionResult(TransResult.SUCC,null));
                        } else {
                            setResult(new ActionResult(TransResult.SUCC,null));
                        }
                    } else {
                        Log.e(EReceiptUtils.TAG, "settlement report for upload was empty");
                        setResult(new ActionResult(TransResult.SUCC,null));
                    }
                } else {
                    Log.e(EReceiptUtils.TAG, "Invalid report directory file type");
                    setResult(new ActionResult(TransResult.ERCM_ESETTLE_REPORT_INVALID_DIRECTORY_TYPE,null));
                }
            } else {
                Log.e(EReceiptUtils.TAG, "Target storage was not found");
                setResult(new ActionResult(TransResult.ERCM_ESETTLE_REPORT_STORAGE_NOT_FOUND,null));
            }
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG, "Failed to upload AllSettleReport :: " + ex.getMessage());
            setResult(new ActionResult(TransResult.ERCM_ESETTLE_REPORT_UPLOAD_FAIL,null));
        }
    }

    private boolean allowAddToHashtable (Hashtable hash, String name, String nii) {
        if (hash != null) {
            if (hash.size()==0) { return true;}
            else {
                if (name != null && nii != null) {
                    return ( ! (hash.containsKey(name) && hash.containsValue(nii)));
                }
            }
        }

        return false;
    }

    private int trySend_AlleSettleReport(String AcquirerName, String Nii, TransProcessListenerImpl listener){
        int result = TransResult.ERCM_UPLOAD_FAIL;
        try{
            //TransProcessListenerImpl listener = new TransProcessListenerImpl(ActivityStack.getInstance().top());
            listener.onShowProgress("Upload E-SettleReport : " + AcquirerName, Constants.FAILED_DIALOG_SHOW_TIME);
            result = trySend_AlleSettleReport(AcquirerName,Nii);
            listener.onHideProgress();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }
    private int trySend_AlleSettleReport(String AcquirerName, String Nii){
         String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory();
         File dir = new File(path);
         int result =-1;
         if (dir.isDirectory() && dir.listFiles().length > 0) {
             File[] eSettleReportFileList = dir.listFiles();
             Acquirer targAcquirer = null;
             int currHostBatchNum = -1;
             for (File file: eSettleReportFileList) {
                 if(file.getName().contains(Nii + AcquirerName)) {
                     TransData reESettleReportTransData;
                     reESettleReportTransData = new TransData();
                     transInit(reESettleReportTransData);

                     String fileName =file.getName().replace(".erm","");
                     int fileLen = fileName.length();
                     targAcquirer=null;
                     currHostBatchNum= Integer.parseInt(fileName.substring(fileLen-6)) ;

                     // Extra settlement from file
                     reESettleReportTransData.seteReceiptUploadSource(TransData.SettleUploadSource.FROM_FILE);
                     reESettleReportTransData.seteReceiptUploadSourcePath(file.getName());
                     reESettleReportTransData.setTransType(ETransType.ERCEIPT_SETTLE_UPLOAD);
                     reESettleReportTransData.setSettleTransTotal(total);

                     Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
                     if (total != null) {
                         reESettleReportTransData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(total.getAcquirer().getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                         reESettleReportTransData.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(total.getAcquirer().getNii()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                         reESettleReportTransData.setInitAcquirerName(total.getAcquirer().getName());
                         acquirer.setCurrBatchNo(total.getBatchNo());//set current batch num for each acquirer
                     } else {
                         targAcquirer = FinancialApplication.getAcqManager().findAcquirer(AcquirerName);
                         reESettleReportTransData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(targAcquirer.getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                         reESettleReportTransData.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(targAcquirer.getNii()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
                         reESettleReportTransData.setInitAcquirerName(AcquirerName);
                         acquirer.setCurrBatchNo(currHostBatchNum);
                     }
                     reESettleReportTransData.setAcquirer(acquirer);
                     reESettleReportTransData.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));

                     //reESettleReportTransData.seteSlipFormat(Tools.str2Bcd(slipformat_test));
                     reESettleReportTransData.setTpdu("60" + EReceiptUtils.StringPadding(acquirer.getNii(),4,"0", Convert.EPaddingPosition.PADDING_LEFT) + "8000");
                     reESettleReportTransData.setRefNo(referencNo_settlemented);

                     String transName = reESettleReportTransData.getTransType().getTransName();
                     transProcessListenerImpl.onUpdateProgressTitle(transName);

                     int ret = uploadESettleReportFromFile(reESettleReportTransData, file.getName().replace(".erm",""), ETransType.ERCEIPT_UPLOAD, targAcquirer);

                     //int ret = online.online(reESettleReportTransData, transProcessListenerImpl);
                     if (ret == TransResult.SUCC && reESettleReportTransData.getResponseCode() != null && reESettleReportTransData.getResponseCode().getCode().equals("00")) {
                         //todo handle after upload flag
                         //setResult(new ActionResult(ret, null));
                         if (this.type == EReceiptType.ERECEIPT_REPORT_FROM_FILE) {
                             Log.d(EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--SUCCESS--UPLOAD--FILE   : " + file.getName());
                         } else {
                             Log.d(EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--SUCCESS--REUPLOAD--FILE : " + file.getName());
                         }

                         DeleteTempSettlementFile(file.getName());
                         result = ret;
                     } else {
                         result =ret;
                         if (this.type == EReceiptType.ERECEIPT_REPORT_FROM_FILE) {
                             Log.d(EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--FAILED--UPLOAD--FILE   : " + file.getName());
                         } else {
                             Log.d(EReceiptUtils.TAG, " E-SETTLEMENT--REPORT--FAILED--REUPLOAD--FILE : " + file.getName());
                         }

                         //todo handle after upload flag
//                         if (ret == TransResult.SUCC && reESettleReportTransData.getResponseCode() != null && !reESettleReportTransData.getResponseCode().getCode().equals("00")) {
//                             transProcessListenerImpl.onShowErrMessage(transName + "\n Error code: " + reESettleReportTransData.getResponseCode().toString(), Constants.FAILED_DIALOG_SHOW_TIME, true);
//                         } else {
//                             transProcessListenerImpl.onShowErrMessage(transName + " " + TransResultUtils.getMessage(TransResult.ERCM_UPLOAD_FAIL), Constants.FAILED_DIALOG_SHOW_TIME, true);
//                         }
//                         setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
                     }

                     if (result != TransResult.SUCC) {
                         break;
                     }
                 }
             }

             return result;
         } else {
             setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
             return  TransResult.ERCM_UPLOAD_FAIL;
         }
     }

    private void DeleteTempSettlementFile(String FileName) {
//        String path = "/sdcard/PAX/BPSLoader/ERCM/UnsettlementList" ;
        String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory() ;
        String fName = "/" + FileName;
        if (new File(path).exists())  {
            if (new File(path + fName).exists()) {
                new File(path + fName).delete();
            }
        }
    }

    private void DeleteTempTransactionFile(Context context, Acquirer acquirer, String FileName) {
        String path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, acquirer) ;
        String fName = "/" + FileName;
        if (new File(path).exists())  {
            if (new File(path + fName).exists()) {
                new File(path + fName).delete();
            }
            if (new File(path + fName.replace(".erm",".irm")).exists()) {
                new File(path + fName.replace(".erm", ".irm")).delete();
            }
        }
    }

    private int uploadESettlement() {

        int cnt = 0;
        TransData eSettleTrans;

//        for (TransTotal total : transTotals) {

            eSettleTrans = new TransData();
            transInit(eSettleTrans);
            eSettleTrans.setTransType(ETransType.ERCEIPT_SETTLE_UPLOAD);

            eSettleTrans.setSettleTransTotal(total);
            eSettleTrans.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(total.getAcquirer().getId()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
            eSettleTrans.setInitAcquirerNii(EReceiptUtils.StringPadding(String.valueOf(total.getAcquirer().getNii()), 3, "0", Convert.EPaddingPosition.PADDING_LEFT));
            eSettleTrans.setInitAcquirerName(total.getAcquirer().getName());

            Log.d(EReceiptUtils.TAG,"=============================================================================");
            Log.d(EReceiptUtils.TAG,"=============================================================================");
            Log.d(EReceiptUtils.TAG,"   ERM : E-SETTLEMENT REPORT UPLOADING");
            Log.d(EReceiptUtils.TAG,"          [ INDEX ] : " + eSettleTrans.getInitAcquirerIndex());
            Log.d(EReceiptUtils.TAG,"          [ NAME  ] : " + eSettleTrans.getInitAcquirerName());
            Log.d(EReceiptUtils.TAG,"          [ NII   ] : " + eSettleTrans.getInitAcquirerNii());
            Log.d(EReceiptUtils.TAG,"- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");

            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_RECEIPT_MANAGEMENT_SERVICE);
            acquirer.setCurrBatchNo(total.getBatchNo());//set current batch num for each acquirer
            eSettleTrans.setAcquirer(acquirer);

            eSettleTrans.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
            //todo gen settlement report format
            //eSettleTrans.seteSlipFormat(Tools.str2Bcd(slipformat_test));
            eSettleTrans.setTpdu("60" + EReceiptUtils.StringPadding(acquirer.getNii(),4,"0", Convert.EPaddingPosition.PADDING_LEFT) + "8000");
            eSettleTrans.setRefNo(referencNo_settlemented);

            String transName = eSettleTrans.getTransType().getTransName();
//            transProcessListenerImpl.onUpdateProgressTitle("E-Settlement Upload" + "[" + (++cnt) + "/" + transTotals.size() + "]");
            transProcessListenerImpl.onUpdateProgressTitle(transName);

            //KeepSettlementReportBeforeUpload(eSettleTrans);
            int ret = online.online(eSettleTrans, transProcessListenerImpl);
            transProcessListenerImpl.onHideProgress();

            if (ret == TransResult.SUCC && eSettleTrans.getResponseCode() != null && eSettleTrans.getResponseCode().getCode().equals("00")) {
                //todo handle after upload flag
                //DeleteTempSettlementFile(eSettleTrans.getpath);
                Log.d(EReceiptUtils.TAG," E-SETTLE REPORT UPLOAD RESULT : SUCCESS");

                int reUploadResult = 0;
                if(check_PendingUpload_eSettleReport()) {

                    reUploadResult = trySend_AlleSettleReport(total.getAcquirer().getName(), total.getAcquirer().getNii());
                    transProcessListenerImpl.onHideProgress();
                }
                if (reUploadResult != TransResult.SUCC) {
                    Log.d(EReceiptUtils.TAG,"                      >> reupload e-settle report result : failed");
                    setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
                } else {
                    Log.d(EReceiptUtils.TAG,"                      >> reupload e-settle report result : success");
                    setResult(new ActionResult(ret, null));
                }
            } else {
                //todo handle after upload flag
                if (ret == TransResult.SUCC && eSettleTrans.getResponseCode() != null && !eSettleTrans.getResponseCode().getCode().equals("00")) {
                    Log.d(EReceiptUtils.TAG," E-SETTLE REPORT UPLOAD RESULT : FAILED (error-code=" + eSettleTrans.getResponseCode().toString() + ")");
                    if ("51".equals(eSettleTrans.getResponseCode().getCode())) {
                        transProcessListenerImpl.onShowErrMessage(transName + "\n Error code: " + eSettleTrans.getResponseCode().getCode() + "\n" + Utils.getString(R.string.err_e_settle_contact_kbiz), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    } else {
                        transProcessListenerImpl.onShowErrMessage(transName + "\n Error code: " + eSettleTrans.getResponseCode().toString(), Constants.FAILED_DIALOG_SHOW_TIME, true);
                    }
                } else {
                    Log.d(EReceiptUtils.TAG," E-SETTLE REPORT UPLOAD RESULT : FAILED (no response code)");
                    transProcessListenerImpl.onShowErrMessage(transName + " " + TransResultUtils.getMessage(TransResult.ERCM_UPLOAD_FAIL), Constants.FAILED_DIALOG_SHOW_TIME, true);
                }
                setResult(new ActionResult(TransResult.ERCM_UPLOAD_FAIL, null));
            }

            Log.d(EReceiptUtils.TAG,"=============================================================================");
            Log.d(EReceiptUtils.TAG,"=============================================================================");
//        }
        return TransResult.SUCC;
    }
    private void Keep_eSettlementData(TransData transData) {

    }

    private void KeepSettlementReportBeforeUpload(TransData transData) {
        DE_24 = transData.getInitAcquirerIndex();
        DE_37 = transData.getRefNo();
        DE_41 = FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName()).getTerminalId();
        DE_42 = FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName()).getMerchantId();
        DE_59 = "SETTLEMENT".getBytes();
        DE_60 = Component.getPaddedNumber(transData.getAcquirer().getCurrBatchNo(), 6);

        PackEReceiptSettleUpload packuploadsettle = new PackEReceiptSettleUpload(null);
        DE_61 = packuploadsettle.generateReceiptFormat(transData);

        PackEReceiptUpload packuploadreceipt = new PackEReceiptUpload(null);
        DE_63 = packuploadreceipt.generateERTable(transData);

        keepUnsettlementFile(transData, DE_60, DE_24, DE_37, DE_41, DE_42, DE_59, DE_60, DE_61, DE_63);
    }

    private void keepUnsettlementFile(TransData transData,
                                      String CurrentBatchNo,
                                      String DE_24,
                                      String DE_37,
                                      String DE_41,
                                      String DE_42,
                                      byte[] DE_59,
                                      String DE_60,
                                      byte[] DE_61,
                                      byte[] DE_63 ) {
        String path = EReceiptUtils.getERM_UnsettleExternalStorageDirectory() ;
        String[] pathSplitter = path.split("/");
        String absolutepath="";
        for (int idx=1 ; idx <= pathSplitter.length -1;idx++) {
            absolutepath += File.separator + pathSplitter[idx];
            if (new File(absolutepath).exists() != true) {new File(absolutepath).mkdir();}
        }

        Acquirer acquirer= FinancialApplication.getAcqManager().findAcquirer(transData.getInitAcquirerName());
        String unSettleFile = File.separator
                + EReceiptUtils.StringPadding(String.valueOf(acquirer.getNii()) ,3,"0", Convert.EPaddingPosition.PADDING_LEFT)
                + acquirer.getName()
                + "_"
                + CurrentBatchNo
                + ".erm";
        ByteArrayOutputStream bArrOps = new ByteArrayOutputStream() ;
        String Header = ((DE_42.length() != 15) ? "1" : "0") + Utils.getStringPadding(String.valueOf(DE_42.length()),2,"0", Convert.EPaddingPosition.PADDING_LEFT);

        try {
            bArrOps.write(Header.getBytes());
            bArrOps.write(DE_24.getBytes());
            bArrOps.write(DE_37.getBytes());
            bArrOps.write(DE_41.getBytes());
            bArrOps.write(DE_42.getBytes());
            bArrOps.write(DE_59);
            bArrOps.write(DE_60.getBytes());
            bArrOps.write(DE_61);
            bArrOps.write(DE_63);

            FileOutputStream fOps =  new FileOutputStream(path + unSettleFile);
            fOps.write(bArrOps.toByteArray());
            fOps.close();
            bArrOps.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String DE_24 ;
    String DE_37 ;
    String DE_41 ;
    String DE_42 ;
    byte[] DE_59 ;
    String DE_60 ;
    byte[] DE_61 ;
    byte[] DE_63 ;

    public HashMap<Integer,Object> ReadSettleReportFile(String PathWithFileName){
        File SettleReportFile = new File(EReceiptUtils.getERM_UnsettleExternalStorageDirectory() + File.separator + PathWithFileName);
        byte[] buffer = new byte[(int)SettleReportFile.length()];
        HashMap<Integer, Object> returnHash = new HashMap<Integer, Object>();
        if(SettleReportFile.exists()) {
            try {
                try {
                    FileInputStream fStream = new FileInputStream(SettleReportFile);
                    fStream.read(buffer,0,buffer.length);
                    fStream.close();

                    int Len ;
                    int reerPos=0;
                    int MercLen=15;
                    byte[] tmpArr = new byte[0];
                    Len=  3      ; tmpArr = new byte[Len] ; System.arraycopy(buffer, 0 ,tmpArr, 0, Len) ; reerPos+=Len ; String header = new String(tmpArr,"UTF-8");
                    if(header.substring(0,1).equals("1")) {MercLen=Integer.parseInt(header.substring(1,3));} else {MercLen=15;}
                    Len=  3      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX24 = new String(tmpArr,"UTF-8");
                    Len= 12      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX37 = new String(tmpArr,"UTF-8");
                    Len=  8      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX41 = new String(tmpArr,"UTF-8");
                    Len= MercLen ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX42 = new String(tmpArr,"UTF-8");
                    Len= 10      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX59 = new String(tmpArr,"UTF-8");
                    Len=  6      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; String bitX60 = new String(tmpArr,"UTF-8");
                    Len= buffer.length-30-reerPos ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; byte[] bitX61 = tmpArr;
                    Len= 30      ; tmpArr = new byte[Len] ; System.arraycopy(buffer,  reerPos ,tmpArr, 0, Len) ; reerPos+=Len ; byte[] bitX63 = tmpArr;

                    int BitFieldNumber = -1;
                    Object BitFieldData = "";
                    for (int index =0 ;index<=7; index++) {
                        switch (index) {
                            case 0 : BitFieldNumber=24 ; BitFieldData=bitX24; break;
                            case 1 : BitFieldNumber=37 ; BitFieldData=bitX37; break;
                            case 2 : BitFieldNumber=41 ; BitFieldData=bitX41; break;
                            case 3 : BitFieldNumber=42 ; BitFieldData=bitX42; break;
                            case 4 : BitFieldNumber=59 ; BitFieldData=bitX59; break;
                            case 5 : BitFieldNumber=60 ; BitFieldData=bitX60; break;
                            case 6 : BitFieldNumber=61 ; BitFieldData=bitX61; break;
                            case 7 : BitFieldNumber=63 ; BitFieldData=bitX63; break;
                        }
                        Log.d(EReceiptUtils.TAG,"  FIELD--" + BitFieldNumber + " = " + BitFieldData);
                        returnHash.put(BitFieldNumber, BitFieldData);
                    }

                    return returnHash;
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private String slipformat_test =
            "3030310D0AD720202020202050524520" +
            "52454C454153450AD720202020202050" +
            "48495453414E554C4F4B0AD720542E30" +
            "35352D323531363839202842522E3535" +
            "34290AD72D2D2D2D2D2D2D2D2D2D2D2D" +
            "2D2D2D2D2D2D2D2D2D2D2D0AD7544944" +
            "23202020202020202020202036313139" +
            "383437330AD74D494423202020203430" +
            "313030303839343730303030310AD754" +
            "52414345232020202020202020202020" +
            "3030303034330AD74241544348232020" +
            "2020202020202020203030303031380A" +
            "D75354414E2320202020202020202020" +
            "20203030303133380AD73D3D3D3D3D3D" +
            "3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D" +
            "3D0AD734303530203136585820585858" +
            "58203830313420430AD7564953412020" +
            "202020202020204558503A2058582F58" +
            "580AD730342F31322F31382020202020" +
            "202031393A34353A35340AD741505052" +
            "20434F44452020202020202020303039" +
            "3530380AD7524546204E554D20202020" +
            "3030303031313030303133380AD74150" +
            "50202020202020202020564953412043" +
            "52454449540ACE414944203A20413030" +
            "30303030303033313031300ACE544320" +
            "3A204641303337343435353231413836" +
            "42460ACE545652203A20303038303030" +
            "383030300AD752454631202020202020" +
            "202020202020202020202020580AD752" +
            "45463220202020202020202020202020" +
            "2020202020580AD753414C450AD7414D" +
            "543A5448422020202020202020202020" +
            "31322E31310AD70ACE0ACE[@VF_FORMAT_SIGNATURE]5349474E3A" +
            "5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F" +
            "5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F" +
            "5F5F5F5F5F5F5F5F5F0ACE2020202020" +
            "2020202020202020204B5241494B4145" +
            "572F52414348414E414E540ACE0ACE20" +
            "202020202020202020202020202A2A2A" +
            "204E4F20524546554E44202A2A2A0ACE" +
            "202020202020492041434B4E4F574C45" +
            "444745205341544953464143544F5259" +
            "20524543454950540ACE202020202020" +
            "202020204F462052454C415449564520" +
            "474F4F44532F53455256494345530ACE" +
            "2020202020202020202020202020204B" +
            "42414E4B207265763A312E312E310ACE" +
            "0ACE202020202020202020202A2A2A2A" +
            "2A204D45524348414E5420434F505920" +
            "2A2A2A2A2A";
}
