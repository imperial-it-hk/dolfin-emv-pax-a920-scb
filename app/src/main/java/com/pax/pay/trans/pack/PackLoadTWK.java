package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.TransData;

public class PackLoadTWK extends PackIso8583 {

    public PackLoadTWK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            // nii 006 to load TLE Key
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
            if (acq.isEnableTle() && acq.getTMK()!=null)
            {
                transData.setTpdu("600" + "006" + "0000");
            }
            else
            {
                return "".getBytes();
            }

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            // field 62
            setBitData62(transData);

            if (IsTransTLE(transData))
                return packWithTLE(transData);
            else
                return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData62(@NonNull TransData transData) throws Iso8583Exception {
        String tle_h = "HTLE";
        String tle_ver = "04";
        String reqType = "1";
        String nii = UserParam.KMSIF01.substring(0,3);
        String tid = FinancialApplication.getAcqManager().getCurAcq().getTerminalId();
        String vendor = UserParam.KMSIF01.substring(UserParam.KMSIF01.length()-8,UserParam.KMSIF01.length());

        String tmk_id = FinancialApplication.getAcqManager().getCurAcq().getTMK();
        String twk_id = "0000";

        if (tmk_id==null) {
            throw new Iso8583Exception(3);
        }

        String fieldStr = tle_h + tle_ver + reqType + nii + nii + tid + vendor + tmk_id + twk_id;
        byte[] fieldData = fieldStr.getBytes();

        setBitData("62", fieldData);
    }
}

