package com.pax.pay.trans.action;

import android.content.Context;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.EPedType;
import com.pax.device.DeviceImplNeptune;
import com.pax.edc.opensdk.TransResult;
import java.util.Arrays;
import android.util.Base64;

public class ActionRecoverUPILoadRSA extends AAction {
    private Context context;
    private byte[] field62;
    private EPedType type;
    private byte[] PKCS;

    public ActionRecoverUPILoadRSA(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, byte[] field62,byte[] PKCS) {
        this.context = context;
        this.field62 = field62;
        this.PKCS = PKCS;
    }

    @Override
    protected void process() {

        type = EPedType.INTERNAL;
        int flag;

        // Receive Public.Key in the from of Base64
        // 1. Decode from Base64 to Bin
        byte[] decByte62 = Base64.decode(field62, Base64.DEFAULT);

        // 2.Get Public Key (Modulas,Exp) from BIN (ASN.1 structure)
        byte[] pModulus = Arrays.copyOfRange(decByte62,9,265);
        byte[] pExponent = Arrays.copyOfRange(decByte62,267,270);

        DeviceImplNeptune dev = DeviceImplNeptune.getInstance();

        byte[] exp = new byte[]{(byte) 0x00, 0x01, 0x00, 0x01};

        byte[] EncryptResult = new byte[256];

        flag = dev.rsaRecover(pModulus,256,exp,4, PKCS, EncryptResult );

        byte[] result = Base64.encode(EncryptResult, Base64.NO_WRAP);

        if (flag==0) {
            setResult(new ActionResult(TransResult.SUCC, result));
        } else {
            setResult(new ActionResult(TransResult.ERR_SEND, null));
        }
    }
}