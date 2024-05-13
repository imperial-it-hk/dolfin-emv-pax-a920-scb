package com.pax.eemv;

import android.util.Log;

import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.CandList;
import com.pax.eemv.entity.Capk;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.enums.ETransResult;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Converter;
import com.pax.eemv.utils.Tools;
import com.pax.jemv.clcommon.ACType;
import com.pax.jemv.clcommon.ByteArray;
import com.pax.jemv.clcommon.EMV_APPLIST;
import com.pax.jemv.clcommon.EMV_CAPK;
import com.pax.jemv.clcommon.RetCode;
import com.pax.jemv.device.DeviceManager;
import com.pax.jemv.emv.api.EMVApi;
import com.pax.jemv.emv.api.EMVCallback;
import com.pax.jemv.emv.model.EmvEXTMParam;
import com.pax.jemv.emv.model.EmvMCKParam;
import com.pax.jemv.emv.model.EmvParam;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class EmvImpl extends EmvBase implements IEmv {
    private static final String TAG = "EmvImpl";
    private EMVCallback emvCallback;
    private EmvTrans paxEmvTrans;
    private EmvParam emvParam;
    private EmvMCKParam mckParam;

    private boolean isOfflineTransSend = false;
    private boolean isNotAllowFullEmv = false;

    static {
        System.loadLibrary("F_PUBLIC_LIB_Android");
        System.loadLibrary("F_EMV_LIB_Android");
        System.loadLibrary("F_EMV_LIB_APIMAP_Android");
        System.loadLibrary("JniEMV_V1.00.00_20171114");
    }

    private class Callback implements EMVCallback.EmvCallbackListener {
        @Override
        public void emvWaitAppSel(int tryCnt, EMV_APPLIST[] list, int appNum) {
            CandList[] candLists = new CandList[list.length];
            int size = Math.min(list.length, appNum);
            for (int i = 0; i < size; ++i) {
                candLists[i] = Converter.toCandList(list[i]);
            }
            try {
                int idx = paxEmvTrans.waitAppSelect(tryCnt, candLists);
                if (idx >= 0)
                    emvCallback.setCallBackResult(idx);
                else {
                    emvCallback.setCallBackResult(RetCode.EMV_USER_CANCEL);
                }
            } catch (EmvException e) {
                Log.w(TAG, "", e);
                emvCallback.setCallBackResult(e.getErrCode());
            }
        }

        @Override
        public void emvInputAmount(long[] amt) {
            Amount amount = paxEmvTrans.getAmount();
            if (amount != null) {
                amt[0] = Long.parseLong(amount.getAmount());
                if (amt.length > 1) {
                    if (amount.getCashBackAmt() == null || amount.getCashBackAmt().isEmpty()) {
                        amt[1] = 0;
                    } else {
                        amt[1] = Long.parseLong(amount.getCashBackAmt());
                    }
                }
            }
            emvCallback.setCallBackResult(RetCode.EMV_OK);
        }

        @Override
        public void emvGetHolderPwd(int tryFlag, int remainCnt, byte[] pin) {
            if (pin == null) {
                Log.i("log", "emvGetHolderPwd pin is null, tryFlag" + tryFlag + " remainCnt:" + remainCnt);
            } else {
                Log.i("log", "emvGetHolderPwd pin is not null, tryFlag" + tryFlag + " remainCnt:" + remainCnt);
            }

            int result;
                try {
                    result = paxEmvTrans.cardHolderPwd(pin == null, remainCnt, pin);
                } catch (EmvException e) {
                    Log.w(TAG, "", e);
                    result = e.getErrCode();
                }

                emvCallback.setCallBackResult(result);
        }

        @Override
        public void emvAdviceProc() {
            //do nothing
        }

        @Override
        public void emvVerifyPINOK() {
            //do nothing
        }

        @Override
        public int emvUnknowTLVData(short tag, ByteArray data) {
            Log.i("EMV", "emvUnknowTLVData tag: " + Integer.toHexString(tag) + " data:" + data.data.length);
            switch ((int) tag) {
                case 0x9A:
                    byte[] date = new byte[7];
                    DeviceManager.getInstance().getTime(date);
                    System.arraycopy(date, 1, data.data, 0, 3);
                    break;
                case 0x9F1E:
                    byte[] sn = new byte[10];
                    DeviceManager.getInstance().readSN(sn);
                    System.arraycopy(sn, 0, data.data, 0, Math.min(data.data.length, sn.length));
                    break;
                case 0x9F21:
                    byte[] time = new byte[7];
                    DeviceManager.getInstance().getTime(time);
                    System.arraycopy(time, 4, data.data, 0, 3);
                    break;
                case 0x9F37:
                    byte[] random = new byte[4];
                    DeviceManager.getInstance().getRand(random, 4);
                    System.arraycopy(random, 0, data.data, 0, data.data.length);
                    break;
                case 0xFF01:
                    Arrays.fill(data.data, (byte) 0x00);
                    break;
                default:
                    return RetCode.EMV_PARAM_ERR;
            }
            data.length = data.data.length;
            return RetCode.EMV_OK;
        }

        @Override
        public void certVerify() {
            emvCallback.setCallBackResult(RetCode.EMV_OK);
        }

        @Override
        public int emvSetParam() {
            return RetCode.EMV_OK;
        }

        @Override
        public int cRFU1() {
            return 0;
        }

        @Override
        public int cRFU2() {
            return 0;
        }
    }

    public EmvImpl() {
        super();
        emvParam = new EmvParam();
        mckParam = new EmvMCKParam();
        mckParam.extmParam = new EmvEXTMParam();

        paxEmvTrans = new EmvTrans();
    }

    @Override
    public void init() throws EmvException {
        super.init();
        emvCallback = EMVCallback.getInstance();
        emvCallback.setCallbackListener(new Callback());
        int ret = EMVCallback.EMVCoreInit();
        if (ret == RetCode.EMV_OK) {
            EMVCallback.EMVSetCallback();
            EMVCallback.EMVGetParameter(emvParam);
            EMVCallback.EMVGetMCKParam(mckParam);
            paramToConfig();
            return;
        }

        throw new EmvException(EEmvExceptions.EMV_ERR_FILE);
    }

    private void paramToConfig() {
        cfg.setCapability(Tools.bcd2Str(emvParam.capability));
        cfg.setCountryCode(Tools.bcd2Str(emvParam.countryCode));
        cfg.setExCapability(Tools.bcd2Str(emvParam.exCapability));
        cfg.setForceOnline(Tools.byte2Boolean(emvParam.forceOnline));
        cfg.setGetDataPIN(Tools.byte2Boolean(emvParam.getDataPIN));
        cfg.setMerchCateCode(Tools.bcd2Str(emvParam.merchCateCode));
        cfg.setReferCurrCode(Tools.bcd2Str(emvParam.referCurrCode));
        cfg.setReferCurrCon(emvParam.referCurrCon);
        cfg.setReferCurrExp(emvParam.referCurrExp);
        cfg.setSurportPSESel(Tools.byte2Boolean(emvParam.surportPSESel));
        cfg.setTermType(emvParam.terminalType);
        cfg.setTransCurrCode(Tools.bcd2Str(emvParam.transCurrCode));
        cfg.setTransCurrExp(emvParam.transCurrExp);
        cfg.setTransType(emvParam.transType);
        cfg.setTermId(Arrays.toString(emvParam.termId));
        cfg.setMerchId(Arrays.toString(emvParam.merchId));
        cfg.setMerchName(Arrays.toString(emvParam.merchName));

        cfg.setBypassPin(Tools.byte2Boolean(mckParam.ucBypassPin));
        cfg.setBatchCapture(mckParam.ucBatchCapture);

        cfg.setTermAIP(Tools.bcd2Str(mckParam.extmParam.aucTermAIP));
        cfg.setBypassAllFlag(Tools.byte2Boolean(mckParam.extmParam.ucBypassAllFlg));
        cfg.setUseTermAIPFlag(Tools.byte2Boolean(mckParam.extmParam.ucUseTermAIPFlg));
    }

    private void configToParam() {
        emvParam.capability = Tools.str2Bcd(cfg.getCapability());
        emvParam.countryCode = Tools.str2Bcd(cfg.getCountryCode());
        emvParam.exCapability = Tools.str2Bcd(cfg.getExCapability());
        emvParam.forceOnline = Tools.boolean2Byte(cfg.getForceOnline());
        emvParam.getDataPIN = Tools.boolean2Byte(cfg.getGetDataPIN());
        emvParam.merchCateCode = Tools.str2Bcd(cfg.getMerchCateCode());
        emvParam.referCurrCode = Tools.str2Bcd(cfg.getReferCurrCode());
        emvParam.referCurrCon = cfg.getReferCurrCon();
        emvParam.referCurrExp = cfg.getReferCurrExp();
        emvParam.surportPSESel = Tools.boolean2Byte(cfg.getSurportPSESel());
        emvParam.terminalType = cfg.getTermType();
        emvParam.transCurrCode = Tools.str2Bcd(cfg.getTransCurrCode());
        emvParam.transCurrExp = cfg.getTransCurrExp();
        emvParam.transType = cfg.getTransType();
        emvParam.termId = cfg.getTermId().getBytes();
        emvParam.merchId = cfg.getMerchId().getBytes();
        emvParam.merchName = cfg.getMerchName().getBytes();

        mckParam.ucBypassPin = Tools.boolean2Byte(cfg.getBypassPin());
        mckParam.ucBatchCapture = cfg.getBatchCapture();

        mckParam.extmParam.aucTermAIP = Tools.str2Bcd(cfg.getTermAIP());
        mckParam.extmParam.ucBypassAllFlg = Tools.boolean2Byte(cfg.getBypassAllFlag());
        mckParam.extmParam.ucUseTermAIPFlg = Tools.boolean2Byte(cfg.getUseTermAIPFlag());
    }

    @Override
    public byte[] getTlvSub(int tag) {
        ByteArray byteArray = new ByteArray();
        if (EMVCallback.EMVGetTLVData((short) tag, byteArray) == RetCode.EMV_OK) {
            return Arrays.copyOfRange(byteArray.data, 0, byteArray.length);
        }
        return null;
    }

    @Override
    public void setTlvSub(int tag, byte[] value) throws EmvException {
        int length = 0;
        if (value != null) {
            length = value.length;
        }
        int ret = EMVCallback.EMVSetTLVData((short) tag, value, length);
        if (ret != EEmvExceptions.EMV_OK.getErrCodeFromBasement()) {
            throw new EmvException(ret);
        }
    }

    // Run callback
    // Parameter settings, loading aid,
    // select the application, application initialization,read application data, offline data authentication,
    // terminal risk management,cardholder authentication, terminal behavior analysis,
    // issuing bank data authentication, execution script
    @Override
    public CTransResult process(InputParam inputParam) throws EmvException {
        configToParam();
        EMVCallback.EMVSetParameter(emvParam);
        int ret = EMVCallback.EMVSetMCKParam(mckParam);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        EMVCallback.EMVSetPCIModeParam((byte) 1, "0,4,5,6,7,8,9,10,11,12".getBytes(), inputParam.getPciTimeout());

        for (AidParam i : aidParamList) {
            ret = EMVCallback.EMVAddApp(Converter.toEMVApp(i));
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }

        ret = EMVCallback.EMVAppSelect(0, Long.parseLong(inputParam.getTransStanNo()));   //callback emvWaitAppSel
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = EMVCallback.EMVReadAppData(); //callback emvInputAmount
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ByteArray pan = new ByteArray();
        EMVCallback.EMVGetTLVData((byte) 0x5A, pan);
        String filtPan = Tools.bcd2Str(pan.data, pan.length);
        int indexF = filtPan.indexOf('F');

        if (pan.length > 0 && pan.data != null) {
            ret = paxEmvTrans.confirmCardNo(filtPan.substring(0, indexF != -1 ? indexF : filtPan.length()));
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }

        if (paxEmvTrans.chkForceSettlement()) {
            // check force settlement after reading chip card
            // If settlement is not successfully, the transaction should be blocked.
            throw new EmvException(EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT);
        }

        if (isUnSupportedTrans()) {
            throw new EmvException(EEmvExceptions.EMV_ERR_UNSUPPORTED_TRANS);
        }

        if (isOfflineTransSend) {
            //do process enter auth code before enter pin
            ret = paxEmvTrans.processEnterAuthCode();
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }

        addCapkIntoEmvLib(); // ignore return value for some case which the card doesn't has the capk index

        configToParam();
        EMVCallback.EMVSetParameter(emvParam);

        ret = EMVCallback.EMVCardAuth();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        int var0 = 0;
        byte[] var1 = new byte[2];
        ret = EMVApi.EMVGetDebugInfo(var0, var1);
        Log.e("EMVGetDebugInfo", Integer.toString(ret));

        ACType acType = new ACType();
        ret = EMVCallback.EMVStartTrans(Long.parseLong(inputParam.getAmount()),
                Long.parseLong(inputParam.getCashBackAmount()), acType);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        // Normal Sale
        if (acType.type == ACType.AC_TC) {
            paxEmvTrans.updateOfflineTransDataFromKernel();
            return new CTransResult(ETransResult.OFFLINE_APPROVED);
        } else if (acType.type == ACType.AC_AAC) {
            return new CTransResult(ETransResult.OFFLINE_DENIED);
        }

        if (isNotAllowFullEmv) {
            if (isOfflineTransSend) {
                // Offline trans. via Offline menu.
                return new CTransResult(ETransResult.OFFLINE_APPROVED);
            }
            throw new EmvException(EEmvExceptions.EMV_NEED_MAG_ONLINE);
        }

        ETransResult result = onlineProc();

        //AET-146
        if (result != ETransResult.ONLINE_APPROVED &&
                result != ETransResult.ONLINE_DENIED &&
                result != ETransResult.ONLINE_FAILED) {
            if (isReversalFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_REVERSAL_FAIL);
            }
            if (isTleFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_TLE_FAIL);
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
        }

        byte[] script = combine7172(getTlv(0x71), getTlv(0x72));

        if (script == null){
            script = new byte[0];
        }else{
            setTlvSub(0x91, getTlv(0x91));
        }

        ret = EMVCallback.EMVCompleteTrans(Converter.toOnlineResult(result), script, script.length, acType);


        if (ret != RetCode.EMV_OK) {
            paxEmvTrans.setDe55ForReversal();
            ByteArray scriptResult = new ByteArray();
            EMVCallback.EMVGetScriptResult(scriptResult);
            if (isAmexErrorGotoSecondGen || isOfflineApprNeedChkReverse) {
                throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
            }
            throw new EmvException(ret);
        }else{
            ByteArray scriptResult = new ByteArray();
            int retScriptResult = EMVCallback.EMVGetScriptResult(scriptResult);
            if(retScriptResult == RetCode.EMV_OK){
                // Send Update ScriptResult Message.
                ETransResult resultScript = updateScriptResult();
                if (resultScript != ETransResult.ONLINE_APPROVED ) {
                    throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT);
                }
            }
        }

        if (acType.type == ACType.AC_TC) {
            if (result == ETransResult.ONLINE_FAILED) {
                if (isOfflineApprNeedChkReverse) {
                    // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                    return new CTransResult(ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                }
                return new CTransResult(ETransResult.OFFLINE_APPROVED);
            } else {

                return new CTransResult(ETransResult.ONLINE_APPROVED);
            }
        } else if (acType.type == ACType.AC_AAC) {
            return new CTransResult(ETransResult.ONLINE_CARD_DENIED);
        }

        ETransResult transResult = Tools.getEnum(ETransResult.class, ret - 1);
        if (transResult == null) {
            throw new EmvException(EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement());
        }
        return new CTransResult(transResult);
    }

    private static byte[] combine7172(byte[] f71, byte[] f72) {
        ByteBuffer byteBuffer;
        if (f71 == null || f71.length == 0) {
            if (f72 == null || f72.length == 0) {
                return f72;
            }else{
                byteBuffer =  set7172Bytebuffer(f72,0x72);
            }
            int len = byteBuffer.position();
            byteBuffer.position(0);

            byte[] script = new byte[len];
            byteBuffer.get(script, 0, len);

            return script;
        }
        if (f72 == null || f72.length == 0) {
            if (f71 == null || f71.length == 0) {
                return f71;
            }else{
                byteBuffer =  set7172Bytebuffer(f71,0x71);
            }
            int len = byteBuffer.position();
            byteBuffer.position(0);

            byte[] script = new byte[len];
            byteBuffer.get(script, 0, len);

            return script;
        }
        ByteBuffer bb = ByteBuffer.allocate(f71.length + f72.length + 6);

        bb.put((byte) 0x71);
        if (f71.length > 127)
            bb.put((byte) 0x81);
        bb.put((byte) f71.length);
        bb.put(f71, 0, f71.length);

        bb.put((byte) 0x72);
        if (f72.length > 127)
            bb.put((byte) 0x81);
        bb.put((byte) f72.length);
        bb.put(f72, 0, f72.length);

        int len = bb.position();
        bb.position(0);

        byte[] script = new byte[len];
        bb.get(script, 0, len);

        return script;
    }

    /**
     * The Backend might return
     * I. 71, 72,  combine 71 and 72, say T1L1V1T2L2V2, in which T1 is TAG71 and T2 is TAG 72
     * II. 71 only, return TLV, in wich T is TAG 71
     * III. 72 only, return TLV, in wich T is TAG 72
     * IV. no script, return new byte[0]
     *
     * @param f71 value of 71
     * @param f72 value of 72
     * @return
     */
    public static byte[] clssCombine7172(byte[] f71, byte[] f72) {

        boolean f71Empty = (null == f71 || f71.length <= 0);
        boolean f72Empty = (null == f72 || f72.length <= 0);

        if (f71Empty && f72Empty) {
            return new byte[0];
        }

        if (f71Empty && !f72Empty) {
            return createTLVByTV((byte) 0x72, f72);
        }

        if (!f71Empty && f72Empty) {
            return createTLVByTV((byte) 0x71, f71);
        }

        return mergeByteArrays(createTLVByTV((byte) 0x71, f71), createTLVByTV((byte) 0x72, f72));
    }

    private static byte[] mergeByteArrays(byte[] byteArr1, byte[] byteArr2) {
        if (null == byteArr1 || byteArr1.length <= 0) {
            return byteArr2;
        }
        if (null == byteArr2 || byteArr2.length <= 0) {
            return byteArr1;
        }
        byte[] result = Arrays.copyOf(byteArr1, byteArr1.length + byteArr2.length);
        System.arraycopy(byteArr2, 0, result, byteArr1.length, byteArr2.length);
        return result;
    }

    private static byte[] createTLVByTV(byte tag, byte[] value) {
        if (null == value || value.length <= 0) {
            return new byte[0];
        }
        ByteBuffer bb = ByteBuffer.allocate(value.length + 3);
        bb.put(tag);
        if (value.length > 127) {//need two bytes to indicate length
            bb.put((byte) 0x81);
        }
        bb.put((byte) value.length);
        bb.put(value, 0, value.length);

        int len = bb.position();
        bb.position(0);

        byte[] tlv = new byte[len];
        bb.get(tlv, 0, len);

        return tlv;
    }

//    public static byte[] combine917172(byte[] f91, byte[] f71, byte[] f72) {
//        if (f91 == null || f91.length == 0)
//            return combine7172(f71, f72);
//        if (f71 == null || f71.length == 0)
//            return f72;
//        if (f72 == null || f72.length == 0)
//            return f71;
//
//        ByteBuffer bb = ByteBuffer.allocate(f71.length + f72.length + 6);
//
//        bb.put((byte) 0x91);
//        bb.put((byte) f91.length); //fix 16
//        bb.put(f91, 0, f91.length);
//
//        byte[] f7172 = combine7172(f71, f72);
//        bb.put(f7172, 0, f7172.length);
//
//        int len = bb.position();
//        bb.position(0);
//
//        byte[] script = new byte[len];
//        bb.get(script, 0, len);
//
//        return script;
//    }

    @Override
    public void setListener(IEmvListener listener) {
        paxEmvTrans.setEmvListener(listener);
    }

    @Override
    public void readCardProcess(InputParam inputParam) throws EmvException {
        configToParam();
        EMVCallback.EMVSetParameter(emvParam);
        int ret = EMVCallback.EMVSetMCKParam(mckParam);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        EMVCallback.EMVSetPCIModeParam((byte) 1, "0,4,5,6,7,8,9,10,11,12".getBytes(), inputParam.getPciTimeout());

        for (AidParam i : aidParamList) {
            ret = EMVCallback.EMVAddApp(Converter.toEMVApp(i));
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }

        ret = EMVCallback.EMVAppSelect(0, Long.parseLong(inputParam.getTransStanNo()));   //callback emvWaitAppSel
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ret = EMVCallback.EMVReadAppData(); //callback emvInputAmount
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ByteArray pan = new ByteArray();
        EMVCallback.EMVGetTLVData((byte) 0x5A, pan);
        String filtPan = Tools.bcd2Str(pan.data, pan.length);
        int indexF = filtPan.indexOf('F');

        if (pan.length > 0 && pan.data != null) {
            ret = paxEmvTrans.confirmCardNo(filtPan.substring(0, indexF != -1 ? indexF : filtPan.length()));
            if (ret != RetCode.EMV_OK) {
                throw new EmvException(ret);
            }
        }

        if (paxEmvTrans.chkForceSettlement()) {
            // check force settlement after reading chip card
            // If settlement is not successfully, the transaction should be blocked.
            throw new EmvException(EEmvExceptions.EMV_ERR_FORCE_SETTLEMENT);
        }

        addCapkIntoEmvLib(); // ignore return value for some case which the card doesn't has the capk index

        configToParam();
        EMVCallback.EMVSetParameter(emvParam);

        ret = EMVCallback.EMVCardAuth();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }
    }

    @Override
    public CTransResult afterReadCardProcess(InputParam inputParam) throws EmvException {
        if (isUnSupportedTrans()) {
            throw new EmvException(EEmvExceptions.EMV_ERR_UNSUPPORTED_TRANS);
        }

        ACType acType = new ACType();
        int ret = EMVCallback.EMVStartTrans(Long.parseLong(inputParam.getAmount()),
                Long.parseLong(inputParam.getCashBackAmount()), acType);
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        if (acType.type == ACType.AC_TC) {
            paxEmvTrans.updateOfflineTransDataFromKernel();
            return new CTransResult(ETransResult.OFFLINE_APPROVED);
        } else if (acType.type == ACType.AC_AAC) {
            return new CTransResult(ETransResult.OFFLINE_DENIED);
        }

        if (paxEmvTrans.onChkIsDynamicOffline()) {
            return new CTransResult(ETransResult.OFFLINE_APPROVED);
        }

        if (isNotAllowFullEmv) {
            if (isOfflineTransSend) {
                // Offline trans. via Offline menu.
                return new CTransResult(ETransResult.OFFLINE_APPROVED);
            }
            throw new EmvException(EEmvExceptions.EMV_NEED_MAG_ONLINE);
        }

        ret = paxEmvTrans.processEnterRefNo();
        if (ret != RetCode.EMV_OK) {
            throw new EmvException(ret);
        }

        ETransResult result = onlineProc();

        //AET-146
        if (result != ETransResult.ONLINE_APPROVED &&
                result != ETransResult.ONLINE_DENIED &&
                result != ETransResult.ONLINE_FAILED) {
            if (isReversalFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_REVERSAL_FAIL);
            }
            if (isTleFail) {
                throw new EmvException(EEmvExceptions.EMV_ERR_TLE_FAIL);
            }
            throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
        }

        byte[] script = combine7172(getTlv(0x71), getTlv(0x72));

        if (script == null){
            script = new byte[0];
        }else{
            setTlvSub(0x91, getTlv(0x91));
        }

        ret = EMVCallback.EMVCompleteTrans(Converter.toOnlineResult(result), script, script.length, acType);


        if (ret != RetCode.EMV_OK) {
            paxEmvTrans.setDe55ForReversal();
            ByteArray scriptResult = new ByteArray();
            EMVCallback.EMVGetScriptResult(scriptResult);
            if (isAmexErrorGotoSecondGen || isOfflineApprNeedChkReverse) {
                throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT_NO_DIALOG);
            }
            throw new EmvException(ret);
        }else{
            ByteArray scriptResult = new ByteArray();
            int retScriptResult = EMVCallback.EMVGetScriptResult(scriptResult);
            if(retScriptResult == RetCode.EMV_OK){
                // Send Update ScriptResult Message.
                ETransResult resultScript = updateScriptResult();
                if (resultScript != ETransResult.ONLINE_APPROVED ) {
                    throw new EmvException(EEmvExceptions.EMV_ERR_ONLINE_TRANS_ABORT);
                }
            }
        }

        if (acType.type == ACType.AC_TC) {
            if (result == ETransResult.ONLINE_FAILED) {
                if (isOfflineApprNeedChkReverse) {
                    // Fixed EDCBBLAND-235 Modify auto reversal to support offline trans
                    return new CTransResult(ETransResult.OFFLINE_APPROVED_NEED_CHK_REVERSE);
                }
                return new CTransResult(ETransResult.OFFLINE_APPROVED);
            } else {

                return new CTransResult(ETransResult.ONLINE_APPROVED);
            }
        } else if (acType.type == ACType.AC_AAC) {
            return new CTransResult(ETransResult.ONLINE_CARD_DENIED);
        }

        ETransResult transResult = Tools.getEnum(ETransResult.class, ret - 1);
        if (transResult == null) {
            throw new EmvException(EEmvExceptions.EMV_ERR_UNKNOWN.getErrCodeFromBasement());
        }
        return new CTransResult(transResult);
    }

    @Override
    public String getVersion() {
        ByteArray byteArray = new ByteArray();
        EMVCallback.EMVReadVerInfo(byteArray);
        return Arrays.toString(byteArray.data);
    }

    private int addCapkIntoEmvLib() {
        int ret;
        ByteArray dataList = new ByteArray();
        ret = EMVCallback.EMVGetTLVData((short) 0x4F, dataList);
        if (ret != RetCode.EMV_OK) {
            ret = EMVCallback.EMVGetTLVData((short) 0x84, dataList);
        }
        if (ret != RetCode.EMV_OK) {
            return ret;
        }

        byte[] rid = new byte[5];
        System.arraycopy(dataList.data, 0, rid, 0, 5);
        ret = EMVCallback.EMVGetTLVData((short) 0x8F, dataList);
        if (ret != RetCode.EMV_OK) {
            return ret;
        }
        byte keyId = dataList.data[0];
        for (Capk capk : capkList) {
            if (Tools.bytes2String(capk.getRid()).equals(new String(rid)) && capk.getKeyID() == keyId) {
                EMV_CAPK emvCapk = Converter.toEMVCapk(capk);
                ret = EMVCallback.EMVAddCAPK(emvCapk);
            }
        }
        return ret;
    }

    private ETransResult onlineProc() {
        EOnlineResult ret;
        try {
            ret = paxEmvTrans.onlineProc();
        } catch (EmvException e) {
            Log.e("EmvImpl", "", e);
            return ETransResult.ABORT_TERMINATED;
        }
        if (ret == EOnlineResult.APPROVE) {
            return ETransResult.ONLINE_APPROVED;
        } else if (ret == EOnlineResult.ABORT || ret == EOnlineResult.REVERSAL_FAILED || ret == EOnlineResult.TLE_FAILED) {
            isReversalFail = (ret == EOnlineResult.REVERSAL_FAILED);
            isTleFail = (ret == EOnlineResult.TLE_FAILED);
            return ETransResult.ABORT_TERMINATED;
        } else if (ret == EOnlineResult.FAILED || ret == EOnlineResult.AMEX_ERR_GOTO_SECOND_GEN || ret == EOnlineResult.ONLINE_FAILED_NEED_CHK_REVERSAL) {
            isAmexErrorGotoSecondGen = (ret == EOnlineResult.AMEX_ERR_GOTO_SECOND_GEN);
            isOfflineApprNeedChkReverse = (ret == EOnlineResult.ONLINE_FAILED_NEED_CHK_REVERSAL);
            return ETransResult.ONLINE_FAILED;
        } else {
            return ETransResult.ONLINE_DENIED;
        }
    }

    private ETransResult updateScriptResult() {
        EOnlineResult ret;
        try {
            ret = paxEmvTrans.updateScriptResult();
        } catch (EmvException e) {
            Log.e("EmvImpl", "", e);
            return ETransResult.ABORT_TERMINATED;
        }
        if (ret == EOnlineResult.APPROVE) {
            return ETransResult.ONLINE_APPROVED;
        } else if (ret == EOnlineResult.ABORT) {
            return ETransResult.ABORT_TERMINATED;
        } else if (ret == EOnlineResult.FAILED) {
            return ETransResult.ONLINE_FAILED;
        } else {
            return ETransResult.ONLINE_DENIED;
        }
    }

    private static ByteBuffer set7172Bytebuffer(byte[] f71f72,int tag) {
        ByteBuffer byteBuffer;

        if (f71f72[0] == (byte) 0x86) {
            byteBuffer = ByteBuffer.allocate(f71f72.length + 2);
            byteBuffer.put((byte) tag);
            byteBuffer.put((byte) f71f72.length);
            byteBuffer.put(f71f72);
        } else {
            byteBuffer = ByteBuffer.allocate(f71f72.length - 5);
            // remove first 7 from f71f72
            //add 71
            //add 71 removed length
            int n = f71f72.length - 7;
            byte[] newf71f72 = new byte[n];
            System.arraycopy(f71f72, 7, newf71f72, 0, n);

            byteBuffer.put((byte) tag);
            byteBuffer.put((byte) newf71f72.length);
            byteBuffer.put(newf71f72);
        }
        return byteBuffer;
    }

    private boolean isUnSupportedTrans() throws EmvException {
        // 0 SUCC (The transaction is Offline or PreAuth Complete/Cancel)
        // -40 ERR_UNSUPPORTED_FUNC (Not offline or PreAuth Complete/Cancel trans.)
        // -26 ERR_NOT_SUPPORT_TRANS (Issuer does not support offline trans. or PreAuth Complete/Cancel trans.)
        int retOfflineTrans = paxEmvTrans.chkIsOfflineTransSend();
        int retNotAllowRefundFullEmv = paxEmvTrans.chkIsNotAllowRefundFullEmv();

        if (retOfflineTrans == -26 || retNotAllowRefundFullEmv == -26) {
            return true;
        }

        isOfflineTransSend = (retOfflineTrans == RetCode.EMV_OK);
        isNotAllowFullEmv = (retOfflineTrans == RetCode.EMV_OK || retNotAllowRefundFullEmv == RetCode.EMV_OK);

        return false;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.EmvImpl
 * JD-Core Version:    0.6.0
 */