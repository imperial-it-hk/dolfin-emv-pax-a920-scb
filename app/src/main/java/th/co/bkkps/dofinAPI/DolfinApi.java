package th.co.bkkps.dofinAPI;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.widget.Toast;

import com.icg.pay.service.aidl.PayHelper;
import com.pax.pay.constant.Constants;

import org.json.JSONObject;

///import th.co.bkkps.utils.Log;

public class DolfinApi {
    private static final String TAG = DolfinApi.class.getSimpleName();

    private static final String SERVICE_ACTION = "com.icg.pay.SERVICE";
    private static final String PACKAGE_NAME = "com.icg.payment.dolfinpay";

    private static DolfinApi instance;
    private PayHelper payHelper = null;
    private boolean isDolfinServiceBinded = false;

    private String mResult;

    public synchronized static DolfinApi getInstance() {
        if (instance == null) {
            instance = new DolfinApi();
        }
        return instance;
    }

    private final ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            payHelper = PayHelper.Stub.asInterface(binder);
            ///Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            payHelper = null;
            ///Log.d(TAG, "onServiceDisconnected");
        }
    };

    public void bindService(Context mContext) {
        boolean install = isPackageInstalled(PACKAGE_NAME,mContext.getPackageManager());
        if(install) {
            Intent intent = new Intent();
            intent.setAction(SERVICE_ACTION);
            intent.setPackage(PACKAGE_NAME);

            if (payHelper == null) {
                isDolfinServiceBinded = mContext.bindService(intent, mServerConn, Service.BIND_AUTO_CREATE);
                if (isDolfinServiceBinded) {
                    Toast.makeText(mContext, "Dolfin Service connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "Dolfin Service disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String Sale(String mAmount,boolean isPrintQR) {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "SALE");
                json.put("orderNumber", "1");
                json.put("amount", mAmount);
                json.put("prnQrEnabled", isPrintQR);

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return this.mResult;
    }

    public String SaleCScanB(String mAmount,boolean isPrintQR) {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
        try {
            json.put("appId", "com.icg.vasdemo.dolfinpay");
            json.put("transType", "SALE_C_SCAN_B");
            json.put("orderNumber", "1");
            json.put("amount", mAmount);
            json.put("prnQrEnabled", isPrintQR);

            String result = payHelper.doTrans(json.toString());
            android.util.Log.d(TAG, "RESPONSE: " + result);
            SetResult(result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        return this.mResult;
    }


    //{"orderNumber":"111","origTraceNo":"141","appId":"com.icg.vasdemo.dolfinpay","transType":"VOID"}
    public String Void(String traceNo) {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "VOID");
                json.put("orderNumber", "1");
                json.put("origTraceNo", traceNo);

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"SETTLE"}
    public String Settle() {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "SETTLE");

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
                //result = new RespJsonParser().parse("SALE", result);
                //Log.d(TAG, "RESPONSE: " + result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    public String SetConfig(String merchName
                         , String merchId
                         , String termId
                         , String storeId
                         , String storeName
                         , String tpdu
                         , String nii
                         , String ip
                         , String port
                         , String connectTimeout
                         , String receiveTimeout
                         , String reconnectTimes
                         , String traceNo
                         , String sysTraceAuditNo
                         , String isEnableCScanB
                         , String cScanBDisplayQrTimeout
                         , String cScanBRetryTimes
                         , String cScanBDelayRetry
                         , String language
    ){
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "CONFIG");

                json.put("merchName", merchName);
                json.put("merchId", merchId);
                json.put("termId", termId);
                json.put("storeId", storeId);
                json.put("storeName", storeName);
                json.put("tpdu", tpdu);
                json.put("nii", nii);
                json.put("ip", ip);
                json.put("port", port);
                json.put("connectTimeout", connectTimeout);
                json.put("receiveTimeout", receiveTimeout);
                json.put("reconnectTimes", reconnectTimes);
                json.put("traceNo", traceNo);
                json.put("sysTraceAuditNo", sysTraceAuditNo);//STAN No.
                json.put("isEnableCScanB", isEnableCScanB);

                json.put("cScanBRetryTimes", cScanBRetryTimes);
                json.put("cScanBDisplayQrTimeout", cScanBDisplayQrTimeout);
                json.put("cScanBDelayRetry", cScanBDelayRetry);

                json.put("language", language);

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
//        }catch (Exception ex) {
//            ex.printStackTrace();
//        }


        return this.mResult;
    }

    public String GetConfig() {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "CONFIG");

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
//        }catch (Exception ex) {
//            ex.printStackTrace();
//        }


        return this.mResult;
    }

    //{"orderNumber":"111","origTraceNo":"141","appId":"com.icg.vasdemo.dolfinpay","transType":"REPRN_ANY"}
    public String PrintTranNo(String traceNo) {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "REPRN_ANY");
                json.put("orderNumber", "1");
                json.put("origTraceNo", traceNo);

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
                //result = new RespJsonParser().parse("SALE", result);
                //Log.d(TAG, "RESPONSE: " + result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"REPRN_SETTLE"}
    public String ReprintSettle() {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "REPRN_SETTLE");

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
                //result = new RespJsonParser().parse("SALE", result);
                //Log.d(TAG, "RESPONSE: " + result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"PRN_SUM"}
    public String PrintSummary() {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "PRN_SUM");

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
                //result = new RespJsonParser().parse("SALE", result);
                //Log.d(TAG, "RESPONSE: " + result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"PRN_REPORT"}
    public String PrintReport() {
        JSONObject json = new JSONObject();
        this.mResult = "";
//        Thread run = new Thread(() -> {
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
            try {
                json.put("appId", "com.icg.vasdemo.dolfinpay");
                json.put("transType", "PRN_REPORT");

                String result = payHelper.doTrans(json.toString());
                android.util.Log.d(TAG, "RESPONSE: " + result);
                SetResult(result);
                //result = new RespJsonParser().parse("SALE", result);
                //Log.d(TAG, "RESPONSE: " + result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//        run.start();
//        try {
//            run.join(70000);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        //mContext.startService(intent);

        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"CL_REVERSAL"}
    public String ClearReversal() {
        JSONObject json = new JSONObject();
        this.mResult = "";
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
        try {
            json.put("appId", "com.icg.vasdemo.dolfinpay");
            json.put("transType", "CL_REVERSAL");

            String result = payHelper.doTrans(json.toString());
            android.util.Log.d(TAG, "RESPONSE: " + result);
            SetResult(result);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.mResult;
    }

    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"CL_TRANS"}
    public String ClearTransaction() {
        JSONObject json = new JSONObject();
        this.mResult = "";
        if(payHelper == null){
            return Constants.DOLFIN_ERR;
        }
        try {
            json.put("appId", "com.icg.vasdemo.dolfinpay");
            json.put("transType", "CL_TRANS");

            String result = payHelper.doTrans(json.toString());
            android.util.Log.d(TAG, "RESPONSE: " + result);
            SetResult(result);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.mResult;
    }


    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"RVCODE"} show RV
    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"ADVICE"} void advice
    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"REPORT"} report
    //{"appId":"com.icg.vasdemo.dolfinpay","transType":"TRANS_QUERY"}
    //{"merchName":"test","merchId":"1111111","termId":"11111","storeId":"00001","tpdu":"1528000","nii":"124","ip":"121.121.121.121","port":"2002","connectTimeout":"30","receiveTimeout":"30","reconnectTimes":"2","traceNo":"1","sysTraceAuditNo":"3","appId":"com.icg.vasdemo.dolfinpay","transType":"CONFIG"}


    private void SetResult(String result){
        this.mResult = result;
    }

    public void stop(Context mContext) {
        //mContext.stopService(new Intent(mContext, ServiceRemote.class));
        //mContext.unbindService(mServerConn);
    }

    public boolean getDolfinServiceBinded() {return isDolfinServiceBinded;}
}
