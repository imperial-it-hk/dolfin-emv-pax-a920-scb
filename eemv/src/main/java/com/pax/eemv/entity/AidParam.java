package com.pax.eemv.entity;

import com.pax.eemv.utils.Tools;

public class AidParam {
    private byte[] appName;
    private byte[] aid;
    private byte selFlag;
    private byte priority;
    private boolean onlinePin;
    private long rdCVMLmt;
    private long rdClssTxnLmt;
    private long rdClssFLmt;
    private int rdClssFLmtFlag;
    private int rdClssTxnLmtFlag;
    private int rdCVMLmtFlag;
    private long floorLimit;
    private int floorLimitCheckFlg;
    private long threshold;
    private byte targetPer;
    private byte maxTargetPer;
    private boolean randTransSel;
    private boolean velocityCheck;
    private byte[] tacDenial;
    private byte[] tacOnline;
    private byte[] tacDefault;
    private byte[] acquirerId;
    private byte[] dDol;
    private byte[] tDol;
    private byte[] version;
    private byte[] riskManData;
    private byte[] acqHostName;
    private byte[] capability;
    private String issuerBrand;
    private byte[] rdClssTacDenial;
    private byte[] rdClssTacOnline;
    private byte[] rdClssTacDefault;
    private String readerTTQ;
    public AidParam() {
        this.appName = new byte[0];
        this.aid = new byte[0];
        this.selFlag = 0;
        this.priority = 0;
        this.onlinePin = false;

        this.rdCVMLmt = 0L;
        this.rdClssTxnLmt = 0L;
        this.rdClssFLmt = 0L;

        this.rdClssFLmtFlag = 0;
        this.rdClssTxnLmtFlag = 0;
        this.rdCVMLmtFlag = 0;

        this.floorLimit = 0L;
        this.floorLimitCheckFlg = 0;
        this.threshold = 0L;
        this.targetPer = 0;
        this.maxTargetPer = 0;
        this.randTransSel = false;
        this.velocityCheck = false;
        this.tacDenial = new byte[0];
        this.tacOnline = new byte[0];
        this.tacDefault = new byte[0];
        this.acquirerId = new byte[0];
        this.dDol = new byte[0];
        this.tDol = new byte[0];
        this.version = new byte[0];
        this.riskManData = new byte[0];
        this.acqHostName = new byte[0];
        this.capability = new byte[0];
        this.issuerBrand = "";
        this.rdClssTacDenial = new byte[0];
        this.rdClssTacOnline = new byte[0];
        this.rdClssTacDefault = new byte[0];
        this.readerTTQ = "";
    }

    public String getAppName() {
        return Tools.bytes2String(this.appName).trim();
    }

    public void setAppName(String appName) {
        this.appName = Tools.string2Bytes(appName);
    }

    public byte[] getAid() {
        return this.aid;
    }

    public void setAid(byte[] aid) {
        this.aid = aid;
    }

    public byte getSelFlag() {
        return this.selFlag;
    }

    public void setSelFlag(byte selFlag) {
        this.selFlag = selFlag;
    }

    public byte getPriority() {
        return this.priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public boolean getOnlinePin() {
        return this.onlinePin;
    }

    public void setOnlinePin(boolean onlinePin) {
        this.onlinePin = onlinePin;
    }

    public long getRdCVMLmt() {
        return this.rdCVMLmt;
    }

    public void setRdCVMLmt(long rdCVMLmt) {
        this.rdCVMLmt = rdCVMLmt;
    }

    public long getRdClssTxnLmt() {
        return this.rdClssTxnLmt;
    }

    public void setRdClssTxnLmt(long rdClssTxnLmt) {
        this.rdClssTxnLmt = rdClssTxnLmt;
    }

    public long getRdClssFLmt() {
        return this.rdClssFLmt;
    }

    public void setRdClssFLmt(long rdClssFLmt) {
        this.rdClssFLmt = rdClssFLmt;
    }

    /**
     * @return 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    public int getRdClssFLmtFlag() {
        return this.rdClssFLmtFlag;
    }

    public void setRdClssFLmtFlag(int rdClssFLmtFlag) {
        this.rdClssFLmtFlag = rdClssFLmtFlag;
    }

    /**
     * @return 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    public int getRdClssTxnLmtFlag() {
        return this.rdClssTxnLmtFlag;
    }

    public void setRdClssTxnLmtFlag(int rdClssTxnLmtFlag) {
        this.rdClssTxnLmtFlag = rdClssTxnLmtFlag;
    }

    /**
     * @return 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    public int getRdCVMLmtFlag() {
        return this.rdCVMLmtFlag;
    }

    public void setRdCVMLmtFlag(int rdCVMLmtFlag) {
        this.rdCVMLmtFlag = rdCVMLmtFlag;
    }

    public long getFloorLimit() {
        return this.floorLimit;
    }

    public void setFloorLimit(long floorLimit) {
        this.floorLimit = floorLimit;
    }

    /**
     * @return 0- Deactivated
     * 1- Active and exist
     * 2- Active but not exist
     */
    public int getFloorLimitCheckFlg() {
        return this.floorLimitCheckFlg;
    }

    public void setFloorLimitCheckFlg(int floorLimitCheckFlg) {
        this.floorLimitCheckFlg = floorLimitCheckFlg;
    }

    public long getThreshold() {
        return this.threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public byte getTargetPer() {
        return this.targetPer;
    }

    public void setTargetPer(byte targetPer) {
        this.targetPer = targetPer;
    }

    public byte getMaxTargetPer() {
        return this.maxTargetPer;
    }

    public void setMaxTargetPer(byte maxTargetPer) {
        this.maxTargetPer = maxTargetPer;
    }

    public boolean getRandTransSel() {
        return this.randTransSel;
    }

    public void setRandTransSel(boolean randTransSel) {
        this.randTransSel = randTransSel;
    }

    public boolean getVelocityCheck() {
        return this.velocityCheck;
    }

    public void setVelocityCheck(boolean velocityCheck) {
        this.velocityCheck = velocityCheck;
    }

    public byte[] getTacDenial() {
        return this.tacDenial;
    }

    public void setTacDenial(byte[] tacDenial) {
        this.tacDenial = tacDenial;
    }

    public byte[] getTacOnline() {
        return this.tacOnline;
    }

    public void setTacOnline(byte[] tacOnline) {
        this.tacOnline = tacOnline;
    }

    public byte[] getTacDefault() {
        return this.tacDefault;
    }

    public void setTacDefault(byte[] tacDefault) {
        this.tacDefault = tacDefault;
    }

    public byte[] getAcquirerId() {
        return this.acquirerId;
    }

    public void setAcquirerId(byte[] acquirerId) {
        this.acquirerId = acquirerId;
    }

    public byte[] getdDol() {
        return this.dDol;
    }

    public void setdDol(byte[] dDol) {
        this.dDol = dDol;
    }

    public byte[] gettDol() {
        return this.tDol;
    }

    public void settDol(byte[] tDol) {
        this.tDol = tDol;
    }

    public byte[] getVersion() {
        return this.version;
    }

    public void setVersion(byte[] version) {
        this.version = version;
    }

    public byte[] getRiskManData() {
        return this.riskManData;
    }

    public void setRiskManData(byte[] riskManData) {
        this.riskManData = riskManData;
    }

    public String getAcqHostName() {return Tools.bytes2String(this.acqHostName).trim();}

    public void setAcqHostName(String acqHostName) {this.acqHostName = Tools.string2Bytes(acqHostName);}

    public String getCapability() {return Tools.bytes2String(this.capability).trim();}

    public void setCapability(String capability) {this.capability = Tools.string2Bytes(capability);}

    public String getIssuerBrand() {
        return issuerBrand;
    }

    public void setIssuerBrand(String issuerBrand) {
        this.issuerBrand = issuerBrand;
    }

    public byte[] getRdClssTacDenial() {
        return rdClssTacDenial;
    }

    public void setRdClssTacDenial(byte[] rdClssTacDenial) {
        this.rdClssTacDenial = rdClssTacDenial;
    }

    public byte[] getRdClssTacOnline() {
        return rdClssTacOnline;
    }

    public void setRdClssTacOnline(byte[] rdClssTacOnline) {
        this.rdClssTacOnline = rdClssTacOnline;
    }

    public byte[] getRdClssTacDefault() {
        return rdClssTacDefault;
    }

    public void setRdClssTacDefault(byte[] rdClssTacDefault) {
        this.rdClssTacDefault = rdClssTacDefault;
    }

    public String getReaderTTQ() {
        return readerTTQ;
    }

    public void setReaderTTQ(String readerTTQ) {
        this.readerTTQ = readerTTQ;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.entity.AidParam
 * JD-Core Version:    0.6.0
 */