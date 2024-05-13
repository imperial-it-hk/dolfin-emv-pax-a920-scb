package com.pax.device;

import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Bank;
import com.pax.pay.constant.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserParam {

    private static String RSAKey;
    private static String te_id;
    private static String te_pin;
    private Map<Bank, TerminalEncryptionParam> teParams;

    public static String KMSIF01 = "00100000006";

    public static String KMSVR01 = "12000010";					//KMS VENDOR ID
    public static String KMSVR02 = "15000003";					//KMS VENDOR ID - BAY
    public static String KMSAQ01 = "120";					    //KMS ACQ ID
    public static String KMSAQ02 = "150";					    //KMS ACQ ID - BAY
    public static String KMSNI01 = "126";					    //KMS NII, TLE NII (option)
    public static String KMSNI02 = "158";					    //KMS NII, TLE NII - BAY (option)
    public static String TLENI01 = "127";                       //TLE NII (same as above, reduce Env line)
    public static String TLENI02 = "159";                       //TLE NII - BAY (same as above, reduce Env line)

    // Control Limit
    public static String CTRL_LIMIT_NII = "131";				//CONTROL LIMIT NII

    //public static String TLENI03 = "158";                       //AYCAP-TLE NII

    public static final List<Integer> encField = Arrays.asList(2, 14, 35, 45);

    public static int TMK_INDEX = 5;
    public static int TWK_MAK_INDEX = 20;
    public static int TWK_DEK_INDEX = 35;
    public static int TMK_UPI_IDX_BASE = 50;
    public static int TWK_UPI_IDX_BASE = 65;
    public static int TPK_UPI_IDX_BASE = 80;

    public static boolean TEST_MODE = false;

    public UserParam() {
        //do nothing
        teParams = new HashMap<>();
    }

    public String getRSAKey() {
        return RSAKey;
    }

    public String getTE_ID() {
        return te_id;
    }

    public String getTE_PIN() {
        return te_pin;
    }

    public TerminalEncryptionParam getTEParam(Bank bank) {
        TerminalEncryptionParam param = null;

        try {
            if (teParams.containsKey(bank)) {
                param = teParams.get(bank);
            }
        }
        catch (Exception ex) {
            param = null;
        }

        return param;
    }

    public void setRSAKey(String RSAKey) {
        this.RSAKey = RSAKey;
    }

    public void setTE_ID(String te_id) {
        this.te_id = te_id;
    }

    public void setTE_PIN(String te_pin) {
        this.te_pin = te_pin;
    }

    public void putTEParam(TerminalEncryptionParam teParam) {
        if (this.teParams.containsKey(teParam.getBank())) {
            this.teParams.remove(teParam.getBank());
        }

        this.teParams.put(teParam.getBank(), teParam);
    }



    public static int KBANK_TMK_INDEX = 18;
    public static int KBANK_TWK_MAK_INDEX = 19;
    public static int KBANK_TWK_DEK_INDEX = 20;
    public static int KBANK_TPK_TPK_INDEX = 21;
    public static int KBANK_TPK_TDK_INDEX = 22;         // extra key slot for Terminal use for SP200 encryption

    public static int BAY_TMK_INDEX = 23;
    public static int BAY_TWK_MAK_INDEX = 24;
    public static int BAY_TWK_DEK_INDEX = 25;

    public static int getTMKID (Acquirer acquirer) {
        if (acquirer.getTleBankName().equals(Constants.ACQ_KBANK)){
            return KBANK_TMK_INDEX;
        } else  {
           return BAY_TMK_INDEX;
        }
    }

    public static int getMAKID (Acquirer acquirer) {
        if (acquirer.getTleBankName().equals(Constants.ACQ_KBANK)){
            return KBANK_TWK_MAK_INDEX;
        } else  {
            return BAY_TWK_MAK_INDEX;
        }
    }
    public static int getDEKID (Acquirer acquirer) {
        if (acquirer.getTleBankName().equals(Constants.ACQ_KBANK)){
            return KBANK_TWK_DEK_INDEX;
        } else  {
            return BAY_TWK_DEK_INDEX;
        }
    }
    public static int getTPK_TPKID (Acquirer acquirer) {
        if (acquirer.getTleBankName().equals(Constants.ACQ_KBANK)){
            return KBANK_TPK_TPK_INDEX;
        } else  {
            return -1;
        }
    }
    public static int getTPK_TDKID (Acquirer acquirer) {
        if (acquirer.getTleBankName().equals(Constants.ACQ_KBANK)){
            return KBANK_TPK_TDK_INDEX;
        } else  {
            return -1;
        }
    }
}
