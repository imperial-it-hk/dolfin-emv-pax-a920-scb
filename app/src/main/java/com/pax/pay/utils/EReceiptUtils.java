package com.pax.pay.utils;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import androidx.annotation.NonNull;

import com.pax.device.DeviceImplNeptune;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.settings.SysParam;
import com.pax.view.dialog.CustomAlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import th.co.bkkps.utils.Log;

public class EReceiptUtils {

    public static final String TAG = "ERCM";
    public static boolean TESTMODE = false;
    public static boolean PACK_TESTMODE = true;
    public int num_term_alert_txn = 0;

    public static final int ERM_MAX_PENDING_UPLOAD_ERECEIPT_NUMBER = 200;

    public static String getApp_RootDirectory(Context context) { return "/data/data/" + context.getApplicationContext().getPackageName(); }
    public static String getERM_LogoDirectory (Context context) { return getApp_RootDirectory(context) +"/app_imageDir";  }
    public static String getERM_ExternalAppRootDirectory (Context context) { return getApp_RootDirectory(context) +"/files/ERCM/TransactionExternalApp";  }
    public static String getERM_UnsettleExternalStorageDirectory () {
        if (Environment.getExternalStorageDirectory().getAbsolutePath()==null) return null;
        String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PAX/BPSLoader/ERCM/UnsettlementList";
        verifyErcmUnsettleStorage(Path);
        return Path;
    }

    public static String getERM_ExternalAppUploadDicrectory(Context context, Acquirer acquirer) {
        String rootAppDir = getERM_ExternalAppRootDirectory(context)  ;
        if (rootAppDir==null) return null;
        String Path = rootAppDir + File.separator + acquirer.getName();
        verifyPath(Path);
        return Path;
    }

    public static void verifyPath(String ExternalPath) {
        if (ExternalPath==null) {return;}
        String   path = "" ;
        String[] subDirectories = ExternalPath.split("/");
        File targFile;
        try {
            for (String subDir : subDirectories) {
                path += File.separator + subDir;
                targFile = new File(path);
                if(!targFile.exists()) {
                    Log.d(TAG, " create missing sub directory : " + path );
                    targFile.mkdir();
                }
            }
            //Log.d(TAG, " directory : " + path  + "    ---> is OK");
        } catch  (Exception ex) {
            Log.e(TAG, " error on action with directory : " + path );
        }
    }

    public static void verifyErcmUnsettleStorage (String ExternalPath) {
        if (ExternalPath==null) {return;}
        String   path = "" ;
        String[] subDirectories = ExternalPath.split("/");
        File targFile;
        try {
            for (String subDir : subDirectories) {
                path += File.separator + subDir;
                targFile = new File(path);
                if(!targFile.exists()) {
                    Log.d(TAG, " create missing sub directory : " + path );
                    targFile.mkdir();
                }
            }
            //Log.d(TAG, " directory : " + path  + "    ---> is OK");
        } catch  (Exception ex) {
            Log.e(TAG, " error on action with directory : " + path );
        }
    }





    private static EReceiptUtils instance = null;
    public static EReceiptUtils getInstance() {
        if (instance == null) {
            instance = new EReceiptUtils();
        }
        return instance;
    }

    public enum KEK_TYPES {
        _01,  // 01 = RSA KEY SIZE 1024
        _02   // 02 = RSA KEY SIZE 2048
    }
    private enum GetKekMethod{
        _01,    // Get KEK_TYPES
        _02     // Get KEK_LENGTH
    }
    private String  getKekTypes (KEK_TYPES Curr_kekType ) {
        if (Curr_kekType==KEK_TYPES._01) {
            return "1";
        }
        else if (Curr_kekType==KEK_TYPES._02) {
            return "2";
        }
        else {
            return null;
        }
    }
    private int getKekLength (KEK_TYPES Curr_kekType ) {
        if (Curr_kekType==KEK_TYPES._01) {
            return 179;
        }
        else if (Curr_kekType==KEK_TYPES._02) {
            return 307;
        }
        else {
            return 0;
        }
    }
    public byte[] ReProduceSessionKeyBlock(boolean autoIncreaseKekVersion) {
        if(getConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_KCV) == null
                && getConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ENC_DATA) == null
                && getConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION) == null
                && getConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_TYPE) == null
                && getConfig(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE) == null
                && getConfig(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER) == null
        )
        {
            return null;
        }

        String KEK_TYPE = getConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_TYPE);
        String KEK_VERSION_STR = "" ;
        if ((getConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION) != null) && (autoIncreaseKekVersion)) {
            KEK_VERSION_STR =  String.format("%4d",(Integer.parseInt(getConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION)) + 1)).replace(" ","0") ;
        }

        // set SessionKeyBlock (SKB)
        String SKB_str = "ETLE" ;
        SKB_str += "01" ;
        SKB_str += RightPadding(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE)            ,20," ") ;
        SKB_str += RightPadding(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER),15,"0") ;
        SKB_str += KEK_TYPE ;
        SKB_str += KEK_VERSION_STR ;

        byte[] header_byte              = SKB_str.getBytes();
        byte[] session_key_kcv          = Tools.str2Bcd2(getConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_KCV));
        byte[] encrypt_session_key      = Tools.str2Bcd2(getConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ENC_DATA));

        int writePos = 0 ;
        byte[] bOutput = new byte[header_byte.length + session_key_kcv.length + encrypt_session_key.length ];
        System.arraycopy(header_byte,             0, bOutput, writePos,  header_byte.length);           writePos += header_byte.length;
        System.arraycopy(session_key_kcv,         0, bOutput, writePos,  session_key_kcv.length);       writePos += session_key_kcv.length;
        System.arraycopy(encrypt_session_key,     0, bOutput, writePos,  encrypt_session_key.length);   writePos += session_key_kcv.length;

        return bOutput;
    }

    public String LastWriteSessionKeyBlockFile = null;
    public HashMap<String, Object> CreateSessionKeyBlock(@NonNull TransData transData, Context context) {
        HashMap<String, Object> hashmap = new HashMap<String, Object>();
        byte[] PB_MOD       = transData.getPublickeyModulus();
        byte[] PB_EXP       = transData.getPublickeyExponent();
        byte[] PB_HSH       = transData.getPublickeyHash();
        byte[] SK_CLEAR_VAL = null;
        byte[] SK_KCV       = null;
        byte[] SK_ENC_VALUE = null;
        byte[] SKB          = null;

        HashMap<String, byte[]> localHash = EReceiptUtils.getInstance().GenerateSessionKey(PB_MOD, PB_EXP);
        //get value from hash map
        SK_CLEAR_VAL = localHash.get("SESSION_KEY_CLEAR_VALUE");
        SK_KCV       = localHash.get("SESSION_KEY_KCV");
        SK_ENC_VALUE = localHash.get("SESSION_KEY_ENCRYPT_VALUE");

        // saveConfig to sharedPreference
        SaveConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ORI_DATA, Tools.bcd2Str(SK_CLEAR_VAL));
        SaveConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_KCV, Tools.bcd2Str(SK_KCV));
        SaveConfig(SysParam.StringParam.VERIFONE_ERCM_SESSION_KEY_ENC_DATA, Tools.bcd2Str(SK_ENC_VALUE));

        //build session key block
        HashMap<String, Object> BuildSSKResult = EReceiptUtils.getInstance().BuildSessionKeyBlock(SK_KCV, SK_ENC_VALUE);

        hashmap.put("SKB", BuildSSKResult.get("SKB"));            // Raw sessionkeyblock
        hashmap.put("SSK_TXT", SK_CLEAR_VAL);
        hashmap.put("SSK_KCV", SK_KCV);
        hashmap.put("SSK_ENC", SK_ENC_VALUE);
        hashmap.put("TLE_INDICATOR", BuildSSKResult.get("TLE_INDICATOR"));
        hashmap.put("ERC_VER", BuildSSKResult.get("ERC_VER"));
        hashmap.put("BANK_CODE", BuildSSKResult.get("BANK_CODE"));
        hashmap.put("TSN", BuildSSKResult.get("TSN"));
        hashmap.put("KEK_TYPE", BuildSSKResult.get("KEK_TYPE"));
        hashmap.put("KEK_VERSION", BuildSSKResult.get("KEK_VERSION"));

        return  hashmap;
    }

    public String WriteSessionKeyDataToFile(Context context, TransData transData, byte[] data2Write) {
        String path = getERM_LogoDirectory(context).replace("imageDir","sskDir");
        File objFile = new File(path);
        if(objFile.exists() == false) {objFile.mkdir();}
        if (transData.getInitAcquirerName() != null
                && transData.getInitAcquirerNii() != null) {
            String sskFileName = File.separator + transData.getInitAcquirerNii() + "_" + transData.getInitAcquirerName()+".ssk";
            path += sskFileName;
            objFile = new File(path );
            if (objFile.exists() == true) {objFile.delete();}
            try {
                objFile.createNewFile();
                FileWriter fWriter = new FileWriter(path);
                fWriter.write(Tools.bcd2Str(data2Write));
                fWriter.close();
            } catch (IOException ex) {
                path= null;
                Log.i(EReceiptUtils.TAG, ex.getMessage());
            }
        } else {
            path= null;
        }
        return path;
    }

    public String GetSerialNumber(String TSN) {
        String serialNo = TSN;
        int max_len = 15 ;
        if (serialNo.length() < max_len) {
            if (EReceiptUtils.PACK_TESTMODE ==true ){
                String prefixTSN = "EX-" ;
                int diff_len = max_len - serialNo.length() - prefixTSN.length();
                serialNo = prefixTSN + EReceiptUtils.StringPadding(serialNo, serialNo.length() + diff_len, Tools.bytes2String(new byte[] {0x30}), Convert.EPaddingPosition.PADDING_LEFT);
            } else {
                serialNo = EReceiptUtils.StringPadding(serialNo, max_len, "0", Convert.EPaddingPosition.PADDING_RIGHT);
            }

        }

        return serialNo;
    }
    public HashMap<String, Object> BuildSessionKeyBlock( byte[] session_key_kcv,
                                        byte[] encrypt_session_key) {
        HashMap<String, Object> hashmap = new HashMap<String, Object>();
        if(session_key_kcv == null && encrypt_session_key == null ) return null;

        // set KEK_TYPE
        String KEK_TYPE = null;
        if (encrypt_session_key.length == 128) {KEK_TYPE ="1";} else if (encrypt_session_key.length == 256) {KEK_TYPE ="2";}
        SaveConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_TYPE, KEK_TYPE);

        // set  KEK_VERSION
        //if(KekVersion=="INIT_TRANS")  {KekVersion = "0001";}
        //SaveConfig(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION, KekVersion);
        String Indicator  = "ETLE";
        String ERCVersion = "01";
        String BankCode   = RightPadding(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_BANK_CODE) ,           20, Tools.bytes2String(new byte[] {0x00}) ) ;
        String TSN        = GetSerialNumber(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER));//RightPadding(FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_TERMINAL_SERIALNUMBER),15,"0") ;
        String KekVersion = FinancialApplication.getSysParam().get(SysParam.StringParam.VERIFONE_ERCM_KEK_VERSION);

        // set SessionKeyBlock (SKB)
        String SKB_str = Indicator ;
        SKB_str += ERCVersion ;
        SKB_str += BankCode;
        SKB_str += TSN;
        SKB_str += KEK_TYPE ;
        SKB_str += KekVersion ;

        byte[] header_byte = SKB_str.getBytes();
        //byte[] total_byte  = String.valueOf(getKekLength(KekType)).getBytes();

        int writePos = 0 ;
        byte[] bOutput = new byte[header_byte.length + session_key_kcv.length + encrypt_session_key.length ];
        System.arraycopy(header_byte,             0, bOutput, writePos,  header_byte.length);           writePos += header_byte.length;
        System.arraycopy(session_key_kcv,         0, bOutput, writePos,  session_key_kcv.length);       writePos += session_key_kcv.length;
        System.arraycopy(encrypt_session_key,     0, bOutput, writePos,  encrypt_session_key.length);   writePos += session_key_kcv.length;

        hashmap.put("TLE_INDICATOR", Indicator);
        hashmap.put("ERC_VER", ERCVersion);
        hashmap.put("BANK_CODE", BankCode);
        hashmap.put("TSN", TSN);
        hashmap.put("KEK_TYPE", KEK_TYPE);
        hashmap.put("KEK_VERSION", KekVersion);
        hashmap.put("SKB", bOutput);

        return hashmap;
    }
    private static String RightPadding (String exData, int maxLen, String PaddStr) {
        //return String.format("%1$" + maxLen + "s",exData);
        return StringPadding(exData,maxLen,PaddStr, Convert.EPaddingPosition.PADDING_RIGHT);
    }

    private static void SaveConfig(SysParam.StringParam Type,String newValue) {
        FinancialApplication.getSysParam().set(Type,newValue);
    }
    private static String getConfig(SysParam.StringParam Type) {
        return FinancialApplication.getSysParam().get(Type);
    }


    public HashMap<String,byte[]> GenerateSessionKey(byte[] kek_rsa_modulus, byte[] kek_rsa_exponent) {
        HashMap<String,byte[]> hash = new HashMap<String,byte[]>();
        byte[] session_key_clear_value = GenerateTDesKey();
        byte[] session_key_kcv = ComputeTDesKcv(session_key_clear_value,3);
        byte[] session_key_encrypted_value_temp = new byte[1024];

        int modulus_len = kek_rsa_modulus.length;
        int exponent_len = kek_rsa_exponent.length;
        DeviceImplNeptune.getInstance().rsaRecover(kek_rsa_modulus, modulus_len, kek_rsa_exponent, exponent_len, session_key_clear_value, session_key_encrypted_value_temp);


        int eoa = EOA(session_key_encrypted_value_temp) ;
        byte[] session_key_encrypted_value = new byte[eoa];
        System.arraycopy(session_key_encrypted_value_temp,0,session_key_encrypted_value,0, eoa);


        hash.put("SESSION_KEY_CLEAR_VALUE",  session_key_clear_value);
        hash.put("SESSION_KEY_ENCRYPT_VALUE",session_key_encrypted_value);
        hash.put("SESSION_KEY_KCV",          session_key_kcv);
        return hash;
    }

    private byte[] GenerateTDesKey(){
        String RandomStr = UUID.randomUUID().toString().replace("-","").replace(" ","");
        byte[] TDesKey = DirectCast(RandomStr);
        //byte[] TDesKey = new byte[] {0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11};

        return TDesKey;
    }
    public byte[] DirectCast(String hexNibbleStr) {
        byte[] retByte =new byte[0];
        if (hexNibbleStr != null) {
            retByte = new byte[(hexNibbleStr.length()/2)];
            int beginPoS = 0 ;
            for (int idx = 0 ; idx <= retByte.length -1 ;idx++) {
                beginPoS = idx * 2;
                retByte[idx] = (byte) HexValue(hexNibbleStr.substring(beginPoS,beginPoS+2));
            }
        }
        return retByte;
    }

    private int HexValue (String StrNibble) {
        int valStr = 0;
        if (StrNibble.length() ==2) {
            valStr = Integer.parseInt(StrNibble,16);
            return valStr ;
        } else {
            return 0;
        }
    }

    private byte[] ComputeTDesKcv (byte[] key, int kcv_len) {
        byte[] data_in  = new byte[] {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        byte[] data_out = null ;
        byte[] bKcv = new byte[3];
        data_out = tDesEncrypt(data_in,key);
        if(data_out != null) {
            System.arraycopy(data_out,0,bKcv,0,kcv_len);
            return bKcv;
        } else {
            return null;
        }
    }

    private byte[] tDesEncrypt (String message, String keys)  {
        byte[] cipherText = null;
        try {
            cipherText = tDesEncrypt(message.getBytes("utf-8"), keys.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    private byte[] tDesEncrypt (byte[] message,byte[] keys)  {
        byte[] cipherText = null;
        try {
            final SecretKey key = new SecretKeySpec(keys,"DESede");
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            final Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,iv);

            final byte[] plainTextBytes = message;
            cipherText = cipher.doFinal(plainTextBytes);
        } catch (Exception ex) {
            cipherText = null;
        }

        return cipherText;
    }


    private byte[] tDesDecrypt (byte[] message, byte[] keys) {
        byte[] plainText = null;
        try {
            final SecretKey key = new SecretKeySpec(keys,"DESede");
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            final Cipher decipher = Cipher.getInstance("DESede/CBC/NoPadding");
            decipher.init(Cipher.DECRYPT_MODE,key,iv);

            plainText = decipher.doFinal(message);
        } catch (Exception ex) {
            plainText = null;
        }

        return plainText;
    }

    //End of Array
    private int EOA(byte[] SrcArr) {
        int idEOA = -1;
        boolean foundNonZero =false;
        for (int i =0 ; i <= SrcArr.length-1 ; i++) {
            if (SrcArr[i] == 0) {
                if (foundNonZero) {
                    idEOA = i ;
                    foundNonZero=false;
                }
            } else {
                foundNonZero=true;
            }
        }

        return idEOA;
    }

    private static boolean externalPbkFileExists = false;

    public static boolean isFoundKbankPublicKeyFile(){
        detectEReceiptPBKFile();
        return externalPbkFileExists;
    }
    public static HashMap<String,String> detectEReceiptPBKFile() {
        HashMap<String,String> retHash = new HashMap<String,String>();

        try {

            String rootLogoFolder = EReceiptUtils.getERM_LogoDirectory(FinancialApplication.getApp().getApplicationContext());
            String publicKeyFname = "kbnk_ercm_pbk.dat";
            File   publicKeyFile  = new File(rootLogoFolder + File.separator + publicKeyFname);
            InputStream is =null ;
            if (publicKeyFile != null){
                if (publicKeyFile.exists()) {
                    Log.d(EReceiptUtils.TAG," >> USE EXTERNAL PBK-FILE FROM PAX-STORE");
                    externalPbkFileExists=true;
                    is = new FileInputStream(publicKeyFile);
                }  else {
                    Log.d(EReceiptUtils.TAG," >> PBK-FILE MISSING-01");
                    externalPbkFileExists=false;
                    is = null ;
                    //is = FinancialApplication.getApp().getResources().getAssets().open(publicKeyFname);
                }
            } else {
                Log.d(EReceiptUtils.TAG," >> PBK-FILE MISSING-02");
                externalPbkFileExists=false;
            }

            if(is !=null) {
                Scanner scanner = new Scanner(is);
                while (scanner.hasNextLine()) {
                    String tmp_readline = scanner.nextLine();

                    if (tmp_readline.trim().length() > 0) {
                        String[] lineSplitter = tmp_readline.split(":");
                        if (lineSplitter.length == 2) {
                            switch (lineSplitter[0].trim()){
//                                case ("BANK_CODE"):
//                                    retHash.put("BANK_CODE", lineSplitter[1]);
//                                    break;
//                                case ("STORE_CODE"):
//                                    if (lineSplitter[1].equals("{MID}")) {
//                                        retHash.put("STORE_CODE", FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_ERCM_KEY_MANAGEMENT_SERVICE).getMerchantId());
//                                    } else {
//                                        retHash.put("STORE_CODE", lineSplitter[1]);
//                                    }
//                                    break;
//                                case ("MERCHANT_CODE"):
//                                    retHash.put("MERCHANT_CODE", lineSplitter[1]);
//                                    break;
                                case ("KEK_VER") :
                                    retHash.put("KEY_VERSION", lineSplitter[1]);
                                    break;
                                case ("PBK_EXP") :
                                    retHash.put("EXPONENT", lineSplitter[1]);
                                    break;
                                case ("PBK_HSH") :
                                    retHash.put("HASH", lineSplitter[1]);
                                    break;
                                case ("PBK_MOD") :
                                    retHash.put("MODULUS", lineSplitter[1]);
                                    break;
                            }
                        }
                    }
                }
                scanner.close();
                is.close();
                Log.d(EReceiptUtils.TAG," >> SCANNER CLOSED");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(EReceiptUtils.TAG," >> PBK-FILE READ ERROR");
            return retHash;
        }
        Log.d(EReceiptUtils.TAG," >> HASHTABLE LEN = " + retHash.size());
        return retHash;
    }


    private HashMap<Integer, Boolean> binBitMap;
    public byte[] pack (TransData transData, final TransProcessListener listener) {
        byte[] SendData =new byte[0];
        if (transData.getAcquirer() != null) {
            SendData = MergeArray(SendData,createTPDU(transData));
            SendData = MergeArray(SendData,createHeader(transData));
            for (int idx=1 ; idx <= 63 ; idx++) {
                if (binBitMap.get(idx).equals(true)) {
                    switch (idx) {
                        case 3  : SendData = MergeArray(SendData,setDataBit_003(transData)); break;
                        case 24 : SendData = MergeArray(SendData,setDataBit_024(transData)); break;
                        case 46 : SendData = MergeArray(SendData,setDataBit_046(transData)); break;
                        case 55 : SendData = MergeArray(SendData,setDataBit_055(transData)); break;
                        case 56 : SendData = MergeArray(SendData,setDataBit_056(transData)); break;
                        case 57 : SendData = MergeArray(SendData,setDataBit_057(transData)); break;
                        case 61 : SendData = MergeArray(SendData,setDataBit_061(transData)); break;
                        case 62 : SendData = MergeArray(SendData,setDataBit_062(transData)); break;
                        case 63 : SendData = MergeArray(SendData,setDataBit_063(transData)); break;
                    }
                }
            }
        }
        return getSize(SendData) ;
    }

    public static String  StringPadding (String targStr, int maxLen,String PaddStr, Convert.EPaddingPosition paddingPosition) {
        String retuenStr = "" ;
        String tempReturnStr = "" ;
        if (targStr.length() < maxLen) {
            for (int idx = 0 ; idx <= (maxLen - targStr.length() -1) ; idx++ ) {
                retuenStr += PaddStr;
            }

            if (paddingPosition== Convert.EPaddingPosition.PADDING_LEFT) {
                tempReturnStr = retuenStr + targStr ;
            } else {
                tempReturnStr = targStr + retuenStr ;
            }
        } else {
            tempReturnStr = targStr;
        }

        return tempReturnStr;
    }
    public byte[] MergeArray( byte[] desArray, byte[] toBeMergeArray) {
        if (toBeMergeArray != null && desArray != null) {
            byte[] tmpArray = new byte[toBeMergeArray.length + desArray.length];
            if(desArray.length > 0) {
                System.arraycopy(desArray,       0, tmpArray,       0, desArray.length);
            }
            System.arraycopy(toBeMergeArray, 0, tmpArray, desArray.length, toBeMergeArray.length);
            return tmpArray;
        } else {
            return desArray;
        }
    }
    private byte[] createHeader(TransData transData) {
        if(transData.getTransType() != null) {
            return MergeArray(DirectCast(transData.getTransType().getMsgType()),createBitmap(transData));
        } else {
            return null;
        }
    }
    private byte[] createTPDU(TransData transData) {return DirectCast("60" + (((transData.getAcquirer().getNii().length()%2) != 0) ? "0" : "") +transData.getAcquirer().getNii() + "0000"); }
    private byte[] createBitmap (TransData transData) {
        ETransType type = transData.getTransType();
        return DirectCast(getBitmapTemplate(transData)) ;
    }
    private String getBitmapTemplate (TransData transData) {
        String rawBitmap = null;
        switch (transData.getTransType()) {
            case ERCEIPT_TERMINAL_REGISTRATION:
                String HexStr_DE61_DE64 = reConstrucHEXBitMap(((transData.getSessionKeyBlock() != null) ? true : false)
                                                            , ((transData.getERCMFooterImagePath() != null) ? true : false)
                                                            , ((transData.getERCMLogoImagePath() != null) ? true : false)
                                                            , false);
                rawBitmap = ("00 00 00 00 00 04 03 8" + HexStr_DE61_DE64).replace(" ",""); break;
            case ERCEIPT_SESSIONKEY_RENEWAL:    rawBitmap = "20 00 01 00 00 04 03 88".replace(" ",""); break;
        }
        binBitMap = BitmapExtractor(rawBitmap);
        return rawBitmap ;
    }
    private String reConstrucHEXBitMap(boolean Field_01, boolean Field_02, boolean Field_03, boolean Field_04) {
        return BinaryToHex(((Field_01==true) ? "1" : "0")
                +((Field_02==true) ? "1" : "0")
                +((Field_03==true) ? "1" : "0")
                +((Field_04==true) ? "1" : "0"));
    }
    private HashMap<Integer, Boolean> BitmapExtractor(String hexBitMap) {
        if (hexBitMap != null) {
            HashMap<Integer, Boolean> hashBitmap = new HashMap<Integer, Boolean>();
            String binBitmap;
            int currPos = 0 ;
            for (int hex_idx=1; hex_idx <= hexBitMap.length() ; hex_idx++) {
                binBitmap = BinaryMap(hexBitMap.substring(hex_idx-1, hex_idx));
                for (int bin_idx=1 ; bin_idx <= binBitmap.length() ; bin_idx++) {
                    currPos += 1;
                    hashBitmap.put(currPos, (binBitmap.substring(bin_idx-1,bin_idx).equals("1") ? true : false));
                }
            }
            return hashBitmap;
        }
        else {
            return null;
        }
    }
    private String BinaryMap(String hex) {
        switch (hex) {
            case "0" : return "0000";
            case "1" : return "0001";
            case "2" : return "0010";
            case "3" : return "0011";
            case "4" : return "0100";
            case "5" : return "0101";
            case "6" : return "0110";
            case "7" : return "0111";
            case "8" : return "1000";
            case "9" : return "1001";
            case "A" : return "1010";
            case "B" : return "1011";
            case "C" : return "1100";
            case "D" : return "1101";
            case "E" : return "1110";
            case "F" : return "1111";
            default : return null;
        }
    }
    private String BinaryToHex(String binStr) {
        switch (binStr) {
            case "0000" : return "0";
            case "0001" : return "1";
            case "0010" : return "2";
            case "0011" : return "3";
            case "0100" : return "4";
            case "0101" : return "5";
            case "0110" : return "6";
            case "0111" : return "7";
            case "1000" : return "8";
            case "1001" : return "9";
            case "1010" : return "A";
            case "1011" : return "B";
            case "1100" : return "C";
            case "1101" : return "D";
            case "1110" : return "E";
            case "1111" : return "F";
            default : return null;
        }
    }
    public byte[] getSize(byte[] srcArray) {
        if (srcArray != null) {
            if (srcArray.length  >0 ) {
                byte[] tmpArray = new byte[srcArray.length +2];
                String lenStr = String.valueOf(srcArray.length);
                lenStr = (((lenStr.length()%2) != 0) ? "0" : "") + lenStr;
                lenStr = ((lenStr.length() != 4) ? "00" : "") + lenStr;
                byte[] lenArray = DirectCast(lenStr);
                System.arraycopy(lenArray,0, tmpArray, 0 , lenArray.length);
                System.arraycopy(srcArray, 0, tmpArray, lenArray.length, srcArray.length );

                return tmpArray;
            }
        }

        return null;
    }
    public byte[] getSizeHexCount(byte[] srcArray, boolean HexCountMode) {
        if (srcArray != null) {
            if (srcArray.length  >0 ) {
                byte[] tmpArray = new byte[srcArray.length +2];
                String lenStr = String.valueOf(srcArray.length * 2);
                lenStr = (((lenStr.length()%2) != 0) ? "0" : "") + lenStr;
                lenStr = ((lenStr.length() != 4) ? "00" : "") + lenStr;
                byte[] lenArray = DirectCast(lenStr);
                System.arraycopy(lenArray,0, tmpArray, 0 , lenArray.length);
                System.arraycopy(srcArray, 0, tmpArray, lenArray.length, srcArray.length );

                return tmpArray;
            }
        }

        return null;
    }

    private byte[] setDataBit_003(TransData transData) { return DirectCast(transData.getTransType().getProcCode());}
    private byte[] setDataBit_024(TransData transData) { return DirectCast((((transData.getAcquirer().getNii().length()%2) != 0) ? "0" : "")+transData.getAcquirer().getNii());}
    private byte[] setDataBit_046(TransData transData) { return getSize(("EX-" + EReceiptUtils.StringPadding(transData.getERCMTerminalSerialNumber(),12, "0", Convert.EPaddingPosition.PADDING_LEFT)).getBytes());}
    private byte[] setDataBit_055(TransData transData) { return getSize(getSize(transData.getERCMBankCode().getBytes()));}
    private byte[] setDataBit_056(TransData transData) { return getSize(getSize(transData.getERCMMerchantCode().getBytes()));}
    private byte[] setDataBit_057(TransData transData) { return getSize(getSize(transData.getERCMStoreCode().getBytes()));}
    private byte[] setDataBit_061(TransData transData) { return getSize(getSize(transData.getSessionKeyBlock()));}
    private byte[] setDataBit_062(TransData transData) { return getSize(getSize(transData.getERCMFooterImagePath()));}
    private byte[] setDataBit_063(TransData transData) { return getSize(getSize(transData.getERCMLogoImagePath()));}

    public byte[] ReplaceBit63(byte[] SrcIso8583, byte[] ExtField63) {
        byte[] DataField63 = new byte[4];
        byte[] TmptField63 = new byte[] {0x00, 0x02, 0x00, (byte)0xFF};
        byte[] newSendData = new byte[0];
        byte[] newSize = CalculateNewLen(SrcIso8583,ExtField63);
        System.arraycopy(SrcIso8583,SrcIso8583.length-4, DataField63,0,4);
        if (Arrays.equals(DataField63,TmptField63)) {
            newSendData = new byte[SrcIso8583.length-4 + ExtField63.length];
            System.arraycopy(SrcIso8583, 0 , newSendData,0,SrcIso8583.length-4);
            System.arraycopy(ExtField63, 0 , newSendData,SrcIso8583.length-4,ExtField63.length);
            System.arraycopy(newSize, 0 , newSendData,0,2);
        }
        return newSendData;
    }

    public byte[] CalculateNewLen(byte[] SrcIso8583, byte[] ExtField63){
        byte[] len_ISObyte = new byte[] {0x00,0x00,SrcIso8583[0],SrcIso8583[1]};
        byte[] len_DE63byte = new byte[] {ExtField63[0],ExtField63[1]};
        int ISOLen = Tools.byteArray2Int(len_ISObyte,0);
        int DE63Len = Integer.parseInt(Tools.bcd2Str(len_DE63byte)) ;

        String HexStr = Integer.toHexString(ISOLen + DE63Len);
        if (HexStr.length() % 2 > 0 ) {  HexStr = "0" +HexStr; }
        return DirectCast(HexStr);
    }


    public enum BOL_Options{None, NormalSizeWidth, HalSizefWidth}
    public enum EOL_Options{None, AddNewLineFeed, AddCarriageReturn}
    public enum ConcatModes{MergeContentOnly, AddSpaceBetween2Content}
    public enum TextAlignment{None, Center}
    public String WrapContent(String Content_A, String Content_B, EReceiptUtils.ConcatModes mode, EReceiptUtils.BOL_Options bolOption, EReceiptUtils.EOL_Options eolOption, EReceiptUtils.TextAlignment alignment) {
        String returnStr = "";
        int maxPerline = ((bolOption == EReceiptUtils.BOL_Options.HalSizefWidth) ? 44 : 23) ;
        if (Content_A.length() + Content_B.length() <= maxPerline) {
            if (mode== EReceiptUtils.ConcatModes.MergeContentOnly) {
                returnStr =  WrapContent((Content_A + Content_B), bolOption,eolOption,alignment) ;
            } else if (mode== EReceiptUtils.ConcatModes.AddSpaceBetween2Content) {
                int diffLen = maxPerline - Content_A.length() -Content_B.length() ;
                returnStr =  WrapContent(EReceiptUtils.StringPadding(Content_A,Content_A.length()+diffLen, " ", Convert.EPaddingPosition.PADDING_RIGHT) + Content_B,bolOption,eolOption,alignment);
            }
        } else {
            returnStr =  WrapContent((Content_A + Content_B).substring(0,maxPerline), bolOption,eolOption,alignment) ;
        }
        return returnStr;
    }
    public String WrapContent(String Content, EReceiptUtils.BOL_Options bolOption, EReceiptUtils.EOL_Options eolOption, EReceiptUtils.TextAlignment alignment) {
        ByteArrayOutputStream bAOS =new ByteArrayOutputStream();
        int maxPerline = ((bolOption == EReceiptUtils.BOL_Options.HalSizefWidth) ? 44 : 23) ;
        String returnStr = "";
        if (Content.length() < maxPerline && alignment== EReceiptUtils.TextAlignment.Center) {
            int halfDiffLen = (maxPerline-Content.length())/2;
            Content = EReceiptUtils.StringPadding("", halfDiffLen," ", Convert.EPaddingPosition.PADDING_LEFT) + Content;
        }

        byte[] BOA = new byte[0];           // Extra add to BeginOfArray (BOA)
        byte[] DATA = ((Content.length() > 0) ? Content.getBytes() : new byte[0]);
        byte[] EOA = new byte[0];           // Extra add to EndOfArray (EOA)
        switch (bolOption) {
            case None :              BOA = new byte[0];              break;
            case NormalSizeWidth:    BOA = new byte[] {(byte)0xD7} ; break;
            case HalSizefWidth:      BOA = new byte[] {(byte)0xCE} ; break;
        }
        switch (eolOption) {
            case None :              EOA = new byte[0];              break;
            case AddNewLineFeed:     EOA = new byte[] {(byte)0x0A} ; break;
            case AddCarriageReturn:  EOA = new byte[] {(byte)0x0D} ; break;
        }
        try {
            if (BOA.length > 0) bAOS.write(BOA);
            bAOS.write(DATA);
            if (EOA.length > 0) bAOS.write(EOA);
        } catch (Exception ex) {

        }
        return Tools.bcd2Str(bAOS.toByteArray());
    }


    public String AutoAddSpacebetween2String (String Str1, String Str2, int TotalLen) {
        String returnStr = "";
        if (Str1.length() + Str2.length() < TotalLen) {
            int diffLen = TotalLen - Str1.length() -Str2.length() ;
            return EReceiptUtils.StringPadding(Str1, diffLen+Str1.length()," ", Convert.EPaddingPosition.PADDING_RIGHT)  + Str2 ;
        } else if (Str1.length() + Str2.length() == TotalLen) {
            return Str1 + Str2 ;
        } else if (Str1.length() + Str2.length() > TotalLen) {
            return Str1 + Str2 ;
        }

        return returnStr;
    }

    private int ERMDialogTimeout =5;
    private Context ERMCustomAlertContext =null;
    private int ERMCustomAlertDialog = CustomAlertDialog.NORMAL_TYPE;
    private String ERMErrorMessage=null;
    public void showMsgErmError(final Context exContext,int exAlertType, String exErmErrorMessage, int dialogTimeout){
        ERMCustomAlertContext = exContext;
        ERMErrorMessage =exErmErrorMessage;
        ERMCustomAlertDialog = exAlertType;
        ERMDialogTimeout =dialogTimeout;
        FinancialApplication.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CustomAlertDialog dialog = new CustomAlertDialog(ERMCustomAlertContext, ERMCustomAlertDialog);
                dialog.setConfirmClickListener(new CustomAlertDialog.OnCustomClickListener() {
                    @Override
                    public void onClick(CustomAlertDialog alertDialog) {
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ((Activity)ERMCustomAlertContext).finish();
                    }
                });
                dialog.setTimeout(ERMDialogTimeout);
                dialog.show();
                dialog.setNormalText(ERMErrorMessage);
                dialog.showCancelButton(false);
                dialog.showConfirmButton(true);
            }
        });
    }



    public static final int MAX23_CHAR_PER_LINE = 23;
    public static final int MAX41_CHAR_PER_LINE = 41;
    public static final int MAX42_CHAR_PER_LINE = 42;
    public static final int MAX43_CHAR_PER_LINE = 43;
    public static final int MAX44_CHAR_PER_LINE = 44;
    private static final String[] SEPARATORS = new String[] { ".", "-", " " };
    private static final String PLACEHOLDER = "#";
//    public static String[]  TextSplitter(String message) {
//        return TextSplitter(message,MAX_CHAR_PER_LINE);
//    }
    public static String[]  TextSplitter(String message, int maxCharcterPerLine) {
        try {
            return stringSplitter(message, maxCharcterPerLine);
        }
        catch (Exception ex) {
            return new String[]{null};
        }
    }
    public static String[] stringSplitter(String message, int maximumCharacterPerLine) throws Exception {
        if (message == null || message == "") {
            //throw new Exception("String is empty.");
            return new String[] {null};
        }

        if (message.trim().length() <= maximumCharacterPerLine) {
            return new String[] { message.trim() };
        }

        String temp = new String(message.trim());

        for (String separator: SEPARATORS) {
            temp = temp.replace(separator, PLACEHOLDER);
        }

        List<String> listOfStrings = new ArrayList<>();
        int index = temp.lastIndexOf(PLACEHOLDER, temp.length()-1);
        if (index >= 0) {
            while (index >= 0) {
                if (index < maximumCharacterPerLine) {
                    listOfStrings.add(message.substring(0, index+1));
                    listOfStrings.add(message.substring(index+1));

                    break;
                }
                else if (index == maximumCharacterPerLine) {
                    int nextIndex = temp.lastIndexOf(PLACEHOLDER, index - 1);
                    if (nextIndex == -1) {
                        listOfStrings.add(message.substring(0, index-1));
                        listOfStrings.add(message.substring(index-1));
                    }
                    else {
                        listOfStrings.add(message.substring(0, nextIndex+1));
                        listOfStrings.add(message.substring(nextIndex+1));
                    }
                    break;
                }
                index = temp.lastIndexOf(PLACEHOLDER, index - 1);
            }
        }
        else {
            index = maximumCharacterPerLine;
            listOfStrings.add(message.substring(0, index));
            listOfStrings.add(message.substring(index));
        }

        return listOfStrings.toArray(new String[0]);
    }



    public static void setUnSettleRawData(Acquirer acquirer, String RefNo, byte[] data) {
        if (EReceiptUtils.getERM_UnsettleExternalStorageDirectory() == null) {
            Log.e(EReceiptUtils.TAG, "       missing unsettlement storage directory");
            return;
        }
        String FileName = "";
        FileName += acquirer.getNii()+acquirer.getName();
        FileName += "-" ;
        FileName += ((acquirer.getMerchantId().length() != 15) ? "1" : "0") +  EReceiptUtils.StringPadding(String.valueOf(acquirer.getMerchantId().length()),2,"0", Convert.EPaddingPosition.PADDING_LEFT); ;
        FileName += EReceiptUtils.StringPadding(String.valueOf(acquirer.getId()),3,"0", Convert.EPaddingPosition.PADDING_LEFT);           // F-024
        FileName += RefNo;                                                                                                                                      // F-037
        FileName += acquirer.getTerminalId();                                                                                                                   // F-041
        FileName += acquirer.getMerchantId();                                                                                                                   // F-042
        FileName += EReceiptUtils.StringPadding(String.valueOf(acquirer.getCurrBatchNo()) ,6,"0", Convert.EPaddingPosition.PADDING_LEFT);       // F-060
        FileName += ".erm";

        Log.d(EReceiptUtils.TAG, "       TempSettleFileName = " + FileName);

        File file = new File(EReceiptUtils.getERM_UnsettleExternalStorageDirectory() + File.separator + FileName);
        if (new File(EReceiptUtils.getERM_UnsettleExternalStorageDirectory()).isDirectory()) {
            try {
                FileOutputStream fStream = new FileOutputStream(file);
                fStream.write(data);
                fStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {

                Log.d(EReceiptUtils.TAG, "              >> ERM-Settlement-Report : write file error filename = " + FileName);
                Log.d(EReceiptUtils.TAG, "              >> ERM-Settlement-Report : write file error path     = " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    public static void setExternalAppUploadRawData(Context context,String fileName, Acquirer acquirer, String RefNo, byte[] data) {
        setExternalAppUploadRawData(context, fileName, acquirer, RefNo, data, null);
    }
    public static void setExternalAppUploadRawData(Context context,String fileName, Acquirer acquirer, String RefNo, byte[] data, byte[] transInfo) {
        String path = EReceiptUtils.getERM_ExternalAppUploadDicrectory(context, acquirer);
        if (path == null) {
            Log.e(EReceiptUtils.TAG, "       missing transaction storage directory for external application upload");
            return;
        }

        if (data!=null) {
            String FileName = fileName + ".erm";
            File file = new File(path + File.separator + FileName);
            Log.d(EReceiptUtils.TAG, "       TempExternalAppUploadFileName = " + FileName);
            if (new File(path).isDirectory()) {
                try {
                    FileOutputStream fStream = new FileOutputStream(file);
                    fStream.write(data);
                    fStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {

                    Log.d(EReceiptUtils.TAG, "              >> ERM-ExternalAppUpload-Receipt : write file error filename = " + FileName);
                    Log.d(EReceiptUtils.TAG, "              >> ERM-ExternalAppUpload-Receipt : write file error path     = " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }

        if (transInfo!=null) {
            String FileName = fileName + ".irm";
            File file = new File(path + File.separator + FileName);
            Log.d(EReceiptUtils.TAG, "       TempExternalAppUploadFileName = " + FileName);
            if (new File(path).isDirectory()) {
                try {
                    FileOutputStream fStream = new FileOutputStream(file);
                    fStream.write(transInfo);
                    fStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {

                    Log.d(EReceiptUtils.TAG, "              >> ERM-ExternalAppUpload-Receipt : write file error filename = " + FileName);
                    Log.d(EReceiptUtils.TAG, "              >> ERM-ExternalAppUpload-Receipt : write file error path     = " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] getUnSettleRawData(String FileName) {
        File file = new File(EReceiptUtils.getERM_UnsettleExternalStorageDirectory() + File.separator + FileName);
        if (new File(EReceiptUtils.getERM_UnsettleExternalStorageDirectory()).isDirectory()) {
            try {
                byte[] buffer = new byte[(int)file.length()];
                FileInputStream fStream = new FileInputStream(file);
                fStream.read(buffer,0,buffer.length);
                fStream.close();

                return buffer;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean isHostInitialtoERM(int acquirerIndex) {
        return isHostInitialtoERM(String.valueOf(acquirerIndex));
    }
    public static boolean isHostInitialtoERM(TransData transData) {
        boolean hasInitTerminalToERM = false;
        try{
            if(transData.getAcquirer() != null) {
                hasInitTerminalToERM = isHostInitialtoERM(String.valueOf(transData.getAcquirer().getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasInitTerminalToERM=false;
        }
        return false;
    }

    public static boolean isHostInitialtoERM(String acquirerIndex) {
        return (FinancialApplication.getEReceiptDataDbHelper().FindErmInitatedHostcount(acquirerIndex) > 0);
    }
}










