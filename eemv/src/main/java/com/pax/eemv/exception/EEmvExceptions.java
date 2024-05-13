package com.pax.eemv.exception;

import com.pax.jemv.clcommon.RetCode;

import java.util.Locale;
public enum EEmvExceptions {
    EMV_OK(RetCode.EMV_OK, "success", "success"),
    EMV_ERR_ICC_RESET(RetCode.ICC_RESET_ERR, "icc reset error", "icc reset error"),
    EMV_ERR_ICC_CMD(RetCode.ICC_CMD_ERR, "icc cmd error \n Read Card Failed, Please Retry", "icc cmd error \n การอ่านบัตรมีปัญหา, กรุณาลองใหม่"),
    EMV_ERR_ICC_BLOCK(RetCode.ICC_BLOCK, "Card Blocked", "Card Blocked"),/*icc block*/
    EMV_ERR_RSP(RetCode.EMV_RSP_ERR, "emv response error", "emv response error"),
    EMV_ERR_APP_BLOCK(RetCode.EMV_APP_BLOCK, "CARD BLOCK", "CARD BLOCK"),/*emv application block*/
    EMV_ERR_NO_APP(RetCode.EMV_NO_APP, "Card Not Support", "ไม่รองรับบัตรนี้"),
    EMV_ERR_USER_CANCEL(RetCode.EMV_USER_CANCEL, "emv user cancel", "emv user cancel"),
    EMV_ERR_TIMEOUT(RetCode.EMV_TIME_OUT, "emv timeout", "emv timeout"),
    EMV_ERR_DATA(RetCode.EMV_DATA_ERR, "emv data error", "emv data error"),
    EMV_ERR_NOT_ACCEPT(RetCode.EMV_NOT_ACCEPT, "emv not accept", "emv not accept"),
    EMV_ERR_DENIAL(RetCode.EMV_DENIAL, "emv denial", "emv denial"),
    EMV_ERR_KEY_EXP(RetCode.EMV_KEY_EXP, "emv key expiry", "emv key expiry"),
    EMV_ERR_NO_PINPAD(RetCode.EMV_NO_PINPAD, "emv no pinpad", "emv no pinpad"),
    EMV_ERR_NO_PASSWORD(RetCode.EMV_NO_PASSWORD, "emv no password", "emv no password"),
    EMV_ERR_SUM(RetCode.EMV_SUM_ERR, "emv checksum error", "emv checksum error"),
    EMV_ERR_NOT_FOUND(RetCode.EMV_NOT_FOUND, "emv not found", "emv not found"),
    EMV_ERR_NO_DATA(RetCode.EMV_NO_DATA, "emv no data", "emv no data"),
    EMV_ERR_OVERFLOW(RetCode.EMV_OVERFLOW, "emv overflow", "emv overflow"),
    EMV_ERR_NO_TRANS_LOG(RetCode.NO_TRANS_LOG, "emv no trans log", "emv no trans log"),
    EMV_ERR_RECORD_NOT_EXIST(RetCode.RECORD_NOTEXIST, "emv recode is not existed", "emv recode is not existed"),
    EMV_ERR_LOGITEM_NOT_EXIST(RetCode.LOGITEM_NOTEXIST, "emv log item is not existed", "emv log item is not existed"),
    EMV_ERR_ICC_RSP_6985(RetCode.ICC_RSP_6985, "Not Accepted", "Not Accepted"),/*icc response 6985*/
    EMV_ERR_CLSS_USE_CONTACT(RetCode.CLSS_USE_CONTACT, "clss use contact", "clss use contact"),
    EMV_ERR_FILE(RetCode.EMV_FILE_ERR, "emv file error", "emv file error"),
    EMV_ERR_CLSS_TERMINATE(RetCode.CLSS_TERMINATE, "clss terminate", "clss terminate"),
    EMV_ERR_CLSS_FAILED(RetCode.CLSS_FAILED, "clss failed", "clss failed"),
    EMV_ERR_CLSS_DECLINE(RetCode.CLSS_DECLINE, "clss decline", "clss decline"),
    EMV_ERR_PARAM(RetCode.EMV_PARAM_ERR, "emv parameter error", "emv parameter error"),
    EMV_ERR_CLSS_WAVE2_OVERSEA(RetCode.CLSS_WAVE2_OVERSEA, "clss wave2 oversea", "clss wave2 oversea"),
    EMV_ERR_CLSS_WAVE2_US_CARD(RetCode.CLSS_WAVE2_US_CARD, "clss wave2 us card", "clss wave2 us card"),
    EMV_ERR_CLSS_WAVE3_INS_CARD(RetCode.CLSS_WAVE3_INS_CARD, "clss wave3 ins card", "clss wave3 ins card"),
    //EMV_ERR_DATA_OVERFLOW(RetCode.EMV_OVERFLOW, "emv data overflow"),
    EMV_ERR_CLSS_CARD_EXPIRED(RetCode.CLSS_CARD_EXPIRED, "clss card expired", "clss card expired"),
    EMV_ERR_CLSS_NO_APP_PPSE(RetCode.EMV_NO_APP_PPSE_ERR, "clss no app ppse error", "clss no app ppse error"),
    EMV_ERR_CLSS_USE_VSDC(RetCode.CLSS_USE_VSDC, "clss use vsdc", "clss use vsdc"),
    EMV_ERR_CLSS_CVM_DECLINE(RetCode.CLSS_CVMDECLINE, "clss cvm decline", "clss cvm decline"),
    EMV_ERR_CLSS_REFER_CONSUMER_DEVICE(RetCode.CLSS_REFER_CONSUMER_DEVICE, "clss refer consumer device", "clss refer consumer device"),
    EMV_ERR_NEXT_CVM(-8053, "emv next CVM", "emv next CVM"),
    EMV_ERR_QUIT_CVM(-8057, "emv quit CVM", "emv quit CVM"),
    EMV_ERR_SELECT_NEXT(-8059, "emv select next", "emv select next"),

    EMV_ERR_FALL_BACK(-8200, "fall back", "fall back"),
    EMV_ERR_CHANNEL(-8201, "channel error", "channel error"),
    EMV_ERR_NO_KERNEL(-8203, "unknown kernel", "unknown kernel"),
    EMV_ERR_CLSS_NO_BALANCE(-8205, "clss no balance", "clss no balance"),
    EMV_ERR_CLSS_OVER_FLMT(-8206, "clss over floor limit", "clss over floor limit"),
    EMV_ERR_NO_CLSS_PBOC(-8207, "no clss pboc card error", "no clss pboc card error"),
    EMV_ERR_WRITE_FILE_FAIL(-8208, "write file fail error", "write file fail error"),
    EMV_ERR_READ_FILE_FAIL(-8209, "read file fail error", "read file fail error"),
    EMV_ERR_INVALID_PARA(-8210, "invalid parameter error", "invalid parameter error"),

    EMV_ERR_AMOUNT_FORMAT(-8211, "amount format error", "amount format error"),
    EMV_ERR_PARAM_LENGTH(-8212, "parameter length error", "parameter length error"),
    EMV_ERR_LISTENER_IS_NULL(-8213, "listener is null", "listener is null"),
    EMV_ERR_TAG_LENGTH(-8214, "tag length error", "tag length error"),

    EMV_ERR_ONLINE_TRANS_ABORT(-8301, "online transaction abort", "online transaction abort"),
    EMV_ERR_FUNCTION_NOT_IMPLEMENTED(-8302, "function not implemented", "function not implemented"),
    EMV_ERR_PURE_EC_CARD_NOT_ONLINE(-8303, "pure EC card can't online transaction", "pure EC card can't online transaction"),
    EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG(-8304, "", ""),
    EMV_ERR_UNKNOWN(-8999, "unknown error","unknown error"),
    EMV_ERR_REVERSAL_FAIL(-9100, "reversal fail error","reversal fail error"),
    EMV_ERR_TLE_FAIL(-9101, "TLE ENCRYPT ERROR","TLE ENCRYPT ผิดพลาด"),
    EMV_ERR_FORCE_SETTLEMENT(-9102, "Please Settle Firstly","กรุณาสรุปยอด"),
    EMV_ERR_UNSUPPORTED_TRANS(-9103, "Unsupported Transaction", "Unsupported Transaction"),
    EMV_NEED_MAG_ONLINE(-9104, "", ""),
    EMV_ERR_CARD_NOT_SUPPORT(RetCode.EMV_NO_APP, "Card Not Support", "ไม่รองรับบัตรนี้"),;

    private int errCodeFromBasement;
    private String errMsgEn;
    private String errMsgTh;

    EEmvExceptions(int errCodeFromBasement, String errMsgEn, String errMsgTh) {
        this.errCodeFromBasement = errCodeFromBasement;
        this.errMsgEn = errMsgEn;
        this.errMsgTh = errMsgTh;
    }

    public int getErrCodeFromBasement() {
        return this.errCodeFromBasement;
    }

    public String getErrMsg() {
        String lang = Locale.getDefault().getLanguage();
        if(lang.equals("th")){
            return this.errMsgTh;
        }else{
            return this.errMsgEn;
        }
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.exception.EEmvExceptions
 * JD-Core Version:    0.6.0
 */