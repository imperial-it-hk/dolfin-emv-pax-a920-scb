/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-20
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.base;


import androidx.annotation.NonNull;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.pay.trans.transmit.TransactionIPAddress;
import com.pax.pay.trans.transmit.TransactionIPAddressCollection;
import com.pax.settings.SysParam;


import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * acquirer table
 */
@DatabaseTable(tableName = "acquirer")
public class Acquirer implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "acquirer_id";
    public static final String NAME_FIELD_NAME = "acquirer_name";
    public static final String SSL_TYPE_FIELD_NAME = "ssl_type";
    public static final String ENABLE_KEYIN = "key_in";
    public static final String ENABLE_QR = "qr";
    public static final String ENABLE_TLE = "tle";
    public static final String ENABLE_UPI = "upi";
    public static final String NII = "nii";
    public static final String TEST_MODE = "test";
    public static final String BILLER_ID_PROMPTPAY = "biller_id_promptpay";
    public static final String ENABLE = "enable";
    public static final String CURRENT_HOST_ID = "CurrentHostId";
    public static final String ENABLE_UPLOAD_ERM = "enableUploadERM";
    public static final String FORCE_SETTLE_TIME = "force_settle_time";
    public static final String TMK_FIELD_NAME = "TMK";
    public static final String TWK_FIELD_NAME = "TWK";
    public static final String UP_TMK_FIELD_NAME = "UP_TMK";
    public static final String UP_TWK_FIELD_NAME = "UP_TWK";
    public static final String KEY_ID_FIELD_NAME = "key_id";
    public static final String TLE_BANK_NAME = "tleBank";

    /**
     * id
     */
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;

    /**
     * name
     */
    @DatabaseField(unique = true, columnName = NAME_FIELD_NAME)
    private String name;

    @DatabaseField(canBeNull = false)
    private String nii;

    @DatabaseField(canBeNull = false)
    private String terminalId;

    @DatabaseField(canBeNull = false)
    private String merchantId;

    @DatabaseField
    private int currBatchNo;

    @DatabaseField
    private String ip;

    @DatabaseField
    private int port;

    @DatabaseField
    private String ipBak1;

    @DatabaseField
    private int portBak1;

    @DatabaseField
    private String ipBak2;

    @DatabaseField
    private int portBak2;

    @DatabaseField
    private String ipBak3;

    @DatabaseField
    private int portBak3;

    @DatabaseField(columnName = CURRENT_HOST_ID)
    private int CurrentHostId = -1;

    @DatabaseField
    private int tcpTimeOut;

    @DatabaseField
    private int wirelessTimeOut;

    @DatabaseField
    private boolean isDisableTrickFeed = false;

    @DatabaseField(columnName = SSL_TYPE_FIELD_NAME)
    private SysParam.Constant.CommSslType sslType = SysParam.Constant.CommSslType.NO_SSL;

    @DatabaseField(columnName = ENABLE_KEYIN)
    private boolean isEnableKeyIn = false;

    @DatabaseField(columnName = ENABLE_QR)
    private boolean isEnableQR = false;

    @DatabaseField(columnName = ENABLE_TLE)
    private boolean isEnableTle = false;

    @DatabaseField(columnName = ENABLE_UPI)
    private boolean isEnableUpi = false;

    @DatabaseField(columnName = TEST_MODE)
    private boolean isTestMode = false;

    @DatabaseField(columnName = TMK_FIELD_NAME)
    private String TMK;

    @DatabaseField(columnName = TWK_FIELD_NAME)
    private String TWK;

    @DatabaseField(columnName = UP_TMK_FIELD_NAME)
    private String UP_TMK;

    @DatabaseField(columnName = UP_TWK_FIELD_NAME)
    private String UP_TWK;

    @DatabaseField(columnName = BILLER_ID_PROMPTPAY)
    private String billerIdPromptPay;

    @DatabaseField
    private String billerServiceCode;

    @DatabaseField(canBeNull = false)
    private int recvTimeout = 30;

    @DatabaseField
    private int promptQrTimeout;

    @DatabaseField
    private int promptRetryTimeout;

    @DatabaseField
    private boolean isEmvTcAdvice = false;

    @DatabaseField
    private boolean isEnableSmallAmt = false;

    @DatabaseField(columnName = ENABLE_UPLOAD_ERM)
    private boolean enableUploadERM = false;

    @DatabaseField(columnName = ENABLE)
    private boolean isEnable = true;

    @DatabaseField(columnName = FORCE_SETTLE_TIME)
    private String force_settle_time = "23:00";

    @DatabaseField
    private String instalmentTerms;

    @DatabaseField
    private String instalmentMinAmt;

    @DatabaseField (columnName = KEY_ID_FIELD_NAME)
    private int keyId;

    @DatabaseField
    private String storeId;

    @DatabaseField
    private String storeName;

    @DatabaseField (canBeNull = true)
    private String tleBank;

    @DatabaseField
    private String apiDomainName;

    @DatabaseField
    private int apiPortNumber;

    @DatabaseField
    private int apiConnectTimeout;

    @DatabaseField
    private int apiReadTimeout;

    @DatabaseField
    private int apiScreenTimeout;

    @DatabaseField
    private boolean apiHostNameCheck;

    @DatabaseField
    private boolean apiCertCheck;

    @DatabaseField
    private boolean ekycCardEncryption;

    @DatabaseField
    private String settleTime;

    @DatabaseField
    private String latestSettledDateTime;

    @DatabaseField
    private boolean enableControlLimit = false;

    @DatabaseField
    private boolean enablePhoneNumInput = false;

    @DatabaseField
    private boolean signatureRequired;

    @DatabaseField
    private boolean isEnableCScanBMode;
    @DatabaseField
    private int cScanBDisplayQrTimeout;
    @DatabaseField
    private int cScanBRetryTimes;
    @DatabaseField
    private int cScanBDelayRetry;

    public Acquirer() {
    }

    public Acquirer(String name) {
        this.setName(name);
    }

    public Acquirer(int id, String acquirerName) {
        this.setId(id);
        this.setName(acquirerName);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNii() {
        return nii;
    }

    public void setNii(String nii) {
        this.nii = nii;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public int getCurrBatchNo() {
        return currBatchNo;
    }

    public void setCurrBatchNo(int currBatchNo) {
        this.currBatchNo = currBatchNo;
    }

    public void setEnableUploadERM(boolean enableUploadERM) {this.enableUploadERM=enableUploadERM;}
    public boolean getEnableUploadERM() {return this.enableUploadERM;}

    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public int getPort() { return port; }
    public void setPort(int port) {
        this.port = port;
    }

    public String getIpBak1() {
        return ipBak1;
    }
    public void setIpBak1(String ipBak1) {
        this.ipBak1 = ipBak1;
    }
    public int getPortBak1() {
        return portBak1;
    }
    public void setPortBak1(int portBak1) { this.portBak1 = portBak1; }

    public String getIpBak2() {
        return ipBak2;
    }
    public void setIpBak2(String ipBak2) {
        this.ipBak2 = ipBak2;
    }
    public int getPortBak2() {
        return portBak2;
    }
    public void setPortBak2(int portBak2) {
        this.portBak2 = portBak2;
    }

    public String getIpBak3() {
        return ipBak3;
    }
    public void setIpBak3(String ipBak3) {
        this.ipBak3 = ipBak3;
    }
    public int getPortBak3() {
        return portBak3;
    }
    public void setPortBak3(int portBak3) {
        this.portBak3 = portBak3;
    }

    public String getTleBankName() {return this.tleBank;}
    public void setTleBank(String tleBank) { this.tleBank = tleBank;}

    private int maxBackUpIpAddress = 3;
    public void setCurrentBackupAddressId(int currHostId) {
        boolean allowUseCurrentBackupId = false;
        for (int index=0 ;  index <=3 ; index++) {
            if ( ! allowUseCurrentBackupId) {
                if (CanUseThisIpAddress(currHostId)) { allowUseCurrentBackupId =true; break;}
                else {
                    if (currHostId==3) { currHostId=0; } else { currHostId+=1; }
                    //Log.d("BACKUP-IP", ((currHostId==0) ? "1st" : (currHostId==1) ? "2nd" : (currHostId==2) ? "3nd" : "4th")  +" IPADDRESS-PORT contain invalid format.");
                    //Log.d("BACKUP-IP", "set shift-up CurrentHostId = " + currHostId );
                }
            }
        }

        if (allowUseCurrentBackupId) {
            this.CurrentHostId = (currHostId > maxBackUpIpAddress) ? 0 : currHostId;
            //Log.d("BACKUP-IP",  "Request set CurrentHostID = " + currHostId + "\tAcquirer.CurrentHostID = " + this.CurrentHostId + "\tLimit Possible MaximumBackupIP = " + maxBackUpIpAddress +" IPs");
        }
    }

    public void setCurrentIpAddressID(int CurrentIpAddressID) {
        this.CurrentHostId = CurrentIpAddressID;
    }
    public int getCurrentBackupAddressIdWithIncreaseValue() {
        this.CurrentHostId = (this.CurrentHostId == 3)  ? 0  :  this.CurrentHostId+1;
        return this.CurrentHostId;
    }
    public int getCurrentBackupAddressId() {
        //Log.d("BACKUP-IP",  (this.CurrentHostId==-1) ? "Current host is null, require random Ip-Port" : "return CurrentHostId = " + this.CurrentHostId);
        int randomNum = -1 ;
        if (this.CurrentHostId==-1) {
            randomNum = new Random().nextInt(3);
            for (int index=0; index <=3 ; index++) {
                if (CanUseThisIpAddress(randomNum)) {
                    this.CurrentHostId=randomNum;
                    break;
                } else {
                    randomNum = (randomNum==3) ? 0 : randomNum+1;
                }
            }
        }


        return this.CurrentHostId;
    }

    public TransactionIPAddressCollection getAvailableIPAddressCollection() {
        TransactionIPAddressCollection collection = new TransactionIPAddressCollection(new ArrayList<TransactionIPAddress>(), -1);

        List<TransactionIPAddress> allTransactionIPAddresses = getAllIPAddresses(this);
        List<TransactionIPAddress> filteredTransactionIPAddresses = getAvailableIPAddresses(allTransactionIPAddresses);
        int maxItemCountToRandom = filteredTransactionIPAddresses.size();

        if (maxItemCountToRandom > 0 ){
            int randomItemIndex = new Random().nextInt(maxItemCountToRandom);
            collection = new TransactionIPAddressCollection(filteredTransactionIPAddresses, randomItemIndex);
        }

        return collection;
    }

    private boolean CanUseThisIpAddress (int currHostId) {
        boolean allowUseCurrentBackupId=false;
        if      (currHostId == 0 && (ip !=null     && !ip.isEmpty())     || port !=0 )     { allowUseCurrentBackupId = (!ip.equals("0.0.0.0")); }
        else if (currHostId == 1 && (ipBak1 !=null && !ipBak1.isEmpty()) || portBak1 !=0 ) { allowUseCurrentBackupId = (!ipBak1.equals("0.0.0.0")); }
        else if (currHostId == 2 && (ipBak2 !=null && !ipBak2.isEmpty()) || portBak2 !=0 ) { allowUseCurrentBackupId = (!ipBak2.equals("0.0.0.0")); }
        else if (currHostId == 3 && (ipBak3 !=null && !ipBak3.isEmpty()) || portBak3 !=0 ) { allowUseCurrentBackupId = (!ipBak3.equals("0.0.0.0")); }
        return allowUseCurrentBackupId;
    }

    private List<TransactionIPAddress> getAllIPAddresses(Acquirer acquirer) {
        List<TransactionIPAddress> list = new ArrayList<>();

        try {
            list.add(new TransactionIPAddress(0, acquirer.ip, acquirer.port));
            list.add(new TransactionIPAddress(1, acquirer.ipBak1, acquirer.portBak1));
            list.add(new TransactionIPAddress(2, acquirer.ipBak2, acquirer.portBak2));
            list.add(new TransactionIPAddress(3, acquirer.ipBak3, acquirer.portBak3));
        }
        catch (Exception ex) {
            list = new ArrayList<>();
        }

        return list;
    }

    private List<TransactionIPAddress> getAvailableIPAddresses(List<TransactionIPAddress> allIPAddresses) {
        List<TransactionIPAddress> list = new ArrayList<>();
        try {
            for (TransactionIPAddress transactionIPAddress: allIPAddresses) {
                if (StringUtils.isNotBlank(transactionIPAddress.getIpAddress()) && (transactionIPAddress.getIpAddress().compareToIgnoreCase("0.0.0.0") != 0) && transactionIPAddress.getPort() != 0) {
                    list.add(transactionIPAddress);
                }
            }
        }
        catch (Exception ex) {
            list = new ArrayList<>();
        }

        return list;
    }

    public int getTcpTimeOut() {
        return tcpTimeOut;
    }

    public void setTcpTimeOut(int tcpTimeOut) {
        this.tcpTimeOut = tcpTimeOut;
    }

    public int getWirelessTimeOut() {
        return wirelessTimeOut;
    }

    public void setWirelessTimeOut(int wirelessTimeOut) {
        this.wirelessTimeOut = wirelessTimeOut;
    }

    public boolean isDisableTrickFeed() {
        return isDisableTrickFeed;
    }

    public void setDisableTrickFeed(boolean disableTrickFeed) {
        isDisableTrickFeed = disableTrickFeed;
    }

    public String getForceSettleTime () { return force_settle_time; }
    public void setForceSettleTime (String ex_forceSettleTime) {force_settle_time=ex_forceSettleTime;}

    public SysParam.Constant.CommSslType getSslType() {
        return sslType;
    }

    public void setSslType(SysParam.Constant.CommSslType sslType) {
        this.sslType = sslType;
    }

    public boolean isEnableKeyIn() {
        return isEnableKeyIn;
    }

    public void setEnableKeyIn(boolean enableKeyIn) {
        isEnableKeyIn = enableKeyIn;
    }

    public boolean isEnableQR() {
        return isEnableQR;
    }

    public void setEnableQR(boolean enableQR) {
        isEnableQR = enableQR;
    }

    public boolean isEnableTle() {
        return isEnableTle;
    }

    public void setEnableTle(boolean enableTle) {
        isEnableTle = enableTle;
    }

    public boolean isEnableUpi() {
        return isEnableUpi;
    }

    public boolean isEnableUploadERM() {
        return enableUploadERM;
    }

    public void setEnableUpi(boolean enableUpi) {
        isEnableUpi = enableUpi;
    }

    public boolean isTestMode() {
        return isTestMode;
    }

    public void setTestMode(boolean testMode) {
        isTestMode = testMode;
    }

    public String getTMK() { return TMK; }

    public void setTMK(String TMK) { this.TMK = TMK; }

    public String getTWK() {
        return TWK;
    }

    public void setTWK(String TWK) {
        this.TWK = TWK;
    }

    public String getUP_TMK() { return UP_TMK; }

    public void setUP_TMK(String UP_TMK) { this.UP_TMK = UP_TMK; }

    public String getUP_TWK() {
        return UP_TWK;
    }

    public void setUP_TWK(String UP_TWK) {
        this.UP_TWK = UP_TWK;
    }

    public String getBillerIdPromptPay() { return billerIdPromptPay; }

    public void setBillerIdPromptPay(String billerIdPromptPay) { this.billerIdPromptPay = billerIdPromptPay; }

    public String getBillerServiceCode() {
        return billerServiceCode;
    }

    public void setBillerServiceCode(String billerServiceCode) {
        this.billerServiceCode = billerServiceCode;
    }

    public int getRecvTimeout() {
        return recvTimeout;
    }

    public void setRecvTimeout(int recvTimeout) {
        this.recvTimeout = recvTimeout;
    }

    public int getPromptQrTimeout() {
        return promptQrTimeout;
    }

    public void setPromptQrTimeout(int promptQrTimeout) {
        this.promptQrTimeout = promptQrTimeout;
    }

    public int getPromptRetryTimeout() {
        return promptRetryTimeout;
    }

    public void setPromptRetryTimeout(int promptRetryTimeout) {
        this.promptRetryTimeout = promptRetryTimeout;
    }

    public boolean isEmvTcAdvice() {
        return isEmvTcAdvice;
    }

    public void setEmvTcAdvice(boolean emvTcAdvice) {
        isEmvTcAdvice = emvTcAdvice;
    }

    public boolean isEnableSmallAmt() {
        return isEnableSmallAmt;
    }

    public void setEnableSmallAmt(boolean enableSmallAmt) {
        isEnableSmallAmt = enableSmallAmt;
    }

    public boolean isEnable() {return isEnable;}

    public void setEnable(boolean enable) {isEnable = enable;}

    public String getInstalmentTerms() { return instalmentTerms; }

    public void setInstalmentTerms(String instalmentTerms) { this.instalmentTerms = instalmentTerms; }

    public String getInstalmentMinAmt() { return instalmentMinAmt; }

    public void setInstalmentMinAmt(String instalmentMinAmt) { this.instalmentMinAmt = instalmentMinAmt; }

    public String getStoreId() { return storeId; }

    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreName() { return storeName; }

    public void setStoreId(String storeId) { this.storeId = storeId; }

    public void update(@NonNull Acquirer acquirer) {
        nii = acquirer.getNii();
        merchantId = acquirer.getMerchantId();
        terminalId = acquirer.getTerminalId();
        currBatchNo = acquirer.getCurrBatchNo();
        sslType = acquirer.getSslType();
        ip = acquirer.getIp();
        port = acquirer.getPort();
        tcpTimeOut = acquirer.getTcpTimeOut();
        wirelessTimeOut = acquirer.getWirelessTimeOut();
        isDisableTrickFeed = acquirer.isDisableTrickFeed();
        isEnableKeyIn = acquirer.isEnableKeyIn();
        isEnableQR = acquirer.isEnableQR();
        isEnableTle = acquirer.isEnableTle();
        isEnableUpi = acquirer.isEnableUpi();
        isTestMode  = acquirer.isTestMode();
        TMK = acquirer.getTMK();
        TWK = acquirer.getTWK();
        UP_TMK = acquirer.getUP_TMK();
        UP_TWK = acquirer.getUP_TWK();
        isEmvTcAdvice = acquirer.isEmvTcAdvice();
        isEnableSmallAmt = acquirer.isEnableSmallAmt();
        isEnable = acquirer.isEnable();
        enableUploadERM = acquirer.getEnableUploadERM();
        storeId = acquirer.getStoreId();
        storeName = acquirer.getStoreName();
        tleBank = acquirer.getTleBankName();

        //no required data
        String ipBak =null;
        ipBak= acquirer.getIpBak1();
        if (ipBak != null && !ipBak.isEmpty()) {
            ipBak1 = ipBak;
        }
        ipBak = acquirer.getIpBak2();
        if (ipBak != null && !ipBak.isEmpty()) {
            ipBak2 = ipBak;
        }
        ipBak = acquirer.getIpBak3();
        if (ipBak != null && !ipBak.isEmpty()) {
            ipBak3 = ipBak;
        }
        int portBak = 0;
        portBak = acquirer.getPortBak1();
        if (portBak != 0) {
            portBak1 = portBak;
        }
        portBak = acquirer.getPortBak2();
        if (portBak != 0) {
            portBak2 = portBak;
        }
        portBak = acquirer.getPortBak3();
        if (portBak != 0) {
            portBak3 = portBak;
        }

        String promptPay = acquirer.getBillerIdPromptPay();
        if(promptPay != null && !promptPay.isEmpty()){
            billerIdPromptPay = promptPay;
        }
        promptPay = acquirer.getBillerServiceCode();
        if(promptPay != null && !promptPay.isEmpty()){
            billerServiceCode = promptPay;
        }
        int timeout = acquirer.getRecvTimeout();
        if(timeout != 0){
            recvTimeout = timeout;
        }
        timeout = acquirer.getPromptQrTimeout();
        if(timeout != 0){
            promptQrTimeout = timeout;
        }
        timeout = acquirer.getPromptRetryTimeout();
        if(timeout != 0){
            promptRetryTimeout = timeout;
        }

        keyId = acquirer.getKeyId();
        String instalment = acquirer.getInstalmentTerms() ;
        if(instalment != null && !instalment.isEmpty()){
            instalmentTerms = instalment;
        }
        instalment = acquirer.getInstalmentMinAmt() ;
        if(instalment != null && !instalment.isEmpty()){
            instalmentMinAmt = instalment;
        }
    }
    public int getKeyId() { return keyId; }

    public void setKeyId(int keyId) { this.keyId = keyId; }



    public String getApiDomainName() { return apiDomainName; }
    public void setApiDomainName(String apiDomainName) { this.apiDomainName = apiDomainName; }

    public int getApiPortNumber() { return apiPortNumber;}
    public void setApiPortNumber(int apiPortNumber) { this.apiPortNumber = apiPortNumber; }

    public boolean getApiHostNameCheck() {return apiHostNameCheck;}
    public void setApiHostNameCheck(boolean apiHostNameCheck) { this.apiHostNameCheck = apiHostNameCheck; }

    public boolean getApiCertificationCheck() {return apiCertCheck;}
    public void setApiCertificationCheck(boolean apiCertCheck) { this.apiCertCheck = apiCertCheck; }

    public boolean getEKycDataEncryption() {return ekycCardEncryption;}
    public void setEKycDataEncryption(boolean ekycCardEncryption) { this.ekycCardEncryption = ekycCardEncryption; }

    public int getApiConnectTimeout() {return apiConnectTimeout;}
    public void setApiConnectTimeout(int apiConnectTimeout) { this.apiConnectTimeout = apiConnectTimeout; }

    public int getApiReadTimeout() {return apiReadTimeout;}
    public void setApiReadTimeout(int apiReadTimeout) { this.apiReadTimeout = apiReadTimeout; }

    public int getApiScreenTimeout() {return apiScreenTimeout;}
    public void setApiScreenTimeout(int apiScreenTimeout) { this.apiScreenTimeout = apiScreenTimeout; }

    public String getSettleTime() {return settleTime;}
    public void setSettleTime(String settleTime) {this.settleTime = settleTime;}

    public String getLatestSettledDateTime() {return latestSettledDateTime;}
    public void setLatestSettledDateTime(String latestSettledDateTime) {this.latestSettledDateTime = latestSettledDateTime;}

    public boolean getEnableControlLimit() {return enableControlLimit;}
    public void setEnableControlLimit(boolean enableControlLimit) {this.enableControlLimit = enableControlLimit;}

    public boolean getEnablePhoneNumberInput() {return enablePhoneNumInput;}
    public void setEnablePhoneNumberInput(boolean enablePhoneNumInput) {this.enablePhoneNumInput = enablePhoneNumInput;}

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    public boolean isEnableCScanBMode() {
        return isEnableCScanBMode;
    }

    public void setEnableCScanBMode(boolean isEnableCScanBMode) {
        this.isEnableCScanBMode = isEnableCScanBMode;
    }

    public int getCScanBDisplayQrTimeout() {
        return cScanBDisplayQrTimeout;
    }

    public void setCScanBDisplayQrTimeout(int cScanBDisplayQrTimeout) {
        this.cScanBDisplayQrTimeout = cScanBDisplayQrTimeout;
    }

    public int getCScanBRetryTimes() {
        return cScanBRetryTimes;
    }

    public void setCScanBRetryTimes(int cScanBRetryTimes) {
        this.cScanBRetryTimes = cScanBRetryTimes;
    }

    public int getCScanBDelayRetry() {
        return cScanBDelayRetry;
    }

    public void setCScanBDelayRetry(int cScanBDelayRetry) {
        this.cScanBDelayRetry = cScanBDelayRetry;
    }
}
