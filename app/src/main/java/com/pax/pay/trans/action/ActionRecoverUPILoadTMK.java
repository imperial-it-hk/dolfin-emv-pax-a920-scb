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

public class ActionRecoverUPILoadTMK extends AAction {
    private int BBL_TMK_ENCRYPT_KEY_LEN	= 34;
    private int BBL_TMK_ENCRYPT_CVV_LEN = 2;
    private int BBL_TMK_LEN = 16;
    private Context context;
    private byte[] field62;
    private EPedType type;
    private byte[] PKCS;

    public ActionRecoverUPILoadTMK(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, byte[] field62, byte[] PKCS) {
        this.context = context;
        this.field62 = field62;
        this.PKCS = PKCS;
    }

    @Override
    protected void process() {

        type = EPedType.INTERNAL;
        boolean flag;

      //  field62 = new byte[]{(byte)0x33, 0x33, 0x45, 0x36, 0x46, 0x44, 0x38, 0x33, 0x30, 0x33, 0x36, 0x31, 0x44, 0x44, 0x33, 0x34, 0x38, 0x31, 0x37, 0x32, 0x30, 0x45, 0x31, 0x30, 0x30, 0x34, 0x36, 0x42, 0x31, 0x44, 0x32 , 0x44, 0x38, 0x42, 0x35, 0x43 };

        byte[] TMK_ENCRYPT = new byte[BBL_TMK_ENCRYPT_KEY_LEN + BBL_TMK_ENCRYPT_CVV_LEN];
        System.arraycopy(field62,0, TMK_ENCRYPT, 0, BBL_TMK_ENCRYPT_KEY_LEN + BBL_TMK_ENCRYPT_CVV_LEN );

        String valueStr = Tools.hexToAscii(Tools.bcd2Str(TMK_ENCRYPT));
        byte[] TMK_ENC = Tools.str2Bcd(valueStr);

        byte[] DESKey = Arrays.copyOfRange(PKCS,PKCS.length-BBL_TMK_LEN-BBL_TMK_ENCRYPT_CVV_LEN, PKCS.length-BBL_TMK_ENCRYPT_CVV_LEN);

        PedManager ped = FinancialApplication.getPedInstance();
        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 97, DESKey, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
            return;
        }

        int cnt = 0;

        byte[] TMK = new byte[BBL_TMK_LEN];

        while (cnt < BBL_TMK_LEN)
        {
            byte[] ENCDATA = Arrays.copyOfRange(TMK_ENC, cnt, cnt+8);
            byte[] DESResult = ped.calcDes(PedManager.TRI_DECRYPT, (byte) 97, ENCDATA);
            System.arraycopy(DESResult,0, TMK, cnt, 8);
            cnt = cnt+8;
        }

        if (TMK==null)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
            return;
        }

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte) 96, TMK, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
            return;
        }

        byte[] CCV  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte) 96, PedManager.EFTSec_INITIAL_VALUE_TO_GEN_KCV );
        byte[] CCV4 = Arrays.copyOfRange(CCV,0,4);

        if (TMK_ENC[BBL_TMK_LEN] != CCV4[0] || TMK_ENC[BBL_TMK_LEN+1] != CCV4[1])
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
            return;
        }

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        //int acqID = acq.getId();
        int keyId = acq.getKeyId() > 0 ? acq.getKeyId() : Component.generateKeyId();

        flag = ped.writeKey(EPedKeyType.TMK, (byte) 0, EPedKeyType.TDK, (byte)(UserParam.TMK_UPI_IDX_BASE + keyId), TMK, ECheckMode.KCV_NONE, null );
        if (!flag)
        {
            setResult(new ActionResult(TransResult.ERR_UPI_LOAD, null));
            return;
        }

        acq.setKeyId(keyId);
        acq.setUP_TMK("Y");
        FinancialApplication.getAcqManager().updateAcquirer(acq);

        setResult(new ActionResult(TransResult.SUCC, TMK));
    }
}