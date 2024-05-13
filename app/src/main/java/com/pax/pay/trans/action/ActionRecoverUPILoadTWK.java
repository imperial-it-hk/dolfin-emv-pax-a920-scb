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
import com.pax.pay.utils.Utils;

import java.util.Arrays;

public class ActionRecoverUPILoadTWK extends AAction {
    private int BBL_TMK_ENCRYPT_KEY_LEN	= 34;
    private int BBL_TMK_ENCRYPT_CVV_LEN = 2;
    private int BBL_TMK_LEN = 16;
    private Context context;
    private byte[] field62;
    private EPedType type;
    private byte[] PKCS;

    public ActionRecoverUPILoadTWK(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, byte[] field62) {
        this.context = context;
        this.field62 = field62;
    }

    @Override
    protected void process() {

        type = EPedType.INTERNAL;
        boolean flag;

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        //int acqID = acq.getId();
        int keyId = acq.getKeyId() > 0 ? acq.getKeyId() : Component.generateKeyId();

        PedManager ped = FinancialApplication.getPedInstance();
        byte[] TWK_1 =ped.calcDes(PedManager.TRI_DECRYPT, (byte)(UserParam.TMK_UPI_IDX_BASE + keyId), Arrays.copyOfRange(field62,0,8));
        byte[] TWK_2 =ped.calcDes(PedManager.TRI_DECRYPT, (byte)(UserParam.TMK_UPI_IDX_BASE + keyId), Arrays.copyOfRange(field62,8,16));

        if (TWK_1==null || TWK_2==null)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOGON, null));
            return;
        }

        byte[] TWK = Utils.concat(Arrays.copyOfRange(TWK_1,0,8),Arrays.copyOfRange(TWK_2,0,8));

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 96, TWK, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOGON, null));
            return;
        }

        byte[] CCV  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte) 96, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] CCV4 = Arrays.copyOfRange(CCV,0,4);

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TWK_UPI_IDX_BASE + keyId), TWK, ECheckMode.KCV_ENCRYPT_0, CCV4 );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOGON, null));
            return;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TPK, (byte)(UserParam.TPK_UPI_IDX_BASE + keyId), TWK, ECheckMode.KCV_ENCRYPT_0, CCV4 );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOGON, null));
            return;
        }

        acq.setKeyId(keyId);
        FinancialApplication.getAcqManager().updateAcquirer(acq);

        setResult(new ActionResult(TransResult.SUCC, TWK));
    }
}