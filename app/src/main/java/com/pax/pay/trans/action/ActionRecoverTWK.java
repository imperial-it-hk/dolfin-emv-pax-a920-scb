package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EPedType;
import com.pax.device.UserParam;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.ped.PedManager;
import com.pax.pay.trans.component.Component;
import java.util.Arrays;

public class ActionRecoverTWK extends AAction {
    private Context context;
    private String field62;
    private EPedType type;

    public ActionRecoverTWK(ActionStartListener listener) {
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
        int icnt;

        byte[] bytes62 = Tools.str2Bcd(field62);

        // Field 62 data
        byte[] TLEIndicator = Arrays.copyOfRange(bytes62,0,4);
        byte[] Version = Arrays.copyOfRange(bytes62,4,6);
        byte[] RespType = Arrays.copyOfRange(bytes62,6,7);
        byte[] TWK_ID = Arrays.copyOfRange(bytes62,7,11);

        byte[] TWK_DEK = Arrays.copyOfRange(bytes62,11,27);
        byte[] TWK_MAK = Arrays.copyOfRange(bytes62,27,43);
        byte[] TWK_DEK_KCV = Arrays.copyOfRange(bytes62,43,51);
        byte[] TWK_MAK_KCV = Arrays.copyOfRange(bytes62,51,59);
        byte[] REW_ACQ = Arrays.copyOfRange(bytes62,59,62);

        PedManager ped = FinancialApplication.getPedInstance();
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        //int acqID = acq.getId();
        int keyId = acq.getKeyId() > 0 ? acq.getKeyId() : Component.generateKeyId();

        byte[] twk_dek  = ped.calcDes(PedManager.TRI_DECRYPT, (byte) (byte)(UserParam.TMK_INDEX + keyId), TWK_DEK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 98, twk_dek, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        byte[] tmp = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 98, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] dek_kcv = Tools.bcd2Str(tmp,PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(dek_kcv,TWK_DEK_KCV))
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        byte[] twk_mak  = ped.calcDes(PedManager.TRI_DECRYPT, (byte)(UserParam.TMK_INDEX + keyId), TWK_MAK);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 99, twk_mak, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        byte[] tmp2 = ped.calcDes(PedManager.ONE_ENCRYPT, (byte) 99, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] mak_kcv = Tools.bcd2Str(tmp2,PedManager.KCV_SIZE).getBytes();

        if (!Arrays.equals(mak_kcv,TWK_MAK_KCV))
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TWK_MAK_INDEX + keyId), twk_mak, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TWK_DEK_INDEX + keyId), twk_dek, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_TLE_REQUEST, null));
            return;
        }

        String twkStr  = Tools.bytes2String(TWK_ID);
        acq.setKeyId(keyId);
        acq.setTWK(twkStr);
        FinancialApplication.getAcqManager().updateAcquirer(acq);

        setResult(new ActionResult(TransResult.SUCC, TWK_DEK));
    }
}