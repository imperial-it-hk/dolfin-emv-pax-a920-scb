package com.pax.pay.trans.action;

import android.content.Context;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EPedType;
import com.pax.dal.entity.RSARecoverInfo;
import com.pax.device.UserParam;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.ped.PedManager;
import com.pax.dal.entity.RSAKeyInfo;
import com.pax.pay.trans.LoadTMKTrans;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.Utils;
import com.pax.pay.base.Acquirer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionRecoverKBankLoadTMK extends AAction {
    private Context context;
    private String field62;
    private Acquirer acq;
    private EPedType type;

    public ActionRecoverKBankLoadTMK(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String field62) {
        this.context = context;
        this.field62 = field62;
    }

    @Override
    protected void process() {
        type = EPedType.INTERNAL;
        boolean flag;

        acq = FinancialApplication.getAcqManager().getCurAcq();

        byte[] bytes62 = Tools.str2Bcd(field62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62,0,4);
        byte[] Version = Arrays.copyOfRange(bytes62,4,6);
        byte[] DownloadType = Arrays.copyOfRange(bytes62,6,7);
        byte[] RespType = Arrays.copyOfRange(bytes62,7,8);
        byte[] AcqId = Arrays.copyOfRange(bytes62,8,11);
        byte[] RSARespDATA = Arrays.copyOfRange(bytes62,11,293);

        // RSA Data
        byte[] RSAKEY = Arrays.copyOfRange(RSARespDATA,0,256);
        byte[] RSAKCV = Arrays.copyOfRange(RSARespDATA,256,262);
        byte[] RSAIDENT = Arrays.copyOfRange(RSARespDATA,262,282);

        byte[] TMK_ID = Arrays.copyOfRange(RSAIDENT,0,4);


        PedManager ped = FinancialApplication.getPedInstance();
        RSARecoverInfo TMKKey = ped.RSADecrypt(2,RSAKEY);

        byte[] keyinfo = TMKKey.getData();
        //GET LAST TMK 16 bytes
        byte[] ltmk = Arrays.copyOfRange(keyinfo, keyinfo.length - 16, keyinfo.length );

        //int acqID = acq.getId();
        int keyId = acq.getKeyId() > 0 ? acq.getKeyId() : Component.generateKeyId();

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TMK_INDEX + keyId), ltmk, ECheckMode.KCV_NONE, null );
        if (!flag) {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST,null));
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TMK_UPI_IDX_BASE + keyId), ltmk, ECheckMode.KCV_NONE, null );
        if (!flag) {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST,null));
        }

        String tmkStr = Tools.bytes2String(TMK_ID);
        acq.setTMK(tmkStr);
        acq.setUP_TMK("Y");
        acq.setKeyId(keyId);
        FinancialApplication.getAcqManager().updateAcquirer(acq);

        setResult(new ActionResult(TransResult.SUCC,TMK_ID));
    }
}
