package com.pax.pay.trans.action;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.device.Device;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.record.Printer;
import com.pax.pay.splash.SplashListener;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.activity.EReceiptOtherSettingsActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.pay.utils.BitmapImageConverterUtils;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.settings.SysParam;
import com.pax.view.dialog.DialogUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import th.co.bkkps.utils.ERMUtil;
import th.co.bkkps.utils.Log;

public class ActionEReceipt extends AAction {
    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionEReceipt(ActionStartListener listener) {
        super(listener);
    }

    public void setParam (eReceiptMode exMode, Context exContext, String exSerialNumber, Acquirer targetAcquirer) {
        this.context        = exContext;
        this.serialNumber   = exSerialNumber;
        this.mode           = exMode;
        this.acquirer       = ((exMode == eReceiptMode.DL_SESSION_KEY_ALL_HOST) ?  null : targetAcquirer) ;
    }

    public void setExtraParam (boolean isAutoDownloadMode) {
        this.isAutoDownloadMode=isAutoDownloadMode;
    }


    private Acquirer acquirer;
    private Context context;
    private String  serialNumber;
    private eReceiptMode mode;
    private boolean isShowDialog;
    private boolean isAutoDownloadMode;
    private SplashListener progressListener = null;
    public enum eReceiptMode { INIT_TERMINAL, DL_SESSION_KEY_ALL_HOST, DL_SESSION_KEY_SINGLE_HOST, OTHER_SETTING, ERECEIPT_REPORT, CHECK_AND_INIT_TERMINAL_AND_SESSION_KEY}

    public void setEndListener(ActionEndListener listener) {
        super.setEndListener(listener);
    }

    public void setEndProgressListener(SplashListener progressListener) {
        this.progressListener = progressListener;
    }

    private Thread local_Thread = null;

    @Override
    protected void process() {
        boolean isProcessActivity = true;

        // Pre-Check serial number input from management page.
        //if(serialNumber == null ) { isProcessActivity=false; setResult(new ActionResult(TransResult.ERR_PARAM, null));}
        if(serialNumber == null ) {serialNumber=FinancialApplication.getDownloadManager().getSn();}
        FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER,serialNumber);
        if(isProcessActivity) {
            FinancialApplication.getApp().runInBackground(new Runnable() {
                @Override
                public void run() {
                    Online online = new Online();
                    TransProcessListener transProcessListenerImpl =null;
                    if (progressListener==null) { transProcessListenerImpl = new TransProcessListenerImpl(context); }
                    switch (mode) {
                        case INIT_TERMINAL:
                            do_TerminalRegistration(transProcessListenerImpl, online);

                            break;
                        case CHECK_AND_INIT_TERMINAL_AND_SESSION_KEY:
                            doTerminalRegistrationAndDownloadSessionKeySpecificHost(transProcessListenerImpl, online, acquirer);

                            break;
                        case DL_SESSION_KEY_ALL_HOST:
                            do_DownloadSessionKey(transProcessListenerImpl, online, null, false);

                            break;
                        case DL_SESSION_KEY_SINGLE_HOST:
                            do_DownloadSessionKey(transProcessListenerImpl, online, acquirer, false);

                            break;
                        case OTHER_SETTING:
                            do_ActivateOtherSettings();

                            break;
                        case ERECEIPT_REPORT:
                            do_ERMReport();

                            break;
                        default:
                            break;
                    }

                    transProcessListenerImpl.onHideProgress();
                }
            });
        }
    }

    private void do_ActivateOtherSettings() {
        Intent intent = new Intent(context, EReceiptOtherSettingsActivity.class);

        intent.putExtra("img_dir_path",context.getApplicationContext().getPackageName());
        context.startActivity(intent);
    }

    private DialogInterface.OnDismissListener  dismissListener;
    private void setOnDismissListener() {
        dismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActivityStack.getInstance().pop();
                TransContext.getInstance().getCurrentAction().setFinished(false); //AET-229
                TransContext.getInstance().setCurrentAction(null); //fix leaks
            }
        };
    }

    private void updateProgress(String title, String dispMsg) {
        if (progressListener != null) {
            progressListener.onUpdataUI(title, dispMsg);
        }
    }

    private void do_TerminalRegistration(TransProcessListener exListener, Online exOnline){
        int local_ret = -999;

        updateProgress("ERCM Registration", "-- STARTING... --");

        List<String> successAcquirers = new ArrayList<>();

        if ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER) != null)) {

            boolean DownloadPBK_completed = true;
            if (DownloadPBK_completed) {

                String rootLogoDir = EReceiptUtils.getERM_LogoDirectory(context);
                File[] logoFiles = new File(rootLogoDir).listFiles();
                List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();

                // set neccessary information to transData
                TransData templateTransData ;

                // read publickey data
                templateTransData = new TransData();
                templateTransData = setPublicKey(templateTransData);
                templateTransData.setPublickeyExponent(templateTransData.getPublickeyExponent());
                templateTransData.setPublickeyModulus(templateTransData.getPublickeyModulus());
                templateTransData.setPublickeyHash(templateTransData.getPublickeyHash());
                templateTransData.setPublicKeyVersion(templateTransData.getPublicKeyVersion());

                // reproduce session key block table
                HashMap<String,Object> SKB_Output = EReceiptUtils.getInstance().CreateSessionKeyBlock(templateTransData, context);
                templateTransData.setSessionKeyBlock((byte[]) SKB_Output.get("SKB"));
                templateTransData.setSessionKeyOutput((HashMap<String,Object>)SKB_Output);

                boolean hasErrorDuringSendData=false;
                Log.d("ERCM"," Found : " + acquirers.size() + " to initialize ERCM");
                for (Acquirer tmp_acquirer : acquirers) {
                    // ensure this acquire is not a ERCM_KMS or ERCM_RMS
                    // because this two hosts isn't contain logo-image inside root-logo-directory
                    if ((! tmp_acquirer.getName().equals("ERCM_KMS")) && (! tmp_acquirer.getName().equals("ERCM_RMS"))) {

                        if ( (tmp_acquirer.getEnableUploadERM())) {
                            String AcquirerNotiStr ;
                            // ensure target acquirer have logo image to upload
                            //if (BitmapImageConverterUtils.isFileExisting(context, tmp_acquirer)) {
                            TransData TSN_Register_transData = Intial_TransData_ERM_TerminalRegistration(tmp_acquirer);
                            AcquirerNotiStr =tmp_acquirer.getName() + " ("+ tmp_acquirer.getNii() + ")";
                            if (exListener!=null) {exListener.onUpdateProgressTitle("Init. host : " + AcquirerNotiStr);}
                            updateProgress("ERCM Registration", "Init. host : " + AcquirerNotiStr);

                            byte[] bHeaderLogo = BitmapImageConverterUtils.getSlipHeaderLogoFilename(context, tmp_acquirer);
                            if (bHeaderLogo != null) { TSN_Register_transData.setInitHeaderLogoFile(BitmapImageConverterUtils.getLastLogoFileName); }
                            TSN_Register_transData.setERCMHeaderImagePath(bHeaderLogo);
                            TSN_Register_transData.setERCMFooterImagePath(bHeaderLogo);
                            TSN_Register_transData.setERCMLogoImagePath(bHeaderLogo);
                            TSN_Register_transData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(tmp_acquirer.getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT) );
                            TSN_Register_transData.setInitAcquirerNii(tmp_acquirer.getNii());
                            TSN_Register_transData.setInitAcquirerName(tmp_acquirer.getName());
                            TSN_Register_transData.setSessionKeyBlock(templateTransData.getSessionKeyBlock());
                            TSN_Register_transData.setInitSessionKeyFile(EReceiptUtils.getInstance().WriteSessionKeyDataToFile(context, TSN_Register_transData, templateTransData.getSessionKeyBlock()));
                            TSN_Register_transData.setSessionKeyOutput(templateTransData.getSessionKeyOutput());

                            if(TSN_Register_transData != null) {
                                //exOnline.setForceUseExternalSendData(EReceiptUtils.getInstance().pack(TSN_Register_transData,exListener));
                                //Log.d(EReceiptUtils.TAG, " E-Receipt Data : " + Tools.bcd2Str(exOnline.getForceUseExternalSendData()));
                                exOnline.setReplaceExternalField63(true);
                                Log.i("Online"," >> SEND : >> Terminal Registration [NII="+tmp_acquirer.getNii()+"] >> " + tmp_acquirer.getName());
                                local_ret = exOnline.online(TSN_Register_transData,exListener);

                                if (local_ret == TransResult.SUCC) {
                                    if (getRespCode(TSN_Register_transData).equals("00")) {
                                        updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : success");
                                        //exListener.onShowNormalMessage("Successful",2, false);
                                        TSN_Register_transData = do_ExtractField63(TSN_Register_transData);     // Extract Field 63
                                        save_initial_data(TSN_Register_transData);
                                        successAcquirers.add(tmp_acquirer.getName());
                                        Log.d("ERCM","Initial :: " + tmp_acquirer.getName() + " = SUCCESS");
                                    }
                                    else {
                                        updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : declined");
                                        hasErrorDuringSendData=true;
                                        String extraRespCode = ((getRespCode(TSN_Register_transData) != null) ? "( ErrCode : ".concat(getRespCode(TSN_Register_transData)).concat(")") : "") ;
                                        String transTypeStr = TSN_Register_transData.getTransType().getTransName();
                                        if (exListener!=null) {exListener.onShowErrMessage("Result : " +  transTypeStr + " was declined.",Constants.FAILED_DIALOG_SHOW_TIME, true);}
                                        //DialogUtils.showErrMessage(context, "Result " + extraRespCode + " : ", transTypeStr + " was declined.", dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                                        local_ret = TransResult.ERR_HOST_REJECT;
//                                        setResult(new ActionResult(local_ret,null));
//                                        break;
                                    }
                                } else {
                                    updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : failed");
                                    hasErrorDuringSendData=true;
                                    if (exListener!=null) {exListener.onShowErrMessage("Result : " + TransResultUtils.getMessage(local_ret),Constants.FAILED_DIALOG_SHOW_TIME, true);}
//                                    setResult(new ActionResult(local_ret,null));
                                    //DialogUtils.showErrMessage(context, "Result " + extraRespCode + " : ", TransResultUtils.getMessage(TransResult.ERCM_PBK_DOWNLOAD_DECLINED), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
//                                    break;
                                }
                            } else {
                                updateProgress("ERCM Registration", AcquirerNotiStr + " : Transaction data wasn't found");
                                hasErrorDuringSendData=true;
                                if (exListener!=null) {exListener.onShowErrMessage("Result : " + TransResultUtils.getMessage(local_ret),Constants.FAILED_DIALOG_SHOW_TIME, true);}
//                                setResult(new ActionResult(local_ret,null));
                                //DialogUtils.showErrMessage(context, "Result : ", TransResultUtils.getMessage(local_ret), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
//                                break;
                            }
                            //}
                        }
                    }
                }

                if (!hasErrorDuringSendData) {
                    updateProgress("ERCM Registration", "ERM initialization Successful");
                    if (exListener!=null) {exListener.onShowNormalMessage("ERM initialization Successful",Constants.SUCCESS_DIALOG_SHOW_TIME, false);}
//                    do_DownloadSessionKey(exListener, exOnline, null, true);
                    //DialogUtils.showSuccMessage(context, TransResultUtils.getMessage(TransResult.SUCC), dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
                    //setResult(new ActionResult(TransResult.SUCC,null));
                }
            }
        }
        else {
            //do_ActivateOtherSettings();
            if(exListener !=null) {
                exListener.onShowErrMessage("Missing BankCode, MerchantCode & Store Code\n please see E-Receipt Other Setting" ,Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }

        if (exListener!=null) {exListener.onHideProgress();}

        if (!successAcquirers.isEmpty()) {
            setResult(new ActionResult(TransResult.SUCC, null));
        } else {
            setResult(new ActionResult(local_ret, null));
        }

    }

    private void doTerminalRegistrationAndDownloadSessionKeySpecificHost(TransProcessListener exListener, Online exOnline, Acquirer acquirer) {
        int local_ret = -999;

        updateProgress("ERCM Registration", "-- STARTING... --");

        List<String> successAcquirers = new ArrayList<>();

        if ((FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE) != null)
                && (FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER) != null)) {

            boolean DownloadPBK_completed = true;
            if (DownloadPBK_completed) {

                String rootLogoDir = EReceiptUtils.getERM_LogoDirectory(context);
                File[] logoFiles = new File(rootLogoDir).listFiles();

                List<Acquirer> acquirers = new ArrayList<>();
                acquirers.add(acquirer);
                try {
                    EReceiptLogoMapping eReceiptLogoMapping = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerInfos(acquirer.getNii(), acquirer.getName());
                    if (eReceiptLogoMapping != null) {
                        setResult(new ActionResult(TransResult.SUCC, null));
                        return;
                    }
                } catch (SQLException ex) {
                    Log.e(EReceiptUtils.TAG, ex.getMessage());
                }

                // set neccessary information to transData
                TransData templateTransData ;

                // read publickey data
                templateTransData = new TransData();
                templateTransData = setPublicKey(templateTransData);
                templateTransData.setPublickeyExponent(templateTransData.getPublickeyExponent());
                templateTransData.setPublickeyModulus(templateTransData.getPublickeyModulus());
                templateTransData.setPublickeyHash(templateTransData.getPublickeyHash());
                templateTransData.setPublicKeyVersion(templateTransData.getPublicKeyVersion());

                // reproduce session key block table
                HashMap<String,Object> SKB_Output = EReceiptUtils.getInstance().CreateSessionKeyBlock(templateTransData, context);
                templateTransData.setSessionKeyBlock((byte[]) SKB_Output.get("SKB"));
                templateTransData.setSessionKeyOutput((HashMap<String,Object>)SKB_Output);

                Log.d("ERCM"," Found : " + acquirers.size() + " to initialize ERCM");
                for (Acquirer tmp_acquirer : acquirers) {
                    // ensure this acquire is not a ERCM_KMS or ERCM_RMS
                    // because this two hosts isn't contain logo-image inside root-logo-directory
                    if ((! tmp_acquirer.getName().equals("ERCM_KMS")) && (! tmp_acquirer.getName().equals("ERCM_RMS"))) {

                        if ( (tmp_acquirer.getEnableUploadERM())) {
                            String AcquirerNotiStr ;
                            // ensure target acquirer have logo image to upload
                            //if (BitmapImageConverterUtils.isFileExisting(context, tmp_acquirer)) {
                            TransData TSN_Register_transData = Intial_TransData_ERM_TerminalRegistration(tmp_acquirer);
                            AcquirerNotiStr =tmp_acquirer.getName() + " ("+ tmp_acquirer.getNii() + ")";
                            if (exListener!=null) {exListener.onUpdateProgressTitle("Init. host : " + AcquirerNotiStr);}
                            updateProgress("ERCM Registration", "Init. host : " + AcquirerNotiStr);

                            byte[] bHeaderLogo = BitmapImageConverterUtils.getSlipHeaderLogoFilename(context, tmp_acquirer);
                            if (bHeaderLogo != null) { TSN_Register_transData.setInitHeaderLogoFile(BitmapImageConverterUtils.getLastLogoFileName); }
                            TSN_Register_transData.setERCMHeaderImagePath(bHeaderLogo);
                            TSN_Register_transData.setERCMFooterImagePath(bHeaderLogo);
                            TSN_Register_transData.setERCMLogoImagePath(bHeaderLogo);
                            TSN_Register_transData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(tmp_acquirer.getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT) );
                            TSN_Register_transData.setInitAcquirerNii(tmp_acquirer.getNii());
                            TSN_Register_transData.setInitAcquirerName(tmp_acquirer.getName());
                            TSN_Register_transData.setSessionKeyBlock(templateTransData.getSessionKeyBlock());
                            TSN_Register_transData.setInitSessionKeyFile(EReceiptUtils.getInstance().WriteSessionKeyDataToFile(context, TSN_Register_transData, templateTransData.getSessionKeyBlock()));
                            TSN_Register_transData.setSessionKeyOutput(templateTransData.getSessionKeyOutput());

                            if(TSN_Register_transData != null) {
                                //exOnline.setForceUseExternalSendData(EReceiptUtils.getInstance().pack(TSN_Register_transData,exListener));
                                //Log.d(EReceiptUtils.TAG, " E-Receipt Data : " + Tools.bcd2Str(exOnline.getForceUseExternalSendData()));
                                exOnline.setReplaceExternalField63(true);
                                Log.i("Online"," >> SEND : >> Terminal Registration [NII="+tmp_acquirer.getNii()+"] >> " + tmp_acquirer.getName());
                                local_ret = exOnline.online(TSN_Register_transData,exListener);

                                if (local_ret == TransResult.SUCC) {
                                    if (getRespCode(TSN_Register_transData).equals("00")) {
                                        updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : success");
                                        //exListener.onShowNormalMessage("Successful",2, false);
                                        TSN_Register_transData = do_ExtractField63(TSN_Register_transData);     // Extract Field 63
                                        save_initial_data(TSN_Register_transData);

                                        //Download Sessionkey by host
//                                        int sskRet = do_DownloadSessionKeyByHost(exListener, exOnline, tmp_acquirer);
//                                        if (sskRet == TransResult.SUCC) {
//                                            successAcquirers.add(tmp_acquirer.getName());
//                                        } else {
//                                            local_ret = sskRet;
//                                        }
                                        Log.d("ERCM","Initial :: " + tmp_acquirer.getName() + " = SUCCESS");
                                    }
                                    else {
                                        updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : declined");
                                        String extraRespCode = ((getRespCode(TSN_Register_transData) != null) ? "( ErrCode : ".concat(getRespCode(TSN_Register_transData)).concat(")") : "") ;
                                        String transTypeStr = TSN_Register_transData.getTransType().getTransName();
                                        if (exListener!=null) {exListener.onShowErrMessage("Result : " +  transTypeStr + " was declined.",Constants.FAILED_DIALOG_SHOW_TIME, true);}
                                        //DialogUtils.showErrMessage(context, "Result " + extraRespCode + " : ", transTypeStr + " was declined.", dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                                        local_ret = TransResult.ERR_HOST_REJECT;
//                                        setResult(new ActionResult(local_ret,null));
//                                        break;
                                    }
                                } else {
                                    updateProgress("ERCM Registration", AcquirerNotiStr + " : Init. result : failed");
                                    if (exListener!=null) {exListener.onShowErrMessage("Result : " + TransResultUtils.getMessage(local_ret),Constants.FAILED_DIALOG_SHOW_TIME, true);}
//                                    setResult(new ActionResult(local_ret,null));
                                    //DialogUtils.showErrMessage(context, "Result " + extraRespCode + " : ", TransResultUtils.getMessage(TransResult.ERCM_PBK_DOWNLOAD_DECLINED), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
//                                    break;
                                }
                            } else {
                                updateProgress("ERCM Registration", AcquirerNotiStr + " : Transaction data wasn't found");
                                if (exListener!=null) {exListener.onShowErrMessage("Result : " + TransResultUtils.getMessage(local_ret),Constants.FAILED_DIALOG_SHOW_TIME, true);}
//                                setResult(new ActionResult(local_ret,null));
                                //DialogUtils.showErrMessage(context, "Result : ", TransResultUtils.getMessage(local_ret), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
//                                break;
                            }
                            //}
                        }
                    }
                }
            }
        }
        else {
            //do_ActivateOtherSettings();
            if(exListener !=null) {
                exListener.onShowErrMessage("Missing BankCode, MerchantCode & Store Code\n please see E-Receipt Other Setting" ,Constants.FAILED_DIALOG_SHOW_TIME, true);
            }
        }

        if (exListener!=null) {exListener.onHideProgress();}

        if (!successAcquirers.isEmpty()) {
            setResult(new ActionResult(TransResult.SUCC, null));
        } else {
            setResult(new ActionResult(local_ret, null));
        }
    }

    private void save_initial_data(TransData transData) {
        HashMap<String,Object> transHash = transData.getSessionKeyOutput();
        EReceiptLogoMapping EReceipt_Local_obj = new EReceiptLogoMapping();
        EReceipt_Local_obj.setHostIndex(transData.getInitAcquirerIndex());
        EReceipt_Local_obj.setAcquirerNii(transData.getInitAcquirerNii());
        EReceipt_Local_obj.setAcquirerName(transData.getInitAcquirerName());
        EReceipt_Local_obj.setRawFileHeaderLogoFile(transData.getInitHeaderLogoFile());
        EReceipt_Local_obj.setRawFileFooterLogoFile(null);
        EReceipt_Local_obj.setSessionKeyInfosFile(transData.getInitSessionKeyFile());
        if (transHash != null) {
            EReceipt_Local_obj.setTleIndicator(         (String)transHash.get("TLE_INDICATOR"));
            EReceipt_Local_obj.setSessionKeyClearText(  (byte[])transHash.get("SSK_TXT"));
            EReceipt_Local_obj.setSessionKeyKCV(        (byte[])transHash.get("SSK_KCV"));
            EReceipt_Local_obj.setSessionKeyEncrypted(  (byte[])transHash.get("SSK_ENC"));
            EReceipt_Local_obj.setErcVersion(           (String)transHash.get("ERC_VER"));
            EReceipt_Local_obj.setBankCode(             (String)transHash.get("BANK_CODE"));
            EReceipt_Local_obj.setTerminalSerialNumber( (String)transHash.get("TSN"));
            EReceipt_Local_obj.setKekType(              (String)transHash.get("KEK_TYPE"));
            EReceipt_Local_obj.setKekVersion(           (String)transHash.get("KEK_VERSION"));
        }
        EReceipt_Local_obj.setSaveDateTime(Device.getTime(Constants.TIME_PATTERN_TRANS));

        try {
            int existingID = FinancialApplication.getEReceiptDataDbHelper().findLogobyInfos(transData.getInitAcquirerNii(), transData.getInitAcquirerName());
            if ( existingID >= 0 ) {
                EReceipt_Local_obj.setId(existingID);
                FinancialApplication.getEReceiptDataDbHelper().updateEReceiptLogoMapping(EReceipt_Local_obj);
            } else if ( existingID ==-1 ) {
                FinancialApplication.getEReceiptDataDbHelper().insertEReceiptLogoMapping(EReceipt_Local_obj);
            }
        } catch (Exception e) {
            Log.e(EReceiptUtils.TAG,e.getMessage());
        }
    }

    private void save_sessionkey_renewal(TransData transData) {
        HashMap<String,Object> transHash = transData.getSessionKeyOutput();
        EReceiptLogoMapping EReceipt_Local_obj =null;
        try {
            EReceipt_Local_obj = FinancialApplication.getEReceiptDataDbHelper().FindSessionKeyByAcquirerIndex(transData.getInitAcquirerIndex());
            if (transHash != null) {
                EReceipt_Local_obj.setTleIndicator(                 (String)transHash.get("TLE_INDICATOR"));
                EReceipt_Local_obj.setSessionKeyClearText(          (byte[])transHash.get("SSK_TXT"));
                EReceipt_Local_obj.setSessionKeyKCV(                (byte[])transHash.get("SSK_KCV"));
                EReceipt_Local_obj.setSessionKeyEncrypted(          (byte[])transHash.get("SSK_ENC"));
                EReceipt_Local_obj.setErcVersion(                   (String)transHash.get("ERC_VER"));
                EReceipt_Local_obj.setBankCode(                     (String)transHash.get("BANK_CODE"));
                EReceipt_Local_obj.setTerminalSerialNumber(         (String)transHash.get("TSN"));
                EReceipt_Local_obj.setKekType(                      (String)transHash.get("KEK_TYPE"));
                EReceipt_Local_obj.setKekVersion(                   (String)transHash.get("KEK_VERSION"));
                EReceipt_Local_obj.setSaveDateTime(                 Device.getTime(Constants.TIME_PATTERN_TRANS));
            }
            FinancialApplication.getEReceiptDataDbHelper().updateEReceiptLogoMapping(EReceipt_Local_obj);
        } catch (Exception ex) {

        }
    }

    private void do_DownloadSessionKey(TransProcessListener exListener, Online exOnline, Acquirer specifyAcquirer, boolean fromInitialMode) {
        int local_ret = -999;

        String rootLogoDir = EReceiptUtils.getERM_LogoDirectory(context);
        File[] logoFiles = new File(rootLogoDir).listFiles();
        List<Acquirer> acquirers = FinancialApplication.getAcqManager().findEnableAcquirers();


        // set neccessary information to transData
        TransData templateTransData =  new TransData();
        templateTransData = setPublicKey(templateTransData);
        templateTransData.setPublickeyExponent(templateTransData.getPublickeyExponent());
        templateTransData.setPublickeyModulus(templateTransData.getPublickeyModulus());
        templateTransData.setPublickeyHash(templateTransData.getPublickeyHash());
        templateTransData.setPublicKeyVersion(templateTransData.getPublicKeyVersion());

        // reproduce session key block table
        HashMap<String,Object> SKB_Output = EReceiptUtils.getInstance().CreateSessionKeyBlock(templateTransData, context);
        templateTransData.setSessionKeyBlock((byte[]) SKB_Output.get("SKB"));
        templateTransData.setSessionKeyOutput((HashMap<String,Object>)SKB_Output);

        boolean hasErrorDuringSendData = false;

        if (specifyAcquirer == null) {

            for (Acquirer tmp_acquirer : acquirers) {
                // ensure this acquire is not a ERCM_KMS or ERCM_RMS
                // because this two hosts isn't contain logo-image inside root-logo-directory
                if ((! tmp_acquirer.getName().equals("ERCM_KMS")) && (! tmp_acquirer.getName().equals("ERCM_RMS")) && (tmp_acquirer.getEnableUploadERM())) {

                    String AcquirerNotiStr;
                    TransData DL_SSK_transData = Intial_TransData_ERM_DownloadSessionKey(tmp_acquirer);
                    AcquirerNotiStr =tmp_acquirer.getName() + " ("+ tmp_acquirer.getNii() + ")";
                    if (exListener!=null) {exListener.onUpdateProgressTitle("SSK Renewal : " + AcquirerNotiStr);}
                    updateProgress("ERCM Registration", "SSK Renewal : " + AcquirerNotiStr);

                    DL_SSK_transData.setPublickeyExponent(templateTransData.getPublickeyExponent());
                    DL_SSK_transData.setPublickeyModulus(templateTransData.getPublickeyModulus());
                    DL_SSK_transData.setPublickeyHash(templateTransData.getPublickeyHash());
                    DL_SSK_transData.setPublicKeyVersion(DL_SSK_transData.getPublicKeyVersion());
                    DL_SSK_transData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(tmp_acquirer.getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT) );
                    DL_SSK_transData.setInitAcquirerNii(tmp_acquirer.getNii());
                    DL_SSK_transData.setInitAcquirerName(tmp_acquirer.getName());

                    // reproduce session key block table
                    DL_SSK_transData.setSessionKeyBlock(templateTransData.getSessionKeyBlock());
                    DL_SSK_transData.setInitSessionKeyFile(templateTransData.getInitSessionKeyFile());
                    DL_SSK_transData.setSessionKeyOutput(templateTransData.getSessionKeyOutput());

                    // Online
                    Log.i("Online"," >> SEND : >> SessionKey Renewal [NII="+tmp_acquirer.getNii()+"] >> " + tmp_acquirer.getName());
                    local_ret = exOnline.online(DL_SSK_transData,exListener);


                    //Dont update setResult here :: send after loop acquirer was done
                    if (local_ret==TransResult.SUCC) {
                        if (getRespCode(DL_SSK_transData).equals("00")) {
                            updateProgress("ERCM Registration", AcquirerNotiStr + " : Load SSK. result : success");
                            save_sessionkey_renewal(DL_SSK_transData);
                            hasErrorDuringSendData=false;
                        } else {
                            updateProgress("ERCM Registration", AcquirerNotiStr + " : Load SSK. result : declined");
                            hasErrorDuringSendData=true;
                            String extraRespCode = ((getRespCode(DL_SSK_transData) != null) ? "( ErrCode : ".concat(getRespCode(DL_SSK_transData)).concat(")") : "") ;
                            DialogUtils.showErrMessage(context, "Result : " + extraRespCode , TransResultUtils.getMessage(TransResult.ERCM_SSK_DOWNLOAD_DECLINED), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                            break;
                        }
                    } else {
                        updateProgress("ERCM Registration", AcquirerNotiStr + " : Load SSK. result : failed");
                        hasErrorDuringSendData=true;
                        DialogUtils.showErrMessage(context, "Result : " , TransResultUtils.getMessage(local_ret), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                        break;
                    }
                }
            }
            int ix = 0 ;
        }
        else {
            if (specifyAcquirer.getEnableUploadERM()) {
                // incase renew sessionkey by single host
                String AcquirerNotiStr;
                TransData DL_SSK_transData = Intial_TransData_ERM_DownloadSessionKey(specifyAcquirer);
                AcquirerNotiStr =specifyAcquirer.getName() + " ("+ specifyAcquirer.getNii() + ")";
                if (exListener!=null) {exListener.onUpdateProgressTitle("SSK Renewal : " + AcquirerNotiStr);}

                templateTransData = new TransData();
                templateTransData = setPublicKey(templateTransData);
                templateTransData.setPublickeyExponent(templateTransData.getPublickeyExponent());
                templateTransData.setPublickeyModulus(templateTransData.getPublickeyModulus());
                templateTransData.setPublickeyHash(templateTransData.getPublickeyHash());
                templateTransData.setPublicKeyVersion(templateTransData.getPublicKeyVersion());

                // reproduce session key block table
                HashMap<String,Object> SKB_OutputEx = EReceiptUtils.getInstance().CreateSessionKeyBlock(templateTransData, context);
                templateTransData.setSessionKeyBlock((byte[]) SKB_Output.get("SKB"));
                templateTransData.setSessionKeyOutput((HashMap<String,Object>)SKB_OutputEx);

                DL_SSK_transData.setPublickeyExponent(templateTransData.getPublickeyExponent());
                DL_SSK_transData.setPublickeyModulus(templateTransData.getPublickeyModulus());
                DL_SSK_transData.setPublickeyHash(templateTransData.getPublickeyHash());
                DL_SSK_transData.setPublicKeyVersion(DL_SSK_transData.getPublicKeyVersion());
                DL_SSK_transData.setInitAcquirerIndex(EReceiptUtils.StringPadding(String.valueOf(specifyAcquirer.getId()), 3 ,"0", Convert.EPaddingPosition.PADDING_LEFT) );
                DL_SSK_transData.setInitAcquirerNii(specifyAcquirer.getNii());
                DL_SSK_transData.setInitAcquirerName(specifyAcquirer.getName());

                // reproduce session key block table
                DL_SSK_transData.setSessionKeyBlock(templateTransData.getSessionKeyBlock());
                DL_SSK_transData.setInitSessionKeyFile(templateTransData.getInitSessionKeyFile());
                DL_SSK_transData.setSessionKeyOutput(templateTransData.getSessionKeyOutput());

                // Online
                Log.i("Online"," >> SEND : >> SessionKey Renewal [NII="+specifyAcquirer.getNii()+"] >> " + specifyAcquirer.getName());
                local_ret = exOnline.online(DL_SSK_transData,exListener);

                if (local_ret==TransResult.SUCC) {
                    if (getRespCode(DL_SSK_transData).equals("00")) {
                        save_sessionkey_renewal(DL_SSK_transData);
                        hasErrorDuringSendData=false;
                    } else {
                        hasErrorDuringSendData=true;
                        String extraRespCode = ((getRespCode(DL_SSK_transData) != null) ? "( ErrCode : ".concat(getRespCode(DL_SSK_transData)).concat(")") : "") ;
                        //DialogUtils.showErrMessage(context, "Result : " + extraRespCode , TransResultUtils.getMessage(TransResult.ERCM_SSK_DOWNLOAD_DECLINED), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                        setResult(new ActionResult(TransResult.ERCM_SSK_DOWNLOAD_DECLINED, null));
                    }
                } else {
                    hasErrorDuringSendData=true;
                    //DialogUtils.showErrMessage(context, "Result : " , TransResultUtils.getMessage(local_ret), dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                    setResult(new ActionResult(local_ret, null));
                }
            }
           else {
                Log.i("Online"," >> SessionKey Renewal >> Host-Configuation " + specifyAcquirer.getName() +  " was disabled ERM upload for target host ");
                //DialogUtils.showErrMessage(context, "Result : " , "Sorry, Target host was disabled upload ERM.", dismissListener, Constants.FAILED_DIALOG_SHOW_TIME);
                setResult(new ActionResult(local_ret, null));
            }
        }

        if (hasErrorDuringSendData == false) {
            //DialogUtils.showSuccMessage(context, TransResultUtils.getMessage(TransResult.SUCC), dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
            //if (fromInitialMode == false) {
            updateProgress("ERCM Registration", "ERM SessionKey Download Successful");
                setResult(new ActionResult(TransResult.SUCC, null));
            //}
        } else {
            //if (fromInitialMode == false) {
            updateProgress("ERCM Registration", "ERM SessionKey Download Failed");
                setResult(new ActionResult(TransResult.ERCM_UPLOAD_SESSIONKEY_RENEWAL_RETRY_ERROR, null));
            //}
        }

        if (fromInitialMode == false) {
            if (exListener!=null) {exListener.onHideProgress();}
        }
    }

    public String[] ReadPublicKey() {
        HashMap<String,String> Hash = EReceiptUtils.detectEReceiptPBKFile();

        String[] PbkData = new String[4];
        if(Hash.size() == 4) {
            PbkData[0] = Hash.get("BANK_CODE");
            PbkData[1] = Hash.get("MERCHANT_CODE");
            PbkData[2] = Hash.get("STORE_CODE");
            PbkData[3] = Hash.get("KEY_VERSION");
        } else {
            PbkData= null;
            DialogUtils.showErrMessage(context, "Error during read PublicKey file", "ERCM-PublicKey file format unsupported.", dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
        }

        return PbkData;
    }

    public TransData setPublicKey(TransData transData) {
        HashMap<String,String> Hash = EReceiptUtils.detectEReceiptPBKFile();

        if(Hash.size() == 4 || Hash.size() == 7) {
            // Extract PBK details
            byte[] prod_exp = Tools.str2Bcd(Hash.get("EXPONENT").replace(" ", ""));
            byte[] prod_mod = Tools.str2Bcd(Hash.get("MODULUS").replace(" ", ""));
            byte[] prod_hsh = Tools.str2Bcd(Hash.get("HASH").replace(" ", ""));
            transData.setPublickeyExponent(prod_exp);
            transData.setPublickeyModulus(prod_mod);
            transData.setPublickeyHash(prod_hsh);
            transData.setPublicKeyVersion(Hash.get("KEY_VERSION"));

            //FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE, Hash.get("BANK_CODE"));
            //FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE, Hash.get("MERCHANT_CODE"));
            //FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE, Hash.get("STORE_CODE"));
            FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION,Hash.get("KEY_VERSION"));
        } else {
            setResult(new ActionResult(TransResult.ERCM_INITIAL_NO_PBK_FILE,null));
            //DialogUtils.showErrMessage(context, "Error during read PublicKey file", "ERCM-PublicKey file format unsupported.", dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
        }

        return transData;
    }
    public void readAndSetKeyVersion() {
        HashMap<String,String> Hash = EReceiptUtils.detectEReceiptPBKFile();

        if(Hash.size() == 4 || Hash.size() == 7) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION,Hash.get("KEY_VERSION"));
        } else {
            setResult(new ActionResult(TransResult.ERCM_INITIAL_NO_PBK_FILE,null));
            //DialogUtils.showErrMessage(context, "Error during read PublicKey file", "ERCM-PublicKey file format unsupported.", dismissListener, Constants.SUCCESS_DIALOG_SHOW_TIME);
        }


    }


    private TransData do_ExtractField63(TransData local_transData) {
        switch (local_transData.getTransType().toString()) {
            case "ERCEIPT_DOWNLOAD_PUBLICKEY" :
                local_transData = setPublicKey(local_transData);

                break;
            case "ERCEIPT_TERMINAL_REGISTRATION" :
                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED, Device.getTime(Constants.TIME_PATTERN_TRANS));

                break;
            case "ERCEIPT_SESSIONKEY_RENEWAL" :
                FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_INTIATED, Device.getTime(Constants.TIME_PATTERN_TRANS));

                break;
            default:
                break;
        }

        return local_transData;
    }


    private TransData Intial_TransData_ERM_TerminalRegistration (Acquirer currAcquirer){
        // Get Acquirer
        AcqManager acqManager = new AcqManager();
        Acquirer ERM_acquirer = acqManager.findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE);
        Acquirer SRC_acquirer = currAcquirer;

        // create TransData
        TransData transData = Component.transInit();

        if (ERM_acquirer != null &&  SRC_acquirer != null ) {
            Component.transInit(transData, ERM_acquirer);
            transData.setTransType(ETransType.ERCEIPT_TERMINAL_REGISTRATION);
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
            transData.setBatchNo(ERM_acquirer.getCurrBatchNo());
            transData.setAcquirer(ERM_acquirer);
            transData.setNii(SRC_acquirer.getNii());
            transData.setTpdu("600" + ERM_acquirer.getNii() + "8000");
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            transData.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
            transData.setERCMStoreCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE));
            transData.setERCMMerchantCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE));
            transData.setERCMTerminalSerialNumber(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER));

        }
        return transData;
    }

    private TransData Intial_TransData_ERM_DownloadSessionKey(Acquirer currAcquirer) {
        // Get Acquirer
        AcqManager acqManager = new AcqManager();
        Acquirer ERM_acquirer = acqManager.findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE);
        Acquirer SRC_acquirer = currAcquirer;

        // create TransData
        TransData transData = Component.transInit();

        if (ERM_acquirer != null && SRC_acquirer != null ) {
            Component.transInit(transData, ERM_acquirer);
            transData.setTransType(ETransType.ERCEIPT_SESSIONKEY_RENEWAL);
            transData.setReversalStatus(TransData.ReversalStatus.NORMAL);
            transData.setBatchNo(ERM_acquirer.getCurrBatchNo());
            transData.setAcquirer(ERM_acquirer);
            transData.setNii(SRC_acquirer.getNii());
            transData.setTpdu("600" + ERM_acquirer.getNii() + "8000");
            transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
            transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
            transData.setERCMBankCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE));
            transData.setERCMStoreCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE));
            transData.setERCMMerchantCode(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE));
            transData.setERCMTerminalSerialNumber(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER));
        }
        return transData;
    }

    private String getRespCode (TransData exTransData) {
        String respCode = "XX";
        if (exTransData != null) {
            String local_respCode =exTransData.getResponseCode().getCode();
            if (local_respCode != null) {
                respCode = local_respCode;
            }
        }

        return respCode;
    }

    private void do_ERMReport() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                //Sale Success ERM :
                long[] ermSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReport(false, false);
                long ermSuccessAdjust = FinancialApplication.getTransDataDbHelper().countERMReportForAdjust(false);
                ermSuccessTotal[0] = ermSuccessTotal[0] + ermSuccessAdjust;

                //Sale Unsuccessful ERM :
                long[] ermUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReport(true, false);
                long ermUnsuccessfulAdjust = FinancialApplication.getTransDataDbHelper().countERMReportForAdjust(true);
                ermUnsuccessfulTotal[0] = ermUnsuccessfulTotal[0] + ermUnsuccessfulAdjust;

                //Void Success ERM :
                long[] ermVoidSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReport(false, true);
                long[] ermVoidAdjustSuccessTotal = FinancialApplication.getTransDataDbHelper().countERMReportForVoidAdjust(false);
                ermVoidSuccessTotal[0] = ermVoidSuccessTotal[0] + ermVoidAdjustSuccessTotal[0];
                ermVoidSuccessTotal[1] = ermVoidSuccessTotal[1] + ermVoidAdjustSuccessTotal[1];

                //Void Unsuccessful ERM :
                long[] ermVoidUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReport(true, true);
                long[] ermVoidAdjustUnsuccessfulTotal = FinancialApplication.getTransDataDbHelper().countERMReportForVoidAdjust(true);
                ermVoidUnsuccessfulTotal[0] = ermVoidUnsuccessfulTotal[0] + ermVoidAdjustUnsuccessfulTotal[0];
                ermVoidUnsuccessfulTotal[1] = ermVoidUnsuccessfulTotal[1] + ermVoidAdjustUnsuccessfulTotal[1];

                int ret = Printer.printERMReport((Activity) context, ermSuccessTotal, ermUnsuccessfulTotal, ermVoidSuccessTotal, ermVoidUnsuccessfulTotal);
                setResult(new ActionResult(ret, null));
            }
        });
    }
}
