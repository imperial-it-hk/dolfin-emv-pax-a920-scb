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
import com.pax.pay.utils.Utils;
import com.pax.pay.base.Acquirer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.util.Base64;

public class ActionRecoverLoadUPI extends AAction {
    private Context context;
    private String field62;
    private EPedType type;

    public ActionRecoverLoadUPI(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String field62) {
        this.context = context;
        this.field62 = field62;
    }

    @Override
    protected void process() {

        type = EPedType.INTERNAL;
        boolean flag = true;

        byte[] bytes62 = Tools.str2Bcd(field62);

        // Receive Public.Key in the from of Base64
        // 1. Decode from Base64 to Bin
        byte[] decByte62 = Base64.decode(bytes62, Base64.DEFAULT);

        // 2.Get Public Key (Modulas,Exp) from BIN (ASN.1 structure)
        //  0:d=0  hl=4 l= 266 cons: SEQUENCE
        //  4:d=1  hl=4 l= 257 prim:  INTEGER           :Modulus 2048 bit
        //265:d=1  hl=2 l=   3 prim:  INTEGER           :Exponent 65537 (0x10001)

        byte[] pModulus = new byte[256];

        System.arraycopy(decByte62,5, pModulus, 0 , 256);

        byte[] pExponent = new byte[3];
        System.arraycopy(decByte62, 265, pExponent,0, 3);


        if (flag) {
            setResult(new ActionResult(TransResult.SUCC, null));
        } else {
            setResult(new ActionResult(TransResult.ERR_SEND, null));
        }
    }
}