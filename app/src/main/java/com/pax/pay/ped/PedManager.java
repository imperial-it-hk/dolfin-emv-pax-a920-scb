package com.pax.pay.ped;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import th.co.bkkps.utils.Log;

import com.pax.dal.IPed;
import com.pax.dal.entity.DUKPTResult;
import com.pax.dal.entity.EAesCheckMode;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.ECryptOperate;
import com.pax.dal.entity.ECryptOpt;
import com.pax.dal.entity.EDUKPTDesMode;
import com.pax.dal.entity.EDUKPTMacMode;
import com.pax.dal.entity.EDUKPTPinMode;
import com.pax.dal.entity.EFuncKeyMode;
import com.pax.dal.entity.EPedDesMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EPedMacMode;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.EPinBlockMode;
import com.pax.dal.entity.RSAKeyInfo;
import com.pax.dal.entity.RSARecoverInfo;
import com.pax.dal.entity.SM2KeyPair;
import com.pax.dal.exceptions.PedDevException;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

import java.util.Arrays;

public class PedManager {
    private static PedManager pedManager;
    private static EPedType pedType;
    public static int PEDMODE = 0; //全局标志位
    private static int pedMode = 0; //本地变量
    private IPed ped;
    private byte[] byte_test = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private KeyPair kp = null;
    public static byte[] modulus1 = null;
    public static boolean isGenRsaKey = false;

    public static final int ONE_DECRYPT = 0;
    public static final int ONE_ENCRYPT = 1;
    public static final int TRI_DECRYPT = 2;
    public static final int TRI_ENCRYPT = 3;
    public static final int TRI_DECRYPT3 = 4;
    public static final int TRI_ENCRYPT3 = 5;
    public static final int TRI_DECRYPT4 = 6;
    public static final int TRI_ENCRYPT4 = 7;

    public static final int KCV_SIZE = 4;
    public static final int KCV_LEN = KCV_SIZE*2;

    public static final byte[] EFTSec_INITIAL_VALUE_TO_GEN_KCV = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};


    private PedManager(EPedType type) {
        pedType = type;
        pedMode = PEDMODE;
        if(pedMode == 0){
            ped = FinancialApplication.getDal().getPed(type);
        }else{
            ped = FinancialApplication.getDal().getPedKeyIsolation(type);
        }
    }

    public static PedManager getInstance(EPedType type) {
        if (pedManager == null || pedType != type || pedMode != PEDMODE) {
            pedManager = new PedManager(type);
        }

        return pedManager;
    }

    public KeyPair genKeyPair(int len) {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecureRandom random = new SecureRandom();
        kpg.initialize(len, random);
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

    public void setKeyPair(KeyPair kp) { this.kp = kp; }
    public KeyPair getKeyPair() { return kp; }

    // PED writeKey include TMK,TPK,TAK,TDk
    // ===============================================================================================================

    public boolean writeKey(EPedKeyType srcKeyType, byte srcKeyIndex, EPedKeyType destKeyType, byte destkeyIndex,
                            byte[] destKeyValue, ECheckMode checkMode, byte[] checkBuf) {
        try {
            ped.writeKey(srcKeyType, srcKeyIndex, destKeyType, destkeyIndex, destKeyValue, checkMode, checkBuf);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ===================================================================================================================

    public boolean writeTIK() {
        byte[] tik16Clr = { (byte) 0x6A, (byte) 0xC2, (byte) 0x92, (byte) 0xFA, (byte) 0xA1, (byte) 0x31, (byte) 0x5B,
                (byte) 0x4D, (byte) 0x85, (byte) 0x8A, (byte) 0xB3, (byte) 0xA3, (byte) 0xD7, (byte) 0xD5, (byte) 0x93,
                (byte) 0x3A };
        byte[] ksn = { (byte) 0xff, (byte) 0xff, (byte) 0x98, (byte) 0x76, (byte) 0x54, (byte) 0x32, (byte) 0x10,
                (byte) 0xE0, (byte) 0x00, (byte) 0x00 };
        try {
            ped.writeTIK((byte) 0x01, (byte) 0x00, tik16Clr, ksn, ECheckMode.KCV_NONE, null);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] getPinBlock(byte[] dataIn) {
        try {
            byte[] result = ped.getPinBlock((byte) 1, "0,4,6", dataIn, EPinBlockMode.ISO9564_0, 60000);
            return result;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getMac(byte[] bytes) {
        try {
            byte[] bytes_m = ped.getMac((byte) 2, bytes, EPedMacMode.MODE_00);
            return bytes_m;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }

    }

    public byte[] calcDes(int uMode, byte iKey, byte[] input) {
        byte[] result = null, result_1, result_2;
        int icnt;
        byte[] initVector = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        try {
            switch(uMode)
            {
                case ONE_ENCRYPT:
                    result = ped.calcDes(iKey, input, EPedDesMode.ENCRYPT);
                    break;
                case ONE_DECRYPT:
                    result = ped.calcDes(iKey, input, EPedDesMode.DECRYPT);
                    break;
                case TRI_ENCRYPT:
                    result_1 = ped.calcDes(iKey,Arrays.copyOfRange(input,0,8),EPedDesMode.ENCRYPT);
                    result_2 = ped.calcDes(iKey,Arrays.copyOfRange(input,8,16),EPedDesMode.ENCRYPT);
                    result = Utils.concat(result_1, result_2);

                    for (icnt = 0;icnt<8;icnt++)
                    {
                        result[8+icnt] ^= input[icnt];
                    }
                    break;
                case TRI_DECRYPT:
                    result_1 = ped.calcDes(iKey,Arrays.copyOfRange(input,0,8),EPedDesMode.DECRYPT);
                    result_2 = ped.calcDes(iKey,Arrays.copyOfRange(input,8,16),EPedDesMode.DECRYPT);
                    result = Utils.concat(result_1, result_2);

                    for (icnt = 0;icnt<8;icnt++)
                    {
                        result[8+icnt] ^= input[icnt];
                    }
                    break;
                case TRI_ENCRYPT3:
                    result = ped.calcDes(iKey, initVector, input, (byte) 3);//3:ENCRYPT#CBC
                    break;
                case TRI_DECRYPT3:
                    result = ped.calcDes(iKey, initVector, input, (byte) 2);//2:DECRYPT#CBC
                    break;
                case TRI_ENCRYPT4:
                    result = ped.calcDes(iKey, null, input, (byte) 1);//1:ENCRYPT#ECB
                    break;
                case TRI_DECRYPT4:
                    result = ped.calcDes(iKey, null, input, (byte) 0);//0:DECRYPT#ECB
                    break;
            }
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return result;
    }


    public DUKPTResult getDUKPTPin(byte[] dataIn) {
        try {
            DUKPTResult bytes_ped = ped.getDUKPTPin((byte) 1, "4", dataIn, EDUKPTPinMode.ISO9564_0_INC, 20000);
            return bytes_ped;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }

    }

//    public byte[] getDUKPTMac(byte[] bytes) {
//        try {
//            DUKPTResult result = ped.getDUPKTMac((byte) 0x01, bytes, EDUKPTMacMode.MODE_00);
//            if (result != null) {
//                return result.getResult();
//            } else {
//                return null;
//            }
//        } catch (PedDevException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    // ==============================================================================
//    // 获取 TPK TAK TDK 的KCV
//    public byte[] getKCV_TPK() {
//        try {
//            byte[] bytes_tpk = ped.getKCV(EPedKeyType.TPK, (byte) 1, (byte) 0, byte_test);
//            return bytes_tpk;
//        } catch (PedDevException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public byte[] getKCV_TAK() {
//        try {
//            byte[] bytes_tak = ped.getKCV(EPedKeyType.TAK, (byte) 2, (byte) 0, byte_test);
//            return bytes_tak;
//        } catch (PedDevException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public byte[] getKCV_TDK() {
//        try {
//            byte[] bytes_tdk = ped.getKCV(EPedKeyType.TDK, (byte) 3, (byte) 0, byte_test);
//            return bytes_tdk;
//        } catch (PedDevException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    // =======================================================================================================
//
//    public boolean writeKeyVar(byte[] bs) {
//        try {
//            ped.writeKeyVar(EPedKeyType.TPK, (byte) 1, (byte) 5, bs, ECheckMode.KCV_NONE, new byte[] {});
//            return true;
//        } catch (PedDevException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public String getVersion() {
        try {
            String str_verString = ped.getVersion();
            return str_verString;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean erase() {
        try {
            boolean flag = ped.erase();
            return flag;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setIntervalTime(String num1, String num2) {
        try {
            ped.setIntervalTime(Integer.parseInt(num1), Integer.parseInt(num2));
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setFunctionKey(EFuncKeyMode k) {
        try {
            ped.setFunctionKey(k);// EFunckeyKeyMode.ClEAR_ALL
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean genRsaKey(){
        try {
            ped.genRSAKey((byte)2, (byte)1, (short)2048, (byte)1);
            isGenRsaKey = true;
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            isGenRsaKey = false;
            return false;
        }
    }

    // =====================================================================
    // writeRSAkey include public key and private key
    public boolean writeRSAKeyPublic(int iKey) {
        try {
            RSAKeyInfo keyInfo = new RSAKeyInfo();

            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();
            byte[] exponent = pubKey.getPublicExponent().toByteArray();
            byte[] exponent1 = exponent;
            if(exponent[0]==0){
                exponent1 = new byte[exponent.length-1];
                System.arraycopy(exponent, 1, exponent1, 0, exponent1.length);
            }
            keyInfo.setExponent(exponent1);
            keyInfo.setExponentLen(exponent1.length * 8 );
            byte[] modulus = pubKey.getModulus().toByteArray();
            modulus1 = modulus;
            if(modulus[0]== 0){
                modulus1 = new byte[modulus.length-1];
                System.arraycopy(modulus, 1, modulus1, 0, modulus1.length);
            }
            keyInfo.setModulus(modulus1);
            keyInfo.setModulusLen(modulus1.length * 8);
            keyInfo.setKeyInfo(pubKey.getEncoded());
            ped.writeRSAKey((byte) iKey, keyInfo);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public RSARecoverInfo RSAEncrypt(int iKey, byte[] bytes) {
        try {
            RSARecoverInfo info = ped.RSARecover((byte)iKey, bytes);
            return info;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean writeRSAKeyPrivate(int iKey) {
        try {
            RSAKeyInfo keyInfo = new RSAKeyInfo();

            RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
            byte[] exponent = privateKey.getPrivateExponent().toByteArray();
            byte[] exponent1 = exponent;
            if(exponent[0]==0){
                exponent1 = new byte[exponent.length-1];
                System.arraycopy(exponent, 1, exponent1, 0, exponent1.length);
            }
            keyInfo.setExponent(exponent1);
            keyInfo.setExponentLen(exponent1.length * 8);
            byte[] modulus = privateKey.getModulus().toByteArray();
            modulus1 = modulus;
            if(modulus[0]== 0){
                modulus1 = new byte[modulus.length-1];
                System.arraycopy(modulus, 1, modulus1, 0, modulus1.length);
            }
            keyInfo.setModulus(modulus1);
            keyInfo.setModulusLen(modulus1.length * 8);
            keyInfo.setKeyInfo(privateKey.getEncoded());
            ped.writeRSAKey((byte) iKey, keyInfo);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public RSARecoverInfo RSADecrypt(int iKey, byte[] bytes) {
        try {
            RSARecoverInfo info = ped.RSARecover((byte)iKey, bytes);
            return info;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    // =================================================================================
    public byte[] calcDUKPTDesMac(byte[] bytes) {
        try {
            DUKPTResult result = ped.calcDUKPTDes((byte) 0x01, (byte) 0x00, null, bytes, EDUKPTDesMode.CBC_ENCRYPTION);
            if (result != null) {
                return result.getResult();
            } else {
                return null;
            }
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] calcDUKPTDesDes(byte[] bytes) {
        try {
            DUKPTResult result = ped.calcDUKPTDes((byte) 0x01, (byte) 0x01, null, bytes, EDUKPTDesMode.CBC_ENCRYPTION);
            if (result != null) {
                return result.getResult();
            } else {
                return null;
            }
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getDUKPTKsn() {
        try {
            byte[] bytes_ksn = ped.getDUKPTKsn((byte) 1);
            return bytes_ksn;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }

    }

    public boolean incDUKPTKsn() {
        try {
            ped.incDUKPTKsn((byte) 0x01);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setExMode(byte b) {
        ped.setExMode(b);
    }

    public boolean clearScreen() {
        try {
            ped.clearScreen();
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String inputStr_1(int num1, int num2) {
        try {
            String str = ped.inputStr((byte) 0x00, (byte) num1, (byte) num2, 10000);
            return str;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String inputStr_2(int num1, int num2) {
        try {
            String str = ped.inputStr((byte) 0x01, (byte) num1, (byte) num2, 10000);
            return str;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean showStr(String str) {
        try {
            ped.showStr((byte) 0x00, (byte) 0x00, str);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getSN() {
        String sn = null;
        try {
            sn = ped.getSN();
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return sn == null ? "null" : sn;
    }

    public void showInputBox(boolean flag, String title) {
        try {
            ped.showInputBox(flag, title);
        } catch (PedDevException e) {
            e.printStackTrace();
        }
    }

    public SM2KeyPair genSM2KeyPair(short keyLenBit) {
        try {
            SM2KeyPair keyPair = ped.genSM2KeyPair(keyLenBit);
            return keyPair;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean writeSM2CipherKey(EPedKeyType srcKeyType, byte srcKeyIdx, EPedKeyType dstKeyType, byte dstKeyIdx,
                                     byte[] keyValue) {
        try {
            ped.writeSM2CipherKey(srcKeyType, srcKeyIdx, dstKeyType, dstKeyIdx, keyValue);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeSM2Key(byte keyIdx, EPedKeyType keyType, byte[] keyValue) {
        try {
            ped.writeSM2Key(keyIdx, keyType, keyValue);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] SM2Recover(byte keyIdx, byte[] input, ECryptOperate operation) {
        byte[] res = null;
        try {
            res = ped.SM2Recover(keyIdx, input, operation);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public byte[] SM2Sign(byte pubKeyIdx, byte pvtKeyIdx, byte[] uid, byte[] input) {
        byte[] res = null;
        try {
            res = ped.SM2Sign(pubKeyIdx, pvtKeyIdx, uid, input);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean SM2Verify(byte pubKeyIdx, byte[] uid, byte[] input, byte[] signature) {
        try {
            ped.SM2Verify(pubKeyIdx, uid, input, signature);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] SM3(byte[] input) {
        byte[] res = null;
        try {
            res = ped.SM3(input, (byte) 0);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public byte[] SM4(byte keyIdx, byte[] initVector, byte[] input, ECryptOperate operation, ECryptOpt option) {
        byte[] res = null;
        try {
            res = ped.SM4(keyIdx, initVector, input, operation, option);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public byte[] getMacSM(byte keyIdx, byte[] initVector, byte[] input, byte mode) {
        byte[] res = null;
        try {
            res = ped.getMacSM(keyIdx, initVector, input, mode);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public byte[] getPinBlockSM4(byte keyIndex, String expPinLen, byte[] dataIn, EPinBlockMode mode, int timeoutMs) {
        byte[] res = null;
        try {
            res = ped.getPinBlockSM4(keyIndex, expPinLen, dataIn, mode, timeoutMs);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
        }
        return res;
    }

    public void setKeyboardLayoutLandscape(boolean landscape) {
        try {
            ped.setKeyboardLayoutLandscape(landscape);
        } catch (PedDevException e) {
            e.printStackTrace();
        }
    }

    public boolean writeAesKey(EPedKeyType srcKeyType, byte srcKeyIndex, byte destkeyIndex,
                               byte[] destKeyValue, EAesCheckMode checkMode, byte[] checkBuf){
        try {
            ped.writeAesKey(srcKeyType, srcKeyIndex, destkeyIndex, destKeyValue, checkMode, checkBuf);
            return true;
        } catch (PedDevException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] calcAes(byte keyIdx, byte[] initvector, byte[] dataIn,  ECryptOperate operation, ECryptOpt option){
        try {
            byte[] res = ped.calcAes(keyIdx, initvector, dataIn, operation, option);
            return res;
        } catch (PedDevException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setKeyboardRandom(boolean flag){
        try {
            ped.setKeyboardRandom(flag);
        } catch (PedDevException e) {
            e.printStackTrace();
        }
    }
}
