package com.pax.pay.trans.action;

import android.content.Context;
import android.widget.TextView;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.dal.entity.EPedType;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.ped.PedManager;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

public class ActionGenRSA extends AAction {
    private Context context;
    private String title;

    private TextView resultView;
    private EPedType type;

    public ActionGenRSA(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(Context context, String title) {
        this.context = context;
        this.title = title;
    }

    @Override
    protected void process() {

        boolean flag1, flag2;

        PedManager ped = FinancialApplication.getPedInstance();

        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();

        KeyPair kp = ped.genKeyPair(2048);
        ped.setKeyPair(kp);
        flag1 = ped.writeRSAKeyPublic(1);
        flag2 = ped.writeRSAKeyPrivate(2);

        if (kp != null || !flag1 || !flag2)
        {
            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();
            byte[] modulus = pubKey.getModulus().toByteArray();
            byte[] modByte = Arrays.copyOfRange(modulus,modulus.length - 256, modulus.length);
            String modStr = Tools.bcd2Str(modByte);
            FinancialApplication.getUserParam().setRSAKey(modStr);
            setResult(new ActionResult(TransResult.SUCC, null));
        } else {
            setResult(new ActionResult(TransResult.ERR_SEND, null));
        }
    }
}