package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.TransData;

public class PackUPILoadTWK extends PackIso8583 {

    public PackUPILoadTWK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
            if (acq.isEnableUpi() && acq.getUP_TMK()!=null) {
                transData.setTpdu("600" + transData.getAcquirer().getNii() + "0000");
            }
            else
            {
                return "".getBytes();
            }

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }
}

