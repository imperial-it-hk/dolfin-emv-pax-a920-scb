package com.pax.pay.trans.component;

import th.co.bkkps.utils.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.EReceiptLogoMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.EReceiptDataDb;
import com.pax.pay.trans.model.AcqManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class KeyDataReadWriteJson {
    private static final String TAG = "KeyDataReadWriteJson";
    public static class KeyData{
        public String name;
        public int keyId;
        public String tmk;
        public String twk;
        public String up_tmk;
        public String up_twk;

        public KeyData() {

        }

        public KeyData(String name,int keyId,String tmk,String twk,String up_tmk,String up_twk){
            this.name = name;
            this.keyId = keyId;
            this.tmk = tmk;
            this.twk = twk;
            this.up_tmk = up_tmk;
            this.up_twk = up_twk;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getKeyId() { return keyId; }
        public void setKeyId(int keyId) { this.keyId = keyId; }
        public String getTmk() { return tmk; }
        public void setTmk(String tmk) { this.tmk = tmk; }
        public String getTwk() { return twk; }
        public void setTwk(String twk) { this.twk = twk; }
        public String getUp_tmk() { return up_tmk; }
        public void setUp_tmk(String up_tmk) { this.up_tmk = up_tmk; }
        public String getUp_twk() { return up_twk; }
        public void setUp_twk(String up_twk) { this.up_twk = up_twk; }
    }

    private static final String keyMappingDataJsonFileName = "keyIdMappingData.json";
    private static List<KeyData> getKeyMappingDataFromJsonFile(String fileName){
        List<KeyData> keyDataObj = null;
        String path = FinancialApplication.getApp().getFilesDir() + File.separator + fileName;
        File f = new File(path);

        if(f.exists() && !f.isDirectory()) {

            ObjectMapper mapper = new ObjectMapper();
            try {
                // Convert JSON string from file to Object
                keyDataObj = Arrays.asList(mapper.readValue(new File(path), KeyData[].class));
                Log.i(TAG,"getKeyMappingDataFromJsonFile path : " + path + ", size : " + keyDataObj.size()) ;
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return keyDataObj;
    }

    public static List<KeyData> readKeyMappingDataFile(String readFileName){
        return getKeyMappingDataFromJsonFile(readFileName);
    }

    private static void writeKeyMappingDataToJsonFile(String fileName,List<KeyData> keyDataObjs){
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            // Convert object to JSON string and save into a file directly
            String path = FinancialApplication.getApp().getFilesDir() + File.separator;
            Log.i(TAG,"writeKeyMappingDataToJsonFile path : " + path +", fileName : " + fileName + ", size : " + keyDataObjs.size()) ;
            mapper.writeValue(new File(path + fileName), keyDataObjs);

            /*// Convert object to JSON string
            String jsonInString = mapper.writeValueAsString(keyDataObjs);
            Log.e("menu","jsonInString1 : " + jsonInString);

            // Convert object to JSON string and pretty print
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(keyDataObjs);
            Log.e("menu","jsonInString2 : " + jsonInString);*/

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeKeyMappingDataFile(String saveFileName, List<KeyData> keyDataObjs){
        writeKeyMappingDataToJsonFile(saveFileName,keyDataObjs);
    }

    public static void saveKeyDataToFile(){
        // this process used to backup only TMK of KBANK Acquirer host
        // if cannot find KBANK Acquirer : EDC will skip backup key process
        AcqManager acqManager = AcqManager.getInstance();
        Acquirer kBankAcquirer = acqManager.findActiveAcquirer(Constants.ACQ_KBANK);
        List<Acquirer> acquirers = acqManager.findEnableAcquirersWithKeyData();
        if (kBankAcquirer != null
                && kBankAcquirer.isEnable()
                && kBankAcquirer.isEnableTle()
                && acquirers !=null
                && acquirers.size() > 0) {

            List<KeyData> keyDataObjs = new ArrayList<>();
            for (Acquirer acq : acquirers) {
                KeyData keyBackupObj = new KeyData();
                keyBackupObj.setName(acq.getName());
                keyBackupObj.setKeyId(kBankAcquirer.getKeyId() > 0 ? kBankAcquirer.getKeyId() : kBankAcquirer.getId());
                keyBackupObj.setTmk(kBankAcquirer.getTMK());
                keyBackupObj.setTwk(null);
                keyBackupObj.setUp_tmk(kBankAcquirer.getUP_TMK()!=null ? kBankAcquirer.getUP_TMK() : "");
                keyBackupObj.setUp_twk(kBankAcquirer.getUP_TWK()!=null ? kBankAcquirer.getUP_TWK() : "");
                keyDataObjs.add(keyBackupObj);
            }
            writeKeyMappingDataFile(keyMappingDataJsonFileName, keyDataObjs);
        }
        acqManager.clearInstance();
    }

    public static void updateKeyDataToAcquirers(){
        List<KeyData> keyDataObj = readKeyMappingDataFile(keyMappingDataJsonFileName);
        KeyData KBankHostKeyData = null;
        if (keyDataObj != null && keyDataObj.size() > 0) {
            for (KeyData localData : keyDataObj) {
                if (localData.name.equals(Constants.ACQ_KBANK)) {
                    KBankHostKeyData = localData;
                    break;
                }
            }

            if (KBankHostKeyData != null ) {
                if (KBankHostKeyData.tmk !=null
                    && KBankHostKeyData.twk !=null
                    && !KBankHostKeyData.tmk.isEmpty()
                    && !KBankHostKeyData.twk.isEmpty()) {

                    List<Acquirer> acquirerList =  FinancialApplication.getAcqManager().findEnableAcquirer();
                    if (acquirerList!=null && acquirerList.size()>0) {
                        for (Acquirer localAcquirer : acquirerList) {
                            if (localAcquirer.isEnable() && localAcquirer.isEnableTle()) {
                                localAcquirer.setKeyId(KBankHostKeyData.keyId);
                                localAcquirer.setTMK(KBankHostKeyData.tmk);
                                localAcquirer.setTWK(KBankHostKeyData.twk);
                                localAcquirer.setUP_TMK(KBankHostKeyData.up_tmk);
                                localAcquirer.setUP_TWK(KBankHostKeyData.up_tmk);
                                FinancialApplication.getAcqManager().updateAcquirer(localAcquirer);
                            }
                        }
                    }

                }
            }
        }
//        List<KeyData> keyDataObj = readKeyMappingDataFile(keyMappingDataJsonFileName);
//        if(keyDataObj != null && keyDataObj.size() > 0){
//            for(KeyData keyData : keyDataObj){
//                if(keyData.getName() != null){
//                    Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(keyData.getName());
//                    if(acquirer != null){
//                        acquirer.setKeyId(keyData.getKeyId());
//                        acquirer.setTMK(keyData.getTmk());
//                        acquirer.setTWK(keyData.getTwk());
//                        acquirer.setUP_TMK(keyData.getUp_tmk()!=null && !keyData.getUp_tmk().isEmpty() ? keyData.getUp_tmk():null);
//                        acquirer.setUP_TWK(keyData.getUp_twk()!=null && !keyData.getUp_twk().isEmpty() ? keyData.getUp_twk():null);
//                        FinancialApplication.getAcqManager().updateAcquirer(acquirer);
//                    }
//                }
//            }
//        }
    }
}


