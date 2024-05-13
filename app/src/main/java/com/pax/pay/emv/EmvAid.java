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
package com.pax.pay.emv;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.pax.eemv.entity.AidParam;
import com.pax.glwrapper.convert.IConvert;
import com.pax.glwrapper.convert.IConvert.EPaddingPosition;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.AcqManager;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import th.co.bkkps.utils.Log;

import static com.pax.pay.app.FinancialApplication.TAG;

@DatabaseTable(tableName = "aid")
public class EmvAid implements Serializable {
    @IntDef({PART_MATCH, FULL_MATCH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelType {
    }

    public static final int PART_MATCH = 0;
    public static final int FULL_MATCH = 1;

    public static final String ID_FIELD_NAME = "id";
    public static final String AID_FIELD_NAME = "aid";
    public static final String ISSUER_FIELD_NAME = "issuerName";


    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    @JSONField
    private int id;
    /**
     * name
     */
    @DatabaseField(canBeNull = false)
    private String appName;
    /**
     * aid
     */
    @DatabaseField(unique = true, canBeNull = false, columnName = AID_FIELD_NAME)
    private String aid;
    /**
     * PART_MATCH/FULL_MATCH
     */
    @DatabaseField(canBeNull = false)
    @SelType
    private int selFlag;
    /**
     * priority
     */
    @DatabaseField(canBeNull = false)
    private int priority;
    /**
     * if enable online PIN
     */
    @DatabaseField(canBeNull = false)
    private boolean onlinePin;
    /**
     * tag DF21
     */
    @DatabaseField(canBeNull = false)
    private long rdCVMLmt;
    /**
     * tag DF20
     */
    @DatabaseField(canBeNull = false)
    private long rdClssTxnLmt;
    /**
     * tag DF19
     */
    @DatabaseField(canBeNull = false)
    private long rdClssFLmt;

    /**
     * clss floor limit flag
     * 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 2)
    private int rdClssFLmtFlg;
    /**
     * clss transaction limit flag
     * 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 2)
    private int rdClssTxnLmtFlg;
    /**
     * clss CVM limit flag
     * 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 2)
    private int rdCVMLmtFlg;

    /**
     * target percent
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 100)
    private int targetPer;
    /**
     * max target percent
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 100)
    private int maxTargetPer;
    /**
     * floor limit check flag
     * 0- don't check
     * 1- Check
     */
    @DatabaseField(canBeNull = false)
    @IntRange(from = 0, to = 1)
    private int floorLimitCheckFlg;
    /**
     * do random transaction selection
     */
    @DatabaseField(canBeNull = false)
    private boolean randTransSel;
    /**
     * velocity check
     */
    @DatabaseField(canBeNull = false)
    private boolean velocityCheck;
    /**
     * floor limit
     */
    @DatabaseField(canBeNull = false)
    private long floorLimit;
    /**
     * threshold
     */
    @DatabaseField(canBeNull = false)
    private long threshold;
    /**
     * TAC denial
     */
    @DatabaseField
    private String tacDenial;
    /**
     * TAC online
     */
    @DatabaseField
    private String tacOnline;
    /**
     * TAC default
     */
    @DatabaseField
    private String tacDefault;
    /**
     * acquirer id
     */
    @DatabaseField
    private String acquirerId;
    /**
     * dDOL
     */
    @DatabaseField
    private String dDOL;
    /**
     * tDOL
     */
    @DatabaseField
    private String tDOL;
    /**
     * application version
     */
    @DatabaseField
    private String version;
    /**
     * risk management data
     */
    @DatabaseField
    private String riskManageData;

    /**
     * acquirer name (Host)
     */
    @DatabaseField
    private String acqHostName;

    /**
     * capability
     */
    @DatabaseField
    private String capability;

    /**
     * issuer brand
     */
    @DatabaseField(canBeNull = false)
    private String issuerBrand;

    /**
     * Clss TAC denial
     */
    @DatabaseField
    private String rdClssTacDenial;
    /**
     * Clss TAC online
     */
    @DatabaseField
    private String rdClssTacOnline;
    /**
     * Clss TAC default
     */
    @DatabaseField
    private String rdClssTacDefault;

    /**
     * Clss Reader TTQ (Terminal Transaction Qualifier)
     */
    @DatabaseField
    private String readerTTQ;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public int getSelFlag() {
        return selFlag;
    }

    public void setSelFlag(@SelType int selFlag) {
        this.selFlag = selFlag;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean getOnlinePin() {
        return onlinePin;
    }

    public void setOnlinePin(boolean onlinePin) {
        this.onlinePin = onlinePin;
    }

    public long getRdCVMLmt() {
        return rdCVMLmt;
    }

    public void setRdCVMLmt(long rdCVMLmt) {
        this.rdCVMLmt = rdCVMLmt;
    }

    public long getRdClssTxnLmt() {
        return rdClssTxnLmt;
    }

    public void setRdClssTxnLmt(long rdClssTxnLmt) {
        this.rdClssTxnLmt = rdClssTxnLmt;
    }

    public long getRdClssFLmt() {
        return rdClssFLmt;
    }

    public void setRdClssFLmt(long rdClssFLmt) {
        this.rdClssFLmt = rdClssFLmt;
    }

    public int getRdClssFLmtFlg() {
        return rdClssFLmtFlg;
    }

    public void setRdClssFLmtFlg(@IntRange(from = 0, to = 2) int rdClssFLmtFlg) {
        this.rdClssFLmtFlg = rdClssFLmtFlg;
    }

    public int getRdClssTxnLmtFlg() {
        return rdClssTxnLmtFlg;
    }

    public void setRdClssTxnLmtFlg(@IntRange(from = 0, to = 2) int rdClssTxnLmtFlg) {
        this.rdClssTxnLmtFlg = rdClssTxnLmtFlg;
    }

    public int getRdCVMLmtFlg() {
        return rdCVMLmtFlg;
    }

    public void setRdCVMLmtFlg(@IntRange(from = 0, to = 2) int rdCVMLmtFlg) {
        this.rdCVMLmtFlg = rdCVMLmtFlg;
    }

    public int getTargetPer() {
        return targetPer;
    }

    public void setTargetPer(@IntRange(from = 0, to = 100) int targetPer) {
        this.targetPer = targetPer;
    }

    public int getMaxTargetPer() {
        return maxTargetPer;
    }

    public void setMaxTargetPer(@IntRange(from = 0, to = 100) int maxTargetPer) {
        this.maxTargetPer = maxTargetPer;
    }

    public int getFloorLimitCheckFlg() {
        return floorLimitCheckFlg;
    }

    public void setFloorLimitCheckFlg(@IntRange(from = 0, to = 1) int floorLimitCheckFlg) {
        this.floorLimitCheckFlg = floorLimitCheckFlg;
    }

    public boolean getRandTransSel() {
        return randTransSel;
    }

    public void setRandTransSel(boolean randTransSel) {
        this.randTransSel = randTransSel;
    }

    public boolean getVelocityCheck() {
        return velocityCheck;
    }

    public void setVelocityCheck(boolean velocityCheck) {
        this.velocityCheck = velocityCheck;
    }

    public long getFloorLimit() {
        return floorLimit;
    }

    public void setFloorLimit(long floorLimit) {
        this.floorLimit = floorLimit;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public String getTacDenial() {
        return tacDenial;
    }

    public void setTacDenial(String tacDenial) {
        this.tacDenial = tacDenial;
    }

    public String getTacOnline() {
        return tacOnline;
    }

    public void setTacOnline(String tacOnline) {
        this.tacOnline = tacOnline;
    }

    public String getTacDefault() {
        return tacDefault;
    }

    public void setTacDefault(String tacDefault) {
        this.tacDefault = tacDefault;
    }

    public String getAcquirerId() {
        return acquirerId;
    }

    public void setAcquirerId(String acquirerId) {
        this.acquirerId = acquirerId;
    }

    public String getDDOL() {
        return dDOL;
    }

    public void setDDOL(String dDOL) {
        this.dDOL = dDOL;
    }

    public String getTDOL() {
        return tDOL;
    }

    public void setTDOL(String tDOL) {
        this.tDOL = tDOL;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRiskManageData() {
        return riskManageData;
    }

    public void setRiskManageData(String riskManageData) {
        this.riskManageData = riskManageData;
    }

    public String getAcqHostName() {return acqHostName;}

    public void setAcqHostName(String acqHostName) {this.acqHostName = acqHostName;}

    public String getCapability() {return capability;}

    public void setCapability(String capability) {this.capability = capability;}

    public String getIssuerBrand() {
        return issuerBrand;
    }

    public void setIssuerBrand(String issuerBrand) {
        this.issuerBrand = issuerBrand;
    }

    public String getRdClssTacDenial() {
        return rdClssTacDenial;
    }

    public void setRdClssTacDenial(String rdClssTacDenial) {
        this.rdClssTacDenial = rdClssTacDenial;
    }

    public String getRdClssTacOnline() {
        return rdClssTacOnline;
    }

    public void setRdClssTacOnline(String rdClssTacOnline) {
        this.rdClssTacOnline = rdClssTacOnline;
    }

    public String getRdClssTacDefault() {
        return rdClssTacDefault;
    }

    public void setRdClssTacDefault(String rdClssTacDefault) {
        this.rdClssTacDefault = rdClssTacDefault;
    }

    public String getReaderTTQ() {
        return readerTTQ;
    }

    public void setReaderTTQ(String readerTTQ) {
        this.readerTTQ = readerTTQ;
    }

    /***************************
     * EmvAidParam to AidParam
     ***********************************/
    @NonNull
    public static List<AidParam> toAidParams() {
        IConvert convert = FinancialApplication.getConvert();
        List<AidParam> list = new LinkedList<>();
        AcqManager acqManager = FinancialApplication.getAcqManager();

        List<EmvAid> aidList = FinancialApplication.getEmvDbHelper().findAllAID();
        if (aidList == null) {
            return new ArrayList<>(0);
        }
        for (EmvAid emvAidParam : aidList) {
            if (emvAidParam.getAcqHostName() != null) {
                Acquirer acquirer = acqManager.findAcquirer(emvAidParam.getAcqHostName());
                if(acquirer != null && acquirer.isEnable()){
                    AidParam aidParam = new AidParam();
                    aidParam.setAppName(emvAidParam.getAppName());
                    aidParam.setAid(convert.strToBcd(emvAidParam.getAid(), EPaddingPosition.PADDING_LEFT));
                    aidParam.setSelFlag((byte) emvAidParam.getSelFlag());
                    aidParam.setPriority((byte) emvAidParam.getPriority());
                    aidParam.setOnlinePin(emvAidParam.getOnlinePin());
                    aidParam.setRdCVMLmt(emvAidParam.getRdCVMLmt());
                    aidParam.setRdClssTxnLmt(emvAidParam.getRdClssTxnLmt());
                    aidParam.setRdClssFLmt(emvAidParam.getRdClssFLmt());
                    aidParam.setRdClssFLmtFlag(emvAidParam.getRdClssFLmtFlg());
                    aidParam.setRdClssTxnLmtFlag(emvAidParam.getRdClssTxnLmtFlg());
                    aidParam.setRdCVMLmtFlag(emvAidParam.getRdCVMLmtFlg());
                    aidParam.setFloorLimit(emvAidParam.getFloorLimit());
                    aidParam.setFloorLimitCheckFlg(emvAidParam.getFloorLimitCheckFlg());
                    aidParam.setThreshold(emvAidParam.getThreshold());
                    aidParam.setTargetPer((byte) emvAidParam.getTargetPer());
                    aidParam.setMaxTargetPer((byte) emvAidParam.getMaxTargetPer());
                    aidParam.setRandTransSel(emvAidParam.getRandTransSel());
                    aidParam.setVelocityCheck(emvAidParam.getVelocityCheck());
                    aidParam.setTacDenial(convert.strToBcd(emvAidParam.getTacDenial(), EPaddingPosition.PADDING_LEFT));
                    aidParam.setTacOnline(convert.strToBcd(emvAidParam.getTacOnline(), EPaddingPosition.PADDING_LEFT));
                    aidParam.setTacDefault(convert.strToBcd(emvAidParam.getTacDefault(), EPaddingPosition.PADDING_LEFT));
                    if (emvAidParam.getAcquirerId() != null) {
                        aidParam.setAcquirerId(convert.strToBcd(emvAidParam.getAcquirerId(), EPaddingPosition.PADDING_LEFT));
                    }
                    if (emvAidParam.getDDOL() != null) {
                        aidParam.setdDol(convert.strToBcd(emvAidParam.getDDOL(), EPaddingPosition.PADDING_LEFT));
                    }
                    if (emvAidParam.getTDOL() != null) {
                        aidParam.settDol(convert.strToBcd(emvAidParam.getTDOL(), EPaddingPosition.PADDING_LEFT));
                    }
                    aidParam.setVersion(convert.strToBcd(emvAidParam.getVersion(), EPaddingPosition.PADDING_LEFT));
                    if (emvAidParam.getRiskManageData() != null) {
                        aidParam.setRiskManData(convert.strToBcd(emvAidParam.getRiskManageData(), EPaddingPosition.PADDING_LEFT));
                    }
                    if (emvAidParam.getAcqHostName() != null) {
                        aidParam.setAcqHostName(emvAidParam.getAcqHostName());
                    }
                    if (emvAidParam.getCapability() != null) {
                        aidParam.setCapability(emvAidParam.getCapability());
                    }
                    if (emvAidParam.getIssuerBrand() != null) {
                        aidParam.setIssuerBrand(emvAidParam.getIssuerBrand());
                    }
                    if (emvAidParam.getRdClssTacDenial() != null) {
                        aidParam.setRdClssTacDenial(convert.strToBcd(emvAidParam.getRdClssTacDenial(), EPaddingPosition.PADDING_LEFT));
                    }
                    if (emvAidParam.getRdClssTacOnline() != null) {
                        aidParam.setRdClssTacOnline(convert.strToBcd(emvAidParam.getRdClssTacOnline(), EPaddingPosition.PADDING_LEFT));
                    }
                    if (emvAidParam.getRdClssTacDefault() != null) {
                        aidParam.setRdClssTacDefault(convert.strToBcd(emvAidParam.getRdClssTacDefault(), EPaddingPosition.PADDING_LEFT));
                    }
                    aidParam.setReaderTTQ(emvAidParam.getReaderTTQ());
                    list.add(aidParam);
                    Log.d(TAG, "AID appName: "  + emvAidParam.appName + " issuerBrand:" + emvAidParam.issuerBrand);
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return appName;
    }

    public static void load(List<EmvAid> aids) {
        // test apps
        for (EmvAid i : aids) {
            Log.d("EmvAid", "EmvLoad:[AppName=" + i.getAppName() + ", AID=" + i.getAid() + "]");
            FinancialApplication.getEmvDbHelper().insertAID(i);
        }
    }
}
