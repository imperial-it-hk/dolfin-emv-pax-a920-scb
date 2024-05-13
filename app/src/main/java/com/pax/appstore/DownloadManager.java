/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-8-31
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.appstore;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.pax.abl.utils.EncUtils;
import com.pax.dal.entity.LanParam;
import com.pax.device.Device;
import com.pax.edc.BuildConfig;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.market.android.app.sdk.BaseApiService;
import com.pax.market.android.app.sdk.StoreSdk;
import com.pax.market.android.app.sdk.dto.TerminalInfo;
import com.pax.market.api.sdk.java.base.exception.NotInitException;
import com.pax.market.api.sdk.java.base.exception.ParseXMLException;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.MerchantAcqProfile;
import com.pax.pay.base.MerchantProfile;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.MerchantAcqProfileDb;
import com.pax.pay.db.MerchantProfileDb;
import com.pax.pay.emv.EmvAid;
import com.pax.pay.emv.EmvCapk;
import com.pax.pay.trans.action.ActionEReceipt;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.component.KeyDataReadWriteJson;
import com.pax.pay.trans.model.AcqManager;
import com.pax.pay.trans.model.Controller;
import com.pax.pay.trans.model.MerchantProfileManager;
import com.pax.pay.utils.BitmapImageConverterUtils;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.MultiMerchantUtils;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.TransResultUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import th.co.bkkps.edc.receiver.process.SettleAlarmProcess;
import th.co.bkkps.utils.ArrayListUtils;
import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();
    private static DownloadManager instance;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Set<DocumentBase> documentList = new HashSet<>();
    private String appKey;
    private String appSecret;
    private String sn;
    private String filePath;
    private String saveFilePath;

    public static synchronized DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();

        }
        return instance;
    }

    public DownloadManager addDocument(DocumentBase documentBase) {
        documentList.add(documentBase);
        return instance;
    }

    public void updateData() {
        for (DocumentBase document : documentList) {
            if (document.parse() == 0) {
                document.save();
            }
            document.delete();
        }
    }

    public void clear() {
        documentList.clear();
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public boolean hasUpdateParam() {
        for (DocumentBase documentBase : documentList) {
            if (documentBase.isExit()) {
                return true;
            }
        }
        return false;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    enum updateParamState {
        NONE,
        PARAM_ACQ,
        PARAM_ISU_F,
        PARAM_MER_I,
        PARAM_IMG,
        PARAM_ISU_D,
        PARAM_ISU_R,
        PARAM_ISU_T,
        PARAM_EDC_I,
        PARAM_EDC_M,
        PARAM_SYS,
        PARAM_ERM,
        PARAM_DYM,
        PARAM_PWD,
        PARAM_TLE,
        PARAM_LPS,
        PARAM_COMM,
        PARAM_MLTM
    }

    public enum EdcRef1Ref2Mode {
        DISABLE(0),
        REF_1_MODE(1),
        REF_1_2_MODE(2);

        private final int mode;

        EdcRef1Ref2Mode(int mode) {
            this.mode = mode;
        }

        private static final Map<Integer, EdcRef1Ref2Mode> map;

        static {
            map = new HashMap<>();
            for (EdcRef1Ref2Mode edcRef1Ref2Mode : EdcRef1Ref2Mode.values()) {
                map.put(edcRef1Ref2Mode.mode, edcRef1Ref2Mode);
            }
        }

        public static EdcRef1Ref2Mode getByMode(int mode) {
            return map.get(mode);
        }
    }


    public boolean handleSuccess(Context context) {
        Log.deleteLog();
        //file download to saveFilePath above.
        File parameterFile = null;
        saveFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.SAVE_FILE_PATH_PARAM);
        File[] filelist = new File(saveFilePath).listFiles();
        updateParamState state = updateParamState.NONE;
        if (filelist != null && filelist.length > 0) {
            for (File f : filelist) {
                if (Constants.DOWNLOAD_PARAM_FILE_NAME.equals(f.getName())) {
                    parameterFile = f;
                }
            }
            if (parameterFile != null) {
/*
                String bannerTextValue = "Your push parameters  - "+parameterFile.getName()
                        +" have been successfully pushed at "+ sdf.format(new Date())+".";
                String bannerSubTextValue = "Files are stored in "+parameterFile.getPath();
                Log.i(TAG, "run=====: "+bannerTextValue);*/
                try {
                    //parse the download parameter xml file.
                    List<Map<String, Object>> datalist = new ArrayList<>();
                    //todo call API to parse xml
                    HashMap<String, String> resultMap = StoreSdk.getInstance().paramApi().parseDownloadParamXml(parameterFile);

                    if (resultMap != null && resultMap.size() > 0) {
                        //convert result map to list.
                        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("label", entry.getKey());
                            map.put("value", entry.getValue());
                            datalist.add(map);
                        }
                    }

                    final TerminalInfo[] terminalInfo = new TerminalInfo[1];
                    StoreSdk.getInstance().getBaseTerminalInfo(context, new BaseApiService.ICallBack() {
                        @Override
                        public void onSuccess(Object obj) {
                            TerminalInfo terminalInfoObj = (TerminalInfo) obj;
                            Log.i("onSuccess: ", terminalInfoObj.toString());
                            terminalInfo[0] = terminalInfoObj;
                            //Toast.makeText(getApplicationContext(), terminalInfo[0].toString(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.i("onError: ", e.toString());
                            //Toast.makeText(getApplicationContext(), "getTerminalInfo Error:"+e.toString(), Toast.LENGTH_SHORT).show();

                        }
                    });

                    FinancialApplication.getSysParam().loadNewPrefParam();

                    state = updateParamState.PARAM_ACQ;
                    updateAcquirerParam(resultMap, context);

                    state = updateParamState.PARAM_ISU_F;
                    updateEdcIssuerFileConfig(resultMap);

                    state = updateParamState.PARAM_MER_I;
                    updateMerchantInfo(resultMap);

                    state = updateParamState.PARAM_IMG;
                    updateImageParam(resultMap, saveFilePath, context);

                    state = updateParamState.PARAM_ISU_D;
                    updateIssuerParam(resultMap);

                    state = updateParamState.PARAM_ISU_R;
                    updateIssuerSupportRefundParam(resultMap);

                    state = updateParamState.PARAM_ISU_T;
                    updateIssuerSupportTipAdjustment(resultMap);

                    state = updateParamState.PARAM_EDC_I;
                    updateEDCInfo(resultMap);

                    state = updateParamState.PARAM_EDC_M;
                    updateEDCEnableMenu(resultMap);

                    state = updateParamState.PARAM_SYS;
                    updateSystemInfo(resultMap, context);

                    state = updateParamState.PARAM_ERM;
                    updateERCMParam(resultMap, context);

                    state = updateParamState.PARAM_DYM;
                    updateDynamicOfflineParam(resultMap, context);

                    state = updateParamState.PARAM_PWD;
                    updatePwd(resultMap);

                    state = updateParamState.PARAM_TLE;
                    updateTleParam(resultMap);

                    state = updateParamState.PARAM_LPS;
                    updateLinkPoSParam(resultMap);

                    state = updateParamState.PARAM_COMM;
                    updateCommunicationParameters(resultMap);

                    state = updateParamState.PARAM_MLTM;
                    updateMerchantProfileParameters(resultMap, saveFilePath, context);
                    //update successful info
                    Log.i(TAG, "Update Parameter Complete.");
                    //ToastUtils.showMessage("Update Parameter Complete.");
                } catch (NotInitException e) {
                    Log.e(TAG, "e:" + e);
                    Log.e(TAG, "ParameterUpdate Failed [STATE] : " + state.name());
                    return false;
                } catch (ParseXMLException e) {
                    Log.e("DownloadManager:", "parse xml failed: " + e.getMessage());
                    Log.e(TAG, "ParameterUpdate Failed [STATE] : " + state.name());
                    return false;
                } catch (MultiMerchantException e) {
                    Log.e("MultiMerchant:", " " + e.getMessage());
                    Log.e(TAG, "ParameterUpdate Failed [STATE] : " + state.name());
                    return false;
                } catch (Exception e) {
                    Log.e(TAG, "ExceptionError : " + e.getMessage());
                    Log.e(TAG, "ParameterUpdate Failed [STATE] : " + state.name());
                    return false;
                }
            } else {
                Log.i(TAG, "parameterFile is null ");
                return false;
            }
        } else {
            Log.i(TAG, "saveFile is null ");
            return false;
        }
        //Utils.restart();


        return true;
    }

    private void updateLinkPoSParam(HashMap<String, String> resultMap) {
        String  tempStr = null;
        Boolean tempBol = false;

        tempStr = resultMap.get(Constants.DN_PARAM_LINKPOS_COMM_TYPE);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.LINKPOS_COMM_TYPE, tempStr);
        }

        tempStr = resultMap.get(Constants.DN_PARAM_ECR_FOR_MERCHANT);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.COMM_MERC_NAME, tempStr);
        }

        tempStr = resultMap.get(Constants.DN_PARAM_PROTOCOL);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.LINKPOS_PROTOCOL, tempStr.toUpperCase());
            if (FinancialApplication.getEcrProcess() != null && FinancialApplication.getEcrProcess().mProtoFilter != null) {
                try {
                    FinancialApplication.getEcrProcess().mProtoFilter.setProtoSelect(tempStr);
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        }

        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE, true);
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE, true);
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID, false);
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE, false);

        // ECR MERCHANT NAME DETECTED
//        tempStr = resultMap.get(Constants.DN_PARAM_ECR_FOR_MERCHANT);
//        if (tempStr != null) {
//            FinancialApplication.getSysParam().set(SysParam.StringParam.COMM_MERC_NAME, tempStr);
//            FinancialApplication.getSysParam().set(SysParam.StringParam.LINKPOS_PROTOCOL, EcrProcessClass.getLinkPosProtocolByMerchantName(tempStr));
//            if (FinancialApplication.getEcrProcess() != null && FinancialApplication.getEcrProcess().mProtoFilter != null) {
//                try {
//                    FinancialApplication.getEcrProcess().mProtoFilter.setProtoSelect(tempStr);
//                } catch (Exception e) {
//                    Log.w(TAG, e);
//                }
//            }
//        }
//
//        boolean isAbleToMapMerchantName = (FinancialApplication.getSysParam().get(SysParam.StringParam.LINKPOS_PROTOCOL, null) != null);
//        if (isAbleToMapMerchantName) {
//            tempBol = setBooleanParam(resultMap.get(Constants.DN_PARAM_SETTLEMENT_RECEIPT_NUM));
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE, tempBol);
//
//            tempBol = setBooleanParam(resultMap.get(Constants.DN_PARAM_AUDITREPORT_RECEIPT_NUM));
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE, tempBol);
//
//            tempBol = setBooleanParam(resultMap.get(Constants.DN_PARAM_LINKPOS_BYPASS_CONFIRM_VOID));
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID, tempBol);
//
//            tempBol = setBooleanParam(resultMap.get(Constants.DN_PARAM_LINKPOS_BYPASS_CONFIRM_SETTLE));
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE, tempBol);
//        } else {
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_SETTLEMENT_RECEIPT_ENABLE, true);
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_AUDITREPORT_RECEIPT_ENABLE, true);
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_VOID, false);
//            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_LINKPOS_BYPASS_CONFIRM_SETTLE, false);
//        }
    }

    private void updateMerchantInfo(HashMap<String, String> resultMap) {
        String temp = resultMap.get(Constants.DN_PARAM_MER_NAME);
        FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_MERCHANT_NAME_EN, temp);

        temp = resultMap.get(Constants.DN_PARAM_MER_ADDR);
        FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_MERCHANT_ADDRESS, temp);

        temp = resultMap.get(Constants.DN_PARAM_MER_ADDR1);
        FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_MERCHANT_ADDRESS1, temp);
    }

    private void updateAcquirerParam(HashMap<String, String> resultMap, Context context) {
        if (resultMap.get(Constants.DN_PARAM_ACQ_NAME) != null) {
            boolean isRelease = BuildConfig.BUILD_TYPE.equalsIgnoreCase(Constants.BUILD_TYPE_RELEASE);
            String[] splitedAcqParamListName = resultMap.get(Constants.DN_PARAM_ACQ_NAME).split("\\|",-1);
            String[] splitedAcqParamListTID = resultMap.get(Constants.DN_PARAM_ACQ_TID).split("\\|",-1);
            String[] splitedAcqParamListMID = resultMap.get(Constants.DN_PARAM_ACQ_MID).split("\\|",-1);
            String[] splitedAcqParamListNII = resultMap.get(Constants.DN_PARAM_ACQ_NII).split("\\|",-1);
            String[] splitedAcqParamListBatch = resultMap.get(Constants.DN_PARAM_ACQ_BATCH).split("\\|",-1);

            // Extra 4 BackupIP
            String[] splitedAcqParamListIP = resultMap.get(Constants.DN_PARAM_ACQ_IP).split("\\|",-1);
            String[] splitedAcqParamListPort = resultMap.get(Constants.DN_PARAM_ACQ_PORT).split("\\|",-1);
            String[] splitedAcqParamListIP_2nd = resultMap.get(Constants.DN_PARAM_ACQ_IP_2ND).split("\\|",-1);
            String[] splitedAcqParamListPort_2nd = resultMap.get(Constants.DN_PARAM_ACQ_PORT_2ND).split("\\|",-1);
            String[] splitedAcqParamListIP_3rd = resultMap.get(Constants.DN_PARAM_ACQ_IP_3RD).split("\\|",-1);
            String[] splitedAcqParamListPort_3rd = resultMap.get(Constants.DN_PARAM_ACQ_PORT_3RD).split("\\|",-1);
            String[] splitedAcqParamListIP_4th = resultMap.get(Constants.DN_PARAM_ACQ_IP_4TH).split("\\|",-1);
            String[] splitedAcqParamListPort_4th = resultMap.get(Constants.DN_PARAM_ACQ_PORT_4TH).split("\\|",-1);

            String[] splitedAcqParamTestMode = !isRelease ? resultMap.get(Constants.DN_PARAM_ACQ_TEST_MODE).split("\\|",-1) : null;
            String[] splitedAcqParamListEnable = resultMap.get(Constants.DN_PARAM_ACQ_ENABLE).split("\\|",-1);
            String[] splitedAcqParamListBillerCode = resultMap.get(Constants.DN_PARAM_ACQ_BILLER_CODE).split("\\|",-1);
            String[] splitedAcqParamListBillerID = resultMap.get(Constants.DN_PARAM_ACQ_BILLER_ID).split("\\|",-1);
            String[] splitedAcqParamListHeaderLogo = resultMap.get(Constants.DN_PARAM_ACQ_HEADER_LOGO).split("\\|",-1);
            String[] splitedAcqParamListInstMinAmt = resultMap.get(Constants.DN_PARAM_ACQ_INST_MIN_AMT).split("\\|",-1);
            String[] splitedAcqParamListInstTerms = resultMap.get(Constants.DN_PARAM_ACQ_INST_TERMS).split("\\|",-1);
            String[] splitedAcqParamListUploadErmEnable = resultMap.get(Constants.DN_PARAM_ACQ_UPLOAD_ERM_ENABLE).split("\\|",-1);
            String[] splitedAcqParamListForceSettlementTime = resultMap.get(Constants.DN_PARAM_ACQ_FORCE_SETTLEMENT_TIME).split("\\|",-1);

            String[] splitedAcqParamListStoreID = resultMap.get(Constants.DN_PARAM_ACQ_STORE_ID).split("\\|",-1);
            String[] splitedAcqParamListStoreName = resultMap.get(Constants.DN_PARAM_ACQ_STORE_NAME).split("\\|",-1);
            String[] splitedAcqParamListTleBankName = resultMap.get(Constants.DN_PARAM_ACQ_TLE_BANK_NAME).split("\\|",-1);


            String[] splitedAcqParamListApiDomainName = resultMap.get(Constants.DN_PARAM_ACQ_API_DOMAIN_NAME).split("\\|",-1);
            String[] splitedAcqParamListApiPortNumber = resultMap.get(Constants.DN_PARAM_ACQ_API_PORT_NUMBER).split("\\|",-1);
            String[] splitedAcqParamListApiHostNameCheck = resultMap.get(Constants.DN_PARAM_ACQ_API_HOST_NAME_CHECK).split("\\|",-1);
            String[] splitedAcqParamListApiCertCheck = resultMap.get(Constants.DN_PARAM_ACQ_API_CERT_CHECK).split("\\|",-1);
            String[] splitedAcqParamListApiConnectTimetout = resultMap.get(Constants.DN_PARAM_ACQ_API_CONNECT_TIMEOUT).split("\\|",-1);
            String[] splitedAcqParamListApiReadTimetout = resultMap.get(Constants.DN_PARAM_ACQ_API_READ_TIMEOUT).split("\\|",-1);
            String[] splitedAcqParamListApiScreenTimetout = resultMap.get(Constants.DN_PARAM_ACQ_API_SCREEN_TIMEOUT).split("\\|",-1);
            String[] splitedAcqParamListEnableSSL = resultMap.get(Constants.DN_PARAM_ACQ_SSL_ENABLED).split("\\|",-1);

            String[] splitedAcqParamListEKycCardDataEncryption = resultMap.get(Constants.DN_PARAM_ACQ_EKYC_CHD_ENCRYPTION).split("\\|",-1);
            String[] splitedAcqParamListEnableControlLimit = null;
            String[] splitedAcqParamListEnablePhoneInput = null;
            String[] splitedAcqParamListSettleTime = resultMap.get(Constants.DN_PARAM_ACQ_SETTLE_TIME).split("\\|",-1);

            String[] splitedAcqParamListDolfinIsEnableCScanB = resultMap.get(Constants.DN_PARAM_DOLFIN_IS_ENABLE_C_SCAN_B_MODE).split("\\|",-1);
            String[] splitedAcqParamListDolfinCScanBDisplayQrTimeout = resultMap.get(Constants.DN_PARAM_DOLFIN_C_SCAN_B_DISPLAY_QR_TIMEOUT).split("\\|",-1);
            String[] splitedAcqParamListDolfinCScanBRetryTimes = resultMap.get(Constants.DN_PARAM_DOLFIN_C_SCAN_B_RETRY_TIMES).split("\\|",-1);
            String[] splitedAcqParamListDolfinCScanBDelayRetry = resultMap.get(Constants.DN_PARAM_DOLFIN_C_SCAN_B_DELAY_RETRY).split("\\|",-1);

            try {
                splitedAcqParamListEnableControlLimit = resultMap.get(Constants.DN_PARAM_ACQ_ENABLE_CONTROL_LIMIT).split("\\|",-1);
                splitedAcqParamListEnablePhoneInput = resultMap.get(Constants.DN_PARAM_ACQ_ENABLE_PHONE_INPUT).split("\\|",-1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            String[] splitedAcqParamListEnableSignature = null;
            if (resultMap.containsKey(Constants.DN_PARAM_ACQ_ENABLE_SIGNATURE)) {
                splitedAcqParamListEnableSignature = resultMap.get(Constants.DN_PARAM_ACQ_ENABLE_SIGNATURE).split("\\|",-1);
            }


            AcqManager acqManager = FinancialApplication.getAcqManager();
            List<Acquirer> allAcquirer = acqManager.findAllAcquirers();
            if (allAcquirer.isEmpty()) {
                initEMVParam();
                insertAcquirer();
                KeyDataReadWriteJson.updateKeyDataToAcquirers();
                Component.insertTransTypeMapping();
            }

            for (int i = 0; i < splitedAcqParamListName.length; i++) {
                Acquirer acquirer = acqManager.findAcquirer(splitedAcqParamListName[i].trim());
                boolean isEnable;
                boolean isUploadERMEnable;
                if (acquirer != null) { //Acq is in DB
                    isEnable = setBooleanParam(splitedAcqParamListEnable[i]);
                    isUploadERMEnable = setBooleanParam(splitedAcqParamListUploadErmEnable[i]);
                    if (isEnable) {//Use this Acq, Update Acq in DB
                        acquirer.setTerminalId(splitedAcqParamListTID[i]);
                        acquirer.setMerchantId(splitedAcqParamListMID[i]);
                        acquirer.setNii(splitedAcqParamListNII[i]);
                        acquirer.setCurrBatchNo(Integer.parseInt(splitedAcqParamListBatch[i]));
                        acquirer.setIp(splitedAcqParamListIP[i]);

                        String tmpPort = !("").equals(splitedAcqParamListPort[i]) ? splitedAcqParamListPort[i] : "0";
                        acquirer.setPort(Integer.parseInt(tmpPort));
                        acquirer.setIpBak1(splitedAcqParamListIP_2nd[i]);

                        tmpPort = !("").equals(splitedAcqParamListPort_2nd[i]) ? splitedAcqParamListPort_2nd[i] : "0";
                        acquirer.setPortBak1(Integer.parseInt(tmpPort));
                        acquirer.setIpBak2(splitedAcqParamListIP_3rd[i]);

                        tmpPort = !("").equals(splitedAcqParamListPort_3rd[i]) ? splitedAcqParamListPort_3rd[i] : "0";
                        acquirer.setPortBak2(Integer.parseInt(tmpPort));
                        acquirer.setIpBak3(splitedAcqParamListIP_4th[i]);

                        tmpPort = !("").equals(splitedAcqParamListPort_4th[i]) ? splitedAcqParamListPort_4th[i] : "0";
                        acquirer.setPortBak3(Integer.parseInt(tmpPort));
                        acquirer.setEnable(isEnable);
                        acquirer.setEnableUploadERM(isUploadERMEnable);

                        if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_QR_CREDIT)) {
                            acquirer.setForceSettleTime(splitedAcqParamListForceSettlementTime[i]);
                        } else {
                            acquirer.setForceSettleTime(null);
                        }

                        if (!isRelease && acquirer.getName().equalsIgnoreCase(Constants.ACQ_UP)) {
                            acquirer.setTestMode(setBooleanParam(splitedAcqParamTestMode[i]));
                        }

                        if (splitedAcqParamListBillerCode[i] != null && !splitedAcqParamListBillerCode[i].equalsIgnoreCase("0")) {
                            acquirer.setBillerServiceCode(splitedAcqParamListBillerCode[i]);
                        }

                        if (splitedAcqParamListBillerID[i] != null && !splitedAcqParamListBillerID[i].equalsIgnoreCase("0")) {
                            acquirer.setBillerIdPromptPay(splitedAcqParamListBillerID[i]);
                        }

                        if (!splitedAcqParamListInstMinAmt[i].equalsIgnoreCase("0")) {
                            acquirer.setInstalmentMinAmt(splitedAcqParamListInstMinAmt[i]);
                        }

                        if (!splitedAcqParamListInstTerms[i].equalsIgnoreCase("0")) {
                            acquirer.setInstalmentTerms(splitedAcqParamListInstTerms[i]);
                        }
                        if (splitedAcqParamListStoreID[i] != null && !splitedAcqParamListStoreID[i].equalsIgnoreCase("0")) {
                            acquirer.setStoreId(splitedAcqParamListStoreID[i]);
                        }
                        if (splitedAcqParamListStoreName[i] != null && !splitedAcqParamListStoreName[i].trim().isEmpty()) {
                            acquirer.setStoreName(splitedAcqParamListStoreName[i]);
                        }
                        if (splitedAcqParamListTleBankName[i] != null && !splitedAcqParamListTleBankName[i].trim().isEmpty()) {
                            acquirer.setTleBank(splitedAcqParamListTleBankName[i]);
                        }

                        if (acquirer.getName().equalsIgnoreCase(Constants.ACQ_DOLFIN)) {
                            boolean enableCScanBMode = (splitedAcqParamListDolfinIsEnableCScanB[i].compareToIgnoreCase("Y") == 0) ? true : false;
                            acquirer.setEnableCScanBMode(enableCScanBMode);
                            acquirer.setCScanBDisplayQrTimeout(Integer.parseInt(splitedAcqParamListDolfinCScanBDisplayQrTimeout[i]));
                            acquirer.setCScanBRetryTimes(Integer.parseInt(splitedAcqParamListDolfinCScanBRetryTimes[i]));
                            acquirer.setCScanBDelayRetry(Integer.parseInt(splitedAcqParamListDolfinCScanBDelayRetry[i]));
                        }

                        // API Config for EKYC
                        if (splitedAcqParamListApiDomainName[i] != null && !splitedAcqParamListApiDomainName[i].trim().isEmpty()) {
                            acquirer.setApiDomainName(splitedAcqParamListApiDomainName[i]);
                        }
                        if (splitedAcqParamListApiPortNumber[i] != null && !splitedAcqParamListApiPortNumber[i].trim().isEmpty()) {
                            acquirer.setApiPortNumber(Integer.parseInt(splitedAcqParamListApiPortNumber[i]));
                        }
                        if (splitedAcqParamListApiHostNameCheck[i] != null && !splitedAcqParamListApiHostNameCheck[i].trim().isEmpty()) {
                            acquirer.setApiHostNameCheck(Integer.parseInt(splitedAcqParamListApiHostNameCheck[i]) == 1);
                        }
                        if (splitedAcqParamListApiCertCheck[i] != null && !splitedAcqParamListApiCertCheck[i].trim().isEmpty()) {
                            acquirer.setApiCertificationCheck(Integer.parseInt(splitedAcqParamListApiCertCheck[i]) == 1);
                        }
                        if (splitedAcqParamListApiConnectTimetout[i] != null && !splitedAcqParamListApiConnectTimetout[i].trim().isEmpty()) {
                            acquirer.setApiConnectTimeout(Integer.parseInt(splitedAcqParamListApiConnectTimetout[i]));
                        }
                        if (splitedAcqParamListApiReadTimetout[i] != null && !splitedAcqParamListApiReadTimetout[i].trim().isEmpty()) {
                            acquirer.setApiReadTimeout(Integer.parseInt(splitedAcqParamListApiReadTimetout[i]));
                        }
                        if (splitedAcqParamListApiScreenTimetout[i] != null && !splitedAcqParamListApiScreenTimetout[i].trim().isEmpty()) {
                            acquirer.setApiScreenTimeout(Integer.parseInt(splitedAcqParamListApiScreenTimetout[i]));
                        }
                        if (splitedAcqParamListEnableSSL[i] != null && !splitedAcqParamListEnableSSL[i].trim().isEmpty()) {
                            if (setBooleanParam(splitedAcqParamListEnableSSL[i])) {
                                acquirer.setSslType(SysParam.Constant.CommSslType.SSL);
                            } else {
                                acquirer.setSslType(SysParam.Constant.CommSslType.NO_SSL);
                            }
                        }

                        // EKYC Config
                        if (splitedAcqParamListEKycCardDataEncryption[i] != null && !splitedAcqParamListEKycCardDataEncryption[i].trim().isEmpty()) {
                            acquirer.setEKycDataEncryption(Integer.parseInt(splitedAcqParamListEKycCardDataEncryption[i]) ==1);
                        }

                        if (splitedAcqParamListSettleTime[i] != null && !splitedAcqParamListSettleTime[i].trim().isEmpty()) {
                            acquirer.setSettleTime(splitedAcqParamListSettleTime[i]);
                        }
                        acquirer.setLatestSettledDateTime(Device.getTime(SettleAlarmProcess.STR_FORMAT_DATETIME_LAST_SETTLE));

                        // GENERIC R1&4 : CONTROL LIMIT
                        if (splitedAcqParamListEnableControlLimit !=null && splitedAcqParamListEnableControlLimit.length >= i)  {
                            acquirer.setEnableControlLimit(setBooleanParam(splitedAcqParamListEnableControlLimit[i]));
                        }
                        if (splitedAcqParamListEnablePhoneInput !=null && splitedAcqParamListEnablePhoneInput.length >= i) {
                            acquirer.setEnablePhoneNumberInput(setBooleanParam(splitedAcqParamListEnablePhoneInput[i]));
                        }

                        if (splitedAcqParamListEnableSignature != null) {
                            boolean isSignatureRequired = setBooleanParam(splitedAcqParamListEnableSignature[i]);
                            acquirer.setSignatureRequired(isSignatureRequired);
                        } else {
                            acquirer.setSignatureRequired(true);
                        }

                        acqManager.updateAcquirer(acquirer);

                        BitmapImageConverterUtils.getInstance().saveBmpImageParam(splitedAcqParamListHeaderLogo[i], saveFilePath, context, acquirer.getNii(), acquirer.getName());
                    } else { //NOT use this Acq, Disable it
                        acquirer.setEnable(isEnable);
                        acqManager.updateAcquirer(acquirer);

                        allAcquirer = acqManager.findAllAcquirers();
                        FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, allAcquirer.get(0).getName());
                        acqManager.setCurAcq(allAcquirer.get(0));
                    }
                }
            }
        }
    }

    private void updateIssuerParam(HashMap<String, String> resultMap) {
        if (resultMap.get(Constants.DN_PARAM_SMALL_AMT_ISSUERNAME) != null) {
            String[] splitedSmallAmtListName = resultMap.get(Constants.DN_PARAM_SMALL_AMT_ISSUERNAME).split("\\|",-1);
            String[] splitedSmallAmtListSupport = resultMap.get(Constants.DN_PARAM_SMALL_AMT_SUPPORT).split("\\|",-1);
            String[] splitedSmallAmtListAmount = resultMap.get(Constants.DN_PARAM_SMALL_AMT_AMT).split("\\|",-1);
            String[] splitedSmallAmtListReceipt = resultMap.get(Constants.DN_PARAM_SMALL_AMT_RECEIPT).split("\\|",-1);
            AcqManager acqManager = FinancialApplication.getAcqManager();
            List<String> enabledAcq = new ArrayList<>();

            for (int i = 0; i < splitedSmallAmtListName.length; i++) {
                String acqName = "";
                List<Issuer> issuers = FinancialApplication.getAcqManager().findIssuerByBrand(splitedSmallAmtListName[i].trim());
                if (issuers != null) {
                    for (Issuer issuer : issuers) {
                        Acquirer acquirer = acqManager.findAcquirer(issuer.getAcqHostName());
                        if (acquirer != null) {//will not continue if cannot find Issuer or Acquirer
                            boolean isDuplicateAcq = issuer.getAcqHostName().equalsIgnoreCase(acqName);
                            boolean isEnableSmallAmount = setBooleanParam(splitedSmallAmtListSupport[i]);
                            if (isEnableSmallAmount) {
                                if (!isDuplicateAcq) {
                                    acqName = issuer.getAcqHostName();
                                    enabledAcq.add(acquirer.getName());
                                    if (!acquirer.isEnableSmallAmt()) {
                                        acquirer.setEnableSmallAmt(true);
                                        acqManager.updateAcquirer(acquirer);
                                    }
                                }

                                issuer.setEnableSmallAmt(isEnableSmallAmount);
                                issuer.setSmallAmount(Utils.parseLongSafe(splitedSmallAmtListAmount[i], 0));
                                issuer.setNumberOfReceipt(Integer.parseInt(splitedSmallAmtListReceipt[i]));
                                acqManager.updateIssuer(issuer);
                            } else {
                                if (!isDuplicateAcq) {
                                    acqName = issuer.getAcqHostName();
                                    if (!enabledAcq.contains(acquirer.getName())) {
                                        acquirer.setEnableSmallAmt(false);
                                        acqManager.updateAcquirer(acquirer);
                                    }
                                }
                                if (issuer.isEnableSmallAmt()) {
                                    issuer.setEnableSmallAmt(isEnableSmallAmount);
                                    issuer.setSmallAmount(Utils.parseLongSafe(splitedSmallAmtListAmount[i], 0));
                                    issuer.setNumberOfReceipt(Integer.parseInt(splitedSmallAmtListReceipt[i]));
                                    acqManager.updateIssuer(issuer);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateIssuerSupportRefundParam(HashMap<String, String> resultMap) {
        if (resultMap.get(Constants.DN_PARAM_REFUND_ISUUERNAME) != null) {
            String[] splitedRefundListName = resultMap.get(Constants.DN_PARAM_REFUND_ISUUERNAME).split("\\|",-1);
            String[] splitedRefundListSupport = resultMap.get(Constants.DN_PARAM_REFUND_SUPPORT).split("\\|",-1);
            AcqManager acqManager = FinancialApplication.getAcqManager();
            for (int i = 0; i < splitedRefundListName.length; i++) {
                List<Issuer> issuers = FinancialApplication.getAcqManager().findIssuerByBrand(splitedRefundListName[i].trim());
                if (issuers != null) {
                    for (Issuer issuer : issuers) {
                        boolean isAllowRefund = setBooleanParam(splitedRefundListSupport[i]);
                        issuer.setAllowRefund(isAllowRefund);
                        acqManager.updateIssuer(issuer);
                    }
                }
            }
        }
    }

    private void updateIssuerSupportTipAdjustment(HashMap<String, String> resultMap) { //todo
        try {
            if (resultMap.get(Constants.DN_PARAM_TIP_ADJ_ISUUERNAME) != null) {
                String[] splitedTipAdjListName = resultMap.get(Constants.DN_PARAM_TIP_ADJ_ISUUERNAME).split("\\|",-1);
                String[] splitedTipAdjListSupport = resultMap.get(Constants.DN_PARAM_TIP_ADJ_SUPPORT).split("\\|",-1);
                String[] splitedTipAdjListPecent = resultMap.get(Constants.DN_PARAM_TIP_ADJ_PERCENT).split("\\|",-1);

                AcqManager acqManager = FinancialApplication.getAcqManager();
                for (int i = 0; i < splitedTipAdjListName.length; i++) {
                    if (splitedTipAdjListName[i].trim().equals("VISA-BDMS")){
                        Issuer issuer = FinancialApplication.getAcqManager().findIssuer(splitedTipAdjListName[i].trim());
                        if (issuer != null) {
                            boolean isAllowTipAdj = setBooleanParam1(splitedTipAdjListSupport[i], issuer);
                            int tipAdjustMaxPercent = Utils.parseIntSafe(splitedTipAdjListPecent[i], 15);
                            issuer.setEnableAdjust(isAllowTipAdj);
                            issuer.setAdjustPercent(Integer.valueOf(tipAdjustMaxPercent).floatValue());
                            acqManager.updateIssuer(issuer);
                        }
                    } else {
                        List<Issuer> issuers = FinancialApplication.getAcqManager().findIssuerByBrand(splitedTipAdjListName[i].trim());
                        if (issuers != null) {
                            for (Issuer issuer : issuers) {
                                boolean isAllowTipAdj = setBooleanParam1(splitedTipAdjListSupport[i], issuer);
                                int tipAdjustMaxPercent = Utils.parseIntSafe(splitedTipAdjListPecent[i], 15);
                                issuer.setEnableAdjust(isAllowTipAdj);
                                issuer.setAdjustPercent(Integer.valueOf(tipAdjustMaxPercent).floatValue());
                                acqManager.updateIssuer(issuer);
                            }
                        }
                    }
                }
            }
            Log.e("updateEdcIssuerFileConfig", "-------------");
        } catch (Exception ex) {
        ex.printStackTrace();
        Log.e("updateEdcIssuerFileConfig", "---------");
    }
    }

    private void updateImageParam(HashMap<String, String> resultMap, String saveFilePath, Context context) {
        saveImageParam(resultMap, saveFilePath, context, Constants.DN_PARAM_SLIP_LOGO, Constants.SLIP_LOGO_NAME, Constants.DN_PARAM_SLIP_LOGO_PATH);

        saveImageParam(resultMap, saveFilePath, context, Constants.DN_PARAM_IMG_ON_RECEIPT_FILE, Constants.DN_PARAM_IMG_ON_RECEIPT_FILE_NAME, Constants.DN_PARAM_IMG_ON_RECEIPT_FILE_PATH);
    }

    private void saveImageParam(HashMap<String, String> resultMap, String saveFilePath, Context context, String paramName, String savedFileName, String savedFilePath) {
        String fileName = resultMap.get(paramName);
        try {
            if (!fileName.isEmpty()) {
                File f = new File(saveFilePath, fileName);
                if (f.exists()) {
                    Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                    String filePath = saveToInternalStorage(b, context, savedFileName);
                    saveString(savedFilePath, filePath);
                }
            } else {
                saveString(savedFilePath, null);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateEDCInfo(HashMap<String, String> resultMap) {
        Boolean temp;
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_E_SIGNATURE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_E_SIGNATURE, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_DOL_SHOW_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.KBANK_DYNAMIC_OFFLINE_SHOW_MENU, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_GRANDTOTAL));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_GRAND_TOTAL, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_LINKPOS_KERRYAPI));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_KERRY_API, temp);

        try {
            temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_UPI_FORCE_APP_AUTO_SEL_FOR_DUO_BRAND));
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD, temp);
        } catch (Exception ex) {
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_FORCE_SEL_APP_FOR_DUO_BRAND_CARD, true);
        }


        String tempStr = resultMap.get(Constants.DN_PARAM_CAMERA);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_DEFAULT_CAMERA, tempStr);
        }

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_WALLET_C_SCAN_B));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_WALLET_C_SCAN_B, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_QR_BARCODE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_KIOSK_MODE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_KIOSK_MODE, temp);

        tempStr = resultMap.get(Constants.DN_PARAM_KIOSK_TIMEOUT);
        int time = temp != null && !"".equals(tempStr) && !"0".equals(tempStr) ? Integer.parseInt(tempStr) : 30;
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_KIOSK_TIMEOUT, time);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_IMG_ON_RECEIPT));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_IMG_ON_END_RECEIPT, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_ENABLE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_VISA));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_VISA, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_MASTER));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_MASTER, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_JCB));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_JCB, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_UP));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_UP, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_TPN));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_TPN, temp);
        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_CONTACTLESS_AMEX));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CONTACTLESS_AMEX, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ENABLE_KEYIN));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_KEYIN, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_VOID_WITH_STAND));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_VOID_WITH_STAND, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ENABLE_QR_BARCODE_ALIWECHAT));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_ALIPAY_WECHAT, temp);

        tempStr = resultMap.get(Constants.DN_PARAM_EDC_RECEIPT_NUM);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_RECEIPT_NUM, Utils.parseIntSafe(tempStr, 2));

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_SUPPORT_SP200));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_SUPPORT_SP200, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ENABLE_QR_COD));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_QR_BARCODE_COD, temp);

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_CTLS_TRANS_LIMIT)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_CTLS_TRANS_LIMIT);
            if (tempStr != null) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_CTLS_TRANS_LIMIT, tempStr);
                updateCtlsTransLimit(tempStr);
            }
        }

        tempStr = resultMap.get(Constants.DN_PARAM_EDC_MAX_AMT);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_MAXIMUM_AMOUNT, tempStr);
        }

        tempStr = resultMap.get(Constants.DN_PARAM_EDC_MIN_AMT);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_MINIMUM_AMOUNT, tempStr);
        }

        tempStr = resultMap.get(Constants.DN_PARAM_EDC_UI_LANG);
        if (tempStr != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_LANGUAGE, tempStr);
        }

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_EDC_REFUND));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_SUPPORT_REFUND, temp);

        tempStr = resultMap.get(Constants.DN_PARAM_TIME_OUT_SEARCH_CARD);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.SCREEN_TIME_OUT_SEARCH_CARD, Utils.parseIntSafe(tempStr, TickTimer.DEFAULT_TIMEOUT));


        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_DOUBLE_BLOCKED_TRANS_ENABLE,
                setBooleanParam(resultMap.get(Constants.DN_PARAM_EDC_DOUBLE_BLOCKED_TRANS_ENABLE)));


        // =====================================================================================================
        // QR TAG 31
        // =====================================================================================================
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_QR_TAG_31_ENABLE,
                setBooleanParam(resultMap.get(Constants.DN_PARAM_QRTAG31_ENABLE)));

        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_QR_TAG_31_REPORT_GROUPING_OLD_STYLE,
                setBooleanParam(resultMap.get(Constants.DN_PARAM_QRTAG31_OLD_STYLE_REPORT_ENABLE)));

        int tempNumVal = 0 ;
        if (resultMap.containsKey(Constants.DN_PARAM_QRTAG31_ECR_CARD_LABEL_MODE)) {
            try {
                tempNumVal = Integer.parseInt(resultMap.get(Constants.DN_PARAM_QRTAG31_ECR_CARD_LABEL_MODE)) ;
            } catch (Exception ex) {
                tempNumVal = 0;
            }
        }
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_QR_TAG_31_ECR_RETURN_MODE, tempNumVal);

        tempStr = resultMap.get(Constants.DN_PARAM_MAX_NUMB_OF_INQUIRY_SHOW_VERIFY_QR_BTN);
        int numberMaxInqThaiQrShowVerifyQR =  Utils.parseIntSafe(tempStr, 2) ;
        FinancialApplication.getSysParam().set(SysParam.NumberParam.THAI_QR_INQUIRY_MAX_COUNT_FOR_SHOW_VERIFY_QR_BUTTON, numberMaxInqThaiQrShowVerifyQR);
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_SETTLEMENT_MODE)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_SETTLEMENT_MODE);
            if (tempStr != null) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_SETTLEMENT_MODE, tempStr);
            }
        }

        // =====================================================================================================
        // AUTO & FORCE SETTLEMENT Configuration
        // =====================================================================================================
        temp = (resultMap.get(Constants.DN_PARAM_EDC_SETTLEMENT_TEST_ENBALE).equals("1")) ? true : false ;
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_SETTLE_MODE_TESTING, temp);

        tempStr = resultMap.get(Constants.DN_PARAM_EDC_SETTLEMENT_TEST_TIME_INTERVAL);
        FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_SETTLE_MODE_TESTING_TIME_INTERVAL, tempStr);

        temp = (resultMap.get(Constants.DN_PARAM_EDC_PRINT_ON_EXECUTE_SETTLE).equals("1")) ? true : false ;
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE, temp);


        // =====================================================================================================
        // PreAuthorization Configuration
        // =====================================================================================================
        boolean isEnablePreAuth = false;
        int saleCompMaxPercent = 15;
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_ENABLE_PREAUTH)) {
                isEnablePreAuth = setBooleanParam(resultMap.get(Constants.DN_PARAM_EDC_ENABLE_PREAUTH));
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_PREAUTH, temp);
        }
        
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_PREAUTH_MAX_DAY_KEEP_TRANS)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_PREAUTH_MAX_DAY_KEEP_TRANS);
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_NUMBER_OF_DAY_KEEP_PREAUTH_TRANS, Utils.parseIntSafe(tempStr, 30));     // default valid 30 days
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_SALECOMP_MAX_PERCENT)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_SALECOMP_MAX_PERCENT);
            saleCompMaxPercent = Utils.parseIntSafe(tempStr, 15);
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_MAX_PERCENTAGE_SALE_COMPLETION, saleCompMaxPercent);       // default valid 15%
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_PREAUTH_PWD)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_PREAUTH_PWD);
            if (tempStr != null && tempStr.length() == 6) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_PREAUTH_PWD, EncUtils.sha1(tempStr));
            }
        }

        // =====================================================================================================
        // Offline Configuration
        // =====================================================================================================
        boolean isEnableOffline = false;
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_ENABLE_OFFLINE)) {
            isEnableOffline = setBooleanParam(resultMap.get(Constants.DN_PARAM_EDC_ENABLE_OFFLINE));
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_OFFLINE, isEnableOffline);
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_OFFLINE_PWD)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_OFFLINE_PWD);
            if (tempStr != null && tempStr.length() == 6) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_OFFLINE_PWD, EncUtils.sha1(tempStr));
            }
        }

        // =====================================================================================================
        // Tip Adjustment Configuration
        // =====================================================================================================
        boolean isEnableTipAdjust = false;
        int tipAdjustMaxPercent = 15;
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_ENABLE_TIP_ADJUSTMENT)) {
            isEnableTipAdjust = setBooleanParam(resultMap.get(Constants.DN_PARAM_EDC_ENABLE_TIP_ADJUSTMENT));
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_TIP_ADJUST, isEnableTipAdjust);
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_TIP_ADJUSTMENT_MAX_PERCENT)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_TIP_ADJUSTMENT_MAX_PERCENT);
            tipAdjustMaxPercent = Utils.parseIntSafe(tempStr, 15);
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_MAX_PERCENTAGE_TIP_ADJUST, tipAdjustMaxPercent);// default valid 15%
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_TIP_ADJUSTMENT_PWD)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_TIP_ADJUSTMENT_PWD);
            if (tempStr != null && tempStr.length() == 6) {
                FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_TIP_ADJUSTMENT_PWD, EncUtils.sha1(tempStr));
            }
        }

        /* *
        * Update Offline / Tip Adjust / Pre-Auth config to issuer
        * */
        List<Issuer> issuers = FinancialApplication.getAcqManager().findAllIssuers();
        for (Issuer issuer: issuers) {
            String[] issuerBrands = new String[]{Constants.ISSUER_JCB, Constants.ISSUER_VISA, Constants.ISSUER_MASTER,
                    Constants.ISSUER_UP, Constants.ISSUER_BRAND_TBA};
            if (ArrayListUtils.INSTANCE.isFoundItem(issuerBrands, issuer.getIssuerBrand())) {
                issuer.setEnableOffline(isEnableOffline);
                //issuer.setEnableAdjust(isEnableTipAdjust);
                issuer.setAllowPreAuth(isEnablePreAuth);
            } else {
                issuer.setEnableOffline(false);
                //issuer.setEnableAdjust(false);
                issuer.setAllowPreAuth(false);
            }
            //issuer.setAdjustPercent(Integer.valueOf(tipAdjustMaxPercent).floatValue());
            issuer.setSaleCompPercent(Integer.valueOf(saleCompMaxPercent).floatValue());
            FinancialApplication.getAcqManager().updateIssuer(issuer);
        }

        // =====================================================================================================
        // SUPPORT REF1 & REF2
        // =====================================================================================================
        if (resultMap.containsKey(Constants.DN_PARAM_EDC_ENABLE_REF1_REF2)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_ENABLE_REF1_REF2);
            if (tempStr==null) { tempStr = "0"; }
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE, Utils.parseIntSafe(tempStr, 0));               // 0 = Disable, 1 = REF1 Only, 2 = REF1 & REF2
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_REF1_DISP_TEXT)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_REF1_DISP_TEXT);
            if (tempStr == null) {
                tempStr = "REFERENCE 1";
            }
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_DISP_TEXT_REF1, tempStr);
        }

        if (resultMap.containsKey(Constants.DN_PARAM_EDC_REF2_DISP_TEXT)) {
            tempStr = resultMap.get(Constants.DN_PARAM_EDC_REF2_DISP_TEXT);
            if (tempStr == null) {
                tempStr = "REFERENCE 2";
            }
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_DISP_TEXT_REF2, tempStr);
        }

    }

    private void updateEDCEnableMenu(HashMap<String, String> resultMap) {
        boolean temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_SALE_CREDIT_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_SALE_CREDIT_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_VOID_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_VOID_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_KPLUS_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_KPLUS_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_ALIPAY_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_WECHAT_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_WECHAT_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_QR_CREDIT_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_QR_CREDIT_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_SMART_PAY_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_SMART_PAY_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_REDEEM_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_REDEEM_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_CT1_EPP_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_CT1_EPP_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_AMEX_EPP_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_AMEX_EPP_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_SCB_IPP_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_SCB_IPP_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_SCB_REDEEM_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_SCB_REDEEM_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_DOLFIN_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_MYPROMPT_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_MYPROMPT_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_DOLFIN_IPP_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_DOLFIN_IPP_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_ALIPAY_BSCANC_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_ALIPAY_BSCANC_MENU, temp);

        temp = setBooleanParamDefaultTrue(resultMap.get(Constants.DN_PARAM_ENABLE_BSCANC_WECHAT_MENU));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.EDC_ENABLE_WECHAT_BSCANC_MENU, temp);
    }

    private void updateSystemInfo(HashMap<String, String> resultMap, final Context context) {
        String temp = resultMap.get(Constants.DN_PARAM_SCREEN_TIMEOUT);
        int time = temp != null ? Integer.parseInt(temp) : -1;
        FinancialApplication.getSysParam().set(SysParam.NumberParam.SCREEN_TIMEOUT, time);
        if (time == -1) {
            time = Integer.MAX_VALUE;

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean settingsCanWrite = Settings.System.canWrite(context);
            if (!settingsCanWrite) {
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_SCREEN_TIMEOUT, true);
            } else {
                FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_SCREEN_TIMEOUT, false);
                android.provider.Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, time);
            }
        } else {
            FinancialApplication.getSysParam().set(SysParam.BooleanParam.NEED_UPDATE_SCREEN_TIMEOUT, false);
            android.provider.Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, time);
        }
    }

    private void updateEdcIssuerFileConfig(HashMap<String, String> resultMap) {
        //reset to default status
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_AID_FILE_UPLOAD_STATUS, 2);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_CARD_RANGE_FILE_UPLOAD_STATUS, 2);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_ISSUER_FILE_UPLOAD_STATUS, 2);

        if (resultMap != null) {
            List<Issuer> issuers = null;
            List<CardRange> cardRanges = null;
            List<EmvAid> emvAids = null;
            try {
                issuers = Utils.readObjFromJSON(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_ISSUER), Issuer.class);
                cardRanges = Utils.readObjFromJSON(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_CARD_RANGE), CardRange.class);
                emvAids = Utils.readObjFromJSON(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_AID), EmvAid.class);

                boolean doProcess, isIssuerUpdated;
                AcqManager acqManager = FinancialApplication.getAcqManager();

                if (isIssuerUpdated = !issuers.isEmpty()) {
                    Log.d(TAG, "updateEdcIssuerFileConfig: Issuer file is found.");
                    cardRanges = cardRanges.isEmpty() ? acqManager.findAllCardRanges() : cardRanges;
                    doProcess = acqManager.deleteAcqIssuerRelations(acqManager.findAllRelations())
                            && acqManager.deleteCardRange(acqManager.findAllCardRanges())
                            && acqManager.deleteIssuer(acqManager.findAllIssuers());
                } else if (!cardRanges.isEmpty()) {
                    Log.d(TAG, "updateEdcIssuerFileConfig: Issuer file NOT found.");
                    issuers = acqManager.findAllIssuers();
                    doProcess = acqManager.deleteCardRange(acqManager.findAllCardRanges());
                } else {
                    doProcess = false;
                    Log.d(TAG, "updateEdcIssuerFileConfig: doProcess=false");
                }

                Log.d(TAG, "updateEdcIssuerFileConfig: isIssuerUpdated=" + isIssuerUpdated);
                Log.d(TAG, "updateEdcIssuerFileConfig: doProcess=" + doProcess);
                if (doProcess) {
                    List<Acquirer> acquirers;
                    acquirers = (acquirers = acqManager.findEnableAcquirers()) == null ? acqManager.findAllAcquirers() : acquirers;
                    for (Issuer issuer : issuers) {
                        if (isIssuerUpdated) {
                            acqManager.insertIssuer(issuer);
                            if (acquirers != null) {
                                for (Acquirer acquirer : acquirers)
                                    acqManager.bind(acquirer, issuer);
                            }
                        }
                        for (CardRange cardRange : cardRanges) {
                            if (cardRange.getIssuerName().equals(issuer.getName())) {
                                cardRange.setIssuer(issuer);
                                acqManager.insertCardRange(cardRange);
                            }
                        }
                    }
                }

                if (!emvAids.isEmpty() && FinancialApplication.getEmvDbHelper().deleteAID(FinancialApplication.getEmvDbHelper().findAllAID())) {
                    Log.d(TAG, "updateEdcIssuerFileConfig: AID file is found.");
                    EmvAid.load(emvAids);
                }
            } catch (Exception e) {
                Log.e(TAG, "", e);
            } finally {
                if (issuers != null && resultMap.get(Constants.DN_PARAM_FILENAME_ISSUER) != null)
                    new File(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_ISSUER)).delete();
                if (cardRanges != null && resultMap.get(Constants.DN_PARAM_FILENAME_CARD_RANGE) != null)
                    new File(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_CARD_RANGE)).delete();
                if (emvAids != null && resultMap.get(Constants.DN_PARAM_FILENAME_AID) != null)
                    new File(saveFilePath, resultMap.get(Constants.DN_PARAM_FILENAME_AID)).delete();
            }
        }
    }

    private void updateDynamicOfflineParam(HashMap<String, String> resultMap, Context context) {
        String SessionTimeout = resultMap.get(Constants.DN_PARAM_DOL_SESSION_TIMEOUT);
        String floorLimit_VSC = resultMap.get(Constants.DN_PARAM_DOL_FLOOR_LIMIT_VSC);
        String floorLimit_MCC = resultMap.get(Constants.DN_PARAM_DOL_FLOOR_LIMIT_MCC);
        String floorLimit_JCB = resultMap.get(Constants.DN_PARAM_DOL_FLOOR_LIMIT_JCB);

        //FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP, Utils.parseIntSafe(tempStr, 2));
        DynamicOffline.getInstance().setSessionTimeout(Utils.parseIntSafe(SessionTimeout, 30));
        DynamicOffline.getInstance().setVisaCardFloorlimit(Utils.parseLongSafe(floorLimit_VSC, 150000));
        DynamicOffline.getInstance().setMastercardFloorlimit(Utils.parseLongSafe(floorLimit_MCC, 150000));
        DynamicOffline.getInstance().setJcbCardFloorlimit(Utils.parseLongSafe(floorLimit_JCB, 150000));
        DynamicOffline.getInstance().resetDynamicOffline();
        DynamicOffline.getInstance().SaveDynamicOfflineParam();
    }

    private void updateERCMParam(HashMap<String, String> resultMap, Context context) {

        // ERCM Boolean Param
        Boolean temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ERCM_ENABLE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ERCM_PRN_AFTER_TXN));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_AFTER_TXN, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ERCM_PRN_PRE_SETTLE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_PRINT_PRE_SETTLE, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ERCM_NEXT_TRANS_UPLOAD_ENABLE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_NEXT_TRANS_UPLOAD, temp);

        temp = setBooleanParam(resultMap.get(Constants.DN_PARAM_ERCM_FORCE_SETTLE_PRINT_ALL_ERM_PENDING_ENABLE));
        FinancialApplication.getSysParam().set(SysParam.BooleanParam.VF_ERCM_ENABLE_FORCE_SETTLE_PRINT_ALL_TRANS, temp);


        // ERCM Number Param
        String tempStr = resultMap.get(Constants.DN_PARAM_ERCM_SLIP_NUM);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP, Utils.parseIntSafe(tempStr, 2));

        tempStr = resultMap.get(Constants.DN_PARAM_ERCM_SLIP_NUM_UNABLE_UPLOAD);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.VF_ERCM_NO_OF_SLIP_UNABLE_UPLOAD, Utils.parseIntSafe(tempStr, 2));

        // ERM maximum eReceipt to await for pending upload
        //    > in case it was exceed, EDC will lock and unable to do any transaction
        //    > cashier or user must settlement before do next operation
        tempStr = resultMap.get(Constants.DN_PARAM_ERCM_MAX_PENDING_ERECEIPT_UPLOAD_AWAIT);
        FinancialApplication.getSysParam().set(SysParam.NumberParam.MAX_LIMIT_ERM_ERECEPT_PENDING_UPLOAD, Utils.parseIntSafe(tempStr, 200));


        // ERCM String Param
        String Strtemp = resultMap.get(Constants.DN_PARAM_ERCM_BANK_CODE);
        if (Strtemp != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE, Strtemp);
            Log.d(EReceiptUtils.TAG, "\tBANK-CODE = '" + Strtemp + "'");
        } else {
            Log.d(EReceiptUtils.TAG, "\tBANK-CODE = null");
        }

        Strtemp = resultMap.get(Constants.DN_PARAM_ERCM_MERC_CODE);
        if (Strtemp != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_MERCHANT_CODE, Strtemp);
            Log.d(EReceiptUtils.TAG, "\tMERCHANT-CODE = '" + Strtemp + "'");
        } else {
            Log.d(EReceiptUtils.TAG, "\tMERCHANT-CODE = null");
        }

        Strtemp = resultMap.get(Constants.DN_PARAM_ERCM_STORE_CODE);
        if (Strtemp != null) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_STORE_CODE, Strtemp);
            Log.d(EReceiptUtils.TAG, "\tSTORE-CODE = '" + Strtemp + "'");
        } else {
            Log.d(EReceiptUtils.TAG, "\tSTORE-CODE = null");
        }


        // ERCM File PBK upload path
        BitmapImageConverterUtils.getInstance().saveFileERCMPBK(resultMap.get(Constants.DN_PARAM_ERCM_PBK), saveFilePath, context);
        try {
            ActionEReceipt actionEReceipt = new ActionEReceipt(null);
            actionEReceipt.readAndSetKeyVersion();
        } catch (Exception ex) {
            Log.e(EReceiptUtils.TAG, "Unable to reload PublicKeyData from File");
        }

        // SET ERCM SERIAL NUMBER
        FinancialApplication.getSysParam().set(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER, FinancialApplication.getDownloadManager().getSn());
    }

    private String saveToInternalStorage(Bitmap bitmapImage, Context context, String savedFileName) {
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/myapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, savedFileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    public void saveString(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FinancialApplication.getApp());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(name, value);
        editor.commit();
    }

    private void insertAcquirer() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<Acquirer> acquirers = Utils.readObjFromJSON("acquirer.json", Acquirer.class);
        List<Issuer> issuers = Utils.readObjFromJSON("issuer.json", Issuer.class);
        List<CardRange> cardRanges = Utils.readObjFromJSON("card_range.json", CardRange.class);

        for (Acquirer acquirer : acquirers)
            acqManager.insertAcquirer(acquirer);

        FinancialApplication.getSysParam().set(SysParam.StringParam.ACQ_NAME, acquirers.get(0).getName());
        acqManager.setCurAcq(acquirers.get(0));

        for (Issuer issuer : issuers) {
            acqManager.insertIssuer(issuer);
            for (Acquirer acquirer : acquirers)
                acqManager.bind(acquirer, issuer);
            for (CardRange cardRange : cardRanges) {
                if (cardRange.getIssuerName().equals(issuer.getName())) {
                    cardRange.setIssuer(issuer);
                    acqManager.insertCardRange(cardRange);
                }
            }
        }
    }

    private int initEMVParam() {
        Log.d(TAG, "Initial App: Parameter update");
        //ToastUtils.showMessage(com.pax.edc.R.string.emv_param_load);
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                // emv公钥下载
                Controller controller = FinancialApplication.getController();
                if (controller.get(Controller.NEED_DOWN_CAPK) == Controller.Constant.YES) {
                    EmvCapk.load(Utils.readObjFromJSON("capk.json", EmvCapk.class));
                    controller.set(Controller.NEED_DOWN_CAPK, Controller.Constant.NO);
                }
                // emv 参数下载
                if (controller.get(Controller.NEED_DOWN_AID) == Controller.Constant.YES) {
                    EmvAid.load(Utils.readObjFromJSON("aid.json", EmvAid.class));
                    controller.set(Controller.NEED_DOWN_AID, Controller.Constant.NO);
                }
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.showMessage(R.string.emv_init_succ);
                    }
                });
            }
        });
        return TransResult.SUCC;

    }

    private boolean setBooleanParam(String param) {
        return param != null && param.equalsIgnoreCase("Y");
    }

    private boolean setBooleanParam1(String param, Issuer issuer) {
        Log.d("hw3", "Issuer :" + issuer.getName() + " param: "
                + param);
        return param.equalsIgnoreCase("Y");
    }

    private boolean setBooleanParamDefaultTrue(String param) {
        return param == null || param.equalsIgnoreCase("Y");
    }

    private void updatePwd(HashMap<String, String> resultMap) {
        String tempStr = resultMap.get(Constants.DN_PARAM_PWD_ADMIN);
        if (tempStr != null && tempStr.length() == 8) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.SEC_SYS_PWD, EncUtils.sha1(tempStr));
        }
    }

    private void updateTleParam(HashMap<String, String> resultMap) {
        File f = new File(saveFilePath, resultMap.get(Constants.DN_PARAM_TLE_PARAM));

        if (f.exists()) {
            FinancialApplication.getSysParam().set(SysParam.StringParam.TLE_PARAMETER_FILE_PATH, f.getPath());
        } else {
            FinancialApplication.getSysParam().set(SysParam.StringParam.TLE_PARAMETER_FILE_PATH, "");
        }
    }

    private void updateCommunicationParameters(HashMap<String, String> resultMap) {
        SysParam sysParam = FinancialApplication.getSysParam();
        String temp = resultMap.get(Constants.DN_PARAM_APN_TRANS);
        if (temp != null) {
            sysParam.set(SysParam.StringParam.MOBILE_APN, temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_APN_SYSTEM);
        if (temp != null) {
            sysParam.set(SysParam.StringParam.MOBILE_APN_SYSTEM, temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_COMM_TYPE);
        if (temp != null) {
            if (SysParam.Constant.CommType.LAN.toString().equals(temp)
                || SysParam.Constant.CommType.MOBILE.toString().equals(temp)){
                sysParam.set(SysParam.StringParam.COMM_TYPE, temp);
            }
        }

        LanParam lanParam = new LanParam();

        temp = resultMap.get(Constants.DN_PARAM_LAN_DHCP);
        if (temp != null) {
            lanParam.setDhcp("DHCP".equals(temp));
        }

        temp = resultMap.get(Constants.DN_PARAM_LAN_LOCAL_IP);
        if (temp != null) {
            lanParam.setLocalIp(temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_LAN_SUBNET);
        if (temp != null) {
            lanParam.setSubnetMask(temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_LAN_GATEWAY);
        if (temp != null) {
            lanParam.setGateway(temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_LAN_DNS1);
        if (temp != null) {
            lanParam.setDns1(temp);
        }

        temp = resultMap.get(Constants.DN_PARAM_LAN_DNS2);
        if (temp != null) {
            lanParam.setDns2(temp);
        }

        Utils.updateAndSaveLanParam(lanParam);
    }

    public void updateCtlsTransLimit(String limit) {
        List<EmvAid> aidList = FinancialApplication.getEmvDbHelper().findAllAID();
        if (aidList != null) {
            for (EmvAid aid : aidList) {
                long txnLimit = Utils.parseLongSafe(limit, 0);
                //RdClssTxnLmt is a limit that allowed to make contactless transaction < RdClssTxnLmt
                //e.g. RdClssTxnLmt = 150001; means customer can make transaction <= 1500.00, above 1500.00 is not allowed
                txnLimit = txnLimit > 0 ? txnLimit + 1 : txnLimit;

                //For JCB, set txnLimit + 1 as handled by Clss_GetPreProcInterFlg_Entry
                //For MC, EMV tag (DF8124, DF8125) maximum length is 12 digits or 6 bytes
                //DF8124 Reader Contactless Txn Limit (No On-device CVM)
                //DF8125 Reader Contactless Txn Limit (On-device CVM)
                if (aid.getAid().contains(Constants.MASTER_AID_PREFIX)) {
                    txnLimit = Math.min(txnLimit - 1, 999999999999L);//Mastercard can make txn if amount = ctls txn limit
                }

                aid.setRdClssTxnLmt(txnLimit);

                if (aid.getAcqHostName().equals(Constants.ACQ_UP)) {
                    aid.setRdCVMLmt(150001);//Default 1500.01
                }
                FinancialApplication.getEmvDbHelper().updateAID(aid);
            }
        }
    }

    public void updateTipAdjustPercent(String tipAdjustMaxPercent) {
        List<Issuer> issuers = FinancialApplication.getAcqManager().findAllIssuers();
        for (Issuer issuer: issuers) {
            issuer.setAdjustPercent(Integer.valueOf(tipAdjustMaxPercent).floatValue());
            FinancialApplication.getAcqManager().updateIssuer(issuer);
        }
    }

    public void updateOfflineTipAdjustEnable(boolean value, boolean isOfflineFlag) {
        List<Issuer> issuers = FinancialApplication.getAcqManager().findAllIssuers();
        for (Issuer issuer: issuers) {
            String[] issuerBrands = new String[]{Constants.ISSUER_JCB, Constants.ISSUER_VISA, Constants.ISSUER_MASTER,
                    Constants.ISSUER_UP, Constants.ISSUER_BRAND_TBA};
            if (ArrayListUtils.INSTANCE.isFoundItem(issuerBrands, issuer.getIssuerBrand())) {
                if (isOfflineFlag) {
                    issuer.setEnableOffline(value);
                } else {
                    issuer.setEnableAdjust(value);
                }
            } else {
                issuer.setEnableOffline(false);
                issuer.setEnableAdjust(false);
            }
            FinancialApplication.getAcqManager().updateIssuer(issuer);
        }
    }

    public class MultiMerchantException extends Throwable {
        public MultiMerchantException (String message) {super(message);}
    }

    private void updateMerchantProfileParameters(HashMap<String, String> resultMap, String saveFilePath,  Context context) throws MultiMerchantException {
        SysParam sysParam = FinancialApplication.getSysParam();
        MerchantProfileManager.INSTANCE.clearTable();
        MerchantAcqProfileDb.INSTANCE.clearTable();


        //MerchantProfile
        String[] splitedMerchantEnable = resultMap.get(Constants.DN_PARAM_MULTI_MERC_ENABLE).split("\\|",-1);
        String[] splitedMerchantLabelName = resultMap.get(Constants.DN_PARAM_MULTI_MERC_LABEL_NAME).split("\\|",-1);
        String[] splitedMerchantPrintName = resultMap.get(Constants.DN_PARAM_MULTI_MERC_PRINT_NAME).split("\\|",-1);
        String[] splitedMerchantPrintAdd1 = resultMap.get(Constants.DN_PARAM_MULTI_MERC_PRINT_ADDR_LN1).split("\\|",-1);
        String[] splitedMerchantPrintAdd2 = resultMap.get(Constants.DN_PARAM_MULTI_MERC_PRINT_ADDR_LN2).split("\\|",-1);
        String[] splitedMerchantLogo = resultMap.get(Constants.DN_PARAM_MULTI_MERC_LOGO).split("\\|",-1);
        String[] splitedMerchantScreenLogo = resultMap.get(Constants.DN_PARAM_MULTI_MERC_SCREEN_LOGO).split("\\|",-1);

        if (MultiMerchantUtils.Companion.isDuplicate(Arrays.asList(splitedMerchantLabelName))) {
            throw new MultiMerchantException("Duplicated Merchant Alias Name");
        }

        // Default set enable for merchant 1
        if (splitedMerchantEnable!=null && splitedMerchantEnable.length>0 && splitedMerchantEnable[0] != null) {
            splitedMerchantEnable[0] = "Y";
        }

        // create default-merchant-profile
        MerchantProfile defaultMerchantProfile = new MerchantProfile();

        // 1. set merchant display label
        String labelName = "(Default Merchant)";
        if (splitedMerchantLabelName!=null && splitedMerchantLabelName.length>0 && splitedMerchantLabelName[0] != null) {
            labelName = (splitedMerchantLabelName[0]);
        }
        defaultMerchantProfile.setMerchantLabelName(labelName);

        // 2. set merchant printing name (appear in receipt printing)
        String printingName = "" ;
        if (splitedMerchantPrintName!=null && splitedMerchantPrintName.length>0 && splitedMerchantPrintName[0] != null) {
            printingName = splitedMerchantPrintName[0];
        }
        defaultMerchantProfile.setMerchantPrintName(printingName);

        // 3. set merchant address line1 (appear in receipt printing)
        String addressLn1 = "";
        if (splitedMerchantPrintAdd1!=null && splitedMerchantPrintAdd1.length>0 && splitedMerchantPrintAdd1[0] != null) {
            addressLn1 = splitedMerchantPrintAdd1[0];
        }
        defaultMerchantProfile.setMerchantPrintAddress1(addressLn1);

        // 4. set merchant address line1 (appear in receipt printing)
        String addressLn2 = "";
        if (splitedMerchantPrintAdd2!=null && splitedMerchantPrintAdd2.length>0 && splitedMerchantPrintAdd2[0] != null) {
            addressLn2 = splitedMerchantPrintAdd2[0];
        }
        defaultMerchantProfile.setMerchantPrintAddress2(addressLn2);

        // 5. set merchant default logo
        if (splitedMerchantLogo!=null && splitedMerchantLogo.length>0 && splitedMerchantLogo[0] !=null) {
            String paramFileName = splitedMerchantLogo[0];
            String paramFileExtension = ((paramFileName.length() >=4) ? paramFileName.substring(paramFileName.length()-4) : ".jpg");
            String logo = "merchant_0" ;
            String targSaveDir = saveFilePath + logo + "/";
            String saveFileName = "merchant_default_logo" + paramFileExtension;
            EReceiptUtils.verifyPath(targSaveDir);
            saveImageParam(splitedMerchantLogo[0], saveFilePath, context, targSaveDir, saveFileName);
            defaultMerchantProfile.setMerchantLogo(targSaveDir + saveFileName);

//            boolean saveImgResult = saveImageParam(splitedMerchantLogo[0], saveFilePath, context, Constants.SLIP_LOGO_NAME);
//            if (saveImgResult) { saveString(Constants.DN_PARAM_SLIP_LOGO_PATH, Constants.SLIP_LOGO_NAME);}
        }
//        saveImageParam(resultMap, saveFilePath, context, Constants.DN_PARAM_SLIP_LOGO, Constants.SLIP_LOGO_NAME, Constants.DN_PARAM_SLIP_LOGO_PATH);
//        defaultMerchantProfile.setMerchantLogo("kasikornbanklogo");

        // 6. set merchant Screen Logo
        if ((splitedMerchantScreenLogo != null) && ((splitedMerchantScreenLogo.length)>= 1) && (splitedMerchantScreenLogo[0].length()>0)) {
            String paramFileName = splitedMerchantScreenLogo[0];
            String paramFileExtension = ((paramFileName.length() >=4) ? paramFileName.substring(paramFileName.length()-4) : ".jpg");
            String logo = "merchant_0" ;
            String targSaveDir = saveFilePath + logo + "/";
            String saveFileName = "screen_logo" + paramFileExtension;
            EReceiptUtils.verifyPath(targSaveDir);
            saveImageParam(splitedMerchantScreenLogo[0], saveFilePath, context, targSaveDir, saveFileName);
            defaultMerchantProfile.setMerchantScreenLogoPath(targSaveDir + saveFileName);
        }

        // 7. insert merchant profile to database
        MerchantProfileDb.INSTANCE.insertData(defaultMerchantProfile);

        AcqManager acqManager = FinancialApplication.getAcqManager();
        List<Acquirer> acquirers = acqManager.findAllAcquirers();
        List<String> multiMerchantAcq = MerchantProfileManager.INSTANCE.getSupportAcq();
        for (Acquirer acquirer : acquirers) {
            Acquirer tmpAcquirer = null;
            if (multiMerchantAcq.contains(acquirer.getName())) {
                tmpAcquirer = acquirer;
                String mmAcqName = tmpAcquirer.getName().toLowerCase(Locale.ROOT).replace(" ","");
                String mmAcqNameEnableKey = "merchant.acq." + mmAcqName + ".enable";
                String mmAcqNameTidKey = "merchant.acq." + mmAcqName + ".tid";
                String mmAcqNameMidKey = "merchant.acq." + mmAcqName + ".mid";
                String mmAcqNameBatchKey = "merchant.acq." + mmAcqName + ".batch";

                String[] splitedAcqMercEnable = resultMap.get(mmAcqNameEnableKey).split("\\|",-1);
                String[] splitedAcqMercTid    = resultMap.get(mmAcqNameTidKey).split("\\|",-1);
                String[] splitedAcqMercMid    = resultMap.get(mmAcqNameMidKey).split("\\|",-1);
                String[] splitedAcqMercBatch  = resultMap.get(mmAcqNameBatchKey).split("\\|",-1);

                if (splitedAcqMercEnable!=null && splitedAcqMercTid!=null && splitedAcqMercMid!=null && splitedAcqMercBatch!=null
                    && splitedAcqMercEnable.length>0 && splitedAcqMercTid.length>0 && splitedAcqMercMid.length>0 && splitedAcqMercBatch.length>0) {
                    tmpAcquirer.setEnable(splitedAcqMercEnable[0].toUpperCase().equals("Y"));
                    tmpAcquirer.setTerminalId(splitedAcqMercTid[0]);
                    tmpAcquirer.setMerchantId(splitedAcqMercMid[0]);
                    tmpAcquirer.setCurrBatchNo(Integer.parseInt(splitedAcqMercBatch[0]));
                }

                if (tmpAcquirer.isEnable()) {
                    MerchantAcqProfile defaultMerchantAcqProfile = new MerchantAcqProfile();
                    defaultMerchantAcqProfile.setMerchantName(defaultMerchantProfile.getMerchantLabelName());
                    defaultMerchantAcqProfile.setAcqHostName(tmpAcquirer.getName());
                    defaultMerchantAcqProfile.setMerchantId(tmpAcquirer.getMerchantId());
                    defaultMerchantAcqProfile.setTerminalId(tmpAcquirer.getTerminalId());
                    defaultMerchantAcqProfile.setCurrBatchNo(tmpAcquirer.getCurrBatchNo());
                    MerchantAcqProfileDb.INSTANCE.insertData(defaultMerchantAcqProfile);
                }
            }
        }

        List<String> supportAcq = MerchantProfileManager.INSTANCE.getSupportAcq();

        if (splitedMerchantPrintName.length > 1) {

            for (int i = 1; i < splitedMerchantEnable.length; i++) {
                Log.d("MULTIMERC","MERCHANT NAME = " + splitedMerchantPrintName[i]);
                Log.d("MULTIMERC","       ENABLE = " + splitedMerchantEnable[i]);
                boolean isEnable = setBooleanParam(splitedMerchantEnable[i]);
                if(isEnable){
                    //
                    // Update MerchantProfile
                    //
                    MerchantProfile merchantProfile = new MerchantProfile();
                    merchantProfile.setMerchantLabelName(splitedMerchantLabelName[i]);
                    merchantProfile.setMerchantPrintName(splitedMerchantPrintName[i]);
                    merchantProfile.setMerchantPrintAddress1(splitedMerchantPrintAdd1[i]);
                    merchantProfile.setMerchantPrintAddress2(splitedMerchantPrintAdd2[i]);
                    String logo = "merchant_" + i ;
                    String targSaveDir = saveFilePath + logo + "/";
                    EReceiptUtils.verifyPath(targSaveDir);

                    // Merchant Header Logo
                    if ((splitedMerchantLogo != null) && ((splitedMerchantLogo.length-1)>= i) && (splitedMerchantLogo[i].length()>0)) {
                        String paramFileName = splitedMerchantLogo[i];

                        if (paramFileName != null && paramFileName.length() > 0) {
                            String paramFileExtension = ((paramFileName.length() >=4) ? paramFileName.substring(paramFileName.length()-4) : ".jpg");
                            String saveFileName = "merchant_default_logo" + paramFileExtension;
                            //saveImageParam(splitedMerchantLogo[i], saveFilePath, context, logo);
                            saveImageParam(splitedMerchantLogo[i], saveFilePath, context, targSaveDir, saveFileName);
                            merchantProfile.setMerchantLogo(targSaveDir + saveFileName);
                        }
                    }

                    // Merchant Screen Logo
                    if ((splitedMerchantScreenLogo != null) && ((splitedMerchantScreenLogo.length-1)>= i) && (splitedMerchantScreenLogo[i].length()>0)) {
                        String paramFileName = splitedMerchantScreenLogo[i];

                        if (paramFileName != null && paramFileName.length() > 0) {
                            String paramFileExtension = ((paramFileName.length() >=4) ? paramFileName.substring(paramFileName.length()-4) : ".jpg");
                            String saveFileName = "screen_logo" + paramFileExtension;

                            EReceiptUtils.verifyPath(targSaveDir);
                            saveImageParam(splitedMerchantScreenLogo[i], saveFilePath, context, targSaveDir, saveFileName);
                            merchantProfile.setMerchantScreenLogoPath(targSaveDir + saveFileName);
                        }
                    }

                    MerchantProfileDb.INSTANCE.insertData(merchantProfile);
                    Log.d("MULTIMERC","       MERCHANT PROFILE ADDED" );

                    //
                    // Update acqProfileTable
                    //
                    for (String acq : supportAcq){
                        String acqEnableKey = "merchant.acq." + acq.toLowerCase(Locale.ROOT) + ".enable";
                        String[] acqEnable = null;
                        try {
                            acqEnable = resultMap.get(acqEnableKey).split("\\|", -1);
                        } catch (Exception e){
                            acqEnable = null;
                            e.printStackTrace();
                            continue;
                        }
                        if (acqEnable.length > 0) {
                            boolean isAcqEnable = setBooleanParam(acqEnable[i]);
                            Log.d("MULTIMERC","\t\t\t\t ACQUIRER -- [" + acq + "] \t\t ENABLE = " + isAcqEnable);
                            if(isAcqEnable){
                                String acquirerName = acq.toLowerCase(Locale.ROOT).replace(" ","");
                                String acqTidKey = "merchant.acq." + acquirerName + ".tid";
                                String acqMidKey = "merchant.acq." + acquirerName + ".mid";
                                String acqBatchKey = "merchant.acq." + acquirerName + ".batch";
                                String[] acqTid = resultMap.get(acqTidKey).split("\\|",-1);
                                String[] acqMid = resultMap.get(acqMidKey).split("\\|",-1);
                                String[] acqBatch = resultMap.get(acqBatchKey).split("\\|",-1);
                                MerchantAcqProfile merchantAcqProfile = new MerchantAcqProfile();
                                merchantAcqProfile.setMerchantName(splitedMerchantLabelName[i]);
                                merchantAcqProfile.setAcqHostName(acq);
                                merchantAcqProfile.setMerchantId(acqMid[i]);
                                merchantAcqProfile.setTerminalId(acqTid[i]);
                                merchantAcqProfile.setCurrBatchNo(Integer.parseInt(acqBatch[i]));
                                MerchantAcqProfileDb.INSTANCE.insertData(merchantAcqProfile);
                            }
                        }
                    }
                }
            }
            MerchantProfileManager.INSTANCE.applyProfileAndSave(defaultMerchantProfile.getMerchantLabelName());

            // Set default master profile
            FinancialApplication.getSysParam().set(SysParam.StringParam.EDC_CURRENT_MERCHANT, MultiMerchantUtils.Companion.getMasterProfileName());
        }


    }

    private boolean saveImageParam(String fileName, String saveFilePath, Context context, String savedFileName) {
        try {
            if (!fileName.isEmpty()) {
                File f = new File(saveFilePath, fileName);
                if (f.exists()) {
                   
                    Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                    saveToInternalStorage(b, context, savedFileName);

                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void saveImageParam(String fileName, String paramImageFilePath, Context context, String targetSavedFilePath,String targetSavedFileName) {
        try {

            if (!fileName.isEmpty()) {
                File f = new File(paramImageFilePath, fileName);
                if (f.exists()) {

                    Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                    saveToInternalStorageSpecificPath(b, targetSavedFilePath, targetSavedFileName);
                }
            }
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }
    }

    private String saveToInternalStorageSpecificPath(Bitmap bitmapImage, String savedFilePath, String savedFileName) {
        String absolutePath = null;
        File mypath = new File(savedFilePath, savedFileName);

        String exst = savedFileName.substring(savedFileName.indexOf(".")).toUpperCase();
        Bitmap.CompressFormat compressFormat = null;
        if (exst.contains(".PNG")) {
            compressFormat = Bitmap.CompressFormat.PNG;
        } else {
            compressFormat = Bitmap.CompressFormat.JPEG;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(compressFormat, 100, fos);
            absolutePath = savedFilePath + savedFileName;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return absolutePath;
    }
}
