package th.co.bkkps.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.Math.pow;

public class DynamicOffline {
    private String TAG = "DYNAMIC-OFFLINE-CLASS";
    private static DynamicOffline instance;

    private static final String DynamicOfflineParamFileName = "DOLParam.json";
    private static final String keyString = Utils.getString(R.string.app_name);
    private int sessionTimeoutMin = 30 ;                        // 30 minute(s)
    private int sessionTimeoutSec = sessionTimeoutMin * 60 ;    // 1800 second(s)
    private int activatedTime = 0;
    private String currentPwd = null;
    private long amountFloorLimit_vsc =150000;                    // Default VisaCard   equal 1500 THB
    private long amountFloorLimit_mcc =150000;                    // Default Mastercard equal 1500 THB
    private long amountFloorLimit_jcb =150000;                    // Default JcbCard    equal 1500 THB

    public void setVisaCardFloorlimit       (long exFloorLimitAmout)    { amountFloorLimit_vsc = exFloorLimitAmout; }
    public void setMastercardFloorlimit     (long exFloorLimitAmout)    { amountFloorLimit_mcc = exFloorLimitAmout; }
    public void setJcbCardFloorlimit        (long exFloorLimitAmout)    { amountFloorLimit_jcb = exFloorLimitAmout; }
    public long getVisaCardFloorlimit       ()                          { return amountFloorLimit_vsc; }
    public long getMastercardFloorlimit     ()                          { return amountFloorLimit_mcc; }
    public long getJcbCardFloorlimit        ()                          { return amountFloorLimit_jcb; }

    public void setSessionTimeout           (int exTimeout)             { sessionTimeoutMin=exTimeout; }
    public int  getSessionTimeout           ()                          { return sessionTimeoutMin; }

    public synchronized static DynamicOffline getInstance() {
        if (instance == null) {
            instance = new DynamicOffline();
        }
        return instance;
    }

    public void loadParam () { this.getDynamicOfflineParam(); }
    private void getDynamicOfflineParam(){
        String path = FinancialApplication.getApp().getFilesDir() + File.separator + DynamicOfflineParamFileName;
        File f = new File(path);
        if(f.exists() && !f.isDirectory()) {

            ObjectMapper mapper = new ObjectMapper();
            try {
                // Convert JSON string from file to Object
                DynamicOffline dynamicOffline = mapper.readValue(new File(path), DynamicOffline.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void RemoveDynamicOfflineParam(){

    }



    public void SaveDynamicOfflineParam(){
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Convert object to JSON string and save into a file directly
            String path = FinancialApplication.getApp().getFilesDir() + File.separator;
            mapper.writeValue(new File(path + DynamicOfflineParamFileName), this);

            // Convert object to JSON string
            String jsonInString = mapper.writeValueAsString(this);
            System.out.println(jsonInString);

            // Convert object to JSON string and pretty print
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            System.out.println(jsonInString);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean VerifyInputPassword(String inputPassword){
        try {
            if(inputPassword.equals(getCurrentPwd())) {
                long unixTime = System.currentTimeMillis();
                int offsetFromUtc = TimeZone.getDefault().getOffset(unixTime);

                unixTime = (unixTime + offsetFromUtc) / 1000L;

                activatedTime=(int)unixTime;
                currentPwd=inputPassword;

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean isDynamicOfflineActiveStatus(){
        try {
            long unixTime = System.currentTimeMillis();
            int offsetFromUtc = TimeZone.getDefault().getOffset(unixTime);
            unixTime = (unixTime + offsetFromUtc) / 1000L;

            if (currentPwd != null
                    && activatedTime != 0
                    && (unixTime - activatedTime) < sessionTimeoutSec
                    && currentPwd.equals(getCurrentPwd())) {
                return true;
            } else {
                // Reset when password expired only
                currentPwd = null;
                activatedTime = 0;
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isDynamicOfflineActive(String exIssuerBrand, Long exAmount){
        try {
            long unixTime = System.currentTimeMillis();
            int offsetFromUtc = TimeZone.getDefault().getOffset(unixTime);
            unixTime = (unixTime + offsetFromUtc) / 1000L;

            if (currentPwd != null
                    && activatedTime != 0
                    && (unixTime - activatedTime) < sessionTimeoutSec
                    && currentPwd.equals(getCurrentPwd())) {
                return (exIssuerBrand.equals(Constants.ISSUER_JCB) && exAmount <= amountFloorLimit_jcb)
                        || (exIssuerBrand.equals(Constants.ISSUER_VISA) && exAmount <= amountFloorLimit_vsc)
                        || (exIssuerBrand.equals(Constants.ISSUER_MASTER) && exAmount <= amountFloorLimit_mcc);
            } else {
                // Reset when password expired only
                currentPwd = null;
                activatedTime = 0;
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getCurrentPwd() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        long unixTime = System.currentTimeMillis();
        int offsetFromUtc = TimeZone.getDefault().getOffset(unixTime);

        unixTime = (unixTime + offsetFromUtc) / 1000L / 86400; // 86400 = 24 hr

        String strUnixTime = String.valueOf(unixTime);
        byte[] macSHA1 = sha1(strUnixTime);
        long pwd = Truncate(macSHA1, 6);

        //String currPassword = Utils.StringPadding(String.valueOf(pwd),6,"0", Convert.EPaddingPosition.PADDING_LEFT);

        String currPassword = String.format("%06d", pwd);
        Log.d(TAG, " # [CURPWD.Length] current password length is " +  currPassword.length() + " digits");
        return currPassword ;
    }

    private byte[] sha1(String s) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(key);

        byte[] bytes = mac.doFinal(s.getBytes("UTF-8"));

        return bytes; //new String( Base64.encodeBase64(bytes));
    }

    private int DT(byte[] hmac_result) {
        int offset = hmac_result[19] & 0xf;
        int bin_code = (hmac_result[offset] & 0x7f) << 24
                | (hmac_result[offset + 1] & 0xff) << 16
                | (hmac_result[offset + 2] & 0xff) << 8
                | (hmac_result[offset + 3] & 0xff);
        return bin_code;
    }

    private long Truncate(byte[] hmac_result, int digits)
    {
        int Snum = DT(hmac_result);
        return (long)(Snum % pow(10, digits));
    }


    public void resetDynamicOffline(){
        activatedTime=0;
        currentPwd=null;
    }

}
