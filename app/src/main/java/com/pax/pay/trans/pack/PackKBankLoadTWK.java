package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.TransData;

public class PackKBankLoadTWK extends PackIso8583 {

    public PackKBankLoadTWK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            // nii 126 to load TLE Key
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
            if (acq.isEnableTle())
            {
                transData.setTpdu("600" + UserParam.KMSNI01 + "8000");
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
        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
        String tle_h = "HTLE";
        String tle_ver = "04";
        String reqType = "1";
        String nii = UserParam.KMSAQ01;
        String tid = acq.getTerminalId();
        String vendor = UserParam.KMSVR01;

        String tmk_id = acq.getTMK();
        String twk_id = "0000";

        String tpk_tag = "";
        String tpk_val = "";

        if (acq.isEnableUpi() || acq.getName().equals(Constants.ACQ_KBANK))
        {
            tpk_tag = "PK";
            tpk_val = "1";
        }

        if (tmk_id==null) {
            throw new Iso8583Exception(3);
        }

        String fieldStr = tle_h + tle_ver + reqType + nii + nii + tid + vendor + tmk_id + twk_id;

        if (acq.isEnableUpi() || acq.getName().equals(Constants.ACQ_KBANK))
        {
            fieldStr = fieldStr + tpk_tag + tpk_val;
        }

        byte[] fieldData = fieldStr.getBytes();

        setBitData("62", fieldData);
    }
}

